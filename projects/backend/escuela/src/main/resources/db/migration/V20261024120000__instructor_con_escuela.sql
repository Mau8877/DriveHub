-- CU09: la ficha del instructor pertenece a una escuela.
-- Hasta ahora ningún caso de uso escribía en `instructor` (la tabla está vacía en todos los
-- entornos), por eso la columna nace NOT NULL sin valor por defecto: una fila sin escuela no tiene sentido.

ALTER TABLE instructor ADD COLUMN escuela_codigo UUID NOT NULL;

CREATE INDEX idx_instructor_escuela_codigo_estado ON instructor (escuela_codigo, estado);
