resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "books-lambda-high-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "Alerta cuando hay más de 5 errores en Lambda"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = "*"
  }

  # alarm_actions = [aws_sns_topic.alerts.arn]  # Descomentar cuando tengas SNS
}

resource "aws_cloudwatch_metric_alarm" "api_latency" {
  alarm_name          = "books-api-high-latency"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "Latency"
  namespace           = "AWS/ApiGateway"
  period              = 300
  statistic           = "Average"
  threshold           = 3000
  alarm_description   = "Alerta cuando la latencia supera 3 segundos"
  treat_missing_data  = "notBreaching"

  # alarm_actions = [aws_sns_topic.alerts.arn]  # Descomentar cuando tengas SNS
}

resource "aws_cloudwatch_metric_alarm" "api_5xx_errors" {
  alarm_name          = "books-api-5xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "5XXError"
  namespace           = "AWS/ApiGateway"
  period              = 300
  statistic           = "Sum"
  threshold           = 3
  alarm_description   = "Alerta cuando hay más de 3 errores 5xx"
  treat_missing_data  = "notBreaching"

  # alarm_actions = [aws_sns_topic.alerts.arn]  # Descomentar cuando tengas SNS
}

# Opcional: Topic SNS para recibir notificaciones por email
# resource "aws_sns_topic" "alerts" {
#   name = "books-api-alerts"
# }
#
# resource "aws_sns_topic_subscription" "email" {
#   topic_arn = aws_sns_topic.alerts.arn
#   protocol  = "email"
#   endpoint  = "tu-email@ejemplo.com"
# }
