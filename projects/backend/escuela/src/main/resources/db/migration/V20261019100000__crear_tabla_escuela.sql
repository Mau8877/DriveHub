-- =====================================================================
-- escuela: tenant de la plataforma SaaS (CU01)
-- Cada autoescuela cliente es una fila. `slug` es el identificador legible
-- que viaja en el enlace de registro (/registro/{slug}).
-- =====================================================================

CREATE TABLE escuela (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_escuela UUID         NOT NULL UNIQUE,
    slug          VARCHAR(60)  NOT NULL UNIQUE,
    nombre        VARCHAR(120) NOT NULL,
    estado        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVA'
                  CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
