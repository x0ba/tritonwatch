# Deploying Tritonwatch from GitHub Actions

This is the production CI/CD path: GitHub Actions authenticates to AWS with OIDC,
Terraform state lives in S3, and the existing deploy scripts publish SHA-tagged
images plus the Vite SPA. Do the one-time AWS and GitHub setup locally first.
The workflows cannot create their own trust relationship.

## What you end up with

```text
Pull request
  └─ CI: Gradle unit tests, frontend typecheck, terraform plan (read-only role)

main or "Run workflow"
  └─ Deploy production (GitHub Environment protection)
        ├─ OIDC → tritonwatch-production-github-deploy
        ├─ ./scripts/build-and-push-ecs-images.sh $GITHUB_SHA
        ├─ ./scripts/deploy-ecs.sh $GITHUB_SHA
        ├─ ./scripts/deploy-frontend.sh
        └─ curl https://tritonwatch.app/health/{user,watchlist,ingestion}
```

There are no long-lived AWS access keys in GitHub. The deploy role can only be
assumed from the `production` environment on `x0ba/tritonwatch`.

## 1. Create the Terraform state bucket

`infra/aws-ecs` now uses an S3 backend. The next `terraform` command will fail
until this bucket exists and local state is migrated.

```bash
aws sts get-caller-identity
chmod +x scripts/bootstrap-terraform-backend.sh
./scripts/bootstrap-terraform-backend.sh
```

The script creates `x0ba-tritonwatch-tfstate` in `us-west-2` with versioning,
encryption, and public access blocked. If you need a different bucket name, change
it in both `infra/aws-ecs/backend.tf` and `infra/aws-ecs/github-oidc.tf`.

## 2. Migrate local state to S3

```bash
terraform -chdir=infra/aws-ecs init -migrate-state
```

Answer `yes` when Terraform asks to copy the existing state. Confirm the local
file is no longer authoritative:

```bash
terraform -chdir=infra/aws-ecs init
terraform -chdir=infra/aws-ecs plan
```

Keep `infra/aws-ecs/terraform.tfvars` on disk. It is still gitignored. After
migration, back up that file — CI will use a copy stored as a GitHub secret.

## 3. Check for an existing GitHub OIDC provider

An AWS account can have only one OIDC provider for
`token.actions.githubusercontent.com`.

```bash
aws iam list-open-id-connect-providers
```

If that URL already exists, set this in `infra/aws-ecs/terraform.tfvars`:

```hcl
create_github_oidc_provider = false
```

Otherwise leave the default `true`.

## 4. Create the CI and deploy roles

```bash
terraform -chdir=infra/aws-ecs apply
```

Copy the role ARNs:

```bash
terraform -chdir=infra/aws-ecs output -raw github_ci_role_arn
terraform -chdir=infra/aws-ecs output -raw github_deploy_role_arn
```

They look like:

```text
arn:aws:iam::ACCOUNT:role/tritonwatch-production-github-ci
arn:aws:iam::ACCOUNT:role/tritonwatch-production-github-deploy
```

## 5. Configure GitHub

### Environment

1. Open https://github.com/x0ba/tritonwatch/settings/environments
2. Create `production`
3. Enable **Required reviewers** and add yourself
4. Optionally restrict the environment to the `main` branch

### Variables

Repository variables (Settings → Secrets and variables → Actions → Variables):

| Name | Value |
|---|---|
| `AWS_CI_ROLE_ARN` | output `github_ci_role_arn` |
| `AWS_DEPLOY_ROLE_ARN` | output `github_deploy_role_arn` |

Also add `AWS_DEPLOY_ROLE_ARN` as a **production environment** variable if you
prefer environment-scoped values. The deploy workflow reads
`${{ vars.AWS_DEPLOY_ROLE_ARN }}` from the `production` environment first, then
the repository.

### Secrets

Create a repository secret named `TFVARS` whose value is the exact contents of
your local `infra/aws-ecs/terraform.tfvars`. Easiest from the repo root:

```bash
gh secret set TFVARS --app actions < infra/aws-ecs/terraform.tfvars
```

Create the same secret on the `production` environment (the deploy job can only
see environment secrets when it uses `environment: production`):

```bash
gh secret set TFVARS --app actions --env production < infra/aws-ecs/terraform.tfvars
```

Add the Clerk production publishable key to the `production` environment:

```bash
gh secret set VITE_CLERK_PUBLISHABLE_KEY --app actions --env production
```

Paste `pk_live_...` when prompted. CI plans also need `TFVARS` as a repository
secret because the plan job does not use the production environment.

If you skip the `gh` CLI, paste the files in the GitHub UI instead.

## 6. First production deploy

Push the workflow files to `main`, then run the deploy workflow manually once
before trusting push-to-main:

```bash
git add \
  .github/workflows/ci.yml \
  .github/workflows/deploy.yml \
  docs/ci-cd.md \
  infra/aws-ecs/backend.tf \
  infra/aws-ecs/github-oidc.tf \
  infra/aws-ecs/outputs.tf \
  infra/aws-ecs/variables.tf \
  infra/aws-ecs/terraform.tfvars.example \
  scripts/bootstrap-terraform-backend.sh \
  scripts/deploy-ecs.sh \
  README.md \
  docs/deployment-ecs.md

git commit -m "Add GitHub Actions OIDC deploy for ECS and the SPA"

git push origin HEAD
gh workflow run "Deploy production" --ref main
```

Approve the GitHub Environment prompt if you enabled required reviewers. Watch:

```bash
gh run watch
```

The ECS service is one task on one host and uses stop-then-start, so expect a
short outage. `concurrency: production` with `cancel-in-progress: false` prevents
two deploys from overlapping.

## 7. Everyday workflow

- Open a PR. CI runs tests and `terraform plan`.
- Merge to `main`. Deploy starts after environment approval.
- Or run **Actions → Deploy production → Run workflow**.

Do not apply infrastructure experiments from your laptop against a different
state file. After migration, every apply must use the S3 backend.

## Files

| Path | Role |
|---|---|
| `infra/aws-ecs/backend.tf` | S3 state + native lock file |
| `infra/aws-ecs/github-oidc.tf` | GitHub OIDC provider and IAM roles |
| `.github/workflows/ci.yml` | PR / main verification |
| `.github/workflows/deploy.yml` | Production release |
| `scripts/bootstrap-terraform-backend.sh` | One-time state bucket |
| `scripts/deploy-ecs.sh` | Uses `-auto-approve` when `CI=true` |

## What CI does not run

`*ApplicationTests` (`@SpringBootTest`) need PostgreSQL and Kafka. They are
skipped in Actions until those tests use Testcontainers. Isolated unit tests and
`WebMvcTest` classes still run.

A Terraform plan job is skipped until `AWS_CI_ROLE_ARN` is set, so the first
pushes can land the workflows before AWS trust exists.

## Updating secrets later

When `terraform.tfvars` changes (Clerk, Twilio, Postmark, Route 53):

```bash
gh secret set TFVARS --app actions < infra/aws-ecs/terraform.tfvars
gh secret set TFVARS --app actions --env production < infra/aws-ecs/terraform.tfvars
```

Then run **Deploy production**. `deploy-ecs.sh` applies Terraform, so task
definition environment variables update on the next release.
