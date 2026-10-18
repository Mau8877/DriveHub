-- =====================================================================
-- Esquema inicial del microservicio Pagos
-- Fuente: docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf (sección 3)
-- =====================================================================
-- Convenciones (ver .claude/rules/backend/PERSISTENCIA_BD_BACKEND.md):
--   * id BIGINT IDENTITY: PK interna, nunca sale del servicio.
--   * codigo_{tabla} UUID: identificador público (v7, lo genera la aplicación).
--   * {x}_codigo UUID: referencia a otro servicio (sin FK física).
--   * created_at / updated_at / deleted_at en todas las tablas (soft delete).
-- El estado de la saga reserva-pago (SagaEstado) NO vive acá: ver
-- docs/DriveHub/SAGA_RESERVA_PAGO.md (dueño pendiente de decidir).

-- ---------------------------------------------------------------------
-- plan_pago: planes que ofrece la escuela (CU25)
-- ---------------------------------------------------------------------
CREATE TABLE plan_pago (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_plan_pago UUID           NOT NULL UNIQUE,
    nombre           VARCHAR(80)    NOT NULL,
    categoria_sigla  VARCHAR(3),
    nro_clases       SMALLINT       NOT NULL,
    monto_total      NUMERIC(10, 2) NOT NULL,
    nro_cuotas       SMALLINT       NOT NULL,
    activo           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- inscripcion_plan: plan contratado por un estudiante (CU25, CU28)
-- ---------------------------------------------------------------------
CREATE TABLE inscripcion_plan (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_inscripcion_plan UUID           NOT NULL UNIQUE,
    estudiante_codigo       UUID           NOT NULL,
    plan_pago_id            BIGINT         NOT NULL REFERENCES plan_pago (id),
    monto_total             NUMERIC(10, 2) NOT NULL,
    saldo                   NUMERIC(10, 2) NOT NULL,
    estado                  VARCHAR(12)    NOT NULL DEFAULT 'ACTIVO'
                            CHECK (estado IN ('ACTIVO', 'COMPLETADO', 'CANCELADO')),
    fecha_inicio            DATE           NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ
);
CREATE INDEX idx_inscripcion_plan_estudiante_codigo ON inscripcion_plan (estudiante_codigo);
CREATE INDEX idx_inscripcion_plan_plan_pago_id ON inscripcion_plan (plan_pago_id);

-- ---------------------------------------------------------------------
-- cuota: cuotas de un plan contratado (CU25, CU28)
-- ---------------------------------------------------------------------
CREATE TABLE cuota (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_cuota        UUID           NOT NULL UNIQUE,
    inscripcion_plan_id BIGINT         NOT NULL REFERENCES inscripcion_plan (id),
    nro                 SMALLINT       NOT NULL,
    monto               NUMERIC(10, 2) NOT NULL,
    fecha_vence         DATE           NOT NULL,
    estado              VARCHAR(10)    NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE', 'PAGADA', 'VENCIDA', 'ANULADA')),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    UNIQUE (inscripcion_plan_id, nro)
);
-- Para el job que marca cuotas vencidas.
CREATE INDEX idx_cuota_estado_fecha_vence ON cuota (estado, fecha_vence);

-- ---------------------------------------------------------------------
-- pago: pago realizado (CU26, CU27, CU28, CU29)
-- idempotency_key evita cobros duplicados por reintentos.
-- ---------------------------------------------------------------------
CREATE TABLE pago (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_pago       UUID           NOT NULL UNIQUE,
    estudiante_codigo UUID           NOT NULL,
    concepto          VARCHAR(10)    NOT NULL CHECK (concepto IN ('CUOTA', 'CLASE', 'EXAMEN')),
    cuota_id          BIGINT         REFERENCES cuota (id),
    reserva_codigo    UUID,
    monto             NUMERIC(10, 2) NOT NULL,
    moneda            CHAR(3)        NOT NULL DEFAULT 'BOB',
    metodo            VARCHAR(15)    NOT NULL CHECK (metodo IN ('QR', 'TRANSFERENCIA', 'EFECTIVO')),
    estado            VARCHAR(10)    NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'FALLIDO', 'REVERTIDO')),
    idempotency_key   VARCHAR(64)    NOT NULL UNIQUE,
    saga_codigo       UUID,
    confirmado_en     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);
CREATE INDEX idx_pago_estudiante_created ON pago (estudiante_codigo, created_at DESC);
CREATE INDEX idx_pago_reserva_codigo ON pago (reserva_codigo);
CREATE INDEX idx_pago_cuota_id ON pago (cuota_id);

-- ---------------------------------------------------------------------
-- transaccion_pasarela: intercambio con la pasarela (CU26, CU27)
-- ---------------------------------------------------------------------
CREATE TABLE transaccion_pasarela (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_transaccion_pasarela UUID        NOT NULL UNIQUE,
    pago_id                     BIGINT      NOT NULL REFERENCES pago (id),
    proveedor                   VARCHAR(30) NOT NULL,
    referencia_externa          VARCHAR(80) UNIQUE,
    qr_payload                  TEXT,
    estado                      VARCHAR(12) NOT NULL,
    respuesta                   JSONB,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                  TIMESTAMPTZ
);
CREATE INDEX idx_transaccion_pasarela_pago_id ON transaccion_pasarela (pago_id);

-- ---------------------------------------------------------------------
-- comprobante: comprobante de pago (CU30). Uno por pago.
-- ---------------------------------------------------------------------
CREATE TABLE comprobante (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_comprobante UUID           NOT NULL UNIQUE,
    pago_id            BIGINT         NOT NULL UNIQUE REFERENCES pago (id),
    nro_comprobante    VARCHAR(20)    NOT NULL UNIQUE,
    monto              NUMERIC(10, 2) NOT NULL,
    emitido_en         TIMESTAMPTZ    NOT NULL,
    url_pdf            TEXT,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- reversion_pago: compensación de la saga (CU31)
-- ---------------------------------------------------------------------
CREATE TABLE reversion_pago (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_reversion_pago UUID           NOT NULL UNIQUE,
    pago_id               BIGINT         NOT NULL REFERENCES pago (id),
    motivo                VARCHAR(200),
    monto                 NUMERIC(10, 2),
    estado                VARCHAR(10)    NOT NULL DEFAULT 'SOLICITADA'
                          CHECK (estado IN ('SOLICITADA', 'EJECUTADA', 'FALLIDA')),
    saga_codigo           UUID,
    ejecutado_en          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ
);
CREATE INDEX idx_reversion_pago_pago_id ON reversion_pago (pago_id);

-- ---------------------------------------------------------------------
-- outbox_evento: eventos pendientes de publicar (CU27, CU44).
-- Se escribe en la misma transacción que el cambio de estado del pago;
-- un proceso aparte publica a RabbitMQ y marca publicado_en.
-- ---------------------------------------------------------------------
CREATE TABLE outbox_evento (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_outbox_evento UUID        NOT NULL UNIQUE,
    tipo                 VARCHAR(60) NOT NULL,
    payload              JSONB       NOT NULL,
    publicado_en         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
-- El publicador busca los pendientes en orden de creación.
CREATE INDEX idx_outbox_evento_pendientes ON outbox_evento (created_at) WHERE publicado_en IS NULL;
