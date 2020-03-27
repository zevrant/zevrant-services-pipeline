resource "aws_s3_bucket" "artifact_bucket" {
  bucket = "zevrant-artifact-store"
  acl    = "public-read"

  versioning {
    enabled = true
  }

  lifecycle_rule {
    id      = "artifacts"
    enabled = true

    tags = {
      "rule"      = "artifact-movement"
      "autoclean" = "true"
    }

    transition {
      days          = 30 
      storage_class = "STANDARD_IA"
    }

    noncurrent_version_expiration {
      days = 30
    }

  }
}

resource "aws_s3_bucket" "zevrant_resources" {
  bucket = "zevrant-resources"
  acl    = "private"

  versioning {
    enabled = true
  }

  lifecycle_rule {
    id      = "artifacts"
    enabled = true

    tags = {
      "rule"      = "artifact-movement"
      "autoclean" = "true"
    }

    noncurrent_version_expiration {
      days = 30
    }

  }
}