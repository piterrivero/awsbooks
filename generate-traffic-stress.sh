#!/bin/bash

# Script para generar tráfico que dispare las alarmas de CloudWatch
# Alarmas a disparar:
# 1. Lambda errors: >5 errores en 5 minutos
# 2. API 5xx errors: >3 errores 5xx en 5 minutos
# 3. API latency: >3000ms promedio (difícil de forzar)

# Configurar proxy si es necesario
export HTTPS_PROXY=http://127.0.0.1:9000

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuración
LOGIN_API_URL="https://owv2mgn23e.execute-api.eu-central-1.amazonaws.com/Prod"
API_URL="https://rn7cj8wbua.execute-api.eu-central-1.amazonaws.com/Prod"
USERNAME="piter1234@gmail.com"
PASSWORD="12AWStest??"

echo -e "${BLUE}=== Script de Stress Test para Disparar Alarmas ===${NC}"
echo ""

# Obtener token
echo -e "${BLUE}Obteniendo token...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${LOGIN_API_URL}/login" \
    -H 'Content-Type: application/json' \
    -d @- << EOF
{"email":"${USERNAME}","password":"${PASSWORD}"}
EOF
)

if command -v jq &> /dev/null; then
    AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.idToken // empty')
elif command -v python3 &> /dev/null; then
    AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('idToken', ''))" 2>/dev/null)
else
    AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"idToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
fi

if [ -z "$AUTH_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener el token${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Token obtenido${NC}"
echo ""

# Contadores
TOTAL=0
ERRORS_401=0
ERRORS_404=0
ERRORS_5XX=0

echo -e "${YELLOW}=== FASE 1: Generar errores 401 (sin token) ===${NC}"
echo -e "${BLUE}Objetivo: >5 errores para disparar alarma Lambda${NC}"
echo ""

for i in {1..10}; do
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${API_URL}/books" \
        -H "Content-Type: application/json")
    TOTAL=$((TOTAL + 1))
    
    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        ERRORS_401=$((ERRORS_401 + 1))
        echo -e "${RED}✗${NC} Request $i: ${http_code} (Sin token)"
    else
        echo -e "${YELLOW}?${NC} Request $i: ${http_code}"
    fi
    sleep 0.5
done

echo ""
echo -e "${YELLOW}=== FASE 2: Generar errores 404 (recursos inexistentes) ===${NC}"
echo -e "${BLUE}Objetivo: Más errores Lambda${NC}"
echo ""

for i in {1..10}; do
    INVALID_ID=$((RANDOM % 9000 + 1000))
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${API_URL}/books/${INVALID_ID}" \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -H "Content-Type: application/json")
    TOTAL=$((TOTAL + 1))
    
    if [ "$http_code" = "404" ]; then
        ERRORS_404=$((ERRORS_404 + 1))
        echo -e "${RED}✗${NC} Request $i: ${http_code} (ID: ${INVALID_ID})"
    else
        echo -e "${YELLOW}?${NC} Request $i: ${http_code}"
    fi
    sleep 0.5
done

echo ""
echo -e "${YELLOW}=== FASE 3: Intentar generar errores 5xx ===${NC}"
echo -e "${BLUE}Objetivo: >3 errores 5xx${NC}"
echo -e "${BLUE}Nota: Esto depende de la implementación del API${NC}"
echo ""

# Intentar con payloads malformados o endpoints problemáticos
for i in {1..15}; do
    case $((i % 3)) in
        0)
            # Endpoint inexistente
            http_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${API_URL}/books/invalid/path/deep" \
                -H "Authorization: Bearer $AUTH_TOKEN")
            ;;
        1)
            # ID con formato extraño
            http_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${API_URL}/books/abc123xyz" \
                -H "Authorization: Bearer $AUTH_TOKEN")
            ;;
        2)
            # Parámetros problemáticos
            http_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${API_URL}/search?year=invalid&title=%00" \
                -H "Authorization: Bearer $AUTH_TOKEN")
            ;;
    esac
    
    TOTAL=$((TOTAL + 1))
    
    if [[ "$http_code" =~ ^5 ]]; then
        ERRORS_5XX=$((ERRORS_5XX + 1))
        echo -e "${RED}✗✗✗${NC} Request $i: ${http_code} (ERROR 5XX!)"
    elif [ "$http_code" = "404" ]; then
        ERRORS_404=$((ERRORS_404 + 1))
        echo -e "${RED}✗${NC} Request $i: ${http_code}"
    else
        echo -e "${YELLOW}?${NC} Request $i: ${http_code}"
    fi
    sleep 0.3
done

echo ""
echo -e "${GREEN}=== RESUMEN ===${NC}"
echo -e "${BLUE}Total requests: ${TOTAL}${NC}"
echo -e "${RED}Errores 401/403: ${ERRORS_401}${NC}"
echo -e "${RED}Errores 404: ${ERRORS_404}${NC}"
echo -e "${RED}Errores 5xx: ${ERRORS_5XX}${NC}"
echo ""
echo -e "${YELLOW}=== ESTADO DE ALARMAS ===${NC}"

TOTAL_ERRORS=$((ERRORS_401 + ERRORS_404 + ERRORS_5XX))

if [ $TOTAL_ERRORS -gt 5 ]; then
    echo -e "${RED}✓ Alarma Lambda Errors: DEBERÍA DISPARARSE (${TOTAL_ERRORS} errores > 5)${NC}"
else
    echo -e "${YELLOW}✗ Alarma Lambda Errors: No se disparará (${TOTAL_ERRORS} errores <= 5)${NC}"
fi

if [ $ERRORS_5XX -gt 3 ]; then
    echo -e "${RED}✓ Alarma API 5xx: DEBERÍA DISPARARSE (${ERRORS_5XX} errores > 3)${NC}"
else
    echo -e "${YELLOW}✗ Alarma API 5xx: No se disparará (${ERRORS_5XX} errores <= 3)${NC}"
fi

echo ""
echo -e "${BLUE}Espera 5-10 minutos y verifica las alarmas en CloudWatch${NC}"
