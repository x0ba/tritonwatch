variable "aws_region" {
  description = "AWS region in which Tritonwatch is deployed."
  type        = string
  default     = "us-west-2"
}

variable "availability_zone" {
  description = "Availability Zone for the single cost-optimized ECS host."
  type        = string
  default     = "us-west-2a"

  validation {
    condition     = startswith(var.availability_zone, var.aws_region)
    error_message = "availability_zone must belong to aws_region."
  }
}

variable "project_name" {
  description = "Prefix used for AWS resource names."
  type        = string
  default     = "tritonwatch"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "production"
}

variable "instance_type" {
  description = "ECS container instance type. Increase to t3a.large if 4 GB is insufficient."
  type        = string
  default     = "t3a.medium"
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size used by the OS, images, PostgreSQL, and Kafka."
  type        = number
  default     = 50

  validation {
    condition     = var.root_volume_size_gb >= 30
    error_message = "root_volume_size_gb must be at least 30 GB."
  }
}

variable "image_tag" {
  description = "ECR image tag deployed by the ECS task definition."
  type        = string
  default     = "latest"

  validation {
    condition     = can(regex("^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$", var.image_tag))
    error_message = "image_tag must be a valid Docker tag."
  }
}

variable "deploy_application" {
  description = "Run one application task after its images have been pushed to ECR."
  type        = bool
  default     = false
}

variable "domain_name" {
  description = "Public app hostname for the SPA and API (CloudFront), such as tritonwatch.app."
  type        = string
  default     = "tritonwatch.app"

  validation {
    condition     = can(regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$", var.domain_name))
    error_message = "domain_name must be a fully qualified hostname without a URL scheme or path."
  }
}

variable "clerk_issuer" {
  description = "Clerk Frontend API URL used as the session-token issuer, without a trailing slash."
  type        = string

  validation {
    condition     = startswith(var.clerk_issuer, "https://") && !endswith(var.clerk_issuer, "/")
    error_message = "clerk_issuer must be an HTTPS URL without a trailing slash."
  }
}

variable "clerk_authorized_parties" {
  description = "Comma-separated browser origins allowed in Clerk session-token azp claims."
  type        = string

  validation {
    condition     = length(trimspace(var.clerk_authorized_parties)) > 0
    error_message = "clerk_authorized_parties must contain at least one browser origin."
  }
}

variable "cors_allowed_origins" {
  description = "Comma-separated browser origins permitted by the API services."
  type        = string

  validation {
    condition     = length(trimspace(var.cors_allowed_origins)) > 0
    error_message = "cors_allowed_origins must contain at least one browser origin."
  }
}

variable "ingestion_poll_interval" {
  description = "Delay between UCSD catalog polling passes."
  type        = string
  default     = "2m"
}

variable "ucsd_api_base_url" {
  description = "UCSD catalog API base URL."
  type        = string
  default     = "https://classplanner.apps.ucsd.edu"
}

variable "route53_zone_id" {
  description = "Optional existing Route 53 hosted zone. Leave null when DNS is managed elsewhere."
  type        = string
  default     = null
  nullable    = true
}

variable "budget_email" {
  description = "Optional email address for AWS Budget notifications."
  type        = string
  default     = null
  nullable    = true
}

variable "monthly_budget_usd" {
  description = "Account-wide monthly AWS budget alert threshold."
  type        = number
  default     = 50

  validation {
    condition     = var.monthly_budget_usd > 0
    error_message = "monthly_budget_usd must be greater than zero."
  }
}

variable "postmark_server_token" {
  description = "Postmark server API token used for email delivery and verification."
  type        = string
  sensitive   = true
  default     = ""
}

variable "postmark_from_email" {
  description = "Verified Postmark From address for outbound Tritonwatch email."
  type        = string
  default     = ""
}

variable "twilio_account_sid" {
  description = "Twilio Account SID used for SMS delivery and Verify."
  type        = string
  default     = ""
}

variable "twilio_auth_token" {
  description = "Twilio Auth Token used for SMS delivery and Verify."
  type        = string
  sensitive   = true
  default     = ""
}

variable "twilio_from_number" {
  description = "Optional Twilio From number in E.164. Prefer messaging_service_sid in production."
  type        = string
  default     = ""
}

variable "twilio_messaging_service_sid" {
  description = "Optional Twilio Messaging Service SID for outbound SMS alerts."
  type        = string
  default     = ""
}

variable "twilio_verify_service_sid" {
  description = "Twilio Verify Service SID used for phone number verification."
  type        = string
  default     = ""
}

variable "enable_backups" {
  description = "Create daily AWS Backup recovery points for the ECS host."
  type        = bool
  default     = true
}

variable "backup_retention_days" {
  description = "Number of days to retain daily EC2 backups."
  type        = number
  default     = 7

  validation {
    condition     = var.backup_retention_days >= 1
    error_message = "backup_retention_days must be at least one day."
  }
}
