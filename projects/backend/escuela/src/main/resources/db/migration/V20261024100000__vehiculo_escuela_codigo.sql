-- =====================================================================
-- CU07: cada auto pertenece a una escuela (tenant).
-- escuela_codigo = escuela.codigo_escuela, el claim `escuela` del JWT. Sin FK física, como
-- en el resto de los servicios: el filtro por escuela lo aplica el repositorio.
-- La tabla estaba vacía (CU07 es el primer caso de uso que la usa), por eso es NOT NULL.
-- =====================================================================
ALTER TABLE vehiculo ADD COLUMN escuela_codigo UUID NOT NULL;
CREATE INDEX idx_vehiculo_escuela_codigo ON vehiculo (escuela_codigo);
