# Deploying Tritonwatch to Amazon ECS

This deployment keeps the application architecture unchanged. Amazon ECS runs one EC2-backed task containing
PostgreSQL, Kafka, Caddy, and the four Spring Boot services. It is intentionally a low-cost, single-host deployment,
not a highly available distributed deployment.

## Architecture

```text
Internet
   |
Elastic IP :80/:443
   |
Caddy container
   |-- /api/v1/me*             -> user-service
   `-- /api/v1/watch-requests* -> watchlist-service

One ECS task on one EC2 container instance
   |-- user-service
   |-- watchlist-service
   |-- ingestion-service
   |-- notification-service
   |-- PostgreSQL
   `-- Kafka
```

The host is not reachable over SSH. Use AWS Systems Manager Session Manager or ECS Exec for administration.
PostgreSQL, Kafka, and the application ports are not exposed publicly.

## What Terraform creates

- A VPC, public subnet, internet gateway, security group, and static Elastic IP.
- An ECS cluster and an ECS-optimized Amazon Linux 2023 EC2 instance.
- One ECS service and task definition using EC2 launch type.
- Seven ECR repositories with image scanning and retention policies.
- SecureString Parameter Store values for all database passwords.
- IAM roles for ECS, ECR pulls, Parameter Store, Systems Manager, and ECS Exec.
- A CloudWatch log group with 14-day retention.
- An optional Route 53 A record.
- Optional daily AWS Backup recovery points with seven-day retention.
- An optional AWS Budget notification.

## Expected cost

The main costs are the `t3a.medium` instance, its 50 GB gp3 volume, one public IPv4 address, ECR storage,
CloudWatch logs, and backups. Expect roughly $40-60 per month in `us-west-2`, depending on log volume and backup
storage. The example creates account-wide budget alerts at $40 actual spend and $50 forecasted spend. Change
`instance_type` to `t3a.large` if the 4 GB host experiences memory pressure.

## Prerequisites

Install and authenticate:

- Terraform 1.10 or newer.
- AWS CLI v2.
- Docker with Buildx.
- An AWS identity allowed to manage EC2, ECS, ECR, IAM, SSM, Route 53, CloudWatch, AWS Backup, and Budgets.
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

- `api_domain_name`
- `acme_email`
- `auth0_issuer`
- `auth0_audience`
- `cors_allowed_origins`
- `route53_zone_id`, if Route 53 hosts the domain

Keep `deploy_application = false` for the first apply because ECR is initially empty.

The local Terraform state contains generated database passwords. It is ignored by Git, but it must still be protected.
For team or long-lived use, move the state to a versioned and encrypted S3 backend with S3 lock-file locking.

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

## 4. Configure DNS

When `route53_zone_id` is set, Terraform creates the API A record automatically. Otherwise, create an A record at
your DNS provider pointing `api_domain_name` to:

```bash
terraform -chdir=infra/aws-ecs output -raw public_ip
```

Wait for DNS before starting Caddy:

```bash
dig +short api.example.com
```

The result must match the Terraform `public_ip` output.

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
curl --fail https://api.example.com/health/user
curl --fail https://api.example.com/health/watchlist
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

Because ports 80 and 443 are fixed on one EC2 host, the ECS service uses a stop-then-start deployment. Expect a short
downtime during updates.

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
