-- CU04b: verificación del correo. Todas las cuentas (también las existentes) empiezan sin verificar.
ALTER TABLE usuario
    ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT FALSE;

-- token_recuperacion pasa a guardar también los enlaces de verificación del correo.
ALTER TABLE token_recuperacion
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'RECUPERACION'
        CHECK (tipo IN ('RECUPERACION', 'VERIFICACION'));
