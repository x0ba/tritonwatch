locals {
  image_repositories = toset([
    "user-service",
    "watchlist-service",
    "ingestion-service",
    "notification-service",
    "postgres",
    "kafka",
    "caddy",
  ])
}
resource "aws_ecr_repository" "images" {
  for_each = local.image_repositories

  name                 = "${var.project_name}/${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = false

  encryption_configuration {
    encryption_type = "AES256"
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "images" {
  for_each = aws_ecr_repository.images

  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the ten newest images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
