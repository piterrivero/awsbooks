#!/bin/bash

# Script para generar tráfico aleatorio a los endpoints GET de la API de Books
# Duración: 5 minutos

# Configurar proxy si es necesario
export HTTPS_PROXY=http://127.0.0.1:9000

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuración - URLs hardcodeadas
LOGIN_API_URL="https://owv2mgn23e.execute-api.eu-central-1.amazonaws.com/Prod"
API_URL="https://rn7cj8wbua.execute-api.eu-central-1.amazonaws.com/Prod"

# Credenciales
USERNAME="piter1234@gmail.com"
PASSWORD="12AWStest??"

echo -e "${BLUE}=== Obteniendo token de autenticación ===${NC}"

# Hacer login y obtener token
LOGIN_RESPONSE=$(curl -s -X POST "${LOGIN_API_URL}/login" \
    -H 'Content-Type: application/json' \
    -d @- << EOF
{"email":"${USERNAME}","password":"${PASSWORD}"}
EOF
)

# Extraer el token del response (intentar con jq primero, luego python3, luego sed)
if command -v jq &> /dev/null; then
    AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.idToken // empty')
elif command -v python3 &> /dev/null; then
    AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('idToken', ''))" 2>/dev/null)
else
    AUTH_TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"idToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
fi

if [ -z "$AUTH_TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener el token de autenticación${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Token obtenido exitosamente${NC}"
echo ""

# Duración en segundos (5 minutos)
DURATION=300
END_TIME=$(($(date +%s) + DURATION))

# Arrays de datos de prueba
TITLES=("1984" "Sapiens" "Dune" "Foundation" "Neuromancer" "Snow" "Ender" "Brave" "Fahrenheit" "Hobbit" "Lord" "Harry" "Game" "Hunger" "Divergent")
AUTHORS=("Orwell" "Harari" "Herbert" "Asimov" "Gibson" "Stephenson" "Card" "Huxley" "Bradbury" "Tolkien" "Rowling" "Martin" "Collins" "Roth" "King")
YEARS=("2020" "2021" "2022" "2023" "2024" "2019" "2018" "2017" "2016" "2015")
LANGUAGES=("English" "Spanish" "French" "German" "Italian")
FORMATS=("Physical" "Kindle" "Audiobook")

# Contador de requests
TOTAL_REQUESTS=0
SUCCESSFUL_REQUESTS=0
FAILED_REQUESTS=0

echo -e "${GREEN}=== Iniciando generación de tráfico ===${NC}"
echo -e "${BLUE}Duración: 5 minutos${NC}"
echo -e "${BLUE}API URL: $API_URL${NC}"
echo -e "${BLUE}Usuario: $USERNAME${NC}"
echo ""

# Función para hacer requests
make_request() {
    local endpoint=$1
    local description=$2
    
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))
    
    # 20% de probabilidad de hacer request sin token (para generar errores)
    if [ $((RANDOM % 5)) -eq 0 ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$endpoint" \
            -H "Content-Type: application/json")
        description="$description [SIN TOKEN]"
    else
        response=$(curl -s -w "\n%{http_code}" -X GET "$endpoint" \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            -H "Content-Type: application/json")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        SUCCESSFUL_REQUESTS=$((SUCCESSFUL_REQUESTS + 1))
        echo -e "${GREEN}✓${NC} $description [${http_code}]"
    else
        FAILED_REQUESTS=$((FAILED_REQUESTS + 1))
        echo -e "${YELLOW}✗${NC} $description [${http_code}]"
    fi
}

# Loop principal
while [ $(date +%s) -lt $END_TIME ]; do
    # Generar número aleatorio para seleccionar endpoint
    RANDOM_ENDPOINT=$((RANDOM % 13))
    
    case $RANDOM_ENDPOINT in
        0)
            # GetAllBooksFunction
            make_request "${API_URL}/books" "GetAllBooks"
            ;;
        1)
            # GetBooksCountFunction
            make_request "${API_URL}/books/count" "GetBooksCount"
            ;;
        2)
            # GetBooksCountByYearFunction
            RANDOM_YEAR=${YEARS[$RANDOM % ${#YEARS[@]}]}
            make_request "${API_URL}/books/count/year?year=${RANDOM_YEAR}" "GetBooksCountByYear: ${RANDOM_YEAR}"
            ;;
        3)
            # SearchBooksByTitleFunction
            RANDOM_TITLE=${TITLES[$RANDOM % ${#TITLES[@]}]}
            make_request "${API_URL}/books/search/title?title=${RANDOM_TITLE}" "SearchByTitle: ${RANDOM_TITLE}"
            ;;
        4)
            # SearchBooksByAuthorFunction
            RANDOM_AUTHOR=${AUTHORS[$RANDOM % ${#AUTHORS[@]}]}
            make_request "${API_URL}/books/search/author?author=${RANDOM_AUTHOR}" "SearchByAuthor: ${RANDOM_AUTHOR}"
            ;;
        5)
            # SearchBooksByReadYearFunction
            RANDOM_YEAR=${YEARS[$RANDOM % ${#YEARS[@]}]}
            make_request "${API_URL}/books/search/year?year=${RANDOM_YEAR}" "SearchByReadYear: ${RANDOM_YEAR}"
            ;;
        6)
            # GetBookByIdFunction (IDs del 1 al 50)
            RANDOM_ID=$((RANDOM % 50 + 1))
            make_request "${API_URL}/books/${RANDOM_ID}" "GetBookById: ${RANDOM_ID}"
            ;;
        7)
            # SearchBooksFunction - búsqueda por título
            RANDOM_TITLE=${TITLES[$RANDOM % ${#TITLES[@]}]}
            make_request "${API_URL}/search?title=${RANDOM_TITLE}" "SearchBooks (title): ${RANDOM_TITLE}"
            ;;
        8)
            # SearchBooksFunction - búsqueda combinada
            RANDOM_AUTHOR=${AUTHORS[$RANDOM % ${#AUTHORS[@]}]}
            RANDOM_YEAR=${YEARS[$RANDOM % ${#YEARS[@]}]}
            make_request "${API_URL}/search?author=${RANDOM_AUTHOR}&readYear=${RANDOM_YEAR}" "SearchBooks (author+year): ${RANDOM_AUTHOR}, ${RANDOM_YEAR}"
            ;;
        9)
            # GetBookById con ID inválido (muy alto) - genera 404
            INVALID_ID=$((RANDOM % 9000 + 1000))
            make_request "${API_URL}/books/${INVALID_ID}" "GetBookById: ${INVALID_ID} [ID INVALIDO]"
            ;;
        10)
            # Endpoint que no existe - genera 404
            make_request "${API_URL}/books/invalid/endpoint" "Endpoint inexistente"
            ;;
        11)
            # GetBookById con ID negativo - genera 400
            make_request "${API_URL}/books/-1" "GetBookById: -1 [ID NEGATIVO]"
            ;;
        12)
            # Búsqueda con parámetros vacíos - puede generar 400
            make_request "${API_URL}/search?title=&author=" "SearchBooks [PARAMS VACIOS]"
            ;;
    esac
    
    # Pausa aleatoria entre 0.5 y 2 segundos
    SLEEP_TIME=$(awk -v min=0.5 -v max=2 'BEGIN{srand(); print min+rand()*(max-min)}')
    sleep $SLEEP_TIME
    
    # Mostrar progreso cada 20 requests
    if [ $((TOTAL_REQUESTS % 20)) -eq 0 ]; then
        REMAINING=$((END_TIME - $(date +%s)))
        echo -e "${BLUE}--- Progreso: ${TOTAL_REQUESTS} requests | Tiempo restante: ${REMAINING}s ---${NC}"
    fi
done

# Resumen final
echo ""
echo -e "${GREEN}=== Generación de tráfico completada ===${NC}"
echo -e "${BLUE}Total de requests: ${TOTAL_REQUESTS}${NC}"
echo -e "${GREEN}Exitosos: ${SUCCESSFUL_REQUESTS}${NC}"
echo -e "${YELLOW}Fallidos: ${FAILED_REQUESTS}${NC}"
echo ""
