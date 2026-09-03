terraform {
  backend "s3" {
    bucket       = "x0ba-tritonwatch-tfstate"
    key          = "aws-ecs/production/terraform.tfstate"
    region       = "us-west-2"
    encrypt      = true
    use_lockfile = true
  }
}
