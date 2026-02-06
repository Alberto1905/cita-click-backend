-- ============================================================================
-- MIGRACIÓN: Agregar nuevos campos de funcionalidades a tbl_plan_limites
-- ============================================================================
-- Fecha: 21 de Enero 2026
-- Propósito: Agregar control granular de funcionalidades por plan
-- Ejecutar: psql -U postgres -h HOST -d DATABASE -f migration-add-plan-features.sql
-- ============================================================================

SET search_path TO ccdiad, public;

-- Agregar columna email_recordatorios_habilitado
ALTER TABLE tbl_plan_limites
ADD COLUMN IF NOT EXISTS email_recordatorios_habilitado BOOLEAN NOT NULL DEFAULT false;

-- Agregar columna personalizacion_email_habilitado
ALTER TABLE tbl_plan_limites
ADD COLUMN IF NOT EXISTS personalizacion_email_habilitado BOOLEAN NOT NULL DEFAULT false;

-- Actualizar valores para cada plan
UPDATE tbl_plan_limites
SET
    email_recordatorios_habilitado = CASE
        WHEN tipo_plan IN ('PROFESIONAL', 'PREMIUM') THEN true
        ELSE false
    END,
    personalizacion_email_habilitado = CASE
        WHEN tipo_plan = 'PREMIUM' THEN true
        ELSE false
    END,
    reportes_avanzados_habilitado = CASE
        WHEN tipo_plan = 'PREMIUM' THEN true
        ELSE false
    END;

-- Verificar cambios
SELECT
    tipo_plan,
    email_recordatorios_habilitado,
    sms_whatsapp_habilitado,
    reportes_avanzados_habilitado,
    personalizacion_email_habilitado,
    soporte_prioritario
FROM tbl_plan_limites
ORDER BY
    CASE tipo_plan
        WHEN 'BASICO' THEN 1
        WHEN 'PROFESIONAL' THEN 2
        WHEN 'PREMIUM' THEN 3
    END;

-- ============================================================================
-- RESULTADO ESPERADO:
-- ============================================================================
-- BASICO:       email=false, sms=false, reportes=false, personalizacion=false, soporte=false
-- PROFESIONAL:  email=true,  sms=false, reportes=false, personalizacion=false, soporte=false
-- PREMIUM:      email=true,  sms=false, reportes=true,  personalizacion=true,  soporte=true
-- ============================================================================
