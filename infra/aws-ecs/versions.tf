terraform {
  required_version = ">= 1.10.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }

    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Application = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ACM certificates for CloudFront must be issued in us-west-1.
provider "aws" {
  alias  = "us_west_1"
  region = "us-west-1"

  default_tags {
    tags = {
      Application = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
