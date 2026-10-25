-- ---------------------------------------------------------------------
-- reversion_pago con origen y retención (CU31): la devolución puede venir de la saga
-- (compensación, 100 %) o del estudiante (reembolso, menos la retención). Una sola
-- reversión vigente por pago.
-- La tabla está vacía en todos los entornos (ningún caso de uso la escribía todavía).
-- ---------------------------------------------------------------------

ALTER TABLE reversion_pago
    ADD COLUMN origen VARCHAR(10) NOT NULL,
    ADD COLUMN monto_retenido NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN referencia_externa VARCHAR(80),
    ADD COLUMN motivo_detalle VARCHAR(200);

-- * motivo guarda el CÓDIGO (MotivoReembolso o MotivoCompensacion); el texto que escribe el estudiante va en motivo_detalle
ALTER TABLE reversion_pago
    ALTER COLUMN motivo TYPE VARCHAR(30),
    ALTER COLUMN motivo SET NOT NULL,
    ALTER COLUMN monto SET NOT NULL;

ALTER TABLE reversion_pago
    ADD CONSTRAINT ck_reversion_pago_origen CHECK (origen IN ('SAGA', 'ESTUDIANTE')),
    ADD CONSTRAINT ck_reversion_pago_monto CHECK (monto > 0),
    ADD CONSTRAINT ck_reversion_pago_retenido CHECK (monto_retenido >= 0),
    -- Solo el motivo OTRO lleva detalle, y OTRO siempre lo lleva
    ADD CONSTRAINT ck_reversion_pago_motivo_detalle CHECK ((motivo = 'OTRO') = (motivo_detalle IS NOT NULL));

CREATE INDEX idx_reversion_pago_referencia_externa ON reversion_pago (referencia_externa);

-- Una sola reversión vigente (solicitada o ejecutada) por pago; una fallida no estorba para intentar de nuevo
CREATE UNIQUE INDEX uq_reversion_pago_vigente ON reversion_pago (pago_id) WHERE estado <> 'FALLIDA';
