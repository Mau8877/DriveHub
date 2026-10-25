-- ---------------------------------------------------------------------
-- plan_pago de horas (CU25a): un plan es un nombre, una cantidad de horas y un costo en bolivianos,
-- y es de una escuela. La tabla estaba vacia (ningun caso de uso habia escrito planes), asi que se
-- pueden agregar columnas NOT NULL sin inventar valores.
--   * Se quitan categoria_sigla, nro_clases y nro_cuotas: las cuotas son del CU25, no de un plan.
--   * monto_total pasa a costo_bs (inscripcion_plan ya tiene su propio monto_total).
--   * El costo en dolares no se guarda: lo calcula el servicio con el tipo de cambio configurado.
-- ---------------------------------------------------------------------
ALTER TABLE plan_pago DROP COLUMN categoria_sigla;
ALTER TABLE plan_pago DROP COLUMN nro_clases;
ALTER TABLE plan_pago DROP COLUMN nro_cuotas;

ALTER TABLE plan_pago RENAME COLUMN monto_total TO costo_bs;

-- escuela_codigo: codigo_escuela de Escuela, sin FK fisica (otro servicio)
ALTER TABLE plan_pago ADD COLUMN horas          SMALLINT NOT NULL;
ALTER TABLE plan_pago ADD COLUMN escuela_codigo UUID     NOT NULL;

ALTER TABLE plan_pago ADD CONSTRAINT ck_plan_pago_horas    CHECK (horas BETWEEN 1 AND 500);
ALTER TABLE plan_pago ADD CONSTRAINT ck_plan_pago_costo_bs CHECK (costo_bs > 0);

-- El nombre es unico dentro de la escuela sin distinguir mayusculas, incluidos los planes desactivados
CREATE UNIQUE INDEX uq_plan_pago_escuela_nombre
    ON plan_pago (escuela_codigo, lower(nombre))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_plan_pago_escuela_codigo_activo ON plan_pago (escuela_codigo, activo);
