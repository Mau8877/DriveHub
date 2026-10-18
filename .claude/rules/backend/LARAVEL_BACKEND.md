---
globs: projects/backend/contenido/**/*
---

# Laravel — Contenido

El microservicio **Contenido** (`projects/backend/contenido`) está en
**Laravel 13** (PHP 8.4) con arquitectura de **capas por feature**, el
mismo criterio que [ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md)
traducido a Laravel. Base de datos: **MongoDB** con el paquete oficial
`mongodb/laravel-mongodb` (ver
[PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md#mongodb)).
Solo guarda metadatos: los PDF y videos viven en el bucket
`drivehub-contenido` de MinIO.

## Estructura

```
app/
├── Common/
│   ├── Responses/ApiResponse.php   # Sobre estándar
│   ├── Exceptions/                 # BusinessException + ValidationException, NotFoundException...
│   ├── Models/BaseModel.php        # _id entero (Secuencia) + codigo UUID v7 + soft delete
│   ├── Support/Secuencia.php       # Secuencias por colección (colección `contadores`)
│   └── Enums/                      # Enums que comparten 2+ features (EstadoPublicacion)
├── Health/HealthController.php     # GET /health
└── Features/
    └── {Feature}/                  # Manuales/, Tutoriales/
        ├── Controllers/            # Entrada HTTP
        ├── Requests/               # FormRequest (validación + autorización del request)
        ├── Services/               # Lógica de negocio, logging, lanza BusinessException
        ├── Repositories/           # Acceso a datos (Eloquent), sin lógica
        ├── Models/                 # Models Eloquent de MongoDB
        ├── Resources/              # JsonResource: Model -> forma del DTO de salida
        └── Enums/
bootstrap/app.php                   # Rutas (api/v1 + /health) y render de excepciones al sobre
routes/api.php                      # Rutas de negocio (montadas bajo /api/v1)
database/migrations/                # Colecciones + índices
```

Hoy existen los Models, enums y la migración de colecciones/índices: el
resto de las carpetas de cada feature tiene solo `.gitkeep` hasta el primer
caso de uso (CU32 a CU35).

## Reglas

- **Mismas capas que en Spring**: controller delgado (recibe un
  FormRequest ya validado, llama a un método del service, devuelve
  `response()->json(ApiResponse::success(...))` con un Resource), service
  con la lógica (única capa que loguea y lanza `BusinessException`),
  repository sin lógica. **No** se usa Eloquent directo desde el controller.
- **Errores**: solo `BusinessException` (y subclases) con código
  `ERR_CON_NN`. Nunca `abort()` ni excepciones genéricas desde código de
  negocio. `bootstrap/app.php` traduce las de Laravel (validación →
  `ERR_SYS_01`, ruta inexistente → `ERR_SYS_03`, resto → `ERR_SYS_00`).
- **Models**: extienden `App\Common\Models\BaseModel` y declaran su
  `campoCodigo()` (`codigo_manual`, `codigo_tutorial`). Campos en
  `snake_case` (convención de Eloquent). Sin cast `array` para arrays o
  subdocumentos: MongoDB los guarda nativos.
- **Migraciones**: nombre con timestamp (`2026_10_18_120000_...php`,
  formato estándar de Laravel, cumple el criterio anti-colisión de
  PERSISTENCIA_BD_BACKEND.md). Crean colecciones e índices; los campos no
  se declaran.
- **API stateless**: sin sesiones, caché ni colas en base de datos
  (`SESSION_DRIVER=array`, `CACHE_STORE=array`, `QUEUE_CONNECTION=sync`).
- **Logging**: canal `json` (Monolog `JsonFormatter` a stderr); contexto
  como array (`Log::info('Manual publicado', ['module' => 'manuales'])`).
- **UUID v7**: `Str::uuid7()`, guardado como string.
- **Estilo**: `laravel/pint` (preset laravel), PSR-12. Clases en
  `PascalCase`, métodos/variables en `camelCase`, archivos = nombre de clase.

## Tests

**PHPUnit** (`tests/Unit`, `tests/Feature`). Foco igual que
[TESTING_BACKEND.md](TESTING_BACKEND.md): services y contrato HTTP de los
controllers (forma del sobre). Regla estricta de tests al día: igual que
en Spring.

```bash
cd projects/backend/contenido
composer install
php artisan test
./vendor/bin/pint --test
```

> Requiere PHP 8.4 con la extensión `mongodb` (`pecl install mongodb`). Sin
> PHP local, todo corre dentro del contenedor (`docker compose up contenido`,
> ver su Dockerfile, target `dev`).
