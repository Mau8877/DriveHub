-- CU48: registrarse por WhatsApp. Los datos que la persona dicta por el chat esperan acá hasta que fija su
-- contraseña en la web; entonces se crea la cuenta y la fila se BORRA (o la borra la limpieza al vencer).
-- La dirección no se guarda: se calcula al crear la cuenta, igual que en el registro normal.
CREATE TABLE solicitud_registro_whatsapp (
    id                                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_solicitud_registro_whatsapp  UUID          NOT NULL UNIQUE,
    telefono                            VARCHAR(20)   NOT NULL,
    nombres                             VARCHAR(80)   NOT NULL,
    apellidos                           VARCHAR(80)   NOT NULL,
    fecha_nacimiento                    DATE          NOT NULL,
    sexo                                VARCHAR(10)   NOT NULL CHECK (sexo IN ('MASCULINO', 'FEMENINO')),
    ci                                  VARCHAR(15)   NOT NULL,
    correo                              VARCHAR(120)  NOT NULL,
    latitud                             NUMERIC(9, 6) NOT NULL,
    longitud                            NUMERIC(9, 6) NOT NULL,
    token_hash                          VARCHAR(64)   NOT NULL UNIQUE,
    expira_en                           TIMESTAMPTZ   NOT NULL,
    created_at                          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at                          TIMESTAMPTZ
);

CREATE INDEX idx_solicitud_registro_whatsapp_telefono ON solicitud_registro_whatsapp (telefono);
CREATE INDEX idx_solicitud_registro_whatsapp_expira ON solicitud_registro_whatsapp (expira_en);
