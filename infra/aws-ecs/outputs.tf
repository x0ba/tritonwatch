output "public_ip" {
  description = "Static public IP address used as the CloudFront API origin."
  value       = aws_eip.ecs_host.public_ip
}

output "app_url" {
  description = "Public HTTPS URL for the SPA and API (same origin)."
  value       = local.app_url
}

output "api_url" {
  description = "Public HTTPS API base URL (same as app_url)."
  value       = local.app_url
}

output "frontend_url" {
  description = "Public HTTPS SPA URL (same as app_url)."
  value       = local.app_url
}

output "frontend_bucket_name" {
  description = "S3 bucket that stores the Vite build output."
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_cloudfront_distribution_id" {
  description = "CloudFront distribution ID used for cache invalidation."
  value       = aws_cloudfront_distribution.app.id
}

output "frontend_cloudfront_domain_name" {
  description = "CloudFront domain name for the app."
  value       = aws_cloudfront_distribution.app.domain_name
}

output "api_origin_hostname" {
  description = "Hostname or IP CloudFront uses to reach Caddy."
  value       = local.api_origin_domain_name
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
