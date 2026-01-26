resource "grafana_dashboard" "books_metrics" {
  config_json = jsonencode({
    title   = "Books API Metrics"
    uid     = "books-api-metrics"
    tags    = ["books", "api", "lambda"]
    timezone = "browser"
    
    panels = [
      {
        id    = 1
        title = "Total Books Count"
        type  = "stat"
        gridPos = { h = 8, w = 12, x = 0, y = 0 }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [{
          refId      = "A"
          namespace  = "AWS/Lambda"
          metricName = "Invocations"
          statistic  = "Sum"
        }]
      },
      {
        id    = 2
        title = "API Requests"
        type  = "timeseries"
        gridPos = { h = 8, w = 12, x = 12, y = 0 }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [{
          refId      = "A"
          namespace  = "AWS/ApiGateway"
          metricName = "Count"
          statistic  = "Sum"
        }]
      }
    ]
  })
}
