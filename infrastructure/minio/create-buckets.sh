#!/bin/sh
set -e

# * `mc config host add` (el comando viejo) ya no existe en versiones
# * actuales de mc -- falla como "not a recognized command" y, sin `set -e`,
# * los pasos siguientes operaban sobre una ruta local del contenedor en vez
# * de MinIO real. `mc alias set` es el comando vigente.
mc alias set myminio http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Un bucket por servicio dueño de archivos (cada nombre coincide con la
# config de storage de su servicio):
#   identidad     -> fotos de perfil (usuario.foto_url)
#   pagos         -> PDF de comprobantes (comprobante.url_pdf)
#   contenido     -> manuales (PDF) y tutoriales (video)
#   asistente-ia  -> fotos del tablero (AyudasTablero.imagenKey)
for bucket in identidad pagos contenido asistente-ia; do
  mc mb "myminio/drivehub-$bucket" --ignore-existing
done

# Solo el contenido de la escuela es de lectura pública (manuales/tutoriales
# publicados). Comprobantes, fotos de perfil y fotos del tablero son
# privados: se sirven con URLs prefirmadas desde su servicio.
mc anonymous set download myminio/drivehub-contenido

exit 0
