-- ============================================================================
-- SCRIPT DE DATOS DE PRUEBA PARA H2 - CITA CLICK
-- ============================================================================
-- Propósito: Generar datos realistas para capturas de pantalla (142 clientes)
-- Base de datos: H2 (modo PostgreSQL)
-- Usuario demo: demo@citaclick.mx / Demo1234!
-- ============================================================================

-- ============================================================================
-- 1. PLAN_LIMITES
-- ============================================================================
INSERT INTO tbl_plan_limites (id, tipo_plan, max_usuarios, max_clientes, max_citas_mes, max_servicios,
                               sms_whatsapp_habilitado, reportes_avanzados_habilitado, soporte_prioritario)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'BASICO', 1, 100, 50, 10, false, false, false),
  ('22222222-2222-2222-2222-222222222222', 'PROFESIONAL', 3, 500, 200, 30, true, true, false),
  ('33333333-3333-3333-3333-333333333333', 'PREMIUM', 10, -1, -1, -1, true, true, true);

-- ============================================================================
-- 2. NEGOCIO (Salón de belleza con 142 clientes)
-- ============================================================================
INSERT INTO tbl_negocios (id, nombre, descripcion, email, telefono, tipo,
                          direccion_calle, direccion_colonia, direccion_codigo_postal, direccion_estado,
                          estado_pago, plan, fecha_inicio_plan, fecha_proximo_cobro,
                          stripe_customer_id, fecha_registro, fecha_fin_prueba,
                          en_periodo_prueba, cuenta_activa,
                          notificacion_prueba_enviada, notificacion_vencimiento_enviada,
                          created_at, updated_at)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   'Estética Premium',
   'Salón de belleza y spa con servicios de primera calidad',
   'contacto@esteticapremium.mx',
   '+52 55 9876 5432',
   'salon',
   'Av. Universidad 1500',
   'Del Valle Centro',
   '03100',
   'Ciudad de México',
   'activo',
   'profesional',
   '2025-10-01 10:00:00',
   '2026-02-01 10:00:00',
   'cus_estetica_premium_001',
   '2025-09-25 09:00:00',
   NULL,
   false,
   true,
   true,
   false,
   '2025-09-25 09:00:00',
   '2026-01-16 14:00:00');

-- ============================================================================
-- 3. USUARIO (Owner/Admin del negocio)
-- ============================================================================
-- Password: Demo1234! (hashed con BCrypt)
INSERT INTO tbl_usuarios (id, nombre, apellido_paterno, apellido_materno, email, telefono,
                          password_hash, auth_provider, provider_id, image_url,
                          rol, activo, email_verificado, token_verificacion, token_verificacion_expira,
                          negocio_id, created_at, updated_at)
VALUES
  ('00000001-1111-1111-1111-111111111111',
   'Ana',
   'Martínez',
   'Silva',
   'demo@citaclick.mx',
   '+52 55 9876 5432',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'local',
   NULL,
   NULL,
   'OWNER',
   true,
   true,
   NULL,
   NULL,
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '2025-09-25 09:00:00',
   '2026-01-16 14:00:00');

-- ============================================================================
-- 4. SERVICIOS (15 servicios variados)
-- ============================================================================
INSERT INTO tbl_servicios (id, nombre, descripcion, duracion_minutos, precio, activo, color, negocio_id, created_at, updated_at)
VALUES
  ('s0000001-1111-1111-1111-111111111111', 'Corte de Cabello Dama', 'Corte de cabello profesional para dama', 45, 350.00, true, '#E91E63', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:00:00', '2026-01-16 14:00:00'),
  ('s0000002-1111-1111-1111-111111111111', 'Corte de Cabello Caballero', 'Corte de cabello profesional para caballero', 30, 250.00, true, '#2196F3', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:05:00', '2026-01-16 14:00:00'),
  ('s0000003-1111-1111-1111-111111111111', 'Tinte Completo', 'Aplicación de tinte en todo el cabello', 120, 850.00, true, '#9C27B0', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:10:00', '2026-01-16 14:00:00'),
  ('s0000004-1111-1111-1111-111111111111', 'Mechas/Rayitos', 'Aplicación de mechas o rayitos', 150, 1200.00, true, '#FF9800', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:15:00', '2026-01-16 14:00:00'),
  ('s0000005-1111-1111-1111-111111111111', 'Peinado', 'Peinado profesional para evento', 60, 450.00, true, '#F44336', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:20:00', '2026-01-16 14:00:00'),
  ('s0000006-1111-1111-1111-111111111111', 'Maquillaje', 'Maquillaje profesional', 60, 550.00, true, '#E91E63', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:25:00', '2026-01-16 14:00:00'),
  ('s0000007-1111-1111-1111-111111111111', 'Manicure', 'Servicio de manicure completo', 45, 250.00, true, '#4CAF50', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:30:00', '2026-01-16 14:00:00'),
  ('s0000008-1111-1111-1111-111111111111', 'Pedicure', 'Servicio de pedicure completo', 60, 300.00, true, '#00BCD4', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:35:00', '2026-01-16 14:00:00'),
  ('s0000009-1111-1111-1111-111111111111', 'Uñas Acrílicas', 'Aplicación de uñas acrílicas', 90, 500.00, true, '#FF5722', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:40:00', '2026-01-16 14:00:00'),
  ('s0000010-1111-1111-1111-111111111111', 'Uñas de Gel', 'Aplicación de uñas de gel', 90, 550.00, true, '#673AB7', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:45:00', '2026-01-16 14:00:00'),
  ('s0000011-1111-1111-1111-111111111111', 'Tratamiento Capilar', 'Tratamiento de hidratación profunda', 90, 650.00, true, '#009688', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:50:00', '2026-01-16 14:00:00'),
  ('s0000012-1111-1111-1111-111111111111', 'Keratina', 'Tratamiento de keratina brasileña', 180, 1500.00, true, '#795548', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 10:55:00', '2026-01-16 14:00:00'),
  ('s0000013-1111-1111-1111-111111111111', 'Depilación Facial', 'Depilación de rostro completo', 30, 200.00, true, '#FFC107', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 11:00:00', '2026-01-16 14:00:00'),
  ('s0000014-1111-1111-1111-111111111111', 'Extensiones', 'Aplicación de extensiones de cabello', 180, 2000.00, true, '#607D8B', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 11:05:00', '2026-01-16 14:00:00'),
  ('s0000015-1111-1111-1111-111111111111', 'Masaje Facial', 'Masaje facial relajante', 45, 400.00, true, '#8BC34A', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 11:10:00', '2026-01-16 14:00:00');

-- ============================================================================
-- 5. CLIENTES (142 clientes con datos realistas)
-- ============================================================================

INSERT INTO tbl_clientes (id, nombre, apellido_paterno, apellido_materno, email, telefono, activo, negocio_id, created_at, updated_at)
VALUES
    (c0000001-1111-1111-1111-111111111111, Claudia, García, Ortiz, claudiagarcia1@email.com, +52 55 4524 9158, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-14 16:12:00, 2026-01-16 14:00:00),
    (c0000002-1111-1111-1111-111111111111, Manuel, Delgado, Ruiz, manueldelgado2@email.com, +52 55 7491 7021, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-06 11:07:00, 2026-01-16 14:00:00),
    (c0000003-1111-1111-1111-111111111111, Rocío, Rojas, Hernández, rociorojas3@email.com, +52 55 3652 1858, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-13 09:18:00, 2026-01-16 14:00:00),
    (c0000004-1111-1111-1111-111111111111, Luis, Martínez, Rojas, luismartinez4@email.com, +52 55 2306 3606, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-17 13:11:00, 2026-01-16 14:00:00),
    (c0000005-1111-1111-1111-111111111111, Adriana, González, Álvarez, adrianagonzalez5@email.com, +52 55 5047 1082, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-27 15:18:00, 2026-01-16 14:00:00),
    (c0000006-1111-1111-1111-111111111111, Sergio, Castillo, Torres, sergiocastillo6@email.com, +52 55 7049 1496, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-28 13:21:00, 2026-01-16 14:00:00),
    (c0000007-1111-1111-1111-111111111111, Andrea, Ortiz, Castro, andreaortiz7@email.com, +52 55 7538 9443, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-24 12:29:00, 2026-01-16 14:00:00),
    (c0000008-1111-1111-1111-111111111111, Carlos, Reyes, Álvarez, carlosreyes8@email.com, +52 55 6411 2039, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-15 17:16:00, 2026-01-16 14:00:00),
    (c0000009-1111-1111-1111-111111111111, Gabriela, Díaz, Jiménez, gabrieladiaz9@email.com, +52 55 9048 2689, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-25 10:18:00, 2026-01-16 14:00:00),
    (c0000010-1111-1111-1111-111111111111, Alejandro, Medina, Gómez, alejandromedina10@email.com, +52 55 8251 3555, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-23 10:43:00, 2026-01-16 14:00:00),
    (c0000011-1111-1111-1111-111111111111, Rosa, Torres, Cruz, rosatorres11@email.com, +52 55 7435 6712, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-20 18:52:00, 2026-01-16 14:00:00),
    (c0000012-1111-1111-1111-111111111111, Jorge, González, Martínez, jorgegonzalez12@email.com, +52 55 7937 7891, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-03 12:58:00, 2026-01-16 14:00:00),
    (c0000013-1111-1111-1111-111111111111, Guadalupe, Mendoza, Medina, guadalupemendoza13@email.com, +52 55 2244 1846, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-20 13:24:00, 2026-01-16 14:00:00),
    (c0000014-1111-1111-1111-111111111111, Juan, Hernández, Medina, juanhernandez14@email.com, +52 55 6696 1831, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-02 16:01:00, 2026-01-16 14:00:00),
    (c0000015-1111-1111-1111-111111111111, Sofía, Morales, Vega, sofiamorales15@email.com, +52 55 8733 9175, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-05 20:02:00, 2026-01-16 14:00:00),
    (c0000016-1111-1111-1111-111111111111, Raúl, Silva, Sánchez, raulsilva16@email.com, +52 55 2666 9032, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-22 14:03:00, 2026-01-16 14:00:00),
    (c0000017-1111-1111-1111-111111111111, Adriana, Sánchez, Mendoza, adrianasanchez17@email.com, +52 55 4211 3007, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-25 12:49:00, 2026-01-16 14:00:00),
    (c0000018-1111-1111-1111-111111111111, Eduardo, García, Díaz, eduardogarcia18@email.com, +52 55 7487 7681, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-18 09:56:00, 2026-01-16 14:00:00),
    (c0000019-1111-1111-1111-111111111111, Mónica, Ortiz, Romero, monicaortiz19@email.com, +52 55 5629 3765, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-24 12:36:00, 2026-01-16 14:00:00),
    (c0000020-1111-1111-1111-111111111111, Felipe, Flores, Torres, felipeflores20@email.com, +52 55 6200 6154, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-17 14:24:00, 2026-01-16 14:00:00),
    (c0000021-1111-1111-1111-111111111111, Sandra, Rivera, Flores, sandrarivera21@email.com, +52 55 4156 2612, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-14 09:31:00, 2026-01-16 14:00:00),
    (c0000022-1111-1111-1111-111111111111, David, Rodríguez, Jiménez, davidrodriguez22@email.com, +52 55 2374 9833, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-14 16:24:00, 2026-01-16 14:00:00),
    (c0000023-1111-1111-1111-111111111111, Verónica, Gómez, Martínez, veronicagomez23@email.com, +52 55 5552 2377, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-26 15:25:00, 2026-01-16 14:00:00),
    (c0000024-1111-1111-1111-111111111111, Roberto, Castillo, Reyes, robertocastillo24@email.com, +52 55 8754 6552, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-24 11:26:00, 2026-01-16 14:00:00),
    (c0000025-1111-1111-1111-111111111111, Sofía, Romero, Aguilar, sofiaromero25@email.com, +52 55 7047 4925, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-07 17:19:00, 2026-01-16 14:00:00),
    (c0000026-1111-1111-1111-111111111111, Mario, Torres, Ramos, mariotorres26@email.com, +52 55 8355 2236, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-22 20:08:00, 2026-01-16 14:00:00),
    (c0000027-1111-1111-1111-111111111111, Rosa, Ramírez, García, rosaramirez27@email.com, +52 55 6317 2241, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-21 14:54:00, 2026-01-16 14:00:00),
    (c0000028-1111-1111-1111-111111111111, Javier, Ramos, Torres, javierramos28@email.com, +52 55 4313 1356, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-19 11:53:00, 2026-01-16 14:00:00),
    (c0000029-1111-1111-1111-111111111111, Rocío, Rivera, Reyes, rociorivera29@email.com, +52 55 5498 4003, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-09 13:08:00, 2026-01-16 14:00:00),
    (c0000030-1111-1111-1111-111111111111, Sergio, Romero, Torres, sergioromero30@email.com, +52 55 2469 1820, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-14 18:10:00, 2026-01-16 14:00:00),
    (c0000031-1111-1111-1111-111111111111, Mónica, Ruiz, Aguilar, monicaruiz31@email.com, +52 55 6583 3570, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-04 14:02:00, 2026-01-16 14:00:00),
    (c0000032-1111-1111-1111-111111111111, Felipe, Jiménez, Romero, felipejimenez32@email.com, +52 55 6006 1197, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-20 12:35:00, 2026-01-16 14:00:00),
    (c0000033-1111-1111-1111-111111111111, Silvia, Flores, Sánchez, silviaflores33@email.com, +52 55 8711 1731, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-16 19:43:00, 2026-01-16 14:00:00),
    (c0000034-1111-1111-1111-111111111111, Miguel, Sánchez, Rodríguez, miguelsanchez34@email.com, +52 55 5506 8381, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-18 14:10:00, 2026-01-16 14:00:00),
    (c0000035-1111-1111-1111-111111111111, Adriana, Romero, Vega, adrianaromero35@email.com, +52 55 6894 8586, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-25 10:33:00, 2026-01-16 14:00:00),
    (c0000036-1111-1111-1111-111111111111, Felipe, Romero, Ramírez, feliperomero36@email.com, +52 55 3369 5246, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-26 10:40:00, 2026-01-16 14:00:00),
    (c0000037-1111-1111-1111-111111111111, Mariana, Castillo, Flores, marianacastillo37@email.com, +52 55 1090 1448, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-18 13:56:00, 2026-01-16 14:00:00),
    (c0000038-1111-1111-1111-111111111111, José, Medina, Ortiz, josemedina38@email.com, +52 55 7465 5153, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-26 16:54:00, 2026-01-16 14:00:00),
    (c0000039-1111-1111-1111-111111111111, Rocío, Gómez, Delgado, rociogomez39@email.com, +52 55 6172 8600, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-20 18:26:00, 2026-01-16 14:00:00),
    (c0000040-1111-1111-1111-111111111111, Fernando, Vargas, Martínez, fernandovargas40@email.com, +52 55 3689 3078, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-02 16:42:00, 2026-01-16 14:00:00),
    (c0000041-1111-1111-1111-111111111111, Verónica, Gómez, Álvarez, veronicagomez41@email.com, +52 55 4070 6248, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-12 10:54:00, 2026-01-16 14:00:00),
    (c0000042-1111-1111-1111-111111111111, Arturo, Moreno, Castillo, arturomoreno42@email.com, +52 55 8215 6267, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-25 13:41:00, 2026-01-16 14:00:00),
    (c0000043-1111-1111-1111-111111111111, Sandra, Jiménez, Rodríguez, sandrajimenez43@email.com, +52 55 7498 3680, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-24 12:18:00, 2026-01-16 14:00:00),
    (c0000044-1111-1111-1111-111111111111, Raúl, Torres, González, raultorres44@email.com, +52 55 1617 5433, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-18 16:36:00, 2026-01-16 14:00:00),
    (c0000045-1111-1111-1111-111111111111, Beatriz, Cruz, Díaz, beatrizcruz45@email.com, +52 55 2767 3717, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-10 19:33:00, 2026-01-16 14:00:00),
    (c0000046-1111-1111-1111-111111111111, Fernando, Morales, Torres, fernandomorales46@email.com, +52 55 1692 7365, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-01 14:46:00, 2026-01-16 14:00:00),
    (c0000047-1111-1111-1111-111111111111, Claudia, Mendoza, Gutiérrez, claudiamendoza47@email.com, +52 55 5019 5314, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-15 13:53:00, 2026-01-16 14:00:00),
    (c0000048-1111-1111-1111-111111111111, Jorge, Ruiz, Medina, jorgeruiz48@email.com, +52 55 1918 5812, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-20 15:17:00, 2026-01-16 14:00:00),
    (c0000049-1111-1111-1111-111111111111, Mariana, González, Ramírez, marianagonzalez49@email.com, +52 55 2151 1043, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-18 14:45:00, 2026-01-16 14:00:00),
    (c0000050-1111-1111-1111-111111111111, Felipe, Gutiérrez, Morales, felipegutierrez50@email.com, +52 55 5133 9403, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-27 09:32:00, 2026-01-16 14:00:00),
    (c0000051-1111-1111-1111-111111111111, Laura, Cruz, Flores, lauracruz51@email.com, +52 55 3462 4274, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-13 14:42:00, 2026-01-16 14:00:00),
    (c0000052-1111-1111-1111-111111111111, Jorge, Ortiz, Rivera, jorgeortiz52@email.com, +52 55 3524 5586, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-10 19:00:00, 2026-01-16 14:00:00),
    (c0000053-1111-1111-1111-111111111111, Beatriz, García, Castillo, beatrizgarcia53@email.com, +52 55 5971 2958, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-04 14:22:00, 2026-01-16 14:00:00),
    (c0000054-1111-1111-1111-111111111111, Guillermo, Díaz, González, guillermodiaz54@email.com, +52 55 7385 6072, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-01 18:21:00, 2026-01-16 14:00:00),
    (c0000055-1111-1111-1111-111111111111, Rosa, Martínez, Ortiz, rosamartinez55@email.com, +52 55 5158 5464, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-15 09:34:00, 2026-01-16 14:00:00),
    (c0000056-1111-1111-1111-111111111111, Andrés, Medina, Rojas, andresmedina56@email.com, +52 55 1857 4099, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-02 14:19:00, 2026-01-16 14:00:00),
    (c0000057-1111-1111-1111-111111111111, Elena, Rodríguez, Gómez, elenarodriguez57@email.com, +52 55 1733 3795, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-09 20:20:00, 2026-01-16 14:00:00),
    (c0000058-1111-1111-1111-111111111111, Roberto, Rojas, Sánchez, robertorojas58@email.com, +52 55 9343 2638, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-18 13:22:00, 2026-01-16 14:00:00),
    (c0000059-1111-1111-1111-111111111111, Laura, Gutiérrez, Rivera, lauragutierrez59@email.com, +52 55 3363 6287, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-13 16:28:00, 2026-01-16 14:00:00),
    (c0000060-1111-1111-1111-111111111111, Alejandro, Torres, Díaz, alejandrotorres60@email.com, +52 55 2527 9114, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-09 12:06:00, 2026-01-16 14:00:00),
    (c0000061-1111-1111-1111-111111111111, Patricia, Mendoza, Sánchez, patriciamendoza61@email.com, +52 55 5274 8700, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-05 10:14:00, 2026-01-16 14:00:00),
    (c0000062-1111-1111-1111-111111111111, Francisco, Rojas, Delgado, franciscorojas62@email.com, +52 55 4437 4176, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-18 10:55:00, 2026-01-16 14:00:00),
    (c0000063-1111-1111-1111-111111111111, Mónica, Ramírez, Hernández, monicaramirez63@email.com, +52 55 1016 4607, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-15 11:40:00, 2026-01-16 14:00:00),
    (c0000064-1111-1111-1111-111111111111, Roberto, Castillo, Mendoza, robertocastillo64@email.com, +52 55 4628 3260, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-22 09:27:00, 2026-01-16 14:00:00),
    (c0000065-1111-1111-1111-111111111111, Silvia, García, Ortiz, silviagarcia65@email.com, +52 55 3635 8503, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-27 12:29:00, 2026-01-16 14:00:00),
    (c0000066-1111-1111-1111-111111111111, Eduardo, Moreno, Jiménez, eduardomoreno66@email.com, +52 55 2576 1755, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-21 12:58:00, 2026-01-16 14:00:00),
    (c0000067-1111-1111-1111-111111111111, Guadalupe, Rodríguez, González, guadaluperodriguez67@email.com, +52 55 6013 5621, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-14 17:10:00, 2026-01-16 14:00:00),
    (c0000068-1111-1111-1111-111111111111, Jorge, Castro, García, jorgecastro68@email.com, +52 55 5547 6758, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-02 10:22:00, 2026-01-16 14:00:00),
    (c0000069-1111-1111-1111-111111111111, Laura, Martínez, Vega, lauramartinez69@email.com, +52 55 4127 2430, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-26 17:49:00, 2026-01-16 14:00:00),
    (c0000070-1111-1111-1111-111111111111, Ángel, Rojas, Castro, angelrojas70@email.com, +52 55 8216 9702, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-27 12:43:00, 2026-01-16 14:00:00),
    (c0000071-1111-1111-1111-111111111111, Rosa, Pérez, Mendoza, rosaperez71@email.com, +52 55 3933 1088, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-14 13:53:00, 2026-01-16 14:00:00),
    (c0000072-1111-1111-1111-111111111111, Arturo, Martínez, Morales, arturomartinez72@email.com, +52 55 5602 2206, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-07 11:25:00, 2026-01-16 14:00:00),
    (c0000073-1111-1111-1111-111111111111, Laura, Torres, Delgado, lauratorres73@email.com, +52 55 8933 5854, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-24 12:56:00, 2026-01-16 14:00:00),
    (c0000074-1111-1111-1111-111111111111, Enrique, Gómez, Martínez, enriquegomez74@email.com, +52 55 6570 4375, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-11 16:23:00, 2026-01-16 14:00:00),
    (c0000075-1111-1111-1111-111111111111, Ana, Jiménez, Reyes, anajimenez75@email.com, +52 55 5583 5156, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-25 11:47:00, 2026-01-16 14:00:00),
    (c0000076-1111-1111-1111-111111111111, Jesús, Ramírez, Gutiérrez, jesusramirez76@email.com, +52 55 8588 4575, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-07 17:23:00, 2026-01-16 14:00:00),
    (c0000077-1111-1111-1111-111111111111, Sandra, Torres, Romero, sandratorres77@email.com, +52 55 2684 4354, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-22 14:20:00, 2026-01-16 14:00:00),
    (c0000078-1111-1111-1111-111111111111, Javier, López, Rojas, javierlopez78@email.com, +52 55 1559 9767, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-26 20:39:00, 2026-01-16 14:00:00),
    (c0000079-1111-1111-1111-111111111111, Diana, Mendoza, Jiménez, dianamendoza79@email.com, +52 55 6911 5561, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-15 19:34:00, 2026-01-16 14:00:00),
    (c0000080-1111-1111-1111-111111111111, Javier, Álvarez, Flores, javieralvarez80@email.com, +52 55 2759 1576, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-05 19:03:00, 2026-01-16 14:00:00),
    (c0000081-1111-1111-1111-111111111111, Gabriela, Cruz, Rojas, gabrielacruz81@email.com, +52 55 6781 5431, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-23 17:08:00, 2026-01-16 14:00:00),
    (c0000082-1111-1111-1111-111111111111, Sergio, Gutiérrez, Ruiz, sergiogutierrez82@email.com, +52 55 9290 5707, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-23 09:40:00, 2026-01-16 14:00:00),
    (c0000083-1111-1111-1111-111111111111, Isabel, Álvarez, Mendoza, isabelalvarez83@email.com, +52 55 2052 3490, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-10 14:39:00, 2026-01-16 14:00:00),
    (c0000084-1111-1111-1111-111111111111, Alberto, Rodríguez, Vega, albertorodriguez84@email.com, +52 55 3949 8995, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-01 19:24:00, 2026-01-16 14:00:00),
    (c0000085-1111-1111-1111-111111111111, Valeria, Rivera, Moreno, valeriarivera85@email.com, +52 55 1093 8143, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-02 09:12:00, 2026-01-16 14:00:00),
    (c0000086-1111-1111-1111-111111111111, Sergio, Moreno, Flores, sergiomoreno86@email.com, +52 55 6821 6419, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-03 18:51:00, 2026-01-16 14:00:00),
    (c0000087-1111-1111-1111-111111111111, Paola, Castillo, Romero, paolacastillo87@email.com, +52 55 1977 5969, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-25 14:53:00, 2026-01-16 14:00:00),
    (c0000088-1111-1111-1111-111111111111, Rafael, Rivera, González, rafaelrivera88@email.com, +52 55 1364 2872, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-10 20:44:00, 2026-01-16 14:00:00),
    (c0000089-1111-1111-1111-111111111111, Paola, Delgado, González, paoladelgado89@email.com, +52 55 4701 9240, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-27 13:16:00, 2026-01-16 14:00:00),
    (c0000090-1111-1111-1111-111111111111, Francisco, Reyes, Ruiz, franciscoreyes90@email.com, +52 55 9978 5533, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-13 18:20:00, 2026-01-16 14:00:00),
    (c0000091-1111-1111-1111-111111111111, Mónica, Rivera, Reyes, monicarivera91@email.com, +52 55 4495 8151, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-15 16:04:00, 2026-01-16 14:00:00),
    (c0000092-1111-1111-1111-111111111111, Fernando, Rodríguez, Aguilar, fernandorodriguez92@email.com, +52 55 4048 3337, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-26 20:39:00, 2026-01-16 14:00:00),
    (c0000093-1111-1111-1111-111111111111, María, Castro, López, mariacastro93@email.com, +52 55 6708 1301, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-03 20:43:00, 2026-01-16 14:00:00),
    (c0000094-1111-1111-1111-111111111111, Luis, Pérez, Ortiz, luisperez94@email.com, +52 55 8164 8609, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-25 14:17:00, 2026-01-16 14:00:00),
    (c0000095-1111-1111-1111-111111111111, Gabriela, Castro, Silva, gabrielacastro95@email.com, +52 55 8631 4519, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-07 18:04:00, 2026-01-16 14:00:00),
    (c0000096-1111-1111-1111-111111111111, Ricardo, Rodríguez, Cruz, ricardorodriguez96@email.com, +52 55 6870 3669, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-06 17:36:00, 2026-01-16 14:00:00),
    (c0000097-1111-1111-1111-111111111111, Verónica, Ramos, Gómez, veronicaramos97@email.com, +52 55 1642 2298, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-23 14:50:00, 2026-01-16 14:00:00),
    (c0000098-1111-1111-1111-111111111111, Andrés, Medina, López, andresmedina98@email.com, +52 55 6981 2029, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-02 10:38:00, 2026-01-16 14:00:00),
    (c0000099-1111-1111-1111-111111111111, Rocío, Cruz, Ramírez, rociocruz99@email.com, +52 55 6674 6586, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-15 09:02:00, 2026-01-16 14:00:00),
    (c0000100-1111-1111-1111-111111111111, Felipe, Sánchez, Mendoza, felipesanchez100@email.com, +52 55 6177 1639, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-12 10:17:00, 2026-01-16 14:00:00),
    (c0000101-1111-1111-1111-111111111111, Patricia, Rodríguez, Cruz, patriciarodriguez101@email.com, +52 55 3917 9298, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-11 10:48:00, 2026-01-16 14:00:00),
    (c0000102-1111-1111-1111-111111111111, Enrique, Romero, Medina, enriqueromero102@email.com, +52 55 8847 4328, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-10 10:01:00, 2026-01-16 14:00:00),
    (c0000103-1111-1111-1111-111111111111, Gabriela, Ramos, Álvarez, gabrielaramos103@email.com, +52 55 7667 6674, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-20 19:00:00, 2026-01-16 14:00:00),
    (c0000104-1111-1111-1111-111111111111, Raúl, Rojas, Rojas, raulrojas104@email.com, +52 55 5823 6447, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-27 09:44:00, 2026-01-16 14:00:00),
    (c0000105-1111-1111-1111-111111111111, Rosa, Torres, Vega, rosatorres105@email.com, +52 55 4886 7154, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-09 20:18:00, 2026-01-16 14:00:00),
    (c0000106-1111-1111-1111-111111111111, Miguel, Sánchez, García, miguelsanchez106@email.com, +52 55 4966 6360, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-11 11:38:00, 2026-01-16 14:00:00),
    (c0000107-1111-1111-1111-111111111111, Daniela, Vega, Rivera, danielavega107@email.com, +52 55 6490 7514, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-26 20:07:00, 2026-01-16 14:00:00),
    (c0000108-1111-1111-1111-111111111111, Fernando, Ortiz, Ramírez, fernandoortiz108@email.com, +52 55 1555 2102, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-08 09:07:00, 2026-01-16 14:00:00),
    (c0000109-1111-1111-1111-111111111111, Carmen, Castillo, Reyes, carmencastillo109@email.com, +52 55 4962 6549, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-22 11:02:00, 2026-01-16 14:00:00),
    (c0000110-1111-1111-1111-111111111111, Rafael, Martínez, Castro, rafaelmartinez110@email.com, +52 55 4978 4742, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-18 18:26:00, 2026-01-16 14:00:00),
    (c0000111-1111-1111-1111-111111111111, Valeria, Cruz, Torres, valeriacruz111@email.com, +52 55 1697 9202, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-23 14:02:00, 2026-01-16 14:00:00),
    (c0000112-1111-1111-1111-111111111111, Juan, Álvarez, Vega, juanalvarez112@email.com, +52 55 4459 7332, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-20 15:38:00, 2026-01-16 14:00:00),
    (c0000113-1111-1111-1111-111111111111, Rosa, Aguilar, Aguilar, rosaaguilar113@email.com, +52 55 1020 1337, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-13 14:02:00, 2026-01-16 14:00:00),
    (c0000114-1111-1111-1111-111111111111, José, Gutiérrez, Ramírez, josegutierrez114@email.com, +52 55 5976 9055, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-14 09:25:00, 2026-01-16 14:00:00),
    (c0000115-1111-1111-1111-111111111111, Claudia, Silva, Delgado, claudiasilva115@email.com, +52 55 5336 6149, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-27 19:11:00, 2026-01-16 14:00:00),
    (c0000116-1111-1111-1111-111111111111, Alberto, Moreno, Morales, albertomoreno116@email.com, +52 55 8848 1684, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-11 15:13:00, 2026-01-16 14:00:00),
    (c0000117-1111-1111-1111-111111111111, Paola, Rodríguez, Álvarez, paolarodriguez117@email.com, +52 55 9624 8782, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-05 13:13:00, 2026-01-16 14:00:00),
    (c0000118-1111-1111-1111-111111111111, Víctor, Delgado, González, victordelgado118@email.com, +52 55 4264 4100, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-12 11:34:00, 2026-01-16 14:00:00),
    (c0000119-1111-1111-1111-111111111111, Mónica, Mendoza, López, monicamendoza119@email.com, +52 55 9434 5772, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-15 16:15:00, 2026-01-16 14:00:00),
    (c0000120-1111-1111-1111-111111111111, David, Díaz, Flores, daviddiaz120@email.com, +52 55 1755 4933, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-13 15:38:00, 2026-01-16 14:00:00),
    (c0000121-1111-1111-1111-111111111111, Sofía, Ramírez, Castillo, sofiaramirez121@email.com, +52 55 3775 9020, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-06 15:50:00, 2026-01-16 14:00:00),
    (c0000122-1111-1111-1111-111111111111, Guillermo, Ortiz, Vega, guillermoortiz122@email.com, +52 55 6950 5441, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-14 19:03:00, 2026-01-16 14:00:00),
    (c0000123-1111-1111-1111-111111111111, Diana, Reyes, Castillo, dianareyes123@email.com, +52 55 6320 5019, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-05 18:08:00, 2026-01-16 14:00:00),
    (c0000124-1111-1111-1111-111111111111, Ricardo, Cruz, Ramírez, ricardocruz124@email.com, +52 55 8693 5384, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-05 15:42:00, 2026-01-16 14:00:00),
    (c0000125-1111-1111-1111-111111111111, Silvia, Rodríguez, Silva, silviarodriguez125@email.com, +52 55 1343 6636, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-25 12:35:00, 2026-01-16 14:00:00),
    (c0000126-1111-1111-1111-111111111111, Ricardo, Rodríguez, Rodríguez, ricardorodriguez126@email.com, +52 55 6675 5167, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-06 18:44:00, 2026-01-16 14:00:00),
    (c0000127-1111-1111-1111-111111111111, Sandra, Morales, Mendoza, sandramorales127@email.com, +52 55 9220 6633, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-23 11:12:00, 2026-01-16 14:00:00),
    (c0000128-1111-1111-1111-111111111111, Raúl, Gómez, Cruz, raulgomez128@email.com, +52 55 5652 7774, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-05 14:45:00, 2026-01-16 14:00:00),
    (c0000129-1111-1111-1111-111111111111, Teresa, Ramírez, Ruiz, teresaramirez129@email.com, +52 55 7690 1163, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-27 14:09:00, 2026-01-16 14:00:00),
    (c0000130-1111-1111-1111-111111111111, Mario, García, Aguilar, mariogarcia130@email.com, +52 55 2315 9547, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-11 11:44:00, 2026-01-16 14:00:00),
    (c0000131-1111-1111-1111-111111111111, Carmen, Moreno, Rivera, carmenmoreno131@email.com, +52 55 7853 2259, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-05 16:13:00, 2026-01-16 14:00:00),
    (c0000132-1111-1111-1111-111111111111, Rafael, Rivera, Cruz, rafaelrivera132@email.com, +52 55 9979 8769, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-26 18:36:00, 2026-01-16 14:00:00),
    (c0000133-1111-1111-1111-111111111111, Daniela, Rodríguez, Reyes, danielarodriguez133@email.com, +52 55 8445 5173, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-09 14:42:00, 2026-01-16 14:00:00),
    (c0000134-1111-1111-1111-111111111111, Enrique, Pérez, Ortiz, enriqueperez134@email.com, +52 55 4054 9367, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-10-12 13:04:00, 2026-01-16 14:00:00),
    (c0000135-1111-1111-1111-111111111111, Andrea, Rodríguez, Vargas, andrearodriguez135@email.com, +52 55 7839 1743, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-10 14:29:00, 2026-01-16 14:00:00),
    (c0000136-1111-1111-1111-111111111111, Jesús, Flores, Flores, jesusflores136@email.com, +52 55 8640 6440, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-06 12:13:00, 2026-01-16 14:00:00),
    (c0000137-1111-1111-1111-111111111111, Sofía, Medina, García, sofiamedina137@email.com, +52 55 1097 5181, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-11 18:10:00, 2026-01-16 14:00:00),
    (c0000138-1111-1111-1111-111111111111, Luis, Gutiérrez, Reyes, luisgutierrez138@email.com, +52 55 6665 7550, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-14 17:45:00, 2026-01-16 14:00:00),
    (c0000139-1111-1111-1111-111111111111, Carmen, Cruz, Rivera, carmencruz139@email.com, +52 55 1866 8051, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-11-09 11:02:00, 2026-01-16 14:00:00),
    (c0000140-1111-1111-1111-111111111111, Enrique, Pérez, Morales, enriqueperez140@email.com, +52 55 4910 5414, false, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-12-11 20:47:00, 2026-01-16 14:00:00),
    (c0000141-1111-1111-1111-111111111111, Rosa, Castro, Díaz, rosacastro141@email.com, +52 55 5187 2480, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2025-09-22 10:15:00, 2026-01-16 14:00:00),
    (c0000142-1111-1111-1111-111111111111, Ángel, Castro, Romero, angelcastro142@email.com, +52 55 2549 6914, true, aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 2026-01-25 19:17:00, 2026-01-16 14:00:00);

-- ============================================================================
-- 6. CITAS (35 citas variadas: pasadas, hoy, futuras)
-- ============================================================================
INSERT INTO tbl_citas (id, fecha_hora_inicio, fecha_hora_fin, estado, notas, cliente_id, servicio_id, usuario_id, negocio_id, created_at, updated_at)
VALUES
  -- Citas pasadas (completadas y canceladas)
  ('a0000001-1111-1111-1111-111111111111', '2026-01-10 10:00:00', '2026-01-10 10:45:00', 'completada', 'Cliente satisfecha con el corte', 'c0000001-1111-1111-1111-111111111111', 's0000001-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-08 14:20:00', '2026-01-10 10:50:00'),
  ('a0000002-1111-1111-1111-111111111111', '2026-01-11 11:00:00', '2026-01-11 13:00:00', 'completada', 'Aplicación de tinte castaño', 'c0000005-1111-1111-1111-111111111111', 's0000003-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-09 09:15:00', '2026-01-11 13:05:00'),
  ('a0000003-1111-1111-1111-111111111111', '2026-01-12 09:30:00', '2026-01-12 10:15:00', 'cancelada', 'Cliente canceló por enfermedad', 'c0000020-1111-1111-1111-111111111111', 's0000007-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-10 16:00:00', '2026-01-12 08:00:00'),
  ('a0000004-1111-1111-1111-111111111111', '2026-01-13 14:00:00', '2026-01-13 15:00:00', 'completada', 'Manicure francés', 'c0000007-1111-1111-1111-111111111111', 's0000007-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-11 10:30:00', '2026-01-13 15:10:00'),
  ('a0000005-1111-1111-1111-111111111111', '2026-01-14 16:00:00', '2026-01-14 17:30:00', 'completada', 'Uñas acrílicas con diseño', 'c0000013-1111-1111-1111-111111111111', 's0000009-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-12 11:00:00', '2026-01-14 17:35:00'),
  ('a0000006-1111-1111-1111-111111111111', '2026-01-15 10:00:00', '2026-01-15 10:30:00', 'completada', 'Corte clásico', 'c0000002-1111-1111-1111-111111111111', 's0000002-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-13 15:00:00', '2026-01-15 10:35:00'),
  
  -- Citas de hoy (16 enero)
  ('a0000007-1111-1111-1111-111111111111', '2026-01-16 09:00:00', '2026-01-16 09:45:00', 'completada', 'Corte en capas', 'c0000019-1111-1111-1111-111111111111', 's0000001-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-14 12:00:00', '2026-01-16 09:50:00'),
  ('a0000008-1111-1111-1111-111111111111', '2026-01-16 10:00:00', '2026-01-16 11:00:00', 'confirmada', 'Peinado para fiesta', 'c0000025-1111-1111-1111-111111111111', 's0000005-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),
  ('a0000009-1111-1111-1111-111111111111', '2026-01-16 11:30:00', '2026-01-16 12:30:00', 'confirmada', 'Maquillaje para evento', 'c0000030-1111-1111-1111-111111111111', 's0000006-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-14 16:00:00', '2026-01-14 16:00:00'),
  ('a0000010-1111-1111-1111-111111111111', '2026-01-16 13:00:00', '2026-01-16 14:00:00', 'pendiente', 'Pedicure spa', 'c0000008-1111-1111-1111-111111111111', 's0000008-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 18:00:00', '2026-01-15 18:00:00'),
  ('a0000011-1111-1111-1111-111111111111', '2026-01-16 15:00:00', '2026-01-16 16:30:00', 'pendiente', 'Uñas de gel rosas', 'c0000041-1111-1111-1111-111111111111', 's0000010-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 20:00:00', '2026-01-15 20:00:00'),
  ('a0000012-1111-1111-1111-111111111111', '2026-01-16 17:00:00', '2026-01-16 18:30:00', 'pendiente', 'Tratamiento hidratante', 'c0000055-1111-1111-1111-111111111111', 's0000011-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 08:00:00', '2026-01-16 08:00:00'),
  
  -- Citas futuras (próximos 7 días)
  ('a0000013-1111-1111-1111-111111111111', '2026-01-17 09:00:00', '2026-01-17 09:45:00', 'confirmada', 'Corte degradado', 'c0000012-1111-1111-1111-111111111111', 's0000002-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 14:00:00', '2026-01-15 14:00:00'),
  ('a0000014-1111-1111-1111-111111111111', '2026-01-17 10:00:00', '2026-01-17 12:00:00', 'confirmada', 'Mechas californianas', 'c0000033-1111-1111-1111-111111111111', 's0000004-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-14 11:00:00', '2026-01-14 11:00:00'),
  ('a0000015-1111-1111-1111-111111111111', '2026-01-17 14:00:00', '2026-01-17 14:45:00', 'pendiente', 'Manicure regular', 'c0000064-1111-1111-1111-111111111111', 's0000007-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 10:00:00', '2026-01-16 10:00:00'),
  ('a0000016-1111-1111-1111-111111111111', '2026-01-18 11:00:00', '2026-01-18 11:45:00', 'confirmada', 'Corte bob', 'c0000077-1111-1111-1111-111111111111', 's0000001-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 09:00:00', '2026-01-16 09:00:00'),
  ('a0000017-1111-1111-1111-111111111111', '2026-01-18 15:00:00', '2026-01-18 18:00:00', 'confirmada', 'Keratina completa', 'c0000089-1111-1111-1111-111111111111', 's0000012-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 16:00:00', '2026-01-15 16:00:00'),
  ('a0000018-1111-1111-1111-111111111111', '2026-01-19 10:00:00', '2026-01-19 11:00:00', 'pendiente', 'Pedicure spa premium', 'c0000095-1111-1111-1111-111111111111', 's0000008-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 11:00:00', '2026-01-16 11:00:00'),
  ('a0000019-1111-1111-1111-111111111111', '2026-01-19 12:00:00', '2026-01-19 13:30:00', 'pendiente', 'Uñas acrílicas nude', 'c0000101-1111-1111-1111-111111111111', 's0000009-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 12:00:00', '2026-01-16 12:00:00'),
  ('a0000020-1111-1111-1111-111111111111', '2026-01-20 09:30:00', '2026-01-20 10:15:00', 'confirmada', 'Depilación de cejas', 'c0000003-1111-1111-1111-111111111111', 's0000013-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 08:30:00', '2026-01-16 08:30:00'),
  ('a0000021-1111-1111-1111-1111-111111111111', '2026-01-20 11:00:00', '2026-01-20 13:00:00', 'confirmada', 'Tinte rubio cenizo', 'c0000109-1111-1111-1111-111111111111', 's0000003-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 17:00:00', '2026-01-15 17:00:00'),
  ('a0000022-1111-1111-1111-111111111111', '2026-01-20 16:00:00', '2026-01-20 16:45:00', 'pendiente', 'Masaje facial antiestrés', 'c0000115-1111-1111-1111-111111111111', 's0000015-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 13:00:00', '2026-01-16 13:00:00'),
  ('a0000023-1111-1111-1111-111111111111', '2026-01-21 10:00:00', '2026-01-21 10:45:00', 'confirmada', 'Corte pixie', 'c0000121-1111-1111-1111-111111111111', 's0000001-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 14:00:00', '2026-01-16 14:00:00'),
  ('a0000024-1111-1111-1111-111111111111', '2026-01-21 14:00:00', '2026-01-21 15:00:00', 'confirmada', 'Peinado recogido', 'c0000127-1111-1111-1111-111111111111', 's0000005-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 10:30:00', '2026-01-16 10:30:00'),
  ('a0000025-1111-1111-1111-111111111111', '2026-01-22 09:00:00', '2026-01-22 09:30:00', 'pendiente', 'Corte militar', 'c0000014-1111-1111-1111-111111111111', 's0000002-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 15:00:00', '2026-01-16 15:00:00'),
  ('a0000026-1111-1111-1111-111111111111', '2026-01-22 11:00:00', '2026-01-22 12:30:00', 'pendiente', 'Uñas de gel francesas', 'c0000135-1111-1111-1111-111111111111', 's0000010-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 16:00:00', '2026-01-16 16:00:00'),
  ('a0000027-1111-1111-1111-111111111111', '2026-01-22 15:00:00', '2026-01-22 16:30:00', 'confirmada', 'Tratamiento capilar regenerador', 'c0000141-1111-1111-1111-111111111111', 's0000011-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-15 12:00:00', '2026-01-15 12:00:00'),
  ('a0000028-1111-1111-1111-111111111111', '2026-01-23 10:00:00', '2026-01-23 13:00:00', 'confirmada', 'Extensiones de cabello largo', 'c0000028-1111-1111-1111-111111111111', 's0000014-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-14 13:00:00', '2026-01-14 13:00:00'),
  ('a0000029-1111-1111-1111-111111111111', '2026-01-23 14:00:00', '2026-01-23 15:00:00', 'pendiente', 'Maquillaje social', 'c0000051-1111-1111-1111-111111111111', 's0000006-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 17:00:00', '2026-01-16 17:00:00'),
  ('a0000030-1111-1111-1111-111111111111', '2026-01-23 16:00:00', '2026-01-23 16:45:00', 'pendiente', 'Manicure con diseño', 'c0000067-1111-1111-1111-111111111111', 's0000007-1111-1111-1111-111111111111', '00000001-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2026-01-16 18:00:00', '2026-01-16 18:00:00');

-- ============================================================================
-- 7. HORARIOS DE TRABAJO (Lunes a Sábado)
-- ============================================================================
INSERT INTO tbl_horarios (id, hora_inicio, hora_fin, activo, negocio_id, created_at, updated_at)
VALUES
  ('h0000001-1111-1111-1111-111111111111', '09:00:00', '19:00:00', true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2025-09-26 09:00:00', '2026-01-16 14:00:00');

-- ============================================================================
-- 8. DÍAS DE TRABAJO
-- ============================================================================
INSERT INTO tbl_dias_trabajo (id, dia_semana, horario_id, created_at)
VALUES
  ('d0000001-1111-1111-1111-111111111111', 'LUNES', 'h0000001-1111-1111-1111-111111111111', '2025-09-26 09:00:00'),
  ('d0000002-1111-1111-1111-111111111111', 'MARTES', 'h0000001-1111-1111-1111-111111111111', '2025-09-26 09:00:00'),
  ('d0000003-1111-1111-1111-111111111111', 'MIERCOLES', 'h0000001-1111-1111-1111-111111111111', '2025-09-26 09:00:00'),
  ('d0000004-1111-1111-1111-111111111111', 'JUEVES', 'h0000001-1111-1111-1111-111111111111', '2025-09-26 09:00:00'),
  ('d0000005-1111-1111-1111-111111111111', 'VIERNES', 'h0000001-1111-1111-1111-111111111111', '2025-09-26 09:00:00'),
  ('d0000006-1111-1111-1111-111111111111', 'SABADO', 'h0000001-1111-1111-1111-111111111111', '2025-09-26 09:00:00');

-- ============================================================================
-- DATOS CARGADOS EXITOSAMENTE
-- ============================================================================
-- Resumen:
-- - 1 Negocio: Estética Premium (Plan PROFESIONAL)
-- - 1 Usuario: demo@citaclick.mx (Password: Demo1234!)
-- - 15 Servicios activos
-- - 142 Clientes
-- - 30 Citas (pasadas, hoy y futuras)
-- - Horario: Lun-Sáb 9:00-19:00
-- ============================================================================
