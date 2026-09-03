#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
terraform_directory="$repository_root/infra/aws-ecs"
image_tag="${1:-$(git -C "$repository_root" rev-parse --short HEAD)}"
if (($# > 0)); then
  shift
fi

project_name="${PROJECT_NAME:-tritonwatch}"
target_platform="${TARGET_PLATFORM:-linux/amd64}"
build_parallel="${BUILD_PARALLEL:-1}"

if [[ -n "${DOCKER_CACHE:-}" ]]; then
  docker_cache="$DOCKER_CACHE"
elif [[ "${CI:-}" == "true" ]]; then
  docker_cache="gha"
else
  docker_cache="none"
fi

all_images=(
  user-service
  watchlist-service
  ingestion-service
  notification-service
  postgres
  kafka
  caddy
)

if [[ ! "$image_tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Invalid Docker image tag: $image_tag" >&2
  exit 1
fi

for command_name in aws docker git terraform; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    exit 1
  fi
done

is_known_image() {
  local candidate="$1"
  local known_image
  for known_image in "${all_images[@]}"; do
    if [[ "$known_image" == "$candidate" ]]; then
      return 0
    fi
  done
  return 1
}

should_force_rebuild() {
  case "${FORCE_REBUILD:-0}" in
    1|true|yes) return 0 ;;
    *) return 1 ;;
  esac
}

image_source_paths() {
  local image_name="$1"
  printf '%s\n' ".dockerignore"
  case "$image_name" in
    user-service|watchlist-service|ingestion-service|notification-service)
      printf '%s\n' \
        "infra/production/Dockerfile" \
        "shared/event-contracts" \
        "services/${image_name}"
      ;;
    postgres)
      printf '%s\n' \
        "infra/production/Dockerfile.postgres" \
        "infra/production/init-databases.sh"
      ;;
    kafka)
      printf '%s\n' \
        "infra/production/Dockerfile.kafka" \
        "infra/kafka/create-topics.sh"
      ;;
    caddy)
      printf '%s\n' \
        "infra/production/Dockerfile.caddy" \
        "infra/production/Caddyfile"
      ;;
    *)
      echo "Unknown image: $image_name" >&2
      return 1
      ;;
  esac
}

content_hash_for_image() {
  local image_name="$1"
  local -a source_paths=()
  local source_path
  while IFS= read -r source_path; do
    source_paths+=("$source_path")
  done < <(image_source_paths "$image_name")

  local digest
  digest="$(
    cd "$repository_root"
    find "${source_paths[@]}" \
      -type f \
      ! -path '*/build/*' \
      ! -path '*/.gradle/*' \
      ! -name '.DS_Store' \
      -print \
    | sort \
    | git hash-object --stdin-paths \
    | git hash-object --stdin
  )"
  printf '%s\n' "${digest:0:16}"
}

ecr_image_tag_exists() {
  local image_name="$1"
  local tag="$2"
  aws ecr describe-images \
    --region "$aws_region" \
    --repository-name "${project_name}/${image_name}" \
    --image-ids "imageTag=${tag}" \
    >/dev/null 2>&1
}

retag_existing_image() {
  local image_name="$1"
  local source_tag="$2"
  local destination_tag="$3"
  local source_uri="${registry}/${project_name}/${image_name}:${source_tag}"
  local destination_uri="${registry}/${project_name}/${image_name}:${destination_tag}"

  echo "Reusing $source_uri as $destination_uri"
  docker buildx imagetools create \
    --tag "$destination_uri" \
    "$source_uri"
}

set_cache_arguments() {
  local image_name="$1"
  cache_arguments=()
  case "$docker_cache" in
    gha)
      cache_arguments=(
        "--cache-from=type=gha,scope=${project_name}-${image_name}"
        "--cache-to=type=gha,mode=max,scope=${project_name}-${image_name}"
      )
      ;;
    registry)
      local cache_ref="${registry}/${project_name}/${image_name}:buildcache"
      cache_arguments=(
        "--cache-from=type=registry,ref=${cache_ref}"
        "--cache-to=type=registry,ref=${cache_ref},mode=max"
      )
      ;;
  esac
}

build_application_image() {
  local service_name="$1"
  local content_tag="$2"
  local image_uri="${registry}/${project_name}/${service_name}:${image_tag}"
  local content_uri="${registry}/${project_name}/${service_name}:${content_tag}"
  local -a build_args=(
    --platform "$target_platform"
    --push
    --build-arg "SERVICE_NAME=${service_name}"
    --file "$repository_root/infra/production/Dockerfile"
    --tag "$image_uri"
    --tag "$content_uri"
  )
  set_cache_arguments "$service_name"
  build_args+=("${cache_arguments[@]}")

  echo "Building and pushing $image_uri"
  docker buildx build "${build_args[@]}" "$repository_root"
}

build_infra_image() {
  local image_name="$1"
  local content_tag="$2"
  local dockerfile
  case "$image_name" in
    postgres) dockerfile="Dockerfile.postgres" ;;
    kafka) dockerfile="Dockerfile.kafka" ;;
    caddy) dockerfile="Dockerfile.caddy" ;;
    *)
      echo "Unknown infra image: $image_name" >&2
      return 1
      ;;
  esac

  local image_uri="${registry}/${project_name}/${image_name}:${image_tag}"
  local content_uri="${registry}/${project_name}/${image_name}:${content_tag}"
  local -a build_args=(
    --platform "$target_platform"
    --push
    --file "$repository_root/infra/production/$dockerfile"
    --tag "$image_uri"
    --tag "$content_uri"
  )
  set_cache_arguments "$image_name"
  build_args+=("${cache_arguments[@]}")

  echo "Building and pushing $image_uri"
  docker buildx build "${build_args[@]}" "$repository_root"
}

ensure_image() {
  local image_name="$1"
  local content_hash
  local content_tag
  content_hash="$(content_hash_for_image "$image_name")"
  content_tag="content-${content_hash}"

  if ! should_force_rebuild && ecr_image_tag_exists "$image_name" "$content_tag"; then
    echo "${image_name}: content ${content_hash} already in ECR"
    retag_existing_image "$image_name" "$content_tag" "$image_tag"
    return
  fi

  echo "${image_name}: building content ${content_hash}"
  case "$image_name" in
    user-service|watchlist-service|ingestion-service|notification-service)
      build_application_image "$image_name" "$content_tag"
      ;;
    *)
      build_infra_image "$image_name" "$content_tag"
      ;;
  esac
}

publish_images() {
  local image_name
  if [[ "$build_parallel" == "0" ]]; then
    for image_name in "$@"; do
      ensure_image "$image_name"
    done
    return
  fi

  local pid
  local failed=0
  local -a pids=()
  for image_name in "$@"; do
    ensure_image "$image_name" &
    pids+=("$!")
  done
  for pid in "${pids[@]}"; do
    if ! wait "$pid"; then
      failed=1
    fi
  done
  if ((failed)); then
    echo "One or more images failed to publish." >&2
    exit 1
  fi
}

selected_images=("${all_images[@]}")
if (($# > 0)); then
  selected_images=("$@")
  for image_name in "${selected_images[@]}"; do
    if ! is_known_image "$image_name"; then
      echo "Unknown image: $image_name" >&2
      echo "Expected one of: ${all_images[*]}" >&2
      exit 1
    fi
  done
fi

if [[ -d "$terraform_directory" ]]; then
  aws_region="$(terraform -chdir="$terraform_directory" output -raw aws_region 2>/dev/null || true)"
fi
aws_region="${aws_region:-${AWS_REGION:-${AWS_DEFAULT_REGION:-us-west-1}}}"

account_id="$(aws sts get-caller-identity --query Account --output text)"
registry="${account_id}.dkr.ecr.${aws_region}.amazonaws.com"

echo "Logging in to $registry"
aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry"

for image_name in "${selected_images[@]}"; do
  if ! aws ecr describe-repositories \
    --region "$aws_region" \
    --repository-names "${project_name}/${image_name}" \
    >/dev/null 2>&1; then
    echo "ECR repository does not exist: ${project_name}/${image_name}" >&2
    echo "Run the initial Terraform apply before building images." >&2
    exit 1
  fi
done

echo "Publishing ${#selected_images[@]} image(s) as ${image_tag} (cache=${docker_cache}, parallel=${build_parallel})"
publish_images "${selected_images[@]}"

echo
echo "ECS images are available with tag: $image_tag"
echo "Deploy with: ./scripts/deploy-ecs.sh $image_tag"
