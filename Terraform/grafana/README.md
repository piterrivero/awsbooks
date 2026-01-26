# Grafana Cloud Dashboards con Terraform

## Configuración Inicial

1. **Crear token de API en Grafana Cloud:**
   - Ve a tu stack de Grafana Cloud
   - Navega a Administration → Service Accounts
   - Crea un nuevo Service Account con permisos de Editor
   - Genera un token de API

2. **Configurar variables en terraform.tfvars:**
   ```hcl
   grafana_url   = "https://tu-stack.grafana.net"
   grafana_token = "glsa_tu_token_aqui"
   ```

3. **Inicializar Terraform:**
   ```bash
   terraform init
   ```

4. **Aplicar configuración:**
   ```bash
   terraform plan
   terraform apply
   ```

## Exportar dashboards existentes

Para convertir un dashboard existente a Terraform:

```bash
# Obtén el JSON del dashboard desde Grafana UI
# Dashboard → Settings → JSON Model
# Copia el JSON y úsalo en config_json
```

## Estructura

- `main.tf` - Configuración del provider
- `variables.tf` - Variables de configuración
- `dashboard-*.tf` - Definiciones de dashboards
