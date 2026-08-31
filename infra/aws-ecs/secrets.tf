locals {
  database_secret_names = toset([
    "postgres-admin",
    "user-service",
    "watchlist-service",
    "ingestion-service",
    "notification-service",
  ])
}
resource "random_password" "database" {
  for_each = local.database_secret_names

  length  = 32
  special = false
}

resource "aws_ssm_parameter" "database" {
  for_each = local.database_secret_names

  name  = "/${var.project_name}/${var.environment}/database/${each.key}"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.database[each.key].result

  tags = {
    Name = "${var.project_name}-${var.environment}-${each.key}"
  }
}
