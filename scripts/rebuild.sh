#!/bin/bash

# ============================================
# SCRIPT PARA RECONSTRUIR IMAGEN SIN CACHE
# ============================================

echo "🔨 Reconstruyendo imagen Docker sin caché..."

# Detener contenedores
echo "🛑 Deteniendo contenedores..."
docker-compose down

# Reconstruir sin caché
echo "🏗️  Reconstruyendo..."
docker-compose build --no-cache

# Iniciar servicios
echo "🚀 Iniciando servicios..."
docker-compose up -d

echo ""
echo "✅ Reconstrucción completada"
echo ""
echo "📊 Ver logs:"
echo "   docker-compose logs -f backend"
echo ""
