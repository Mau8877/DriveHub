-- =====================================================================
-- Esquema inicial del microservicio Identidad
-- Fuente: docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf (sección 1)
-- =====================================================================
-- Convenciones (ver .claude/rules/backend/PERSISTENCIA_BD_BACKEND.md):
--   * id BIGINT IDENTITY: PK interna, nunca sale del servicio.
--   * codigo_{tabla} UUID: identificador público (v7, lo genera la aplicación).
--   * created_at / updated_at / deleted_at en todas las tablas (soft delete).
--   * Estados como texto con CHECK de valores fijos.

-- ---------------------------------------------------------------------
-- usuario: cuenta de acceso (CU01, CU02, CU06)
-- codigo_usuario es el identificador de PERSONA en toda la plataforma.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_usuario    UUID         NOT NULL UNIQUE,
    email             VARCHAR(120) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    nombres           VARCHAR(80)  NOT NULL,
    apellidos         VARCHAR(80)  NOT NULL,
    telefono          VARCHAR(20),
    foto_url          TEXT,
    estado            VARCHAR(12)  NOT NULL DEFAULT 'ACTIVO'
                      CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    ultimo_login      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- rol: roles del sistema (CU05)
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_rol  UUID         NOT NULL UNIQUE,
    nombre      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(120),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- permiso: permisos finos (CU05)
-- `clave` es el "codigo" del diseño original (ej. reserva.crear), renombrado
-- para no confundirlo con codigo_permiso (UUID público).
-- ---------------------------------------------------------------------
CREATE TABLE permiso (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_permiso UUID         NOT NULL UNIQUE,
    clave          VARCHAR(60)  NOT NULL UNIQUE,
    descripcion    VARCHAR(120),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- usuario_rol: roles de cada usuario (CU01, CU05)
-- ---------------------------------------------------------------------
CREATE TABLE usuario_rol (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_usuario_rol UUID        NOT NULL UNIQUE,
    usuario_id         BIGINT      NOT NULL REFERENCES usuario (id),
    rol_id             BIGINT      NOT NULL REFERENCES rol (id),
    asignado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ,
    UNIQUE (usuario_id, rol_id)
);
CREATE INDEX idx_usuario_rol_rol_id ON usuario_rol (rol_id);

-- ---------------------------------------------------------------------
-- rol_permiso: permisos de cada rol (CU05)
-- ---------------------------------------------------------------------
CREATE TABLE rol_permiso (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_rol_permiso UUID        NOT NULL UNIQUE,
    rol_id             BIGINT      NOT NULL REFERENCES rol (id),
    permiso_id         BIGINT      NOT NULL REFERENCES permiso (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ,
    UNIQUE (rol_id, permiso_id)
);
CREATE INDEX idx_rol_permiso_permiso_id ON rol_permiso (permiso_id);

-- ---------------------------------------------------------------------
-- refresh_token: sesiones activas, login único (CU02, CU03)
-- Solo el hash SHA-256: el token en claro lo tiene el cliente.
-- Un job borra los tokens vencidos.
-- ---------------------------------------------------------------------
CREATE TABLE refresh_token (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_refresh_token UUID        NOT NULL UNIQUE,
    usuario_id           BIGINT      NOT NULL REFERENCES usuario (id),
    token_hash           VARCHAR(64) NOT NULL UNIQUE,
    dispositivo          VARCHAR(10) NOT NULL CHECK (dispositivo IN ('WEB', 'MOVIL')),
    expira_en            TIMESTAMPTZ NOT NULL,
    revocado             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
CREATE INDEX idx_refresh_token_usuario_revocado ON refresh_token (usuario_id, revocado);

-- ---------------------------------------------------------------------
-- token_recuperacion: restablecer contraseña (CU04), un solo uso
-- ---------------------------------------------------------------------
CREATE TABLE token_recuperacion (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_token_recuperacion UUID        NOT NULL UNIQUE,
    usuario_id                BIGINT      NOT NULL REFERENCES usuario (id),
    token_hash                VARCHAR(64) NOT NULL UNIQUE,
    expira_en                 TIMESTAMPTZ NOT NULL,
    usado                     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ
);
CREATE INDEX idx_token_recuperacion_usuario_id ON token_recuperacion (usuario_id);

-- ---------------------------------------------------------------------
-- Roles base de la plataforma (actores de los casos de uso).
-- uuidv7() es nativa de PostgreSQL 18; en runtime los códigos los genera
-- la aplicación (UuidV7), acá se usa la función solo para el seed.
-- ---------------------------------------------------------------------
INSERT INTO rol (codigo_rol, nombre, descripcion) VALUES
    (uuidv7(), 'ESTUDIANTE', 'Alumno de la autoescuela'),
    (uuidv7(), 'INSTRUCTOR', 'Docente que dicta las clases prácticas'),
    (uuidv7(), 'ADMIN', 'Administración de la escuela');
