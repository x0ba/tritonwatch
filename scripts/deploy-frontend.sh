#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
terraform_directory="$repository_root/infra/aws-ecs"
frontend_directory="$repository_root/frontend"

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

if [[ ! -d "$frontend_directory" ]]; then
  echo "Missing frontend directory: $frontend_directory" >&2
  exit 1
fi

required_env=(
  VITE_CLERK_PUBLISHABLE_KEY
)

for env_name in "${required_env[@]}"; do
  if [[ -z "${!env_name:-}" ]]; then
    echo "Missing required environment variable: $env_name" >&2
    echo "Export the Clerk publishable key before deploying the frontend." >&2
    exit 1
  fi
done

terraform -chdir="$terraform_directory" init -input=false >/dev/null
aws_region="$(terraform -chdir="$terraform_directory" output -raw aws_region)"
app_url="$(terraform -chdir="$terraform_directory" output -raw app_url)"
frontend_url="$(terraform -chdir="$terraform_directory" output -raw frontend_url)"
bucket_name="$(terraform -chdir="$terraform_directory" output -raw frontend_bucket_name)"
distribution_id="$(terraform -chdir="$terraform_directory" output -raw frontend_cloudfront_distribution_id)"

export VITE_USER_API_BASE_URL="${VITE_USER_API_BASE_URL:-$app_url}"
export VITE_WATCHLIST_API_BASE_URL="${VITE_WATCHLIST_API_BASE_URL:-$app_url}"
export VITE_CATALOG_API_BASE_URL="${VITE_CATALOG_API_BASE_URL:-$app_url}"

echo "Building frontend for $app_url (same-origin API)"
cd "$frontend_directory"

if command -v vp >/dev/null 2>&1; then
  vp install
  vp run build
elif command -v pnpm >/dev/null 2>&1; then
  pnpm install
  pnpm run build
else
  echo "Install Vite+ (vp) or pnpm to build the frontend." >&2
  exit 1
fi

if [[ ! -f "$frontend_directory/dist/index.html" ]]; then
  echo "Build did not produce dist/index.html" >&2
  exit 1
fi

echo "Syncing assets to s3://$bucket_name"
aws s3 sync "$frontend_directory/dist/" "s3://$bucket_name/" \
  --region "$aws_region" \
  --delete \
  --cache-control "public,max-age=31536000,immutable" \
  --exclude "index.html"

echo "Uploading index.html with short cache"
aws s3 cp "$frontend_directory/dist/index.html" "s3://$bucket_name/index.html" \
  --region "$aws_region" \
  --cache-control "public,max-age=0,must-revalidate" \
  --content-type "text/html"

echo "Invalidating CloudFront distribution $distribution_id"
aws cloudfront create-invalidation \
  --distribution-id "$distribution_id" \
  --paths "/*" \
  --output text \
  --query 'Invalidation.Id'

echo
echo "Frontend deployed: $frontend_url"
echo "API paths are same-origin under $app_url/api/..."
echo "Confirm the Clerk production instance domain is configured for $frontend_url"
echo "Confirm clerk_authorized_parties includes $frontend_url"
echo "Confirm cors_allowed_origins includes $frontend_url"
