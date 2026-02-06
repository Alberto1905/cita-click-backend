-- ============================================================================
-- MIGRACIÓN: Actualizar IDs a UUID - Paso a Paso
-- ============================================================================

SET search_path TO ccdiad;

-- ============================================================================
-- PASO 1: Eliminar TODAS las restricciones de claves foráneas relevantes
-- ============================================================================

-- Obtener y eliminar todas las FK que involucran servicios
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname, conrelid::regclass AS table_name
        FROM pg_constraint
        WHERE connamespace = 'ccdiad'::regnamespace
        AND (confrelid = 'ccdiad.tbl_servicios'::regclass OR conrelid = 'ccdiad.tbl_servicios'::regclass)
        AND contype = 'f'
    ) LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I', r.table_name, r.conname);
    END LOOP;
END $$;

-- Obtener y eliminar todas las FK que involucran clientes
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname, conrelid::regclass AS table_name
        FROM pg_constraint
        WHERE connamespace = 'ccdiad'::regnamespace
        AND (confrelid = 'ccdiad.tbl_clientes'::regclass OR conrelid = 'ccdiad.tbl_clientes'::regclass)
        AND contype = 'f'
    ) LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I', r.table_name, r.conname);
    END LOOP;
END $$;

-- ============================================================================
-- PASO 2: Convertir columnas a UUID
-- ============================================================================

-- Servicios
ALTER TABLE tbl_servicios ALTER COLUMN id TYPE UUID USING id::uuid;
ALTER TABLE tbl_cita_servicios ALTER COLUMN servicio_id TYPE UUID USING servicio_id::uuid;
ALTER TABLE tbl_citas ALTER COLUMN servicio_id TYPE UUID USING servicio_id::uuid;

-- Clientes
ALTER TABLE tbl_clientes ALTER COLUMN id TYPE UUID USING id::uuid;
ALTER TABLE tbl_citas ALTER COLUMN cliente_id TYPE UUID USING cliente_id::uuid;

-- ============================================================================
-- PASO 3: Recrear restricciones de claves foráneas
-- ============================================================================

-- Servicios
ALTER TABLE tbl_cita_servicios
ADD CONSTRAINT fk_cita_servicio_servicio
FOREIGN KEY (servicio_id) REFERENCES tbl_servicios(id) ON DELETE CASCADE;

ALTER TABLE tbl_citas
ADD CONSTRAINT fk_cita_servicio
FOREIGN KEY (servicio_id) REFERENCES tbl_servicios(id) ON DELETE SET NULL;

-- Clientes
ALTER TABLE tbl_citas
ADD CONSTRAINT fk_cita_cliente
FOREIGN KEY (cliente_id) REFERENCES tbl_clientes(id) ON DELETE SET NULL;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================
\echo '=== VERIFICACIÓN DE TIPOS DE COLUMNAS ==='
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'ccdiad'
AND table_name IN ('tbl_servicios', 'tbl_clientes', 'tbl_citas', 'tbl_cita_servicios')
AND column_name IN ('id', 'servicio_id', 'cliente_id')
ORDER BY table_name, column_name;

\echo '=== VERIFICACIÓN DE FOREIGN KEYS ==='
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_schema = 'ccdiad'
AND (tc.table_name IN ('tbl_citas', 'tbl_cita_servicios') OR ccu.table_name IN ('tbl_servicios', 'tbl_clientes'))
ORDER BY tc.table_name, tc.constraint_name;
