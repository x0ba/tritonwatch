#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
image_tag="${1:-$(git -C "$repository_root" rev-parse --short HEAD)}"
aws_region="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-west-2}}"
project_name="${PROJECT_NAME:-tritonwatch}"
target_platform="${TARGET_PLATFORM:-linux/amd64}"

if [[ ! "$image_tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Invalid Docker image tag: $image_tag" >&2
  exit 1
fi

for command_name in aws docker; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    exit 1
  fi
done

account_id="$(aws sts get-caller-identity --query Account --output text)"
registry="${account_id}.dkr.ecr.${aws_region}.amazonaws.com"

echo "Logging in to $registry"
aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry"

required_repositories=(
  user-service
  watchlist-service
  ingestion-service
  notification-service
  postgres
  kafka
  caddy
)

for repository_name in "${required_repositories[@]}"; do
  if ! aws ecr describe-repositories \
    --region "$aws_region" \
    --repository-names "${project_name}/${repository_name}" \
    >/dev/null 2>&1; then
    echo "ECR repository does not exist: ${project_name}/${repository_name}" >&2
    echo "Run the initial Terraform apply before building images." >&2
    exit 1
  fi
done

application_services=(
  user-service
  watchlist-service
  ingestion-service
  notification-service
)

for service_name in "${application_services[@]}"; do
  image_uri="${registry}/${project_name}/${service_name}:${image_tag}"
  echo "Building and pushing $image_uri"

  docker buildx build \
    --platform "$target_platform" \
    --push \
    --build-arg "SERVICE_NAME=${service_name}" \
    --file "$repository_root/infra/production/Dockerfile" \
    --tag "$image_uri" \
    "$repository_root"
done

declare -A infrastructure_dockerfiles=(
  [postgres]="Dockerfile.postgres"
  [kafka]="Dockerfile.kafka"
  [caddy]="Dockerfile.caddy"
)

for image_name in postgres kafka caddy; do
  image_uri="${registry}/${project_name}/${image_name}:${image_tag}"
  dockerfile="${infrastructure_dockerfiles[$image_name]}"

  echo "Building and pushing $image_uri"
  docker buildx build \
    --platform "$target_platform" \
    --push \
    --file "$repository_root/infra/production/$dockerfile" \
    --tag "$image_uri" \
    "$repository_root"
done

echo
echo "All ECS images were pushed with tag: $image_tag"
echo "Deploy with: ./scripts/deploy-ecs.sh $image_tag"
