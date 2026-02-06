-- ============================================================================
-- MIGRACIÓN COMPLETA: Actualizar IDs a UUID para Cliente y Servicio
-- ============================================================================

SET search_path TO ccdiad;

-- ============================================================================
-- PARTE 1: SERVICIOS
-- ============================================================================

-- Paso 1.1: Eliminar restricciones de claves foráneas que referencian tbl_servicios
ALTER TABLE tbl_cita_servicios DROP CONSTRAINT IF EXISTS fkrcfki67ny1lv38j5a7ujvd5tp;
ALTER TABLE tbl_cita_servicios DROP CONSTRAINT IF EXISTS fk_cita_servicio_servicio;
ALTER TABLE tbl_citas DROP CONSTRAINT IF EXISTS fk85abf8y6u15vl78jefx0csrt2;
ALTER TABLE tbl_citas DROP CONSTRAINT IF EXISTS fk_cita_servicio;

-- Paso 1.2: Cambiar tipo de columna en tbl_servicios
ALTER TABLE tbl_servicios ALTER COLUMN id TYPE UUID USING id::uuid;

-- Paso 1.3: Cambiar tipo de columnas que referencian servicios
ALTER TABLE tbl_cita_servicios ALTER COLUMN servicio_id TYPE UUID USING servicio_id::uuid;
ALTER TABLE tbl_citas ALTER COLUMN servicio_id TYPE UUID USING servicio_id::uuid;

-- Paso 1.4: Recrear restricciones de claves foráneas
ALTER TABLE tbl_cita_servicios
ADD CONSTRAINT fk_cita_servicio_servicio
FOREIGN KEY (servicio_id) REFERENCES tbl_servicios(id);

ALTER TABLE tbl_citas
ADD CONSTRAINT fk_cita_servicio
FOREIGN KEY (servicio_id) REFERENCES tbl_servicios(id);

-- ============================================================================
-- PARTE 2: CLIENTES
-- ============================================================================

-- Paso 2.1: Eliminar restricciones de claves foráneas que referencian tbl_clientes
ALTER TABLE tbl_citas DROP CONSTRAINT IF EXISTS fkc9m5td55e5qcv2hstumnpweu8;
ALTER TABLE tbl_citas DROP CONSTRAINT IF EXISTS fk_cita_cliente;

-- Paso 2.2: Cambiar tipo de columna en tbl_clientes
ALTER TABLE tbl_clientes ALTER COLUMN id TYPE UUID USING id::uuid;

-- Paso 2.3: Cambiar tipo de columnas que referencian clientes
ALTER TABLE tbl_citas ALTER COLUMN cliente_id TYPE UUID USING cliente_id::uuid;

-- Paso 2.4: Recrear restricciones de claves foráneas
ALTER TABLE tbl_citas
ADD CONSTRAINT fk_cita_cliente
FOREIGN KEY (cliente_id) REFERENCES tbl_clientes(id);

-- ============================================================================
-- VERIFICACIÓN
-- ============================================================================

-- Verificar tipos de columnas
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'ccdiad'
AND table_name IN ('tbl_servicios', 'tbl_clientes', 'tbl_citas', 'tbl_cita_servicios')
AND column_name IN ('id', 'servicio_id', 'cliente_id')
ORDER BY table_name, column_name;
