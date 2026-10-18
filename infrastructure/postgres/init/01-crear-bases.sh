#!/bin/sh
# Crea una base y un usuario dueño por cada microservicio que usa PostgreSQL.
# * Se ejecuta UNA sola vez, cuando el volumen de datos está vacío (comportamiento
# * estándar de /docker-entrypoint-initdb.d). Si se agrega un servicio nuevo con el
# * volumen ya creado, hay que crear su base a mano o borrar el volumen
# * (docker volume rm drivehub-postgres_data).
# * Cada usuario solo es dueño de SU base: "ningún servicio lee las tablas de otro".
set -e

crear_base() {
  nombre="$1"
  password="$2"
  echo "Creando base y usuario '$nombre'"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    CREATE USER $nombre WITH PASSWORD '$password';
    CREATE DATABASE $nombre OWNER $nombre;
    REVOKE ALL ON DATABASE $nombre FROM PUBLIC;
SQL
}

crear_base identidad "$IDENTIDAD_DB_PASSWORD"
crear_base escuela "$ESCUELA_DB_PASSWORD"
crear_base pagos "$PAGOS_DB_PASSWORD"
