-- =====================================================================
-- Billetera de horas (CU27): el saldo de horas de cada estudiante y el libro de
-- movimientos del que se puede reconstruir. Las horas las acredita el aviso
-- confirmado de la pasarela (CU27) y las gasta Academico (consumo y devolucion).
-- Ver docs/DriveHub/DISENO_BASE_DE_DATOS.md (seccion 4).
-- =====================================================================

-- ---------------------------------------------------------------------
-- billetera_horas: una por estudiante y escuela. El saldo nunca es negativo.
-- version: bloqueo optimista, ultima defensa ante dos cambios simultaneos.
-- ---------------------------------------------------------------------
CREATE TABLE billetera_horas (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_billetera_horas   UUID        NOT NULL UNIQUE,
    escuela_codigo           UUID        NOT NULL,
    estudiante_codigo        UUID        NOT NULL,
    horas_disponibles        INT         NOT NULL DEFAULT 0,
    version                  BIGINT      NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ,
    CONSTRAINT ck_billetera_horas_saldo CHECK (horas_disponibles >= 0),
    CONSTRAINT uq_billetera_horas_escuela_estudiante UNIQUE (escuela_codigo, estudiante_codigo)
);

-- ---------------------------------------------------------------------
-- movimiento_horas: libro de la billetera. Cada fila mueve horas de UNA compra.
-- tipo: ACREDITACION (suma), CONSUMO (resta), DEVOLUCION (suma, clase cancelada o
-- reembolso fallido), REEMBOLSO (resta, horas apartadas por un reembolso).
-- ---------------------------------------------------------------------
CREATE TABLE movimiento_horas (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_movimiento_horas   UUID        NOT NULL UNIQUE,
    billetera_horas_id        BIGINT      NOT NULL REFERENCES billetera_horas (id),
    pago_id                   BIGINT      NOT NULL REFERENCES pago (id),
    tipo                      VARCHAR(13) NOT NULL,
    horas                     SMALLINT    NOT NULL,
    instructor_codigo         UUID,
    clase_codigo              UUID,
    idempotency_key           VARCHAR(64) NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ,
    CONSTRAINT ck_movimiento_horas_tipo CHECK (tipo IN ('ACREDITACION', 'CONSUMO', 'DEVOLUCION', 'REEMBOLSO')),
    CONSTRAINT ck_movimiento_horas_horas CHECK (horas > 0),
    -- Un reintento no acredita ni descuenta dos veces
    CONSTRAINT uq_movimiento_horas_clave_pago UNIQUE (idempotency_key, pago_id)
);
CREATE INDEX idx_movimiento_horas_billetera_created ON movimiento_horas (billetera_horas_id, created_at DESC);
CREATE INDEX idx_movimiento_horas_pago_id ON movimiento_horas (pago_id);
CREATE INDEX idx_movimiento_horas_clase_codigo ON movimiento_horas (clase_codigo);
