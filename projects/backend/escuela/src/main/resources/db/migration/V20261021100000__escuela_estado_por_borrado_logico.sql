-- =====================================================================
-- escuela: el borrado logico (deleted_at) pasa a ser la unica fuente del estado (CU05)
-- Desactivar una escuela es reversible: poner deleted_at. Reactivarla: quitarlo.
-- La columna estado era una segunda fuente que podia contradecirlo.
-- =====================================================================

UPDATE escuela
   SET deleted_at = now(),
       updated_at = now()
 WHERE estado = 'INACTIVA'
   AND deleted_at IS NULL;

ALTER TABLE escuela DROP COLUMN estado;
