-- =====================================================================
-- CU08a: el perfil de la persona se separa de la cuenta de acceso
-- =====================================================================
-- usuario queda solo con lo de acceso (correo, contraseña, estado, intentos, bloqueo,
-- verificación). Los datos de la persona pasan a perfil, 1 a 1 con usuario, y se suman CI,
-- sexo, ubicación y la categoría de licencia del estudiante.
--
-- ci y sexo son NOT NULL para todos. direccion, latitud, longitud y categoria_sigla son
-- nulables en la base (el Super admin no tiene ubicación; solo el estudiante tiene categoría):
-- el caso de uso las exige al crear un Administrador, un Instructor o un Estudiante.
-- categoria_sigla referencia escuela.categoria_licencia.sigla, sin FK (otro servicio).

CREATE TABLE perfil (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_perfil    UUID          NOT NULL UNIQUE,
    usuario_id       BIGINT        NOT NULL UNIQUE REFERENCES usuario (id),
    nombres          VARCHAR(80)   NOT NULL,
    apellidos        VARCHAR(80)   NOT NULL,
    telefono         VARCHAR(20),
    foto_url         TEXT,
    fecha_nacimiento DATE          NOT NULL,
    ci               VARCHAR(15)   NOT NULL UNIQUE,
    sexo             VARCHAR(10)   NOT NULL CHECK (sexo IN ('MASCULINO', 'FEMENINO')),
    direccion        VARCHAR(300),
    latitud          NUMERIC(9, 6),
    longitud         NUMERIC(9, 6),
    categoria_sigla  VARCHAR(3),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    CHECK ((latitud IS NULL) = (longitud IS NULL)),
    CHECK (latitud BETWEEN -90 AND 90),
    CHECK (longitud BETWEEN -180 AND 180)
);

-- Cuentas existentes (entorno de desarrollo): los datos de la persona se copian tal cual y los
-- campos nuevos reciben valores provisionales. ci = 'X' + id con ceros (único); los estudiantes
-- arrancan en la categoría SIN.
INSERT INTO perfil (codigo_perfil, usuario_id, nombres, apellidos, telefono, foto_url,
                    fecha_nacimiento, ci, sexo, direccion, categoria_sigla,
                    created_at, updated_at, deleted_at)
SELECT uuidv7(),
       u.id,
       u.nombres,
       u.apellidos,
       u.telefono,
       u.foto_url,
       u.fecha_nacimiento,
       'X' || lpad(u.id::text, 9, '0'),
       'MASCULINO',
       CASE WHEN u.escuela_codigo IS NULL THEN NULL ELSE 'X' END,
       CASE WHEN EXISTS (SELECT 1
                         FROM usuario_rol ur
                                  JOIN rol r ON r.id = ur.rol_id
                         WHERE ur.usuario_id = u.id
                           AND r.nombre = 'ESTUDIANTE') THEN 'SIN' END,
       u.created_at,
       u.updated_at,
       u.deleted_at
FROM usuario u;

ALTER TABLE usuario
    DROP COLUMN nombres,
    DROP COLUMN apellidos,
    DROP COLUMN telefono,
    DROP COLUMN foto_url,
    DROP COLUMN fecha_nacimiento;
