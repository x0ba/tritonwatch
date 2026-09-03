# Deploying Tritonwatch to Amazon ECS

Amazon ECS runs one EC2-backed task containing PostgreSQL, Kafka, Caddy, and the four Spring Boot services.
CloudFront sits in front of both the Vite SPA (S3) and the API (Caddy on the ECS host). The public site is a
same-origin app at `tritonwatch.app`. This is intentionally a low-cost, single-host API deployment, not a highly
available distributed deployment.

## Architecture

```text
Internet
   |
tritonwatch.app (CloudFront + ACM)
   |-- /*            -> S3 (Vite build)
   |-- /api/*        -> origin.tritonwatch.app (Caddy :80 on ECS)
   `-- /health/*     -> origin.tritonwatch.app (Caddy :80 on ECS)

Caddy on the ECS host
   |-- /api/v1/me*             -> user-service
   |-- /api/v1/watch-requests* -> watchlist-service
   `-- /api/v1/catalog*        -> ingestion-service

One ECS task on one EC2 container instance
   |-- user-service
   |-- watchlist-service
   |-- ingestion-service
   |-- notification-service
   |-- PostgreSQL
   `-- Kafka
```

The host is not reachable over SSH. Use AWS Systems Manager Session Manager or ECS Exec for administration.
PostgreSQL, Kafka, and the application ports are not exposed publicly. Port 80 on the ECS host accepts traffic only
from the CloudFront origin-facing managed prefix list.

## What Terraform creates

- A VPC, public subnet, internet gateway, security group, and static Elastic IP.
- An ECS cluster and an ECS-optimized Amazon Linux 2023 EC2 instance.
- One ECS service and task definition using EC2 launch type.
- Seven ECR repositories with image scanning and retention policies.
- SecureString Parameter Store values for all database passwords.
- IAM roles for ECS, ECR pulls, Parameter Store, Systems Manager, and ECS Exec.
- A CloudWatch log group with 14-day retention.
- An S3 bucket, CloudFront distribution (SPA + API behaviors), and optional ACM certificate.
- Optional Route 53 records: apex → CloudFront, `origin.<domain>` → Elastic IP.
- Optional daily AWS Backup recovery points with seven-day retention.
- An optional AWS Budget notification.

## Expected cost

The main costs are the EC2 instance, its EBS volume, one public IPv4 address, ECR storage, CloudWatch logs, and
backups. S3 + CloudFront is typically a few dollars per month. Expect roughly $40-65 per month depending on region,
instance type, log volume, backup storage, and traffic. The example creates account-wide budget alerts at $40 actual
spend and $50 forecasted spend.

## Prerequisites

Install and authenticate:

- Terraform 1.10 or newer.
- AWS CLI v2.
- Docker with Buildx.
- An AWS identity allowed to manage EC2, ECS, ECR, IAM, SSM, Route 53, CloudFront, ACM, CloudWatch, AWS Backup, and Budgets.
- A registered domain whose DNS you can update.

Confirm the AWS identity before creating resources:

```bash
aws sts get-caller-identity
```

## 1. Configure Terraform

Create the local variables file:

```bash
cp infra/aws-ecs/terraform.tfvars.example infra/aws-ecs/terraform.tfvars
```

Set at least:

- `domain_name` (defaults to `tritonwatch.app`)
- `clerk_issuer` (the production instance's Frontend API URL, such as `https://clerk.tritonwatch.app`)
- `clerk_authorized_parties` (`https://tritonwatch.app`)
- `cors_allowed_origins` (include `https://tritonwatch.app` and local Vite)
- `route53_zone_id`, if Route 53 hosts the domain (required for the apex hostname and ACM)

Keep `deploy_application = false` for the first apply because ECR is initially empty.

The local Terraform state contains generated database passwords. Production CI
requires the S3 backend in `backend.tf`. Follow [CI/CD](ci-cd.md) to create the
bucket, migrate state, and create the GitHub OIDC deploy role before the next
`terraform apply`.

## 2. Create AWS infrastructure and ECR repositories

```bash
terraform -chdir=infra/aws-ecs init
terraform -chdir=infra/aws-ecs fmt -check
terraform -chdir=infra/aws-ecs validate
terraform -chdir=infra/aws-ecs plan
terraform -chdir=infra/aws-ecs apply
```

At this point the ECS service exists with a desired count of zero.

## 3. Build and push all images

Use an immutable Git commit tag:

```bash
IMAGE_TAG="$(git rev-parse --short HEAD)"
./scripts/build-and-push-ecs-images.sh "$IMAGE_TAG"
```

The script builds Linux AMD64 images for all four services, PostgreSQL, Kafka, and Caddy, then pushes them to ECR.
Rebuild Caddy whenever `infra/production/Caddyfile` changes.

## 4. Configure DNS

When `route53_zone_id` is set, Terraform creates:

- apex A/AAAA aliases for `domain_name` pointing at CloudFront, plus ACM validation records; and
- `origin.<domain_name>` A record pointing at the ECS Elastic IP (CloudFront API origin).

Without Route 53, the app is still available on the CloudFront default domain from
`terraform -chdir=infra/aws-ecs output -raw app_url`, and CloudFront reaches Caddy via the Elastic IP directly.

Confirm the origin resolves before deploying the ECS task:

```bash
dig +short origin.tritonwatch.app
terraform -chdir=infra/aws-ecs output -raw public_ip
```

## 5. Deploy the ECS task

```bash
./scripts/deploy-ecs.sh "$IMAGE_TAG"
```

The script applies the selected image tag, raises the ECS desired count to one, waits for the service to stabilize,
and prints recent ECS events if deployment fails. It also writes ignored local deployment state to
`infra/aws-ecs/deployment.auto.tfvars.json`, so later direct Terraform applies keep the service running on the selected
image instead of reverting to the first-run settings.

## 6. Verify

```bash
curl --fail https://tritonwatch.app/health/user
curl --fail https://tritonwatch.app/health/watchlist
curl --fail https://tritonwatch.app/health/ingestion
```

Inspect ECS:

```bash
CLUSTER="$(terraform -chdir=infra/aws-ecs output -raw ecs_cluster_name)"
SERVICE="$(terraform -chdir=infra/aws-ecs output -raw ecs_service_name)"

aws ecs describe-services \
  --cluster "$CLUSTER" \
  --services "$SERVICE"
```

Tail logs:

```bash
aws logs tail /ecs/tritonwatch/production --follow
```

Open a Session Manager shell on the ECS host:

```bash
INSTANCE_ID="$(terraform -chdir=infra/aws-ecs output -raw ecs_instance_id)"
aws ssm start-session --target "$INSTANCE_ID"
```

## Deploying an update

Always push a new immutable image tag:

```bash
IMAGE_TAG="$(git rev-parse --short HEAD)"
./scripts/build-and-push-ecs-images.sh "$IMAGE_TAG"
./scripts/deploy-ecs.sh "$IMAGE_TAG"
```

Because port 80 is fixed on one EC2 host, the ECS service uses a stop-then-start deployment. Expect a short
downtime during updates.

## Deploying the frontend

After Terraform has created the S3 bucket and CloudFront distribution, build and publish the Vite SPA:

```bash
export VITE_CLERK_PUBLISHABLE_KEY='pk_live_REPLACE_ME'

./scripts/deploy-frontend.sh
```

The script builds with same-origin API base URLs from Terraform `app_url`, syncs `frontend/dist` to S3, and
invalidates CloudFront.

Also confirm:

- the Clerk production instance is active and its domain is `tritonwatch.app`
- `clerk_issuer` exactly matches the production instance's Frontend API URL
- `clerk_authorized_parties` includes `https://tritonwatch.app`
- `cors_allowed_origins` includes `https://tritonwatch.app`

## Backups and recovery

AWS Backup captures the EC2 instance and its EBS data. These backups are crash-consistent; they are not a substitute
for testing PostgreSQL restores. The task data remains tied to this one host, so do not terminate or replace the
instance casually.

Terraform intentionally ignores new recommended ECS AMI versions for the existing host, because an automatic EC2
replacement would discard the live root volume. Plan AMI upgrades as recovery exercises: make a backup, replace the
host deliberately, and restore the data.

To preserve a recovery point before a risky change, create an on-demand AWS Backup job or an EBS snapshot first.

## Destroying the environment

`terraform destroy` removes the ECS host and its root volume. Confirm that a usable recovery point exists before
destroying production:

```bash
terraform -chdir=infra/aws-ecs destroy
```

ECR repositories deliberately have `force_delete = false`, so destruction will refuse to delete repositories that
still contain images. Delete images explicitly only when you truly intend to tear the environment down.
