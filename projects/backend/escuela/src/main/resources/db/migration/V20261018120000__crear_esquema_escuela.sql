-- =====================================================================
-- Esquema inicial del microservicio Escuela
-- Fuente: docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf (sección 2)
-- =====================================================================
-- Convenciones (ver .claude/rules/backend/PERSISTENCIA_BD_BACKEND.md):
--   * id BIGINT IDENTITY: PK interna, nunca sale del servicio.
--   * codigo_{tabla} UUID: identificador público (v7, lo genera la aplicación).
--   * {x}_codigo UUID: referencia a otro servicio (sin FK física).
--   * created_at / updated_at / deleted_at en todas las tablas (soft delete).

-- ---------------------------------------------------------------------
-- categoria_licencia: catálogo. `sigla` (A, B, C...) es el "codigo" del
-- diseño original: el valor que usan los demás servicios.
-- ---------------------------------------------------------------------
CREATE TABLE categoria_licencia (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_categoria_licencia UUID         NOT NULL UNIQUE,
    sigla                     VARCHAR(3)   NOT NULL UNIQUE,
    descripcion               VARCHAR(100) NOT NULL,
    edad_minima               SMALLINT,
    clases_minimas            SMALLINT,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- estudiante: perfil (CU08). usuario_codigo = Identidad usuario.codigo_usuario
-- ---------------------------------------------------------------------
CREATE TABLE estudiante (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_estudiante UUID         NOT NULL UNIQUE,
    usuario_codigo    UUID         NOT NULL UNIQUE,
    ci                VARCHAR(15)  NOT NULL UNIQUE,
    fecha_nacimiento  DATE,
    direccion         VARCHAR(200),
    categoria_id      BIGINT       REFERENCES categoria_licencia (id),
    estado            VARCHAR(12)  NOT NULL DEFAULT 'ACTIVO'
                      CHECK (estado IN ('ACTIVO', 'EGRESADO', 'RETIRADO')),
    fecha_inscripcion DATE,
    observaciones     TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);
CREATE INDEX idx_estudiante_categoria_id ON estudiante (categoria_id);

-- ---------------------------------------------------------------------
-- instructor: perfil del docente (CU09)
-- ---------------------------------------------------------------------
CREATE TABLE instructor (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_instructor  UUID        NOT NULL UNIQUE,
    usuario_codigo     UUID        NOT NULL UNIQUE,
    ci                 VARCHAR(15) NOT NULL UNIQUE,
    nro_licencia       VARCHAR(30) NOT NULL UNIQUE,
    licencia_vence     DATE,
    estado             VARCHAR(12) NOT NULL DEFAULT 'ACTIVO'
                       CHECK (estado IN ('ACTIVO', 'INACTIVO', 'LICENCIA')),
    fecha_contratacion DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- instructor_categoria: categorías que puede enseñar cada instructor (CU09)
-- ---------------------------------------------------------------------
CREATE TABLE instructor_categoria (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_instructor_categoria UUID        NOT NULL UNIQUE,
    instructor_id               BIGINT      NOT NULL REFERENCES instructor (id),
    categoria_id                BIGINT      NOT NULL REFERENCES categoria_licencia (id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                  TIMESTAMPTZ,
    UNIQUE (instructor_id, categoria_id)
);
CREATE INDEX idx_instructor_categoria_categoria_id ON instructor_categoria (categoria_id);

-- ---------------------------------------------------------------------
-- vehiculo: autos de la escuela (CU07)
-- ---------------------------------------------------------------------
CREATE TABLE vehiculo (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_vehiculo  UUID        NOT NULL UNIQUE,
    placa            VARCHAR(10) NOT NULL UNIQUE,
    marca            VARCHAR(40) NOT NULL,
    modelo           VARCHAR(40) NOT NULL,
    anio             SMALLINT,
    color            VARCHAR(20),
    transmision      VARCHAR(10) NOT NULL CHECK (transmision IN ('MANUAL', 'AUTOMATICO')),
    categoria_id     BIGINT      REFERENCES categoria_licencia (id),
    estado           VARCHAR(14) NOT NULL DEFAULT 'DISPONIBLE'
                     CHECK (estado IN ('DISPONIBLE', 'MANTENIMIENTO', 'BAJA')),
    kilometraje      INT         NOT NULL DEFAULT 0,
    soat_vence       DATE,
    inspeccion_vence DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
CREATE INDEX idx_vehiculo_categoria_id ON vehiculo (categoria_id);

-- ---------------------------------------------------------------------
-- mantenimiento_vehiculo: historial de mantenimiento (CU07)
-- ---------------------------------------------------------------------
CREATE TABLE mantenimiento_vehiculo (
    id                            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_mantenimiento_vehiculo UUID        NOT NULL UNIQUE,
    vehiculo_id                   BIGINT      NOT NULL REFERENCES vehiculo (id),
    fecha                         DATE        NOT NULL,
    tipo                          VARCHAR(12) NOT NULL CHECK (tipo IN ('PREVENTIVO', 'CORRECTIVO')),
    descripcion                   TEXT,
    costo                         NUMERIC(10, 2),
    km_al_momento                 INT,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                    TIMESTAMPTZ
);
CREATE INDEX idx_mantenimiento_vehiculo_vehiculo_id ON mantenimiento_vehiculo (vehiculo_id);

-- ---------------------------------------------------------------------
-- asignacion_vehiculo: auto asignado a un instructor (CU10)
-- ---------------------------------------------------------------------
CREATE TABLE asignacion_vehiculo (
    id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_asignacion_vehiculo UUID        NOT NULL UNIQUE,
    vehiculo_id                BIGINT      NOT NULL REFERENCES vehiculo (id),
    instructor_id              BIGINT      NOT NULL REFERENCES instructor (id),
    fecha_inicio               DATE        NOT NULL,
    fecha_fin                  DATE,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                 TIMESTAMPTZ
);
-- Un auto tiene una sola asignación vigente.
CREATE UNIQUE INDEX uq_asignacion_vehiculo_vigente ON asignacion_vehiculo (vehiculo_id)
    WHERE fecha_fin IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_asignacion_vehiculo_instructor_id ON asignacion_vehiculo (instructor_id);

-- ---------------------------------------------------------------------
-- asignacion_alumno: instructor titular de cada estudiante (CU08, CU29).
-- Pagos la consulta (por API, nunca por SQL) para resolver "mis alumnos".
-- ---------------------------------------------------------------------
CREATE TABLE asignacion_alumno (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_asignacion_alumno UUID        NOT NULL UNIQUE,
    instructor_id            BIGINT      NOT NULL REFERENCES instructor (id),
    estudiante_id            BIGINT      NOT NULL REFERENCES estudiante (id),
    fecha_inicio             DATE        NOT NULL,
    fecha_fin                DATE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ
);
-- Un estudiante tiene un solo instructor vigente.
CREATE UNIQUE INDEX uq_asignacion_alumno_vigente ON asignacion_alumno (estudiante_id)
    WHERE fecha_fin IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_asignacion_alumno_instructor_id ON asignacion_alumno (instructor_id);
