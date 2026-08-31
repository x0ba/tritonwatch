resource "aws_vpc" "main" {
  cidr_block           = "10.42.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.project_name}-${var.environment}"
  }
}
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-${var.environment}"
  }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  availability_zone       = var.availability_zone
  cidr_block              = "10.42.1.0/24"
  map_public_ip_on_launch = false

  tags = {
    Name = "${var.project_name}-${var.environment}-public"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-public"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "ecs_host" {
  name        = "${var.project_name}-${var.environment}-ecs-host"
  description = "Public HTTPS ingress for the Tritonwatch ECS host"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP for ACME redirects"
    protocol    = "tcp"
    from_port   = 80
    to_port     = 80
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP/3"
    protocol    = "udp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description      = "Application and AWS API egress"
    protocol         = "-1"
    from_port        = 0
    to_port          = 0
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = []
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-ecs-host"
  }
}

resource "aws_eip" "ecs_host" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-${var.environment}"
  }
}
