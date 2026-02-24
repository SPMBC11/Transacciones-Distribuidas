#!/bin/bash

# Script para iniciar el sistema de transferencias interbancarias
# Autor: Sistema Bancario
# Fecha: 2026-02-23

set -e  # Salir si hay algún error

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║   Sistema de Transferencias Interbancarias              ║"
echo "║   PostgreSQL (Nacional) + MySQL (Internacional)         ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Verificar si Docker está instalado
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Error: Docker no está instalado${NC}"
    exit 1
fi

# Verificar si Docker Compose está disponible
if ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Error: Docker Compose no está disponible${NC}"
    exit 1
fi

echo -e "${YELLOW}🐳 Deteniendo contenedores previos...${NC}"
sudo docker compose down 2>/dev/null || true

echo -e "${YELLOW}🚀 Iniciando servicios Docker...${NC}"
sudo docker compose up -d --build

echo -e "${YELLOW}⏳ Esperando a que los servicios estén listos...${NC}"

# Esperar a que PostgreSQL esté listo
echo -n "   Banco Nacional (PostgreSQL): "
for i in {1..30}; do
    if sudo docker exec banco_nacional_db pg_isready -U admin -d banco_nacional &> /dev/null; then
        echo -e "${GREEN}✓ Listo${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Esperar a que MySQL esté listo
echo -n "   Banco Internacional (MySQL): "
for i in {1..30}; do
    if sudo docker exec banco_internacional_db mysqladmin ping -h localhost -uroot -padmin 2>/dev/null | grep -q "mysqld is alive"; then
        echo -e "${GREEN}✓ Listo${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Esperar a que Spring Boot esté listo
echo -n "   Backend Spring Boot: "
for i in {1..60}; do
    if curl -s http://localhost:8080/api/cuentas/nacional/BN-001 &> /dev/null; then
        echo -e "${GREEN}✓ Listo${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""
echo -e "${GREEN}✅ Todos los servicios están corriendo!${NC}"
echo ""
echo -e "${BLUE}📊 Estado de los contenedores:${NC}"
sudo docker compose ps

echo ""
echo -e "${BLUE}🌐 Accediendo a la interfaz web...${NC}"
echo ""

# Obtener la ruta absoluta del index.html
INDEX_PATH="$(pwd)/index.html"

# Intentar abrir el navegador predeterminado
if command -v xdg-open &> /dev/null; then
    xdg-open "$INDEX_PATH" &
    echo -e "${GREEN}✓ Interfaz web abierta en el navegador${NC}"
elif command -v gnome-open &> /dev/null; then
    gnome-open "$INDEX_PATH" &
    echo -e "${GREEN}✓ Interfaz web abierta en el navegador${NC}"
elif command -v firefox &> /dev/null; then
    firefox "$INDEX_PATH" &
    echo -e "${GREEN}✓ Interfaz web abierta en Firefox${NC}"
elif command -v google-chrome &> /dev/null; then
    google-chrome "$INDEX_PATH" &
    echo -e "${GREEN}✓ Interfaz web abierta en Google Chrome${NC}"
else
    echo -e "${YELLOW}⚠️  No se pudo detectar el navegador predeterminado${NC}"
    echo -e "   Abre manualmente: file://$INDEX_PATH"
fi

echo ""
echo -e "${BLUE}📍 URLs disponibles:${NC}"
echo -e "   🌐 Interfaz Web: file://$INDEX_PATH"
echo -e "   🔌 API Backend:  http://localhost:8080/api"
echo -e "   🐘 PostgreSQL:   localhost:5432 (admin/admin)"
echo -e "   🐬 MySQL:        localhost:3306 (root/admin)"
echo ""
echo -e "${BLUE}📖 Endpoints API:${NC}"
echo -e "   GET  /api/cuentas/nacional/{cuenta}"
echo -e "   GET  /api/cuentas/internacional/{cuenta}"
echo -e "   POST /api/transferencias/nacional-a-internacional"
echo -e "   POST /api/transferencias/internacional-a-nacional"
echo ""
echo -e "${YELLOW}💡 Comandos útiles:${NC}"
echo -e "   Ver logs:     sudo docker compose logs -f backend"
echo -e "   Detener todo: sudo docker compose down"
echo -e "   Reiniciar:    sudo docker compose restart"
echo ""
echo -e "${GREEN}🎉 Sistema listo para usar!${NC}"
echo ""
