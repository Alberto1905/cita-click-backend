#!/bin/bash

# ============================================
# SCRIPT DE INICIO - DESARROLLO
# ============================================

echo "🚀 Iniciando Sistema de Reservas - Modo Desarrollo"

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

# Construir y levantar servicios
echo "🔨 Construyendo servicios..."
docker-compose up --build -d

# Esperar a que los servicios estén listos
echo "⏳ Esperando a que los servicios estén listos..."
sleep 10

# Mostrar logs
echo ""
echo "✅ Servicios iniciados:"
echo ""
docker-compose ps
echo ""
echo "📊 Ver logs en tiempo real:"
echo "   docker-compose logs -f backend"
echo ""
echo "🌐 Endpoints disponibles:"
echo "   Backend API:  http://localhost:8080/api"
echo "   PostgreSQL:   localhost:5432"
echo "   PgAdmin:      http://localhost:5050 (solo con --profile dev)"
echo ""
echo "🛑 Para detener los servicios:"
echo "   docker-compose down"
echo ""
