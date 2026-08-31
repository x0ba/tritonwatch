#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
terraform_directory="$repository_root/infra/aws-ecs"
image_tag="${1:-$(git -C "$repository_root" rev-parse --short HEAD)}"
deployment_variables="$terraform_directory/deployment.auto.tfvars.json"

if [[ ! "$image_tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Invalid Docker image tag: $image_tag" >&2
  exit 1
fi

for command_name in aws terraform; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    exit 1
  fi
done

if [[ ! -f "$terraform_directory/terraform.tfvars" ]]; then
  echo "Missing $terraform_directory/terraform.tfvars" >&2
  exit 1
fi

temporary_variables="$(mktemp "$terraform_directory/.deployment.auto.tfvars.XXXXXX")"
trap 'rm -f "$temporary_variables"' EXIT
printf '{\n  "deploy_application": true,\n  "image_tag": "%s"\n}\n' "$image_tag" > "$temporary_variables"
mv "$temporary_variables" "$deployment_variables"
trap - EXIT

terraform -chdir="$terraform_directory" init
terraform -chdir="$terraform_directory" apply

cluster_name="$(terraform -chdir="$terraform_directory" output -raw ecs_cluster_name)"
service_name="$(terraform -chdir="$terraform_directory" output -raw ecs_service_name)"
aws_region="$(terraform -chdir="$terraform_directory" output -raw aws_region)"

echo "Waiting for ECS service ${service_name} in ${cluster_name}..."

if ! aws ecs wait services-stable \
  --region "$aws_region" \
  --cluster "$cluster_name" \
  --services "$service_name"; then
  echo "ECS did not reach a stable state. Recent service events:" >&2
  aws ecs describe-services \
    --region "$aws_region" \
    --cluster "$cluster_name" \
    --services "$service_name" \
    --query 'services[0].events[0:10].[createdAt,message]' \
    --output table >&2
  exit 1
fi

api_url="$(terraform -chdir="$terraform_directory" output -raw api_url)"

echo
echo "Deployment is stable: $api_url"
