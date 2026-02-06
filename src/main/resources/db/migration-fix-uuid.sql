-- ============================================================================
-- MIGRACIÓN: Actualizar servicio_id de VARCHAR a UUID en tbl_cita_servicios
-- ============================================================================

-- Paso 1: Eliminar la restricción de clave foránea existente
ALTER TABLE ccdiad.tbl_cita_servicios
DROP CONSTRAINT IF EXISTS fk_cita_servicio_servicio;

-- Paso 2: Cambiar el tipo de columna de VARCHAR a UUID
ALTER TABLE ccdiad.tbl_cita_servicios
ALTER COLUMN servicio_id TYPE UUID USING servicio_id::uuid;

-- Paso 3: Recrear la restricción de clave foránea
ALTER TABLE ccdiad.tbl_cita_servicios
ADD CONSTRAINT fk_cita_servicio_servicio
FOREIGN KEY (servicio_id) REFERENCES ccdiad.tbl_servicios(id);

-- ============================================================================
-- MIGRACIÓN: Actualizar cliente_id de VARCHAR a UUID (si es necesario)
-- ============================================================================

-- Verificar si tbl_citas necesita actualización
-- ALTER TABLE ccdiad.tbl_citas
-- ALTER COLUMN cliente_id TYPE UUID USING cliente_id::uuid;

-- ALTER TABLE ccdiad.tbl_citas
-- ALTER COLUMN servicio_id TYPE UUID USING servicio_id::uuid;
