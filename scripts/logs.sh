#!/bin/bash

# ============================================
# SCRIPT PARA VER LOGS
# ============================================

SERVICE=${1:-backend}

echo "📊 Mostrando logs de: $SERVICE"
echo "   (Ctrl+C para salir)"
echo ""

docker-compose logs -f $SERVICE
