data "aws_ssm_parameter" "ecs_optimized_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_cloudwatch_log_group" "application" {
  name              = "/ecs/${var.project_name}/${var.environment}"
  retention_in_days = 14
}

resource "aws_instance" "ecs_host" {
  ami                         = data.aws_ssm_parameter.ecs_optimized_ami.value
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.ecs_host.id]
  iam_instance_profile        = aws_iam_instance_profile.ecs_instance.name
  associate_public_ip_address = false
  monitoring                  = false
  disable_api_termination     = false

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
    instance_metadata_tags      = "enabled"
  }

  root_block_device {
    encrypted             = true
    volume_type           = "gp3"
    volume_size           = var.root_volume_size_gb
    delete_on_termination = true

    tags = {
      Name   = "${var.project_name}-${var.environment}-data"
      Backup = var.enable_backups ? "true" : "false"
    }
  }

  user_data = <<-USER_DATA
    #!/bin/bash
    set -euxo pipefail

    echo 'ECS_CLUSTER=${aws_ecs_cluster.main.name}' >> /etc/ecs/ecs.config

    install -d -m 0750 /opt/tritonwatch
    install -d -m 0700 -o 999 -g 999 /opt/tritonwatch/postgres
    install -d -m 0750 -o 1000 -g 1000 /opt/tritonwatch/kafka
    install -d -m 0750 -o 1000 -g 1000 /opt/tritonwatch/caddy-data
    install -d -m 0750 -o 1000 -g 1000 /opt/tritonwatch/caddy-config

    systemctl enable --now ecs
  USER_DATA

  user_data_replace_on_change = false

  # The recommended AMI parameter changes regularly. Do not replace the stateful
  # single host during an ordinary Terraform refresh; upgrade it deliberately
  # after taking and testing a backup.
  lifecycle {
    ignore_changes = [ami]
  }

  depends_on = [
    aws_internet_gateway.main,
    aws_route_table_association.public,
    aws_iam_role_policy_attachment.ecs_instance,
    aws_iam_role_policy_attachment.ssm_instance,
  ]

  tags = {
    Name   = "${var.project_name}-${var.environment}-ecs-host"
    Backup = var.enable_backups ? "true" : "false"
  }
}

resource "aws_eip_association" "ecs_host" {
  allocation_id = aws_eip.ecs_host.id
  instance_id   = aws_instance.ecs_host.id
}
