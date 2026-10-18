# Contenido

Manuales de la escuela y tutoriales de los instructores (metadatos; los archivos viven en MinIO). CU32 a CU35.

| | |
| --- | --- |
| Stack | Laravel 13, PHP 8.4 (`ext-mongodb`) |
| Arquitectura | capas por feature — ver [`LARAVEL_BACKEND.md`](../../../.claude/rules/backend/LARAVEL_BACKEND.md) |
| Base de datos | MongoDB (`contenido`), paquete `mongodb/laravel-mongodb` |
| Puerto | 8085 |

## Correr

```bash
composer install
cp .env.example .env && php artisan key:generate
php artisan migrate          # colecciones + índices
php artisan serve --port=8085
php artisan test
./vendor/bin/pint --test
```

Sin PHP local: `docker compose up contenido` (Dockerfile, target `dev`). `GET /health` para verificar.
