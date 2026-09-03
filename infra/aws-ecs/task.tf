locals {
  java_tool_options = join(" ", [
    "-XX:InitialRAMPercentage=25",
    "-XX:MaxRAMPercentage=65",
    "-XX:+UseSerialGC",
    "-XX:+ExitOnOutOfMemoryError",
  ])

  log_configuration = {
    logDriver = "awslogs"
    options = {
      "awslogs-group"         = aws_cloudwatch_log_group.application.name
      "awslogs-region"        = var.aws_region
      "awslogs-stream-prefix" = "container"
    }
  }

  database_parameter_arns = {
    postgres_admin       = aws_ssm_parameter.database["postgres-admin"].arn
    user_service         = aws_ssm_parameter.database["user-service"].arn
    watchlist_service    = aws_ssm_parameter.database["watchlist-service"].arn
    ingestion_service    = aws_ssm_parameter.database["ingestion-service"].arn
    notification_service = aws_ssm_parameter.database["notification-service"].arn
  }
}

resource "aws_ecs_task_definition" "application" {
  family                   = "${var.project_name}-${var.environment}"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  volume {
    name      = "postgres-data"
    host_path = "/opt/tritonwatch/postgres"
  }

  volume {
    name      = "kafka-data"
    host_path = "/opt/tritonwatch/kafka"
  }

  volume {
    name      = "caddy-data"
    host_path = "/opt/tritonwatch/caddy-data"
  }

  volume {
    name      = "caddy-config"
    host_path = "/opt/tritonwatch/caddy-config"
  }

  container_definitions = jsonencode([
    {
      name              = "postgres"
      image             = "${aws_ecr_repository.images["postgres"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 128
      memory            = 448
      memoryReservation = 256
      environment = [
        { name = "POSTGRES_USER", value = "postgres" },
      ]
      secrets = [
        { name = "POSTGRES_PASSWORD", valueFrom = local.database_parameter_arns.postgres_admin },
        { name = "USER_DATABASE_PASSWORD", valueFrom = local.database_parameter_arns.user_service },
        { name = "WATCHLIST_DATABASE_PASSWORD", valueFrom = local.database_parameter_arns.watchlist_service },
        { name = "INGESTION_DATABASE_PASSWORD", valueFrom = local.database_parameter_arns.ingestion_service },
        { name = "NOTIFICATION_DATABASE_PASSWORD", valueFrom = local.database_parameter_arns.notification_service },
      ]
      mountPoints = [{
        sourceVolume  = "postgres-data"
        containerPath = "/var/lib/postgresql/data"
        readOnly      = false
      }]
      healthCheck = {
        command     = ["CMD-SHELL", "pg_isready -U postgres -d postgres"]
        interval    = 10
        timeout     = 5
        retries     = 10
        startPeriod = 20
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
    {
      name              = "kafka"
      image             = "${aws_ecr_repository.images["kafka"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 256
      memory            = 768
      memoryReservation = 512
      environment = [
        { name = "KAFKA_NODE_ID", value = "1" },
        { name = "KAFKA_PROCESS_ROLES", value = "broker,controller" },
        { name = "KAFKA_LISTENERS", value = "CONTROLLER://:9093,INTERNAL://:29092" },
        { name = "KAFKA_ADVERTISED_LISTENERS", value = "INTERNAL://kafka:29092" },
        { name = "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", value = "CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT" },
        { name = "KAFKA_INTER_BROKER_LISTENER_NAME", value = "INTERNAL" },
        { name = "KAFKA_CONTROLLER_LISTENER_NAMES", value = "CONTROLLER" },
        { name = "KAFKA_CONTROLLER_QUORUM_VOTERS", value = "1@localhost:9093" },
        { name = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", value = "1" },
        { name = "KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", value = "1" },
        { name = "KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", value = "1" },
        { name = "KAFKA_AUTO_CREATE_TOPICS_ENABLE", value = "false" },
        { name = "KAFKA_HEAP_OPTS", value = "-Xms384m -Xmx384m" },
        { name = "KAFKA_LOG_RETENTION_HOURS", value = "168" },
      ]
      mountPoints = [{
        sourceVolume  = "kafka-data"
        containerPath = "/var/lib/kafka/data"
        readOnly      = false
      }]
      healthCheck = {
        command = [
          "CMD-SHELL",
          "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:29092 >/dev/null 2>&1",
        ]
        interval    = 15
        timeout     = 10
        retries     = 10
        startPeriod = 30
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
    {
      name              = "kafka-init"
      image             = "${aws_ecr_repository.images["kafka"].repository_url}:${var.image_tag}"
      essential         = false
      cpu               = 64
      memory            = 128
      memoryReservation = 64
      links             = ["kafka"]
      dependsOn = [{
        containerName = "kafka"
        condition     = "HEALTHY"
      }]
      environment = [
        { name = "KAFKA_BOOTSTRAP_SERVER", value = "kafka:29092" },
        { name = "KAFKA_TOPIC_PARTITIONS", value = "3" },
        { name = "KAFKA_TOPIC_RETENTION_MS", value = "604800000" },
      ]
      entryPoint       = ["/bin/sh", "/opt/tritonwatch/create-topics.sh"]
      logConfiguration = local.log_configuration
    },
    {
      name              = "user-service"
      image             = "${aws_ecr_repository.images["user-service"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 128
      memory            = 416
      memoryReservation = 320
      links             = ["postgres", "kafka"]
      dependsOn = [
        { containerName = "postgres", condition = "HEALTHY" },
        { containerName = "kafka-init", condition = "SUCCESS" },
      ]
      environment = [
        { name = "SERVER_PORT", value = "8081" },
        { name = "DATABASE_URL", value = "jdbc:postgresql://postgres:5432/user_service" },
        { name = "DATABASE_USERNAME", value = "user_service" },
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = "kafka:29092" },
        { name = "AUTH0_ISSUER", value = var.auth0_issuer },
        { name = "AUTH0_AUDIENCE", value = var.auth0_audience },
        { name = "CORS_ALLOWED_ORIGINS", value = var.cors_allowed_origins },
        { name = "POSTMARK_SERVER_TOKEN", value = var.postmark_server_token },
        { name = "POSTMARK_FROM_EMAIL", value = var.postmark_from_email },
        { name = "TWILIO_ACCOUNT_SID", value = var.twilio_account_sid },
        { name = "TWILIO_AUTH_TOKEN", value = var.twilio_auth_token },
        { name = "TWILIO_VERIFY_SERVICE_SID", value = var.twilio_verify_service_sid },
        { name = "JAVA_TOOL_OPTIONS", value = local.java_tool_options },
      ]
      secrets = [{
        name      = "DATABASE_PASSWORD"
        valueFrom = local.database_parameter_arns.user_service
      }]
      healthCheck = {
        command     = ["CMD", "curl", "--fail", "--silent", "http://localhost:8081/actuator/health"]
        interval    = 20
        timeout     = 5
        retries     = 5
        startPeriod = 60
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
    {
      name              = "watchlist-service"
      image             = "${aws_ecr_repository.images["watchlist-service"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 128
      memory            = 416
      memoryReservation = 320
      links             = ["postgres", "kafka"]
      dependsOn = [
        { containerName = "postgres", condition = "HEALTHY" },
        { containerName = "kafka-init", condition = "SUCCESS" },
      ]
      environment = [
        { name = "SERVER_PORT", value = "8082" },
        { name = "DATABASE_URL", value = "jdbc:postgresql://postgres:5432/watchlist_service" },
        { name = "DATABASE_USERNAME", value = "watchlist_service" },
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = "kafka:29092" },
        { name = "AUTH0_ISSUER", value = var.auth0_issuer },
        { name = "AUTH0_AUDIENCE", value = var.auth0_audience },
        { name = "CORS_ALLOWED_ORIGINS", value = var.cors_allowed_origins },
        { name = "JAVA_TOOL_OPTIONS", value = local.java_tool_options },
      ]
      secrets = [{
        name      = "DATABASE_PASSWORD"
        valueFrom = local.database_parameter_arns.watchlist_service
      }]
      healthCheck = {
        command     = ["CMD", "curl", "--fail", "--silent", "http://localhost:8082/actuator/health"]
        interval    = 20
        timeout     = 5
        retries     = 5
        startPeriod = 60
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
    {
      name              = "ingestion-service"
      image             = "${aws_ecr_repository.images["ingestion-service"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 128
      memory            = 480
      memoryReservation = 320
      links             = ["postgres", "kafka"]
      dependsOn = [
        { containerName = "postgres", condition = "HEALTHY" },
        { containerName = "kafka-init", condition = "SUCCESS" },
      ]
      environment = [
        { name = "SERVER_PORT", value = "8083" },
        { name = "DATABASE_URL", value = "jdbc:postgresql://postgres:5432/ingestion_service" },
        { name = "DATABASE_USERNAME", value = "ingestion_service" },
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = "kafka:29092" },
        { name = "INGESTION_POLL_INTERVAL", value = var.ingestion_poll_interval },
        { name = "UCSD_API_BASE_URL", value = var.ucsd_api_base_url },
        { name = "CORS_ALLOWED_ORIGINS", value = var.cors_allowed_origins },
        { name = "JAVA_TOOL_OPTIONS", value = local.java_tool_options },
      ]
      secrets = [{
        name      = "DATABASE_PASSWORD"
        valueFrom = local.database_parameter_arns.ingestion_service
      }]
      healthCheck = {
        command     = ["CMD", "curl", "--fail", "--silent", "http://localhost:8083/actuator/health"]
        interval    = 20
        timeout     = 5
        retries     = 5
        startPeriod = 60
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
    {
      name              = "notification-service"
      image             = "${aws_ecr_repository.images["notification-service"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 128
      memory            = 416
      memoryReservation = 320
      links             = ["postgres", "kafka"]
      dependsOn = [
        { containerName = "postgres", condition = "HEALTHY" },
        { containerName = "kafka-init", condition = "SUCCESS" },
      ]
      environment = [
        { name = "SERVER_PORT", value = "8084" },
        { name = "DATABASE_URL", value = "jdbc:postgresql://postgres:5432/notification_service" },
        { name = "DATABASE_USERNAME", value = "notification_service" },
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = "kafka:29092" },
        { name = "POSTMARK_SERVER_TOKEN", value = var.postmark_server_token },
        { name = "POSTMARK_FROM_EMAIL", value = var.postmark_from_email },
        { name = "TWILIO_ACCOUNT_SID", value = var.twilio_account_sid },
        { name = "TWILIO_AUTH_TOKEN", value = var.twilio_auth_token },
        { name = "TWILIO_FROM_NUMBER", value = var.twilio_from_number },
        { name = "TWILIO_MESSAGING_SERVICE_SID", value = var.twilio_messaging_service_sid },
        { name = "JAVA_TOOL_OPTIONS", value = local.java_tool_options },
      ]
      secrets = [{
        name      = "DATABASE_PASSWORD"
        valueFrom = local.database_parameter_arns.notification_service
      }]
      healthCheck = {
        command     = ["CMD", "curl", "--fail", "--silent", "http://localhost:8084/actuator/health"]
        interval    = 20
        timeout     = 5
        retries     = 5
        startPeriod = 60
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
    {
      name              = "caddy"
      image             = "${aws_ecr_repository.images["caddy"].repository_url}:${var.image_tag}"
      essential         = true
      cpu               = 64
      memory            = 128
      memoryReservation = 64
      links             = ["user-service", "watchlist-service", "ingestion-service"]
      dependsOn = [
        { containerName = "user-service", condition = "HEALTHY" },
        { containerName = "watchlist-service", condition = "HEALTHY" },
        { containerName = "ingestion-service", condition = "HEALTHY" },
      ]
      environment = [
        { name = "API_DOMAIN", value = var.api_domain_name },
        { name = "ACME_EMAIL", value = var.acme_email },
      ]
      portMappings = [
        { containerPort = 80, hostPort = 80, protocol = "tcp" },
        { containerPort = 443, hostPort = 443, protocol = "tcp" },
        { containerPort = 443, hostPort = 443, protocol = "udp" },
      ]
      mountPoints = [
        { sourceVolume = "caddy-data", containerPath = "/data", readOnly = false },
        { sourceVolume = "caddy-config", containerPath = "/config", readOnly = false },
      ]
      healthCheck = {
        command     = ["CMD", "wget", "--quiet", "--spider", "http://localhost:2019/config/"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 10
      }
      linuxParameters = {
        initProcessEnabled = true
      }
      logConfiguration = local.log_configuration
    },
  ])

  depends_on = [
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_parameters,
  ]
}

resource "aws_ecs_service" "application" {
  name            = "${var.project_name}-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.application.arn
  desired_count   = var.deploy_application ? 1 : 0
  launch_type     = "EC2"

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100
  enable_execute_command             = true
  wait_for_steady_state              = false
  propagate_tags                     = "SERVICE"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [
    aws_instance.ecs_host,
    aws_eip_association.ecs_host,
    aws_iam_role_policy.ecs_exec,
  ]
}
