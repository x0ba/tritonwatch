output "public_ip" {
  description = "Static public IP address for the API DNS record."
  value       = aws_eip.ecs_host.public_ip
}

output "api_url" {
  description = "Public HTTPS API base URL."
  value       = "https://${var.api_domain_name}"
}

output "aws_region" {
  description = "AWS region used by deployment helper commands."
  value       = var.aws_region
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.application.name
}

output "ecs_instance_id" {
  description = "Use this ID with AWS Systems Manager Session Manager."
  value       = aws_instance.ecs_host.id
}

output "ecr_repository_urls" {
  value = {
    for name, repository in aws_ecr_repository.images : name => repository.repository_url
  }
}

output "database_parameter_names" {
  description = "SecureString parameters injected into the ECS task."
  value = {
    for name, parameter in aws_ssm_parameter.database : name => parameter.name
  }
}
