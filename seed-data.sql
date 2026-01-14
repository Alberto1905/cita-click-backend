-- ============================================================================
-- SEED DATA - Datos Iniciales para Cita Click
-- ============================================================================
-- Propósito: Insertar catálogos y datos maestros necesarios para el funcionamiento
-- Ejecutar: Manualmente después de que Hibernate haya generado el esquema
-- Uso: psql -U postgres -h HOST -d DATABASE -f seed-data.sql
-- ============================================================================

-- Establecer schema
SET search_path TO ccdiad, public;

-- ============================================================================
-- 1. PLANES Y LÍMITES
-- ============================================================================
-- Eliminar planes existentes si se quiere reiniciar
-- DELETE FROM tbl_plan_limites;

-- Plan BÁSICO
INSERT INTO tbl_plan_limites (
    id,
    tipo_plan,
    max_usuarios,
    max_clientes,
    max_citas_mes,
    max_servicios,
    sms_whatsapp_habilitado,
    reportes_avanzados_habilitado,
    soporte_prioritario
) VALUES (
    gen_random_uuid(),
    'BASICO',
    2,              -- 2 usuarios (dueño + 1 empleado)
    50,             -- 50 clientes
    100,            -- 100 citas/mes
    10,             -- 10 servicios
    false,          -- Sin WhatsApp/SMS
    false,          -- Sin reportes avanzados
    false           -- Sin soporte prioritario
)
ON CONFLICT (tipo_plan) DO UPDATE SET
    max_usuarios = EXCLUDED.max_usuarios,
    max_clientes = EXCLUDED.max_clientes,
    max_citas_mes = EXCLUDED.max_citas_mes,
    max_servicios = EXCLUDED.max_servicios,
    sms_whatsapp_habilitado = EXCLUDED.sms_whatsapp_habilitado,
    reportes_avanzados_habilitado = EXCLUDED.reportes_avanzados_habilitado,
    soporte_prioritario = EXCLUDED.soporte_prioritario;

-- Plan PROFESIONAL
INSERT INTO tbl_plan_limites (
    id,
    tipo_plan,
    max_usuarios,
    max_clientes,
    max_citas_mes,
    max_servicios,
    sms_whatsapp_habilitado,
    reportes_avanzados_habilitado,
    soporte_prioritario
) VALUES (
    gen_random_uuid(),
    'PROFESIONAL',
    5,              -- 5 usuarios
    300,            -- 300 clientes
    500,            -- 500 citas/mes
    30,             -- 30 servicios
    false,          -- Sin WhatsApp/SMS (Q2 2026)
    true,           -- CON reportes avanzados
    false           -- Sin soporte prioritario
)
ON CONFLICT (tipo_plan) DO UPDATE SET
    max_usuarios = EXCLUDED.max_usuarios,
    max_clientes = EXCLUDED.max_clientes,
    max_citas_mes = EXCLUDED.max_citas_mes,
    max_servicios = EXCLUDED.max_servicios,
    sms_whatsapp_habilitado = EXCLUDED.sms_whatsapp_habilitado,
    reportes_avanzados_habilitado = EXCLUDED.reportes_avanzados_habilitado,
    soporte_prioritario = EXCLUDED.soporte_prioritario;

-- Plan PREMIUM
INSERT INTO tbl_plan_limites (
    id,
    tipo_plan,
    max_usuarios,
    max_clientes,
    max_citas_mes,
    max_servicios,
    sms_whatsapp_habilitado,
    reportes_avanzados_habilitado,
    soporte_prioritario
) VALUES (
    gen_random_uuid(),
    'PREMIUM',
    999999,         -- Ilimitado (representado como número alto)
    999999,         -- Ilimitado
    999999,         -- Ilimitado
    999999,         -- Ilimitado
    false,          -- WhatsApp/SMS disponible Q2 2026 (aún no activo)
    true,           -- CON reportes avanzados
    true            -- CON soporte prioritario
)
ON CONFLICT (tipo_plan) DO UPDATE SET
    max_usuarios = EXCLUDED.max_usuarios,
    max_clientes = EXCLUDED.max_clientes,
    max_citas_mes = EXCLUDED.max_citas_mes,
    max_servicios = EXCLUDED.max_servicios,
    sms_whatsapp_habilitado = EXCLUDED.sms_whatsapp_habilitado,
    reportes_avanzados_habilitado = EXCLUDED.reportes_avanzados_habilitado,
    soporte_prioritario = EXCLUDED.soporte_prioritario;

-- ============================================================================
-- 2. MIGRACIÓN DE NEGOCIOS EXISTENTES (si aplica)
-- ============================================================================
-- Actualizar negocios con planes antiguos a los nuevos nombres
UPDATE tbl_negocios
SET plan = 'basico'
WHERE plan IN ('starter', 'STARTER') OR plan IS NULL OR plan = '';

UPDATE tbl_negocios
SET plan = 'profesional'
WHERE plan IN ('professional', 'PROFESSIONAL');

UPDATE tbl_negocios
SET plan = 'premium'
WHERE plan IN ('enterprise', 'ENTERPRISE');

-- Asegurarse de que todos los negocios tengan un plan asignado
UPDATE tbl_negocios
SET plan = 'basico'
WHERE plan NOT IN ('basico', 'profesional', 'premium');

-- ============================================================================
-- 3. VERIFICACIÓN
-- ============================================================================
-- Ver planes insertados
SELECT tipo_plan, max_usuarios, max_clientes, max_citas_mes, max_servicios
FROM tbl_plan_limites
ORDER BY
    CASE tipo_plan
        WHEN 'BASICO' THEN 1
        WHEN 'PROFESIONAL' THEN 2
        WHEN 'PREMIUM' THEN 3
    END;

-- ============================================================================
-- FIN DE SEED DATA
-- ============================================================================
