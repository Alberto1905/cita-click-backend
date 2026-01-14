#!/bin/bash

# ============================================
# SCRIPT PARA DETENER SERVICIOS
# ============================================

echo "🛑 Deteniendo Sistema de Reservas..."

docker-compose down

echo ""
echo "✅ Servicios detenidos"
echo ""
echo "💾 Para eliminar también los volúmenes (datos de BD):"
echo "   docker-compose down -v"
echo ""
