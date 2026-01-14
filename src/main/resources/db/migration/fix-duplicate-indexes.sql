-- ============================================================================
-- SCRIPT DE MIGRACIÓN: Renombrar índices duplicados
-- ============================================================================
-- PROPÓSITO: Corregir nombres de índices duplicados entre tablas
-- FECHA: 2026-01-13
-- VERSIÓN: 1.0
--
-- PROBLEMA:
-- - PostgreSQL requiere nombres únicos de índices a nivel de schema
-- - Múltiples tablas usaban idx_usuario_id, idx_status, idx_created_at
-- - Esto generaba errores DDL al arrancar con ddl-auto: update
--
-- SOLUCIÓN:
-- - Renombrar índices con prefijo del nombre de tabla
-- - Eliminar índices antiguos duplicados si existen
-- ============================================================================

-- Usar el schema correcto
SET search_path TO ccdiad;

-- ============================================================================
-- 1. tbl_payments (Payment entity)
-- ============================================================================

-- Eliminar índices antiguos si existen (por si quedaron duplicados)
DROP INDEX IF EXISTS idx_usuario_id;
DROP INDEX IF EXISTS idx_status;
DROP INDEX IF EXISTS idx_created_at;
DROP INDEX IF EXISTS idx_cita_id;

-- Renombrar o crear nuevos índices con nombres únicos
DO $$
BEGIN
    -- idx_payments_usuario_id
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND tablename = 'tbl_payments' AND indexname = 'idx_usuario_id') THEN
        ALTER INDEX ccdiad.idx_usuario_id RENAME TO idx_payments_usuario_id;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_payments_usuario_id ON ccdiad.tbl_payments (usuario_id);
    END IF;

    -- idx_payments_cita_id
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_payments_cita_id') THEN
        CREATE INDEX idx_payments_cita_id ON ccdiad.tbl_payments (cita_id);
    END IF;

    -- idx_payments_status
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_payments_status') THEN
        CREATE INDEX idx_payments_status ON ccdiad.tbl_payments (status);
    END IF;

    -- idx_payments_created_at
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_payments_created_at') THEN
        CREATE INDEX idx_payments_created_at ON ccdiad.tbl_payments (created_at);
    END IF;
END $$;

-- ============================================================================
-- 2. tbl_stripe_subscriptions (StripeSubscription entity)
-- ============================================================================

DO $$
BEGIN
    -- idx_stripe_subscriptions_subscription_id
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND tablename = 'tbl_stripe_subscriptions' AND indexname = 'idx_subscription_id') THEN
        ALTER INDEX ccdiad.idx_subscription_id RENAME TO idx_stripe_subscriptions_subscription_id;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_stripe_subscriptions_subscription_id ON ccdiad.tbl_stripe_subscriptions (subscription_id);
    END IF;

    -- idx_stripe_subscriptions_usuario_id
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_stripe_subscriptions_usuario_id') THEN
        CREATE INDEX idx_stripe_subscriptions_usuario_id ON ccdiad.tbl_stripe_subscriptions (usuario_id);
    END IF;

    -- idx_stripe_subscriptions_status
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_stripe_subscriptions_status') THEN
        CREATE INDEX idx_stripe_subscriptions_status ON ccdiad.tbl_stripe_subscriptions (status);
    END IF;

    -- idx_stripe_subscriptions_period_end
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND tablename = 'tbl_stripe_subscriptions' AND indexname = 'idx_current_period_end') THEN
        ALTER INDEX ccdiad.idx_current_period_end RENAME TO idx_stripe_subscriptions_period_end;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_stripe_subscriptions_period_end ON ccdiad.tbl_stripe_subscriptions (current_period_end);
    END IF;
END $$;

-- ============================================================================
-- 3. tbl_notification_logs (NotificationLog entity)
-- ============================================================================

DO $$
BEGIN
    -- idx_notification_logs_provider_msg_id
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND tablename = 'tbl_notification_logs' AND indexname = 'idx_provider_message_id') THEN
        ALTER INDEX ccdiad.idx_provider_message_id RENAME TO idx_notification_logs_provider_msg_id;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_notification_logs_provider_msg_id ON ccdiad.tbl_notification_logs (provider_message_id);
    END IF;

    -- idx_notification_logs_usuario_id
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_notification_logs_usuario_id') THEN
        CREATE INDEX idx_notification_logs_usuario_id ON ccdiad.tbl_notification_logs (usuario_id);
    END IF;

    -- idx_notification_logs_channel
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND tablename = 'tbl_notification_logs' AND indexname = 'idx_channel') THEN
        ALTER INDEX ccdiad.idx_channel RENAME TO idx_notification_logs_channel;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_notification_logs_channel ON ccdiad.tbl_notification_logs (channel);
    END IF;

    -- idx_notification_logs_status
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_notification_logs_status') THEN
        CREATE INDEX idx_notification_logs_status ON ccdiad.tbl_notification_logs (status);
    END IF;

    -- idx_notification_logs_created_at
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_notification_logs_created_at') THEN
        CREATE INDEX idx_notification_logs_created_at ON ccdiad.tbl_notification_logs (created_at);
    END IF;
END $$;

-- ============================================================================
-- 4. tbl_registro_ips (RegistroIP entity)
-- ============================================================================

DO $$
BEGIN
    -- idx_registro_ips_ip_address
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND tablename = 'tbl_registro_ips' AND indexname = 'idx_ip_address') THEN
        ALTER INDEX ccdiad.idx_ip_address RENAME TO idx_registro_ips_ip_address;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_registro_ips_ip_address ON ccdiad.tbl_registro_ips (ip_address);
    END IF;

    -- idx_registro_ips_created_at
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'ccdiad' AND indexname = 'idx_registro_ips_created_at') THEN
        CREATE INDEX idx_registro_ips_created_at ON ccdiad.tbl_registro_ips (created_at);
    END IF;
END $$;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================

-- Listar todos los índices creados/renombrados
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'ccdiad'
  AND (
    indexname LIKE 'idx_payments_%' OR
    indexname LIKE 'idx_stripe_subscriptions_%' OR
    indexname LIKE 'idx_notification_logs_%' OR
    indexname LIKE 'idx_registro_ips_%'
  )
ORDER BY tablename, indexname;

-- ============================================================================
-- INSTRUCCIONES DE EJECUCIÓN
-- ============================================================================
-- 1. Hacer backup de la base de datos antes de ejecutar
-- 2. Conectarse a la BD: psql -h 34.29.36.27 -U postgres -d db-cita-click-dev
-- 3. Ejecutar: \i /ruta/a/fix-duplicate-indexes.sql
-- 4. Verificar resultados con la consulta final
-- 5. Arrancar el backend con DDL_AUTO=validate
-- ============================================================================
