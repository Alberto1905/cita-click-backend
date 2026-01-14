#!/bin/bash

# ============================================
# SCRIPT DE INICIO - DESARROLLO CON PGADMIN
# ============================================

echo "🚀 Iniciando Sistema de Reservas - Modo Desarrollo (con PgAdmin)"

# Verificar que existe .env
if [ ! -f .env ]; then
    echo "❌ Error: Archivo .env no encontrado"
    echo "📝 Copia .env.example a .env y configura tus valores"
    echo "   cp .env.example .env"
    exit 1
fi

# Verificar Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Error: Docker no está instalado"
    echo "📥 Instala Docker desde: https://docs.docker.com/get-docker/"
    exit 1
fi

# Detener contenedores existentes
echo "🛑 Deteniendo contenedores existentes..."
docker-compose down

# Construir y levantar servicios incluyendo PgAdmin
echo "🔨 Construyendo servicios (con PgAdmin)..."
docker-compose --profile dev up --build -d

# Esperar a que los servicios estén listos
echo "⏳ Esperando a que los servicios estén listos..."
sleep 15

# Mostrar logs
echo ""
echo "✅ Servicios iniciados:"
echo ""
docker-compose ps
echo ""
echo "📊 Ver logs en tiempo real:"
echo "   docker-compose logs -f backend"
echo "   docker-compose logs -f postgres"
echo ""
echo "🌐 Endpoints disponibles:"
echo "   Backend API:  http://localhost:8080/api"
echo "   PostgreSQL:   localhost:5432"
echo "   PgAdmin:      http://localhost:5050"
echo ""
echo "🔑 Credenciales PgAdmin:"
echo "   Email:    admin@reservas.com"
echo "   Password: admin"
echo ""
echo "🔌 Conectar a PostgreSQL desde PgAdmin:"
echo "   Host:     postgres"
echo "   Port:     5432"
echo "   Database: reservas-dev"
echo "   Username: postgres"
echo "   Password: admin"
echo ""
echo "🛑 Para detener los servicios:"
echo "   docker-compose --profile dev down"
echo ""
