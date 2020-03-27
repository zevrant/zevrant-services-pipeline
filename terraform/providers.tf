#
# http://www.terraform.io/docs/backends/config.html
#
# S3 bucket to store infrastructure state
#
terraform {
  backend "s3" {
    bucket = "terraform.zevrant-terraform"
    key    = "tfstate/"
    region = "us-east-1"
  }

  required_version = "0.12.9"
}

#
# http://www.terraform.io/docs/providers/aws/index.html
#
# AWS provider so terraform can talk to AWS
#
provider "aws" {
  # not listed as require in documentation but will be asked for it if not set
  region = "us-east-1"

  version = "~>2.21"
}

provider "template" {
  version = "~>2.1"
}