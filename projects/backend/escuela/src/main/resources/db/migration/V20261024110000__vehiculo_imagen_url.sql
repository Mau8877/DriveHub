-- =====================================================================
-- CU07: foto del auto. El archivo vive en Cloudinary; acá solo se guarda su URL https.
-- =====================================================================
ALTER TABLE vehiculo ADD COLUMN imagen_url VARCHAR(300);
