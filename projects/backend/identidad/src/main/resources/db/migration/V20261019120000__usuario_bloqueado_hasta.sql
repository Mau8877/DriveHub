-- =====================================================================
-- CU02: hasta cuándo está bloqueada una cuenta
-- El bloqueo por intentos fallidos dura 15 minutos y se levanta solo.
-- =====================================================================

ALTER TABLE usuario ADD COLUMN bloqueado_hasta TIMESTAMPTZ;
