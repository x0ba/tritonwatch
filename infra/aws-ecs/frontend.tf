data "aws_caller_identity" "current" {}

locals {
  frontend_bucket_name   = "${var.project_name}-${var.environment}-frontend-${data.aws_caller_identity.current.account_id}"
  has_route53            = var.route53_zone_id != null
  # CloudFront origins must be hostnames, not raw IPs.
  api_origin_domain_name = local.has_route53 ? "origin.${var.domain_name}" : aws_eip.ecs_host.public_dns
  app_url                = local.has_route53 ? "https://${var.domain_name}" : "https://${aws_cloudfront_distribution.app.domain_name}"
}

moved {
  from = aws_acm_certificate.frontend
  to   = aws_acm_certificate.app
}

moved {
  from = aws_acm_certificate_validation.frontend
  to   = aws_acm_certificate_validation.app
}

moved {
  from = aws_cloudfront_distribution.frontend
  to   = aws_cloudfront_distribution.app
}

moved {
  from = aws_route53_record.frontend_certificate_validation
  to   = aws_route53_record.app_certificate_validation
}

moved {
  from = aws_route53_record.frontend_ipv4
  to   = aws_route53_record.app_ipv4
}

moved {
  from = aws_route53_record.frontend_ipv6
  to   = aws_route53_record.app_ipv6
}

resource "aws_s3_bucket" "frontend" {
  bucket = local.frontend_bucket_name
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  versioning_configuration {
    status = "Suspended"
  }
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project_name}-${var.environment}-frontend"
  description                       = "Sign CloudFront requests to the frontend S3 origin."
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_acm_certificate" "app" {
  count    = local.has_route53 ? 1 : 0
  provider = aws.us_east_1

  domain_name       = var.domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "app_certificate_validation" {
  for_each = local.has_route53 ? toset([var.domain_name]) : toset([])

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name = one([
    for option in aws_acm_certificate.app[0].domain_validation_options :
    option.resource_record_name if option.domain_name == each.value
  ])
  type = one([
    for option in aws_acm_certificate.app[0].domain_validation_options :
    option.resource_record_type if option.domain_name == each.value
  ])
  ttl = 60
  records = [
    one([
      for option in aws_acm_certificate.app[0].domain_validation_options :
      option.resource_record_value if option.domain_name == each.value
    ])
  ]
}

resource "aws_acm_certificate_validation" "app" {
  count    = local.has_route53 ? 1 : 0
  provider = aws.us_east_1

  certificate_arn         = aws_acm_certificate.app[0].arn
  validation_record_fqdns = [for record in aws_route53_record.app_certificate_validation : record.fqdn]
}

# SPA deep links (BrowserRouter) rewrite to index.html without mapping API 404s.
resource "aws_cloudfront_function" "spa_router" {
  name    = "${var.project_name}-${var.environment}-spa-router"
  runtime = "cloudfront-js-2.0"
  comment = "Rewrite non-file SPA paths to /index.html"
  publish = true
  code    = <<-EOF
    function handler(event) {
      var request = event.request;
      var uri = request.uri;

      if (uri.startsWith('/api/') || uri.startsWith('/health/')) {
        return request;
      }

      if (uri.indexOf('.') !== -1) {
        return request;
      }

      request.uri = '/index.html';
      return request;
    }
  EOF
}

resource "aws_cloudfront_distribution" "app" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.project_name} ${var.environment} app"
  default_root_object = "index.html"
  price_class         = "PriceClass_100"
  http_version        = "http2and3"
  aliases             = local.has_route53 ? [var.domain_name] : []

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "frontend-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  origin {
    domain_name = local.api_origin_domain_name
    origin_id   = "api-caddy"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  ordered_cache_behavior {
    path_pattern           = "/api/*"
    target_origin_id       = "api-caddy"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    # AWS managed CachingDisabled
    cache_policy_id = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    # AWS managed AllViewer — forwards Authorization and other viewer headers.
    origin_request_policy_id = "216adef6-5c7f-47e4-b989-5492eafa07d3"
  }

  ordered_cache_behavior {
    path_pattern             = "/health/*"
    target_origin_id         = "api-caddy"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS"]
    cached_methods           = ["GET", "HEAD"]
    compress                 = true
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    origin_request_policy_id = "216adef6-5c7f-47e4-b989-5492eafa07d3"
  }

  default_cache_behavior {
    target_origin_id       = "frontend-s3"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    # AWS managed CachingOptimized — honors Cache-Control from the deploy sync.
    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.spa_router.arn
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = !local.has_route53
    acm_certificate_arn            = local.has_route53 ? aws_acm_certificate_validation.app[0].certificate_arn : null
    ssl_support_method             = local.has_route53 ? "sni-only" : null
    minimum_protocol_version       = local.has_route53 ? "TLSv1.2_2021" : null
  }
}

data "aws_iam_policy_document" "frontend_bucket" {
  statement {
    sid = "AllowCloudFrontRead"
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.frontend.arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.app.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_bucket.json
}

resource "aws_route53_record" "app_ipv4" {
  count = local.has_route53 ? 1 : 0

  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.app.domain_name
    zone_id                = aws_cloudfront_distribution.app.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "app_ipv6" {
  count = local.has_route53 ? 1 : 0

  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.app.domain_name
    zone_id                = aws_cloudfront_distribution.app.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "api_origin" {
  count = local.has_route53 ? 1 : 0

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name            = "origin.${var.domain_name}"
  type            = "A"
  ttl             = 60
  records         = [aws_eip.ecs_host.public_ip]
}

# Clerk production DNS. Values must match the Clerk Dashboard exactly.
resource "aws_route53_record" "clerk_frontend_api" {
  count = local.has_route53 ? 1 : 0

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name            = "clerk.${var.domain_name}"
  type            = "CNAME"
  ttl             = 60
  records         = ["frontend-api.clerk.services"]
}

resource "aws_route53_record" "clerk_accounts" {
  count = local.has_route53 ? 1 : 0

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name            = "accounts.${var.domain_name}"
  type            = "CNAME"
  ttl             = 60
  records         = ["accounts.clerk.services"]
}

resource "aws_route53_record" "clerk_dkim1" {
  count = local.has_route53 ? 1 : 0

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name            = "clk._domainkey.${var.domain_name}"
  type            = "CNAME"
  ttl             = 60
  records         = ["dkim1.jh0ilzdugbnt.clerk.services"]
}

resource "aws_route53_record" "clerk_dkim2" {
  count = local.has_route53 ? 1 : 0

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name            = "clk2._domainkey.${var.domain_name}"
  type            = "CNAME"
  ttl             = 60
  records         = ["dkim2.jh0ilzdugbnt.clerk.services"]
}

resource "aws_route53_record" "clerk_mail" {
  count = local.has_route53 ? 1 : 0

  allow_overwrite = true
  zone_id         = var.route53_zone_id
  name            = "clkmail.${var.domain_name}"
  type            = "CNAME"
  ttl             = 60
  records         = ["mail.jh0ilzdugbnt.clerk.services"]
}
