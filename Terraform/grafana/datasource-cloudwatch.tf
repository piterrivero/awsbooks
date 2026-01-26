resource "grafana_data_source" "cloudwatch" {
  type = "cloudwatch"
  name = "AWS CloudWatch"

  json_data_encoded = jsonencode({
    authType      = "keys"
    defaultRegion = "eu-central-1"
  })

  secure_json_data_encoded = jsonencode({
    accessKey = local.cloudwatch_credentials.grafanaUserAccessKey
    secretKey = local.cloudwatch_credentials.grafanaUserSecretKey
  })
}
