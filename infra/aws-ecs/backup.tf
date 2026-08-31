data "aws_iam_policy_document" "backup_assume_role" {
  count = var.enable_backups ? 1 : 0

  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["backup.amazonaws.com"]
    }
  }
}
resource "aws_iam_role" "backup" {
  count = var.enable_backups ? 1 : 0

  name               = "${var.project_name}-${var.environment}-backup"
  assume_role_policy = data.aws_iam_policy_document.backup_assume_role[0].json
}

resource "aws_iam_role_policy_attachment" "backup" {
  count = var.enable_backups ? 1 : 0

  role       = aws_iam_role.backup[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSBackupServiceRolePolicyForBackup"
}

resource "aws_backup_vault" "application" {
  count = var.enable_backups ? 1 : 0

  name = "${var.project_name}-${var.environment}"
}

resource "aws_backup_plan" "application" {
  count = var.enable_backups ? 1 : 0

  name = "${var.project_name}-${var.environment}-daily"

  rule {
    rule_name         = "daily"
    target_vault_name = aws_backup_vault.application[0].name
    schedule          = "cron(0 10 * * ? *)"
    start_window      = 60
    completion_window = 180

    lifecycle {
      delete_after = var.backup_retention_days
    }

    recovery_point_tags = {
      Application = var.project_name
      Environment = var.environment
    }
  }
}

resource "aws_backup_selection" "application" {
  count = var.enable_backups ? 1 : 0

  name         = "${var.project_name}-${var.environment}"
  plan_id      = aws_backup_plan.application[0].id
  iam_role_arn = aws_iam_role.backup[0].arn
  resources    = [aws_instance.ecs_host.arn]

  depends_on = [aws_iam_role_policy_attachment.backup]
}
