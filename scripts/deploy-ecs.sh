#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
terraform_directory="$repository_root/infra/aws-ecs"
image_tag="${1:-$(git -C "$repository_root" rev-parse --short HEAD)}"
deployment_variables="$terraform_directory/deployment.auto.tfvars.json"
container_instance_timeout_seconds="${ECS_CONTAINER_INSTANCE_TIMEOUT_SECONDS:-300}"
deployment_timeout_seconds="${ECS_DEPLOYMENT_TIMEOUT_SECONDS:-600}"
poll_interval_seconds="${ECS_DEPLOYMENT_POLL_INTERVAL_SECONDS:-15}"

for timeout_value in \
  "$container_instance_timeout_seconds" \
  "$deployment_timeout_seconds" \
  "$poll_interval_seconds"; do
  if [[ ! "$timeout_value" =~ ^[1-9][0-9]*$ ]]; then
    echo "Deployment timeout and poll interval values must be positive integers." >&2
    exit 1
  fi
done

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

terraform -chdir="$terraform_directory" init -input=false

if [[ "${CI:-}" == "true" || "${TF_IN_AUTOMATION:-}" == "true" ]]; then
  terraform -chdir="$terraform_directory" apply -input=false -auto-approve
else
  terraform -chdir="$terraform_directory" apply
fi

cluster_name="$(terraform -chdir="$terraform_directory" output -raw ecs_cluster_name)"
service_name="$(terraform -chdir="$terraform_directory" output -raw ecs_service_name)"
aws_region="$(terraform -chdir="$terraform_directory" output -raw aws_region)"

print_service_events() {
  echo "Recent ECS service events:" >&2
  aws ecs describe-services \
    --region "$aws_region" \
    --cluster "$cluster_name" \
    --services "$service_name" \
    --query 'services[0].events[0:10].[createdAt,message]' \
    --output table >&2 || true
}

echo "Waiting for an active ECS container instance in ${cluster_name}..."
container_instance_deadline=$((SECONDS + container_instance_timeout_seconds))

while ((SECONDS < container_instance_deadline)); do
  registered_instance_count="$(aws ecs list-container-instances \
    --region "$aws_region" \
    --cluster "$cluster_name" \
    --status ACTIVE \
    --filter 'agentConnected == true' \
    --query 'length(containerInstanceArns)' \
    --output text)"

  if ((registered_instance_count > 0)); then
    echo "Found ${registered_instance_count} active ECS container instance(s)."
    break
  fi

  sleep "$poll_interval_seconds"
done

if ((registered_instance_count == 0)); then
  echo "No ECS container instance registered within ${container_instance_timeout_seconds} seconds." >&2
  echo "Check cloud-init and ecs.service on the EC2 host before retrying." >&2
  print_service_events
  exit 1
fi

echo "Waiting for ECS service ${service_name} in ${cluster_name}..."
deployment_deadline=$((SECONDS + deployment_timeout_seconds))

while ((SECONDS < deployment_deadline)); do
  read -r service_status running_count pending_count desired_count deployment_count rollout_state <<< "$(
    aws ecs describe-services \
      --region "$aws_region" \
      --cluster "$cluster_name" \
      --services "$service_name" \
      --query 'services[0].[status,runningCount,pendingCount,desiredCount,length(deployments),deployments[?status==`PRIMARY`].rolloutState | [0]]' \
      --output text
  )"

  if [[ "$service_status" != "ACTIVE" ]]; then
    echo "ECS service ${service_name} is not active (status: ${service_status})." >&2
    print_service_events
    exit 1
  fi

  echo "ECS deployment: ${running_count}/${desired_count} running, ${pending_count} pending, rollout ${rollout_state}."

  if [[ "$running_count" == "$desired_count" &&
        "$pending_count" == "0" &&
        "$deployment_count" == "1" &&
        "$rollout_state" == "COMPLETED" ]]; then
    break
  fi

  if [[ "$rollout_state" == "FAILED" ]]; then
    echo "ECS deployment failed." >&2
    print_service_events
    exit 1
  fi

  sleep "$poll_interval_seconds"
done

if ((SECONDS >= deployment_deadline)); then
  echo "ECS did not reach a stable state within ${deployment_timeout_seconds} seconds." >&2
  print_service_events
  exit 1
fi

api_url="$(terraform -chdir="$terraform_directory" output -raw api_url)"

echo
echo "Deployment is stable: $api_url"
