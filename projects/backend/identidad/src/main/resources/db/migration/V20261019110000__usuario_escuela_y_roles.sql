-- =====================================================================
-- CU01: fecha de nacimiento, escuela (tenant) y roles reales del sistema
-- =====================================================================

-- fecha_nacimiento: el registro valida la edad mínima. NOT NULL porque aún
-- no hay usuarios cargados (ningún caso de uso estaba implementado).
ALTER TABLE usuario ADD COLUMN fecha_nacimiento DATE NOT NULL;

-- escuela_codigo: escuela (tenant) a la que pertenece la cuenta, referencia a
-- Escuela sin FK física. NULL para el Super admin, que no pertenece a ninguna.
ALTER TABLE usuario ADD COLUMN escuela_codigo UUID;
CREATE INDEX idx_usuario_escuela_codigo ON usuario (escuela_codigo);

-- Roles: ADMIN pasa a ser ADMIN_ESCUELA y se agrega SUPER_ADMIN.
UPDATE rol
   SET nombre = 'ADMIN_ESCUELA',
       descripcion = 'Administrador de una escuela'
 WHERE nombre = 'ADMIN';

INSERT INTO rol (codigo_rol, nombre, descripcion) VALUES
    (uuidv7(), 'SUPER_ADMIN', 'Administrador de la plataforma');
