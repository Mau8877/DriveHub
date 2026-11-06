-- CU47: iniciar sesión por WhatsApp. El número se guarda solo como hash (HMAC); nunca en claro.
CREATE TABLE vinculo_whatsapp (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_vinculo_whatsapp  UUID        NOT NULL UNIQUE,
    usuario_id               BIGINT      NOT NULL UNIQUE REFERENCES usuario (id),
    telefono_hash            VARCHAR(64)  NOT NULL UNIQUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ
);

-- Códigos de 6 dígitos enviados al correo. `usuario_id` es nulo cuando el correo no tenía una cuenta válida:
-- se registra igual (ya descartado) para que el límite de pedidos cuente lo mismo exista o no la cuenta.
CREATE TABLE desafio_whatsapp (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_desafio_whatsapp   UUID        NOT NULL UNIQUE,
    usuario_id                BIGINT      REFERENCES usuario (id),
    telefono_hash             VARCHAR(64)  NOT NULL,
    correo_hash               VARCHAR(64)  NOT NULL,
    codigo_hash               VARCHAR(64)  NOT NULL,
    intentos                  INTEGER     NOT NULL DEFAULT 0,
    expira_en                 TIMESTAMPTZ NOT NULL,
    usado                     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ
);

CREATE INDEX idx_desafio_whatsapp_telefono ON desafio_whatsapp (telefono_hash, created_at);
CREATE INDEX idx_desafio_whatsapp_correo ON desafio_whatsapp (correo_hash, created_at);
CREATE INDEX idx_desafio_whatsapp_usuario ON desafio_whatsapp (usuario_id, created_at);
