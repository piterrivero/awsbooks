data "aws_secretsmanager_secret_version" "cloudwatch_creds" {
  secret_id = "grafana/cloudwatch-credentials"
}

data "aws_secretsmanager_secret_version" "grafana_creds" {
  secret_id = "grafana/credentials"
}

locals {
  cloudwatch_credentials = jsondecode(data.aws_secretsmanager_secret_version.cloudwatch_creds.secret_string)
  grafana_credentials    = jsondecode(data.aws_secretsmanager_secret_version.grafana_creds.secret_string)
}
