-- =============================================================================
-- CarMotors - Sistema de Gestión de Taller (SGT)
-- database/data.sql  |  Datos iniciales para carmotors_test
-- =============================================================================
--
-- USO (siempre en este orden)
--   database/schema.sql
--   database/data.sql
--
--
--
-- FECHA DE REFERENCIA: 2026-11-13
--   "Hoy" para todos los ejemplos es el 2026-11-13. Las consultas deben usar esa
--   fecha fija (variable @fecha_referencia) y NO CURDATE()/NOW(), para que los
--   resultados esperados se reproduzcan igual en cualquier fecha.
--   Ventana de "por vencer": 30 días, es decir hasta el 2026-12-13.
--
-- CARGA POR ETAPAS (cada etapa depende de las anteriores)
--   Etapa 1  Catálogos iniciales (estados, tipos, marcas, servicios, repuesto_tipos)
--   Etapa 2  Clientes, vehículos, técnicos y proveedores
--   Etapa 3  Repuestos y catálogo de proveedores
--   Etapa 4  Servicios, detalle de operaciones y técnicos asignados
--   Etapa 5  Lotes y existencias por lote
--   Etapa 6  Líneas de repuestos y reservas por lote
--   Etapa 7  Facturas
--   Etapa 8  Evaluaciones de proveedor
--   Un módulo que solo necesite hasta la etapa N puede ejecutar los bloques
--   1..N copiados en ese orden.
--
-- CONVENCIONES DE CONSISTENCIA (los fixtures de otros módulos deben respetarlas)
--   * Todos los ids son explícitos y estables; no depender de AUTO_INCREMENT.
--   * Consumir una reserva descuenta lotes_repuestos.cantidad_actual:
--       cantidad_actual = cantidad_ingresada - SUM(reservas CONSUMIDAS del lote).
--   * Stock físico     = SUM(cantidad_actual) por repuesto.
--     Stock disponible = stock físico - reservas ACTIVAS.
--   * Una línea de servicios_repuestos nunca supera cantidad_requerida con la
--     suma de reservas ACTIVAS + CONSUMIDAS (las LIBERADAS no cuentan).
--   * Facturas: solo servicios Completado o Entregado.
--       total_repuestos = SUM(cantidad_requerida * precio_unitario_venta)
--       total_mano_obra = SUM(servicios_detalle.precio_cobrado)
--       impuestos       = 19 % de (total_repuestos + total_mano_obra)
--       total_final     = total_repuestos + total_mano_obra + impuestos
--   * Una reserva CONSUMIDA lleva fecha_consumo; ACTIVA y LIBERADA la dejan NULL.
--
-- CONTEOS ESPERADOS TRAS CARGAR TODO
--   servicio_estados 5 | mantenimiento_tipos 2 | reserva_estados 3
--   repuesto_tipos 7 | marcas_vehiculo 6 | modelos_vehiculo 10 | marcas_repuesto 8
--   servicios_catalogo 8 | clientes 8 | vehiculos 10 | tecnicos 6 | proveedores 4
--   repuestos 12 | proveedores_repuestos 17 | servicios 10 | servicios_tecnicos 13
--   servicios_detalle 14 | lotes 6 | lotes_repuestos 15 | servicios_repuestos 16
--   reservas_repuestos 18 | facturas 5 | evaluaciones_proveedor 5
--
-- =============================================================================

USE carmotors_test;
SET NAMES utf8mb4;

-- =============================================================================
-- ETAPA 1: Catálogos iniciales
-- =============================================================================

-- Cinco estados exactos del servicio (ver PENDIENTE en el encabezado)
INSERT INTO servicio_estados (id, nombre) VALUES
  (1, 'Pendiente'),
  (2, 'En Proceso'),
  (3, 'Facturado'),
  (4, 'Completado'),
  (5, 'Entregado');

INSERT INTO mantenimiento_tipos (id, nombre) VALUES
  (1, 'Preventivo'),
  (2, 'Correctivo');

-- Obligatorios: el código y las reglas del esquema dependen de estos nombres
INSERT INTO reserva_estados (id, nombre) VALUES
  (1, 'ACTIVA'),
  (2, 'CONSUMIDA'),
  (3, 'LIBERADA');

INSERT INTO repuesto_tipos (id, nombre) VALUES
  (1, 'Lubricantes'),
  (2, 'Filtros'),
  (3, 'Frenos'),
  (4, 'Encendido'),
  (5, 'Suspensión'),
  (6, 'Eléctrico'),
  (7, 'Transmisión');

INSERT INTO marcas_repuesto (id, nombre) VALUES
  (1, 'Castrol'),
  (2, 'Mobil'),
  (3, 'Bosch'),
  (4, 'NGK'),
  (5, 'Brembo'),
  (6, 'Monroe'),
  (7, 'Mann-Filter'),
  (8, 'ACDelco');

INSERT INTO marcas_vehiculo (id, nombre) VALUES
  (1, 'Chevrolet'),
  (2, 'Renault'),
  (3, 'Mazda'),
  (4, 'Toyota'),
  (5, 'Kia'),
  (6, 'Nissan');

INSERT INTO modelos_vehiculo (id, marca_id, nombre) VALUES
  (1,  1, 'Spark GT'),
  (2,  1, 'Onix'),
  (3,  2, 'Logan'),
  (4,  2, 'Duster'),
  (5,  3, 'Mazda 3'),
  (6,  4, 'Corolla'),
  (7,  4, 'Hilux'),
  (8,  5, 'Picanto'),
  (9,  5, 'Sportage'),
  (10, 6, 'March');

-- Operaciones del taller. intervalo_meses NULL = no se programa próximo mantenimiento
INSERT INTO servicios_catalogo (id, nombre, precio_base, intervalo_meses) VALUES
  (1, 'Cambio de aceite y filtro',    90000.00,  6),
  (2, 'Alineación y balanceo',        70000.00, 12),
  (3, 'Cambio de pastillas de freno', 120000.00, 12),
  (4, 'Revisión del sistema eléctrico', 60000.00, NULL),
  (5, 'Cambio de bujías',             80000.00, 24),
  (6, 'Mantenimiento de suspensión',  150000.00, 24),
  (7, 'Diagnóstico general',          50000.00, NULL),
  (8, 'Cambio de batería',            40000.00, NULL);

-- =============================================================================
-- ETAPA 2: Clientes, vehículos, técnicos y proveedores
-- =============================================================================

INSERT INTO clientes (id, nombre, documento_identidad, telefono, email) VALUES
  (1, 'Camila Rojas Prada',            '1000000001',   '3001110001', 'camila.rojas@example.com'),
  (2, 'Andrés Felipe Mora',            '1000000002',   '3001110002', 'andres.mora@example.com'),
  (3, 'Laura Sofía Cárdenas',          '1000000003',   '3001110003', NULL),
  (4, 'Jorge Iván Peñaranda',          '1000000004',   '3001110004', 'jorge.penaranda@example.com'),
  (5, 'María Fernanda Quintero',       '1000000005',   '3001110005', 'maria.quintero@example.com'),
  (6, 'Carlos Eduardo Villamizar',     '1000000006',   '3001110006', NULL),
  (7, 'Daniela Ortiz Suárez',          '1000000007',   '3001110007', 'daniela.ortiz@example.com'),
  (8, 'Transportes El Cerrito S.A.S.', '900000001-1',  '3001110008', 'flota@elcerrito.example.com');

INSERT INTO vehiculos (id, cliente_id, modelo_id, placa) VALUES
  (1,  1, 1,  'FKA101'),   -- Spark GT
  (2,  2, 2,  'FKB202'),   -- Onix
  (3,  3, 3,  'FKC303'),   -- Logan
  (4,  4, 5,  'FKD404'),   -- Mazda 3
  (5,  5, 6,  'FKE505'),   -- Corolla
  (6,  6, 9,  'FKF606'),   -- Sportage
  (7,  7, 8,  'FKG707'),   -- Picanto
  (8,  8, 7,  'FKH808'),   -- Hilux (flota)
  (9,  8, 4,  'FKJ909'),   -- Duster (flota)
  (10, 2, 10, 'FKK010');   -- March (segundo vehículo del cliente 2)

-- El técnico 6 está inactivo y no tiene servicios asignados
INSERT INTO tecnicos (id, nombre, documento_identidad, telefono, especialidad, activo) VALUES
  (1, 'Luis Alberto Gómez',     '2000000001', '3002220001', 'Mecánica general',          TRUE),
  (2, 'Wilson Arturo Sánchez',  '2000000002', '3002220002', 'Frenos y suspensión',       TRUE),
  (3, 'Paola Andrea Contreras', '2000000003', '3002220003', 'Electricidad automotriz',   TRUE),
  (4, 'Héctor Julián Rincón',   '2000000004', NULL,         'Motor y encendido',         TRUE),
  (5, 'Mauricio Duarte Lizcano','2000000005', '3002220005', 'Alineación y balanceo',     TRUE),
  (6, 'Ricardo Parra Becerra',  '2000000006', '3002220006', 'Mecánica general',          FALSE);

INSERT INTO proveedores (id, nombre, contacto, email) VALUES
  (1, 'Repuestos del Oriente S.A.S.',     'Marta Villamizar', 'ventas@repuestosoriente.example.com'),
  (2, 'Lubricantes Andinos Ltda.',        'Diego Cáceres',    'pedidos@lubricantesandinos.example.com'),
  (3, 'Frenos y Suspensión Cúcuta',       NULL,               'contacto@frenoscucuta.example.com'),
  (4, 'ElectroAuto Distribuciones',       'Sandra Moreno',    NULL);

-- =============================================================================
-- ETAPA 3: Repuestos y catálogo de proveedores
-- stock_minimo es PROVISIONAL (pendiente de consultar al docente)
-- =============================================================================

INSERT INTO repuestos (id, nombre, repuesto_tipo_id, marca_id, precio_venta, stock_minimo) VALUES
  (1,  'Aceite sintético 5W-30 1 L',          1, 1, 38000.00,  10),
  (2,  'Aceite mineral 20W-50 1 L',           1, 2, 28000.00,   6),
  (3,  'Filtro de aceite',                    2, 7, 25000.00,   6),
  (4,  'Filtro de aire',                      2, 7, 42000.00,   4),
  (5,  'Pastillas de freno delanteras',       3, 5, 135000.00,  3),
  (6,  'Discos de freno delanteros (par)',    3, 5, 320000.00,  2),
  (7,  'Bujía de iridio',                     4, 4, 32000.00,   8),
  (8,  'Amortiguador delantero',              5, 6, 280000.00,  2),
  (9,  'Batería 12V 45Ah',                    6, 8, 390000.00,  2),
  (10, 'Kit correa de distribución',          7, 3, 260000.00,  2),
  (11, 'Líquido de frenos DOT 4 500 ml',      3, 3, 22000.00,   5),
  (12, 'Escobillas limpiaparabrisas (par)',   6, 3, 48000.00,   3);

INSERT INTO proveedores_repuestos (proveedor_id, repuesto_id, costo) VALUES
  (1, 3,  15000.00), (1, 4,  26000.00), (1, 5,  85000.00), (1, 6, 200000.00),
  (1, 7,  20000.00), (1, 10, 160000.00), (1, 11, 13000.00),
  (2, 1,  25000.00), (2, 2,  17000.00), (2, 3,  16000.00),
  (3, 5,  86000.00), (3, 6, 205000.00), (3, 8, 175000.00), (3, 11, 12000.00),
  (4, 7,  21000.00), (4, 9, 245000.00), (4, 12, 30000.00);

-- =============================================================================
-- ETAPA 4: Servicios, operaciones realizadas y técnicos asignados
-- Estados: 1 Pendiente, 2 En Proceso, 3 Facturado, 4 Completado, 5 Entregado
-- Tipos:   1 Preventivo, 2 Correctivo
-- =============================================================================

INSERT INTO servicios (id, vehiculo_id, mantenimiento_tipo_id, estado_id,
                       fecha_recepcion, fecha_completado, fecha_entrega, descripcion_problema) VALUES
  (1,  1,  1, 5, '2026-10-05 08:00:00', '2026-10-05 15:00:00', '2026-10-06 09:00:00', 'Mantenimiento de los 10.000 km'),
  (2,  2,  2, 5, '2026-10-08 09:00:00', '2026-10-09 16:00:00', '2026-10-10 09:30:00', 'Ruido y vibración al frenar'),
  (3,  3,  1, 4, '2026-11-09 08:30:00', '2026-11-10 14:00:00', NULL,                  'Cambio de aceite y bujías por kilometraje'),
  (4,  4,  2, 2, '2026-11-11 09:00:00', NULL,                  NULL,                  'Golpes en la suspensión delantera'),
  (5,  5,  2, 5, '2026-10-14 08:00:00', '2026-10-16 17:00:00', '2026-10-17 10:30:00', 'Suspensión delantera desgastada'),
  (6,  6,  1, 2, '2026-11-12 10:00:00', NULL,                  NULL,                  'Testigo de revisión del motor encendido'),
  (7,  7,  2, 1, '2026-11-13 08:00:00', NULL,                  NULL,                  'No enciende en las mañanas'),
  (8,  8,  1, 2, '2026-11-12 07:30:00', NULL,                  NULL,                  'Mantenimiento programado de flota'),
  (9,  9,  2, 5, '2026-10-22 08:00:00', '2026-10-23 16:00:00', '2026-10-24 08:30:00', 'Frenos sin respuesta'),
  (10, 10, 2, 5, '2026-10-27 08:00:00', '2026-10-27 15:00:00', '2026-10-28 08:30:00', 'La batería no retiene carga');

-- Operaciones del catálogo realizadas (precio_cobrado puede diferir del precio base)
INSERT INTO servicios_detalle (servicio_id, servicio_catalogo_id, precio_cobrado) VALUES
  (1,  1,  90000.00),
  (2,  3, 120000.00), (2,  7,  50000.00),
  (3,  1,  90000.00), (3,  5,  80000.00),
  (4,  6, 150000.00), (4,  2,  70000.00),
  (5,  6, 150000.00), (5,  2,  70000.00),
  (6,  7,  50000.00),
  (8,  1,  90000.00),
  (9,  3, 110000.00),            -- precio de flota, menor al base (120000)
  (10, 8,  40000.00), (10, 4,  60000.00);

-- Técnicos por servicio (servicios 2, 3, 4 y 5 tienen dos técnicos)
INSERT INTO servicios_tecnicos (servicio_id, tecnico_id, horas_trabajadas) VALUES
  (1,  1, 1.50),
  (2,  2, 3.00), (2,  1, 1.00),
  (3,  4, 2.50), (3,  1, 1.50),
  (4,  2, 2.00), (4,  5, 1.00),
  (5,  2, 4.00), (5,  5, 1.50),
  (6,  3, 0.50),
  (8,  1, 0.75),
  (9,  2, 2.00),
  (10, 3, 1.50);

-- =============================================================================
-- ETAPA 5: Lotes y existencias por lote
-- cantidad_actual = cantidad_ingresada - reservas CONSUMIDAS (etapa 6)
-- =============================================================================

INSERT INTO lotes (id, codigo, proveedor_id, fecha_ingreso) VALUES
  (1, 'L-2026-001', 2, '2026-08-20'),
  (2, 'L-2026-002', 1, '2026-09-05'),
  (3, 'L-2026-003', 3, '2026-09-18'),
  (4, 'L-2025-014', 3, '2025-10-31'),
  (5, 'L-2026-004', 4, '2026-10-02'),
  (6, 'L-2026-005', 2, '2026-10-20');

INSERT INTO lotes_repuestos (lote_id, repuesto_id, cantidad_ingresada, cantidad_actual, costo_unitario, fecha_caducidad) VALUES
  -- Lote 1 (Lubricantes Andinos): el aceite 20W-50 caduca pronto
  (1, 1,   8,  4,  24000.00, '2028-08-20'),
  (1, 2,  12,  8,  17000.00, '2026-12-05'),
  -- Lote 2 (Repuestos del Oriente)
  (2, 3,  20, 18,  15000.00, NULL),
  (2, 4,  10, 10,  26000.00, NULL),
  (2, 7,  16, 12,  20000.00, NULL),
  (2, 10,  4,  4, 160000.00, NULL),
  -- Lote 3 (Frenos y Suspensión Cúcuta): discos casi agotados, amortiguadores reservados
  (3, 5,   6,  4,  85000.00, NULL),
  (3, 6,   3,  1, 200000.00, NULL),
  (3, 8,   4,  2, 175000.00, NULL),
  (3, 11, 10, 10,  13000.00, '2027-09-18'),
  -- Lote 4: líquido de frenos VENCIDO el 2026-10-31 con 3 unidades
  (4, 11,  5,  3,  12000.00, '2026-10-31'),
  -- Lote 5 (ElectroAuto)
  (5, 9,   6,  5, 245000.00, NULL),
  (5, 12,  8,  8,  30000.00, NULL),
  (5, 7,   8,  8,  21000.00, NULL),
  -- Lote 6: segundo lote de aceite 5W-30 (reserva repartida del servicio 8)
  (6, 1,  12, 12,  25000.00, '2029-10-20');

-- =============================================================================
-- ETAPA 6: Líneas de repuestos y reservas por lote
-- =============================================================================

INSERT INTO servicios_repuestos (servicio_id, repuesto_id, cantidad_requerida, precio_unitario_venta) VALUES
  (1,  1,  4,  38000.00), (1,  3, 1,  25000.00),
  (2,  5,  1, 135000.00), (2,  6, 1, 320000.00), (2, 11, 1, 22000.00),
  (3,  2,  4,  28000.00), (3,  3, 1,  25000.00), (3,  7, 4,  32000.00),
  (4,  8,  2, 280000.00),
  (5,  8,  2, 280000.00),
  (8,  1,  5,  38000.00), (8,  3, 1,  25000.00),
  (9,  5,  1, 135000.00), (9,  6, 1, 320000.00), (9, 11, 1,  22000.00),
  (10, 9,  1, 390000.00);

INSERT INTO reservas_repuestos
  (id, servicio_id, repuesto_id, lote_id, estado_id, cantidad, fecha_reserva, fecha_consumo) VALUES
  -- Servicios ya terminados: reservas CONSUMIDAS (estado 2)
  (1,  1,  1, 1, 2, 4, '2026-10-05 08:30:00', '2026-10-05 12:00:00'),
  (2,  1,  3, 2, 2, 1, '2026-10-05 08:30:00', '2026-10-05 12:00:00'),
  (3,  2,  5, 3, 2, 1, '2026-10-08 10:00:00', '2026-10-09 09:00:00'),
  (4,  2,  6, 3, 2, 1, '2026-10-08 10:00:00', '2026-10-09 09:00:00'),
  (5,  2, 11, 4, 2, 1, '2026-10-08 10:00:00', '2026-10-09 09:00:00'),
  (6,  3,  2, 1, 2, 4, '2026-11-09 09:00:00', '2026-11-10 10:00:00'),
  (7,  3,  3, 2, 2, 1, '2026-11-09 09:00:00', '2026-11-10 10:00:00'),
  (8,  3,  7, 2, 2, 4, '2026-11-09 09:00:00', '2026-11-10 10:00:00'),
  (9,  5,  8, 3, 2, 2, '2026-10-14 09:00:00', '2026-10-15 10:00:00'),
  (10, 9,  5, 3, 2, 1, '2026-10-22 09:00:00', '2026-10-23 09:00:00'),
  (11, 9,  6, 3, 2, 1, '2026-10-22 09:00:00', '2026-10-23 09:00:00'),
  (12, 9, 11, 4, 2, 1, '2026-10-22 09:00:00', '2026-10-23 09:00:00'),
  (13, 10, 9, 5, 2, 1, '2026-10-27 09:00:00', '2026-10-27 15:00:00'),
  -- Servicio 4: reserva LIBERADA (estado 3) y nueva reserva ACTIVA (estado 1) de la misma línea
  (14, 4,  8, 3, 3, 2, '2026-11-11 10:00:00', NULL),
  (15, 4,  8, 3, 1, 2, '2026-11-11 16:00:00', NULL),
  -- Servicio 8: 5 unidades del repuesto 1 repartidas en dos lotes (3 + 2), ACTIVAS
  (16, 8,  1, 1, 1, 3, '2026-11-12 08:00:00', NULL),
  (17, 8,  1, 6, 1, 2, '2026-11-12 08:00:00', NULL),
  (18, 8,  3, 2, 1, 1, '2026-11-12 08:00:00', NULL);

-- =============================================================================
-- ETAPA 7: Facturas (solo servicios Completado o Entregado; el servicio 3 queda sin facturar)
-- impuestos = 19 % de (total_repuestos + total_mano_obra)
-- =============================================================================

INSERT INTO facturas (id, servicio_id, fecha_emision, total_repuestos, total_mano_obra, impuestos, total_final) VALUES
  (1, 1,  '2026-10-06 09:30:00', 177000.00,  90000.00,  50730.00, 317730.00),
  (2, 2,  '2026-10-10 10:00:00', 477000.00, 170000.00, 122930.00, 769930.00),
  (3, 5,  '2026-10-17 11:00:00', 560000.00, 220000.00, 148200.00, 928200.00),
  (4, 9,  '2026-10-24 09:00:00', 477000.00, 110000.00, 111530.00, 698530.00),
  (5, 10, '2026-10-28 09:00:00', 390000.00, 100000.00,  93100.00, 583100.00);

-- =============================================================================
-- ETAPA 8: Evaluaciones de proveedor (el lote 6 aún no se evalúa)
-- =============================================================================

INSERT INTO evaluaciones_proveedor
  (id, proveedor_id, lote_id, calificacion_calidad, calificacion_puntualidad, fecha_evaluacion, observaciones) VALUES
  (1, 2, 1, 5, 4, '2026-08-22', 'Producto en buen estado, entregado a tiempo'),
  (2, 1, 2, 4, 5, '2026-09-07', 'Entrega completa y puntual'),
  (3, 3, 3, 4, 3, '2026-09-20', 'Llegó con un día de retraso'),
  (4, 3, 4, 2, 3, '2025-11-03', 'Líquido de frenos con fecha de caducidad muy corta'),
  (5, 4, 5, 5, 5, '2026-10-04', NULL);