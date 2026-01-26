resource "grafana_dashboard" "books_metrics" {
  config_json = jsonencode({
    title    = "Books API Metrics"
    uid      = "books-api-metrics"
    tags     = ["books", "api", "lambda"]
    timezone = "browser"
    time = {
      from = "now-6h"
      to   = "now"
    }
    refresh = "30s"

    panels = [
      {
        id    = 1
        title = "API Gateway - Latency"
        type  = "timeseries"
        gridPos = {
          h = 10
          w = 12
          x = 0
          y = 0
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace  = "AWS/ApiGateway"
            metricName = "Latency"
            statistic  = "Average"
            dimensions = {
              ApiName = "*"
            }
            period     = "300"
            region     = "eu-central-1"
            matchExact = true
          }
        ]
        fieldConfig = {
          defaults = {
            color = {
              mode = "palette-classic"
            }
            custom = {
              lineWidth   = 2
              fillOpacity = 0
            }
            unit = "ms"
          }
        }
        options = {
          tooltip = {
            mode = "multi"
          }
          legend = {
            displayMode = "table"
            placement   = "bottom"
            calcs       = ["mean", "max"]
          }
        }
      },
      {
        id    = 2
        title = "Lambda Errors by Function"
        type  = "timeseries"
        gridPos = {
          h = 10
          w = 12
          x = 12
          y = 0
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace  = "AWS/Lambda"
            metricName = "Errors"
            statistic  = "Sum"
            dimensions = {
              FunctionName = "*"
            }
            period     = "300"
            region     = "eu-central-1"
            matchExact = true
          }
        ]
        fieldConfig = {
          defaults = {
            color = {
              mode = "palette-classic"
            }
            custom = {
              lineWidth   = 2
              fillOpacity = 0
            }
            unit = "short"
          }
        }
        options = {
          tooltip = {
            mode = "multi"
          }
          legend = {
            displayMode = "table"
            placement   = "bottom"
            calcs       = ["lastNotNull", "sum"]
          }
        }
      },
      {
        id    = 3
        title = "Lambda Duration by Function"
        type  = "timeseries"
        gridPos = {
          h = 10
          w = 12
          x = 0
          y = 10
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace  = "AWS/Lambda"
            metricName = "Duration"
            statistic  = "Average"
            dimensions = {
              FunctionName = "*"
            }
            period     = "300"
            region     = "eu-central-1"
            matchExact = true
          }
        ]
        fieldConfig = {
          defaults = {
            color = {
              mode = "palette-classic"
            }
            custom = {
              lineWidth   = 2
              fillOpacity = 0
            }
            unit = "ms"
          }
        }
        options = {
          tooltip = {
            mode = "multi"
          }
          legend = {
            displayMode = "table"
            placement   = "bottom"
            calcs       = ["mean", "max"]
          }
        }
      },
      {
        id    = 5
        title = "Top 5 Most Invoked Functions"
        type  = "timeseries"
        gridPos = {
          h = 10
          w = 12
          x = 12
          y = 10
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace  = "AWS/Lambda"
            metricName = "Invocations"
            statistic  = "Sum"
            dimensions = {
              FunctionName = "*"
            }
            period     = "300"
            region     = "eu-central-1"
            matchExact = true
          }
        ]
        fieldConfig = {
          defaults = {
            color = {
              mode = "palette-classic"
            }
            custom = {
              lineWidth   = 2
              fillOpacity = 0
            }
            unit = "short"
          }
        }
        options = {
          tooltip = {
            mode = "multi"
          }
          legend = {
            displayMode = "table"
            placement   = "bottom"
            calcs       = ["lastNotNull", "sum"]
            sortBy      = "Total"
            sortDesc    = true
          }
        }
      }
    ]
  })
}
