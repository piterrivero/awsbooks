variable "aws_access_key" {
  description = "AWS Access Key para Terraform"
  type        = string
  sensitive   = true
}

variable "aws_secret_key" {
  description = "AWS Secret Key para Terraform"
  type        = string
  sensitive   = true
}
