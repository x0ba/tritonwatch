data "aws_iam_openid_connect_provider" "github" {
  count = var.create_github_oidc_provider ? 0 : 1
  url   = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_github_oidc_provider ? 1 : 0

  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  # AWS validates the GitHub TLS cert against its trust store. Terraform still
  # requires a thumbprint; this dummy value is the documented workaround.
  thumbprint_list = ["ffffffffffffffffffffffffffffffffffffffff"]
}

locals {
  github_oidc_provider_arn = var.create_github_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn
  github_owner_name        = split("/", var.github_repository)[0]
  github_repository_name   = split("/", var.github_repository)[1]
  # Repos created after 2026-07-15 emit repo:owner@id/name@id:... instead of repo:owner/name:...
  github_legacy_sub_prefix    = "repo:${var.github_repository}"
  github_immutable_sub_prefix = "repo:${local.github_owner_name}@${var.github_owner_id}/${local.github_repository_name}@${var.github_repository_id}"
  github_ci_subjects = [
    "${local.github_legacy_sub_prefix}:*",
    "${local.github_immutable_sub_prefix}:*",
  ]
  github_deploy_subjects = [
    "${local.github_legacy_sub_prefix}:environment:${var.github_deploy_environment}",
    "${local.github_immutable_sub_prefix}:environment:${var.github_deploy_environment}",
  ]
}

data "aws_iam_policy_document" "github_oidc_assume_ci" {
  statement {
    sid     = "GitHubActionsCi"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = local.github_ci_subjects
    }
  }
}

data "aws_iam_policy_document" "github_oidc_assume_deploy" {
  statement {
    sid     = "GitHubActionsDeploy"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = local.github_deploy_subjects
    }
  }
}

resource "aws_iam_role" "github_ci" {
  name               = "${var.project_name}-${var.environment}-github-ci"
  assume_role_policy = data.aws_iam_policy_document.github_oidc_assume_ci.json
}

resource "aws_iam_role" "github_deploy" {
  name               = "${var.project_name}-${var.environment}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_oidc_assume_deploy.json
}

resource "aws_iam_role_policy_attachment" "github_ci_readonly" {
  role       = aws_iam_role.github_ci.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

resource "aws_iam_role_policy_attachment" "github_deploy_poweruser" {
  role       = aws_iam_role.github_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/PowerUserAccess"
}

data "aws_iam_policy_document" "github_ci_state" {
  statement {
    sid = "TerraformStateLock"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]
    resources = ["arn:aws:s3:::x0ba-tritonwatch-tfstate/aws-ecs/production/*"]
  }

  statement {
    sid       = "TerraformStateList"
    actions   = ["s3:ListBucket", "s3:GetBucketVersioning"]
    resources = ["arn:aws:s3:::x0ba-tritonwatch-tfstate"]
  }
}

resource "aws_iam_role_policy" "github_ci_state" {
  name   = "terraform-state-lock"
  role   = aws_iam_role.github_ci.id
  policy = data.aws_iam_policy_document.github_ci_state.json
}

data "aws_iam_policy_document" "github_deploy_iam" {
  statement {
    sid = "ManagePrefixedRoles"
    actions = [
      "iam:AddClientIDToOpenIDConnectProvider",
      "iam:AddRoleToInstanceProfile",
      "iam:AttachRolePolicy",
      "iam:CreateInstanceProfile",
      "iam:CreateOpenIDConnectProvider",
      "iam:CreateRole",
      "iam:DeleteInstanceProfile",
      "iam:DeleteOpenIDConnectProvider",
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
      "iam:DetachRolePolicy",
      "iam:GetInstanceProfile",
      "iam:GetOpenIDConnectProvider",
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:ListOpenIDConnectProviders",
      "iam:ListRolePolicies",
      "iam:PassRole",
      "iam:PutRolePolicy",
      "iam:RemoveRoleFromInstanceProfile",
      "iam:TagInstanceProfile",
      "iam:TagOpenIDConnectProvider",
      "iam:TagRole",
      "iam:UntagInstanceProfile",
      "iam:UntagOpenIDConnectProvider",
      "iam:UntagRole",
      "iam:UpdateAssumeRolePolicy",
      "iam:UpdateOpenIDConnectProviderThumbprint",
      "iam:UpdateRole",
      "iam:UpdateRoleDescription",
    ]
    resources = [
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:instance-profile/${var.project_name}-${var.environment}-*",
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com",
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${var.project_name}-${var.environment}-*",
    ]
  }
}

resource "aws_iam_role_policy" "github_deploy_iam" {
  name   = "manage-prefixed-iam"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy_iam.json
}
