-- =============================================================================
-- CarMotors - Sistema de Gestión de Taller (SGT)
-- database/schema.sql  |  Motor: MySQL 8.0.16 o superior (InnoDB, utf8mb4)
-- Origen: Diagrama_entidades.drawio (ER CarMotors, versión con reservas por lote)
-- =============================================================================
--
-- USO
--   mysql -u root -p < database/schema.sql
--   Crea (desde cero) la base de pruebas `carmotors_test`, VACÍA: sin datos.
--   El script es repetible: si la base ya existe, la borra y la vuelve a crear.
--   Para otra base (por ejemplo producción) cambiar el nombre en las dos
--   primeras sentencias y QUITAR el DROP DATABASE.
--
-- REQUISITOS
--   MySQL 8.0.16+ : antes de esa versión los CHECK se aceptan pero se ignoran.
--   MySQL 8.0.13+ : DEFAULT con expresión, usado en evaluaciones_proveedor.
--
-- ORDEN DE CREACIÓN (padres antes que hijos)
--    1 marcas_vehiculo           9 servicios_tecnicos       17 proveedores
--    2 modelos_vehiculo         10 servicios_catalogo       18 proveedores_repuestos
--    3 clientes                 11 servicios_detalle        19 lotes
--    4 vehiculos                12 facturas                 20 lotes_repuestos
--    5 mantenimiento_tipos      13 repuesto_tipos           21 servicios_repuestos
--    6 servicio_estados         14 marcas_repuesto          22 reservas_repuestos
--    7 servicios                15 repuestos                23 evaluaciones_proveedor
--    8 tecnicos                 16 reserva_estados
--   Los fixtures deben INSERTAR en este mismo orden y BORRAR en el inverso.
--   Todas las FK usan ON DELETE RESTRICT y ON UPDATE RESTRICT: no hay borrado
--   en cascada, así que un padre con hijos no se puede borrar.
--
-- CONVENCIONES
--   * PK simples: INT UNSIGNED AUTO_INCREMENT con nombre `id`.
--   * Tablas intermedias N:M: PK compuesta, sin columna `id`
--     (servicios_tecnicos, servicios_detalle, servicios_repuestos,
--      proveedores_repuestos, lotes_repuestos).
--   * Dinero: DECIMAL(10,2), siempre en la misma moneda y sin símbolo.
--     Horas: DECIMAL(5,2). Nunca usar FLOAT/DOUBLE en fixtures ni en el código.
--   * Cantidades: INT UNSIGNED, unidades enteras de repuesto.
--   * Nombres de restricciones: uk_ (única), fk_ (foránea), ck_ (CHECK),
--     idx_ (índice adicional).
--   * Un valor NULL pasa los CHECK (comportamiento estándar de SQL): para
--     exigir un campo se usa NOT NULL, no CHECK.
--
-- CONTRATOS PARA FIXTURES (qué debe traer cada fila)
--   Catálogos (cargar primero, el script NO los siembra):
--     servicio_estados, mantenimiento_tipos, reserva_estados, repuesto_tipos,
--     marcas_vehiculo, marcas_repuesto, servicios_catalogo.
--     reserva_estados DEBE contener exactamente los nombres ACTIVA, CONSUMIDA
--     y LIBERADA (en mayúsculas): el código y las reglas de abajo dependen de
--     ellos. Los demás catálogos los define cada fixture.
--   Obligatorios (NOT NULL) y opcionales (NULL):
--     clientes   : nombre, documento_identidad, telefono obligatorios; email opcional.
--     vehiculos  : cliente_id, modelo_id, placa obligatorios (placa única).
--     servicios  : vehiculo_id, mantenimiento_tipo_id, estado_id obligatorios;
--                  fecha_recepcion se llena sola con la fecha actual;
--                  fecha_completado, fecha_entrega y descripcion_problema opcionales.
--     tecnicos   : nombre y documento_identidad obligatorios; activo = TRUE por defecto.
--     facturas   : una sola por servicio; total_final = total_repuestos +
--                  total_mano_obra + impuestos (lo valida la base).
--     lotes      : codigo único; fecha_ingreso obligatoria.
--     lotes_repuestos : cantidad_ingresada > 0 y 0 <= cantidad_actual <= cantidad_ingresada.
--     servicios_repuestos : cantidad_requerida > 0 y precio_unitario_venta >= 0.
--     reservas_repuestos : cantidad > 0; la FK (servicio_id, repuesto_id) debe
--                  existir en servicios_repuestos y la FK (lote_id, repuesto_id)
--                  en lotes_repuestos, es decir, el mismo repuesto en ambos lados.
--     evaluaciones_proveedor : calificaciones de 1 a 5; el lote debe pertenecer
--                  al proveedor indicado (FK compuesta).
--
-- DATOS DERIVADOS (no hay columna, se calculan con consultas)
--   * Stock físico      = SUM(lotes_repuestos.cantidad_actual) por repuesto.
--   * Stock disponible  = stock físico - SUM(reservas_repuestos.cantidad) con
--                         estado ACTIVA.
--   * Mano de obra del servicio = SUM(servicios_detalle.precio_cobrado).
--     facturas.total_mano_obra guarda ese valor al momento de facturar.
--   * Próximo mantenimiento = servicios.fecha_entrega +
--     servicios_catalogo.intervalo_meses, por vehículo y operación realizada.
--
-- REGLAS QUE LA BASE NO PUEDE VALIDAR POR SÍ SOLA (las aplica la capa de
-- servicio, dentro de una transacción)
--   * Por cada línea de servicios_repuestos, la suma de reservas ACTIVAS y
--     CONSUMIDAS (excluyendo LIBERADAS) no puede superar cantidad_requerida.
--     Así se puede liberar y volver a reservar sin contar dos veces.
--   * Las reservas ACTIVAS de un lote no pueden superar su cantidad_actual.
--   * Al pasar una reserva a CONSUMIDA se descuenta cantidad_actual del lote y
--     se llena fecha_consumo.
-- =============================================================================

DROP DATABASE IF EXISTS carmotors_test;
CREATE DATABASE carmotors_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE carmotors_test;

-- -----------------------------------------------------------------------------
-- Clientes y vehículos
-- -----------------------------------------------------------------------------

-- 1
CREATE TABLE marcas_vehiculo (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50)  NOT NULL,
  CONSTRAINT pk_marcas_vehiculo PRIMARY KEY (id),
  CONSTRAINT uk_marcas_vehiculo_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- 2
CREATE TABLE modelos_vehiculo (
  id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  marca_id INT UNSIGNED NOT NULL,
  nombre   VARCHAR(50)  NOT NULL,
  CONSTRAINT pk_modelos_vehiculo PRIMARY KEY (id),
  CONSTRAINT uk_modelos_vehiculo_marca_nombre UNIQUE (marca_id, nombre),
  CONSTRAINT fk_modelos_vehiculo_marca FOREIGN KEY (marca_id)
    REFERENCES marcas_vehiculo (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- 3
CREATE TABLE clientes (
  id                  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre              VARCHAR(255) NOT NULL,
  documento_identidad VARCHAR(20)  NOT NULL,
  telefono            VARCHAR(20)  NOT NULL,
  email               VARCHAR(100) NULL,
  CONSTRAINT pk_clientes PRIMARY KEY (id),
  CONSTRAINT uk_clientes_documento UNIQUE (documento_identidad),
  CONSTRAINT uk_clientes_email UNIQUE (email)
) ENGINE=InnoDB;

-- 4
CREATE TABLE vehiculos (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  cliente_id INT UNSIGNED NOT NULL,
  modelo_id  INT UNSIGNED NOT NULL,
  placa      VARCHAR(10)  NOT NULL,
  CONSTRAINT pk_vehiculos PRIMARY KEY (id),
  CONSTRAINT uk_vehiculos_placa UNIQUE (placa),
  CONSTRAINT fk_vehiculos_cliente FOREIGN KEY (cliente_id)
    REFERENCES clientes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_vehiculos_modelo FOREIGN KEY (modelo_id)
    REFERENCES modelos_vehiculo (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Servicios y técnicos
-- -----------------------------------------------------------------------------

-- 5
CREATE TABLE mantenimiento_tipos (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_mantenimiento_tipos PRIMARY KEY (id),
  CONSTRAINT uk_mantenimiento_tipos_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- 6
CREATE TABLE servicio_estados (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_servicio_estados PRIMARY KEY (id),
  CONSTRAINT uk_servicio_estados_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- 7
CREATE TABLE servicios (
  id                    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  vehiculo_id           INT UNSIGNED NOT NULL,
  mantenimiento_tipo_id INT UNSIGNED NOT NULL,
  estado_id             INT UNSIGNED NOT NULL,
  fecha_recepcion       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_completado      DATETIME     NULL,
  fecha_entrega         DATETIME     NULL,
  descripcion_problema  TEXT         NULL,
  CONSTRAINT pk_servicios PRIMARY KEY (id),
  CONSTRAINT fk_servicios_vehiculo FOREIGN KEY (vehiculo_id)
    REFERENCES vehiculos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_servicios_mantenimiento_tipo FOREIGN KEY (mantenimiento_tipo_id)
    REFERENCES mantenimiento_tipos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_servicios_estado FOREIGN KEY (estado_id)
    REFERENCES servicio_estados (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_servicios_completado
    CHECK (fecha_completado IS NULL OR fecha_completado >= fecha_recepcion),
  CONSTRAINT ck_servicios_entrega
    CHECK (fecha_entrega IS NULL OR fecha_entrega >= fecha_recepcion),
  INDEX idx_servicios_vehiculo_fecha (vehiculo_id, fecha_recepcion),
  INDEX idx_servicios_estado_fecha (estado_id, fecha_recepcion)
) ENGINE=InnoDB;

-- 8
CREATE TABLE tecnicos (
  id                  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre              VARCHAR(255) NOT NULL,
  documento_identidad VARCHAR(20)  NOT NULL,
  telefono            VARCHAR(20)  NULL,
  especialidad        VARCHAR(150) NULL,
  activo              BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_tecnicos PRIMARY KEY (id),
  CONSTRAINT uk_tecnicos_documento UNIQUE (documento_identidad)
) ENGINE=InnoDB;

-- 9  N:M servicios - tecnicos
CREATE TABLE servicios_tecnicos (
  servicio_id      INT UNSIGNED  NOT NULL,
  tecnico_id       INT UNSIGNED  NOT NULL,
  horas_trabajadas DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  CONSTRAINT pk_servicios_tecnicos PRIMARY KEY (servicio_id, tecnico_id),
  CONSTRAINT fk_servicios_tecnicos_servicio FOREIGN KEY (servicio_id)
    REFERENCES servicios (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_servicios_tecnicos_tecnico FOREIGN KEY (tecnico_id)
    REFERENCES tecnicos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_servicios_tecnicos_horas CHECK (horas_trabajadas >= 0)
) ENGINE=InnoDB;

-- 10
CREATE TABLE servicios_catalogo (
  id              INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(100)  NOT NULL,
  precio_base     DECIMAL(10,2) NOT NULL,
  intervalo_meses INT UNSIGNED  NULL,
  CONSTRAINT pk_servicios_catalogo PRIMARY KEY (id),
  CONSTRAINT uk_servicios_catalogo_nombre UNIQUE (nombre),
  CONSTRAINT ck_servicios_catalogo_precio CHECK (precio_base >= 0),
  CONSTRAINT ck_servicios_catalogo_intervalo
    CHECK (intervalo_meses IS NULL OR intervalo_meses > 0)
) ENGINE=InnoDB;

-- 11  N:M servicios - servicios_catalogo
CREATE TABLE servicios_detalle (
  servicio_id          INT UNSIGNED  NOT NULL,
  servicio_catalogo_id INT UNSIGNED  NOT NULL,
  precio_cobrado       DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_servicios_detalle PRIMARY KEY (servicio_id, servicio_catalogo_id),
  CONSTRAINT fk_servicios_detalle_servicio FOREIGN KEY (servicio_id)
    REFERENCES servicios (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_servicios_detalle_catalogo FOREIGN KEY (servicio_catalogo_id)
    REFERENCES servicios_catalogo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_servicios_detalle_precio CHECK (precio_cobrado >= 0)
) ENGINE=InnoDB;

-- 12  Una factura como máximo por servicio (UNIQUE sobre servicio_id)
CREATE TABLE facturas (
  id               INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  servicio_id      INT UNSIGNED  NOT NULL,
  fecha_emision    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_repuestos  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total_mano_obra  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  impuestos        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total_final      DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_facturas PRIMARY KEY (id),
  CONSTRAINT uk_facturas_servicio UNIQUE (servicio_id),
  CONSTRAINT fk_facturas_servicio FOREIGN KEY (servicio_id)
    REFERENCES servicios (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_facturas_montos CHECK (
    total_repuestos >= 0 AND total_mano_obra >= 0 AND impuestos >= 0
    AND total_final >= 0),
  CONSTRAINT ck_facturas_total
    CHECK (total_final = total_repuestos + total_mano_obra + impuestos)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Repuestos
-- -----------------------------------------------------------------------------

-- 13
CREATE TABLE repuesto_tipos (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_repuesto_tipos PRIMARY KEY (id),
  CONSTRAINT uk_repuesto_tipos_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- 14
CREATE TABLE marcas_repuesto (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_marcas_repuesto PRIMARY KEY (id),
  CONSTRAINT uk_marcas_repuesto_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- 15  Sin cantidad_stock: el stock se calcula desde lotes_repuestos
CREATE TABLE repuestos (
  id                INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  nombre            VARCHAR(255)  NOT NULL,
  repuesto_tipo_id  INT UNSIGNED  NOT NULL,
  marca_id          INT UNSIGNED  NOT NULL,
  precio_venta      DECIMAL(10,2) NOT NULL,
  stock_minimo      INT UNSIGNED  NOT NULL DEFAULT 0,
  CONSTRAINT pk_repuestos PRIMARY KEY (id),
  CONSTRAINT fk_repuestos_tipo FOREIGN KEY (repuesto_tipo_id)
    REFERENCES repuesto_tipos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_repuestos_marca FOREIGN KEY (marca_id)
    REFERENCES marcas_repuesto (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_repuestos_precio CHECK (precio_venta >= 0),
  INDEX idx_repuestos_nombre (nombre)
) ENGINE=InnoDB;

-- 16
CREATE TABLE reserva_estados (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50)  NOT NULL,
  CONSTRAINT pk_reserva_estados PRIMARY KEY (id),
  CONSTRAINT uk_reserva_estados_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Proveedores y lotes
-- -----------------------------------------------------------------------------

-- 17
CREATE TABLE proveedores (
  id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre   VARCHAR(255) NOT NULL,
  contacto VARCHAR(255) NULL,
  email    VARCHAR(150) NULL,
  CONSTRAINT pk_proveedores PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 18  N:M proveedores - repuestos (catálogo de lo que vende cada proveedor)
CREATE TABLE proveedores_repuestos (
  proveedor_id INT UNSIGNED  NOT NULL,
  repuesto_id  INT UNSIGNED  NOT NULL,
  costo        DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_proveedores_repuestos PRIMARY KEY (proveedor_id, repuesto_id),
  CONSTRAINT fk_proveedores_repuestos_proveedor FOREIGN KEY (proveedor_id)
    REFERENCES proveedores (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_proveedores_repuestos_repuesto FOREIGN KEY (repuesto_id)
    REFERENCES repuestos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_proveedores_repuestos_costo CHECK (costo >= 0)
) ENGINE=InnoDB;

-- 19  uk_lotes_id_proveedor permite la FK compuesta de evaluaciones_proveedor
CREATE TABLE lotes (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  codigo        VARCHAR(50)  NOT NULL,
  proveedor_id  INT UNSIGNED NOT NULL,
  fecha_ingreso DATE         NOT NULL,
  CONSTRAINT pk_lotes PRIMARY KEY (id),
  CONSTRAINT uk_lotes_codigo UNIQUE (codigo),
  CONSTRAINT uk_lotes_id_proveedor UNIQUE (id, proveedor_id),
  CONSTRAINT fk_lotes_proveedor FOREIGN KEY (proveedor_id)
    REFERENCES proveedores (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  INDEX idx_lotes_fecha_ingreso (fecha_ingreso)
) ENGINE=InnoDB;

-- 20  cantidad_ingresada = lo que llegó; cantidad_actual = lo que queda
CREATE TABLE lotes_repuestos (
  lote_id           INT UNSIGNED  NOT NULL,
  repuesto_id       INT UNSIGNED  NOT NULL,
  cantidad_ingresada INT UNSIGNED NOT NULL,
  cantidad_actual   INT UNSIGNED  NOT NULL,
  costo_unitario    DECIMAL(10,2) NOT NULL,
  fecha_caducidad   DATE          NULL,
  CONSTRAINT pk_lotes_repuestos PRIMARY KEY (lote_id, repuesto_id),
  CONSTRAINT fk_lotes_repuestos_lote FOREIGN KEY (lote_id)
    REFERENCES lotes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_lotes_repuestos_repuesto FOREIGN KEY (repuesto_id)
    REFERENCES repuestos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_lotes_repuestos_ingresada CHECK (cantidad_ingresada > 0),
  CONSTRAINT ck_lotes_repuestos_actual
    CHECK (cantidad_actual >= 0 AND cantidad_actual <= cantidad_ingresada),
  CONSTRAINT ck_lotes_repuestos_costo CHECK (costo_unitario >= 0),
  INDEX idx_lotes_repuestos_caducidad (fecha_caducidad)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Repuestos usados en servicios y reservas por lote
-- -----------------------------------------------------------------------------

-- 21  Línea de repuesto requerida por un servicio
CREATE TABLE servicios_repuestos (
  servicio_id           INT UNSIGNED  NOT NULL,
  repuesto_id           INT UNSIGNED  NOT NULL,
  cantidad_requerida    INT UNSIGNED  NOT NULL,
  precio_unitario_venta DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_servicios_repuestos PRIMARY KEY (servicio_id, repuesto_id),
  CONSTRAINT fk_servicios_repuestos_servicio FOREIGN KEY (servicio_id)
    REFERENCES servicios (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_servicios_repuestos_repuesto FOREIGN KEY (repuesto_id)
    REFERENCES repuestos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_servicios_repuestos_cantidad CHECK (cantidad_requerida > 0),
  CONSTRAINT ck_servicios_repuestos_precio CHECK (precio_unitario_venta >= 0)
) ENGINE=InnoDB;

-- 22  De qué lote sale cada cantidad de una línea. Dos FK compuestas que
--     comparten repuesto_id obligan a que línea y lote sean del mismo repuesto.
CREATE TABLE reservas_repuestos (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  servicio_id    INT UNSIGNED NOT NULL,
  repuesto_id    INT UNSIGNED NOT NULL,
  lote_id        INT UNSIGNED NOT NULL,
  estado_id      INT UNSIGNED NOT NULL,
  cantidad       INT UNSIGNED NOT NULL,
  fecha_reserva  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_consumo  DATETIME     NULL,
  CONSTRAINT pk_reservas_repuestos PRIMARY KEY (id),
  CONSTRAINT fk_reservas_linea FOREIGN KEY (servicio_id, repuesto_id)
    REFERENCES servicios_repuestos (servicio_id, repuesto_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_reservas_lote FOREIGN KEY (lote_id, repuesto_id)
    REFERENCES lotes_repuestos (lote_id, repuesto_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_reservas_estado FOREIGN KEY (estado_id)
    REFERENCES reserva_estados (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_reservas_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_reservas_fechas
    CHECK (fecha_consumo IS NULL OR fecha_consumo >= fecha_reserva),
  INDEX idx_reservas_estado_lote (estado_id, lote_id, repuesto_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Evaluación de proveedores
-- -----------------------------------------------------------------------------

-- 23  La FK compuesta (lote_id, proveedor_id) garantiza que el lote evaluado
--     pertenece al proveedor indicado.
CREATE TABLE evaluaciones_proveedor (
  id                       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  proveedor_id             INT UNSIGNED NOT NULL,
  lote_id                  INT UNSIGNED NOT NULL,
  calificacion_calidad     TINYINT UNSIGNED NOT NULL,
  calificacion_puntualidad TINYINT UNSIGNED NOT NULL,
  fecha_evaluacion         DATE         NOT NULL DEFAULT (CURRENT_DATE),
  observaciones            TEXT         NULL,
  CONSTRAINT pk_evaluaciones_proveedor PRIMARY KEY (id),
  CONSTRAINT fk_evaluaciones_proveedor FOREIGN KEY (proveedor_id)
    REFERENCES proveedores (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_evaluaciones_lote_proveedor FOREIGN KEY (lote_id, proveedor_id)
    REFERENCES lotes (id, proveedor_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_evaluaciones_calidad
    CHECK (calificacion_calidad BETWEEN 1 AND 5),
  CONSTRAINT ck_evaluaciones_puntualidad
    CHECK (calificacion_puntualidad BETWEEN 1 AND 5),
  INDEX idx_evaluaciones_proveedor_fecha (proveedor_id, fecha_evaluacion)
) ENGINE=InnoDB;

-- creacion del usuario y permisos

CREATE USER IF NOT EXISTS 'carmotors_admin'@'localhost'
  IDENTIFIED BY 'CambiarEstaClave123!';

-- Si ya existía, dejar la clave actualizada
ALTER USER 'carmotors_admin'@'localhost'
  IDENTIFIED BY 'CambiarEstaClave123!';

-- -----------------------------------------------------------------------------
-- administrador solo de las bases del proyecto
-- -----------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON carmotors_test.* TO 'carmotors_admin'@'localhost';

FLUSH PRIVILEGES;

-- Comprobar lo que quedó asignado
SHOW GRANTS FOR 'carmotors_admin'@'localhost';
