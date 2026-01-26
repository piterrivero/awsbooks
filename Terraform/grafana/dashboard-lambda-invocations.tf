resource "grafana_dashboard" "lambda_invocations" {
  config_json = jsonencode({
    title    = "Lambda Invocations"
    uid      = "lambda-invocations"
    tags     = ["lambda", "aws"]
    timezone = "browser"
    time = {
      from = "now-6h"
      to   = "now"
    }
    refresh = "30s"

    panels = [
      {
        id    = 1
        title = "Total Lambda Invocations"
        type  = "timeseries"
        gridPos = {
          h = 12
          w = 24
          x = 0
          y = 0
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId       = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace   = "AWS/Lambda"
            metricName  = "Invocations"
            statistic   = "Sum"
            dimensions  = {
              FunctionName = "*"
            }
            period      = "300"
            region      = "eu-central-1"
            matchExact  = true
          }
        ]
        fieldConfig = {
          defaults = {
            color = {
              mode = "palette-classic"
            }
            custom = {
              lineWidth       = 2
              fillOpacity     = 0
              spanNulls       = false
              showPoints      = "never"
              pointSize       = 5
              stacking = {
                mode  = "none"
                group = "A"
              }
            }
            unit = "short"
          }
        }
        options = {
          tooltip = {
            mode = "multi"
            sort = "desc"
          }
          legend = {
            displayMode = "table"
            placement   = "bottom"
            calcs       = ["lastNotNull", "sum"]
          }
        }
      },
      {
        id    = 2
        title = "API Gateway - Successful Requests (2xx)"
        type  = "timeseries"
        gridPos = {
          h = 10
          w = 12
          x = 0
          y = 12
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId       = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace   = "AWS/ApiGateway"
            metricName  = "Count"
            statistic   = "Sum"
            dimensions  = {
              ApiName = "*"
            }
            period      = "300"
            region      = "eu-central-1"
            matchExact  = true
          }
        ]
        fieldConfig = {
          defaults = {
            color = {
              mode = "fixed"
              fixedColor = "green"
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
        title = "API Gateway - Failed Requests by Status Code"
        type  = "timeseries"
        gridPos = {
          h = 10
          w = 12
          x = 12
          y = 12
        }
        datasource = {
          type = "cloudwatch"
          uid  = grafana_data_source.cloudwatch.uid
        }
        targets = [
          {
            refId       = "A"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace   = "AWS/ApiGateway"
            metricName  = "4XXError"
            statistic   = "Sum"
            dimensions  = {
              ApiName = "*"
            }
            period      = "300"
            region      = "eu-central-1"
            matchExact  = true
          },
          {
            refId       = "B"
            datasource = {
              type = "cloudwatch"
              uid  = grafana_data_source.cloudwatch.uid
            }
            namespace   = "AWS/ApiGateway"
            metricName  = "5XXError"
            statistic   = "Sum"
            dimensions  = {
              ApiName = "*"
            }
            period      = "300"
            region      = "eu-central-1"
            matchExact  = true
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
      }
    ]
  })
}
