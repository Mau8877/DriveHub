-- =====================================================================
-- MIGRACIÓN INICIAL (V1)
-- Descripción: Creación de esquemas base o tablas de control iniciales.
-- =====================================================================

-- Ejemplo de una tabla de auditoría global o extensión necesaria
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla de prueba/ejemplo para validar que Flyway opera correctamente
-- (Más adelante diseñaremos el esquema real del dominio)
CREATE TABLE IF NOT EXISTS system_metadata (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_metadata (key, value)
VALUES ('project_name', 'App')
ON CONFLICT (key) DO NOTHING;
