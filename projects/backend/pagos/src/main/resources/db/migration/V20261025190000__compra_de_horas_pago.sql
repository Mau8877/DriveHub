-- =====================================================================
-- Compra de horas (CU26): el pago guarda copia de lo comprado y se eliminan
-- las cuotas y las inscripciones a un plan, que ya no tienen caso de uso.
-- Las tablas pago, cuota e inscripcion_plan estan vacias en todos los entornos.
-- Ver docs/DriveHub/DISENO_BASE_DE_DATOS.md (seccion 4).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Cuotas e inscripciones: se eliminan (el plan se paga completo, sin cuotas)
-- ---------------------------------------------------------------------
ALTER TABLE pago DROP COLUMN cuota_id;
DROP TABLE cuota;
DROP TABLE inscripcion_plan;

-- ---------------------------------------------------------------------
-- pago: concepto, metodo y moneda de la compra de horas
-- ---------------------------------------------------------------------
ALTER TABLE pago DROP CONSTRAINT pago_concepto_check;
ALTER TABLE pago DROP CONSTRAINT pago_metodo_check;
ALTER TABLE pago ADD CONSTRAINT ck_pago_concepto CHECK (concepto IN ('HORAS', 'CLASE', 'EXAMEN'));
ALTER TABLE pago ADD CONSTRAINT ck_pago_metodo CHECK (metodo IN ('TARJETA'));
ALTER TABLE pago ALTER COLUMN moneda SET DEFAULT 'USD';

-- ---------------------------------------------------------------------
-- pago: lo comprado queda copiado (un cambio posterior del plan o del tipo de
-- cambio no altera un pago ya creado) y la vigencia de la pagina de pago
-- ---------------------------------------------------------------------
ALTER TABLE pago ADD COLUMN escuela_codigo     UUID NOT NULL;
ALTER TABLE pago ADD COLUMN plan_pago_id       BIGINT REFERENCES plan_pago (id);
ALTER TABLE pago ADD COLUMN plan_nombre        VARCHAR(80);
ALTER TABLE pago ADD COLUMN horas              SMALLINT;
ALTER TABLE pago ADD COLUMN horas_restantes    SMALLINT;
ALTER TABLE pago ADD COLUMN monto_bs           NUMERIC(10, 2);
ALTER TABLE pago ADD COLUMN tipo_cambio        NUMERIC(8, 4);
ALTER TABLE pago ADD COLUMN estudiante_nombre  VARCHAR(160);
ALTER TABLE pago ADD COLUMN escuela_nombre     VARCHAR(120);
ALTER TABLE pago ADD COLUMN expira_en          TIMESTAMPTZ;

ALTER TABLE pago ADD CONSTRAINT ck_pago_horas
    CHECK (horas IS NULL OR horas BETWEEN 1 AND 500);
ALTER TABLE pago ADD CONSTRAINT ck_pago_horas_restantes
    CHECK (horas_restantes IS NULL OR horas_restantes BETWEEN 0 AND horas);
ALTER TABLE pago ADD CONSTRAINT ck_pago_horas_requiere_plan
    CHECK (concepto <> 'HORAS'
           OR (plan_pago_id IS NOT NULL AND horas IS NOT NULL AND monto_bs IS NOT NULL AND tipo_cambio IS NOT NULL));

CREATE INDEX idx_pago_escuela_codigo ON pago (escuela_codigo);
CREATE INDEX idx_pago_plan_pago_id ON pago (plan_pago_id);
-- Para el proceso que vence los pagos pendientes.
CREATE INDEX idx_pago_estado_expira_en ON pago (estado, expira_en);

-- ---------------------------------------------------------------------
-- transaccion_pasarela: la direccion de la pagina de pago ya no es un QR
-- ---------------------------------------------------------------------
ALTER TABLE transaccion_pasarela RENAME COLUMN qr_payload TO url_pago;
