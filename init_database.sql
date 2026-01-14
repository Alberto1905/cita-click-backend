-- ============================================================================
-- Script de Inicialización de Base de Datos
-- ============================================================================
-- Propósito: Crear base de datos, usuario y schema para cita-click
-- Ejecutar como superusuario de PostgreSQL (postgres)
-- ============================================================================

-- Terminar conexiones existentes a la base de datos si existe
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'ccdiad' AND pid <> pg_backend_pid();

-- Eliminar base de datos si existe (CUIDADO: Elimina todos los datos)
DROP DATABASE IF EXISTS ccdiad;

-- Eliminar usuario si existe
DROP USER IF EXISTS ccdiad;

-- Crear usuario
CREATE USER ccdiad WITH PASSWORD '<c9eTj=TG4h%I)Q|}';

-- Crear base de datos
CREATE DATABASE ccdiad
    WITH OWNER = ccdiad
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Conectarse a la base de datos
\c ccdiad

-- Crear schema
CREATE SCHEMA IF NOT EXISTS ccdiad AUTHORIZATION ccdiad;

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE ccdiad TO ccdiad;
GRANT ALL PRIVILEGES ON SCHEMA ccdiad TO ccdiad;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ccdiad TO ccdiad;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ccdiad TO ccdiad;

-- Establecer search_path por defecto
ALTER DATABASE ccdiad SET search_path TO ccdiad, public;
ALTER USER ccdiad SET search_path TO ccdiad, public;

-- Verificar
\dt ccdiad.*

-- ============================================================================
-- LISTO: Base de datos inicializada
-- ============================================================================
-- Usuario: ccdiad
-- Password: <c9eTj=TG4h%I)Q|}
-- Database: ccdiad
-- Schema: ccdiad
-- ============================================================================
