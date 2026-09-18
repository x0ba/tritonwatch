terraform {
  backend "s3" {
    bucket       = "x0ba-tritonwatch-tfstate-372815112156"
    key          = "aws-ecs/production/terraform.tfstate"
    region       = "us-west-2"
    encrypt      = true
    use_lockfile = true
  }
}
