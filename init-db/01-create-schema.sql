-- ============================================
-- SCRIPT DE INICIALIZACIÓN DE BASE DE DATOS
-- Sistema de Reservas y Citas
-- ============================================

-- Crear schemas para diferentes ambientes
CREATE SCHEMA IF NOT EXISTS rdiad;  -- Development
CREATE SCHEMA IF NOT EXISTS rdiaq;  -- QA
CREATE SCHEMA IF NOT EXISTS rdiap;  -- Production

-- Comentarios en schemas
COMMENT ON SCHEMA rdiad IS 'Schema de desarrollo';
COMMENT ON SCHEMA rdiaq IS 'Schema de QA/Testing';
COMMENT ON SCHEMA rdiap IS 'Schema de producción';

-- Configurar search_path por defecto
ALTER DATABASE "reservas-dev" SET search_path TO rdiad, public;

-- Extensiones útiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- Para búsquedas de texto similares

COMMENT ON EXTENSION "uuid-ossp" IS 'Generación de UUIDs';
COMMENT ON EXTENSION "pg_trgm" IS 'Índices de texto para búsquedas similares';
