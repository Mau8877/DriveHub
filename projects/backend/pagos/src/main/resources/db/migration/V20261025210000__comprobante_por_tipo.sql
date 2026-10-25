-- ---------------------------------------------------------------------
-- comprobante por tipo (CU30): un pago confirmado tiene su comprobante de PAGO y, si se
-- reembolsa, otro de REEMBOLSO ligado a la reversión. Cada uno con su número.
-- La tabla está vacía en todos los entornos (ningún caso de uso la usaba todavía).
-- ---------------------------------------------------------------------

-- * Antes era un solo comprobante por pago (UNIQUE de la columna, con el nombre que le dio Postgres)
ALTER TABLE comprobante DROP CONSTRAINT comprobante_pago_id_key;

ALTER TABLE comprobante
    ADD COLUMN tipo VARCHAR(10) NOT NULL DEFAULT 'PAGO',
    ADD COLUMN reversion_pago_id BIGINT REFERENCES reversion_pago (id);

ALTER TABLE comprobante
    ADD CONSTRAINT ck_comprobante_tipo CHECK (tipo IN ('PAGO', 'REEMBOLSO')),
    ADD CONSTRAINT uq_comprobante_pago_tipo UNIQUE (pago_id, tipo),
    ADD CONSTRAINT ck_comprobante_reembolso_con_reversion CHECK (tipo <> 'REEMBOLSO' OR reversion_pago_id IS NOT NULL);

CREATE INDEX idx_comprobante_reversion_pago_id ON comprobante (reversion_pago_id);

-- * url_pdf guarda la CLAVE del objeto en el almacenamiento (NULL = PDF pendiente de generar), no una dirección pública
COMMENT ON COLUMN comprobante.url_pdf IS 'Clave del PDF en el bucket privado; NULL mientras esta pendiente';

-- Números correlativos de los comprobantes (CMP-2026-000123)
CREATE SEQUENCE comprobante_nro_seq START WITH 1 INCREMENT BY 1;
