#!/usr/bin/env bash
set -euo pipefail

bucket="${TFSTATE_BUCKET:-x0ba-tritonwatch-tfstate}"
region="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-west-2}}"

if ! command -v aws >/dev/null 2>&1; then
  echo "Required command is not installed: aws" >&2
  exit 1
fi

account_id="$(aws sts get-caller-identity --query Account --output text)"
echo "Creating Terraform state bucket s3://${bucket} in ${region} (account ${account_id})"

if aws s3api head-bucket --bucket "$bucket" 2>/dev/null; then
  echo "Bucket already exists."
else
  aws s3api create-bucket \
    --bucket "$bucket" \
    --region "$region" \
    --create-bucket-configuration "LocationConstraint=${region}"
fi

aws s3api put-public-access-block \
  --bucket "$bucket" \
  --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

aws s3api put-bucket-versioning \
  --bucket "$bucket" \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket "$bucket" \
  --server-side-encryption-configuration '{
    "Rules": [
      {
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        },
        "BucketKeyEnabled": true
      }
    ]
  }'

aws s3api put-bucket-tagging \
  --bucket "$bucket" \
  --tagging "TagSet=[{Key=Application,Value=tritonwatch},{Key=Purpose,Value=terraform-state}]"

echo
echo "State bucket is ready. Next:"
echo "  terraform -chdir=infra/aws-ecs init -migrate-state"
echo "  terraform -chdir=infra/aws-ecs apply"
