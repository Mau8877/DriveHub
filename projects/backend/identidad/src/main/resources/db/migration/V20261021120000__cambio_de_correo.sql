-- CU06: cambio de correo. token_recuperacion guarda también los enlaces de cambio de correo,
-- junto con la dirección nueva que todavía no es la de la cuenta.
ALTER TABLE token_recuperacion
    ADD COLUMN correo_nuevo VARCHAR(120);

ALTER TABLE token_recuperacion
    DROP CONSTRAINT token_recuperacion_tipo_check;

ALTER TABLE token_recuperacion
    ADD CONSTRAINT token_recuperacion_tipo_check
        CHECK (tipo IN ('RECUPERACION', 'VERIFICACION', 'CAMBIO_CORREO'));

-- Solo los enlaces de cambio de correo llevan la dirección nueva, y todos la llevan.
ALTER TABLE token_recuperacion
    ADD CONSTRAINT token_recuperacion_correo_nuevo_check
        CHECK ((tipo = 'CAMBIO_CORREO') = (correo_nuevo IS NOT NULL));
