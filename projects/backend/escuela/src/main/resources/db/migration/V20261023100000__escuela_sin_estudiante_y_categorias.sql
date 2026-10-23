-- =====================================================================
-- CU08a: Escuela deja de tener tabla de estudiantes y carga el catálogo de categorías
-- =====================================================================
-- El estudiante es una cuenta con su perfil en Identidad (CU08a): todo lo que Escuela necesita
-- de él lo referencia por el codigo_usuario de la persona, sin FK (otro servicio). El CI de la
-- persona también pasa a perfil, así que instructor deja de repetirlo.

-- asignacion_alumno: ahora apunta a la persona por su codigo. Se vacía antes (entorno de
-- desarrollo; ningún caso de uso la usa todavía).
DELETE FROM asignacion_alumno;
DROP INDEX IF EXISTS uq_asignacion_alumno_vigente;
ALTER TABLE asignacion_alumno DROP COLUMN estudiante_id;
ALTER TABLE asignacion_alumno ADD COLUMN estudiante_codigo UUID NOT NULL;
-- Un estudiante tiene un solo instructor vigente.
CREATE UNIQUE INDEX uq_asignacion_alumno_vigente ON asignacion_alumno (estudiante_codigo)
    WHERE fecha_fin IS NULL AND deleted_at IS NULL;

DROP TABLE estudiante;

ALTER TABLE instructor DROP COLUMN ci;

-- Catálogo de categorías de licencia (Bolivia) y la opción "sin categoría", con la que arranca
-- todo estudiante. edad_minima y clases_minimas quedan sin definir hasta confirmar los valores.
INSERT INTO categoria_licencia (codigo_categoria_licencia, sigla, descripcion)
VALUES (uuidv7(), 'P', 'Particular'),
       (uuidv7(), 'M', 'Motocicleta'),
       (uuidv7(), 'A', 'Profesional A'),
       (uuidv7(), 'B', 'Profesional B'),
       (uuidv7(), 'C', 'Profesional C'),
       (uuidv7(), 'SIN', 'Sin categoría');
