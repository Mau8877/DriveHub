# Documentación

Documentación oficial del proyecto.

## Poniendo en marcha un proyecto nuevo a partir de esta plantilla

Este repo es un esqueleto de arquitectura (Spring Boot + React/Vite + Flutter + Docker). Todos los identificadores están puestos como placeholder genérico (`app`, `com.example`, etc.) para que se reemplacen por el nombre real del proyecto nuevo. Sigue esta lista en orden.

### 1. Elegí el nombre del proyecto

Antes de tocar nada, definí:

- **Nombre del proyecto** (ej: `Acme`) → se usa en textos y README.
- **Slug en minúsculas con guiones** (ej: `acme`) → se usa en nombres de contenedores, red, artefactos.
- **Paquete reverso de dominio** (ej: `com.acme`) → se usa en Java (backend) y en bundle IDs (mobile).

En los ejemplos de abajo se usa `acme` / `com.acme` como referencia.

### 2. Raíz del repo

- [ ] `README.md`: cambiá el título y la descripción.
- [ ] `package.json`: cambiá `"name": "developlayout"` por el slug del proyecto.
- [ ] `docker-compose.yml`: reemplazá todas las ocurrencias de `app` por el slug elegido:
  - `name: app` (nombre del stack de compose)
  - `container_name: app-*` (proxy, frontend, backend, postgres, redis, minio, minio-init, prometheus, grafana)
  - `networks: app-network`
  - `volumes: app-postgres_data`, `app-redis_data`, `app-minio_data` — **importante:** si esta máquina corre varios proyectos hechos con esta plantilla, no dejes estos nombres sin el prefijo del proyecto. Docker Compose solo corre la inicialización de Postgres (crear la DB/usuario del `.env`) la primera vez que un volumen está vacío; si dos proyectos comparten el mismo nombre de volumen, el segundo hereda los datos del primero y su base de datos nunca se crea.
- [ ] `.env.example` (raíz): actualizá `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` con el nuevo slug, y generá tu propio `.env` real (no se commitea). `MINIO_ROOT_USER` necesita mínimo 3 caracteres y `MINIO_ROOT_PASSWORD` mínimo 8 — si son más cortos, el contenedor de MinIO termina en `exited (1)` y todo lo que depende de él (`minio-init`, `backend`) nunca arranca.

### 3. Backend (Spring Boot)

- [ ] Renombrá el paquete Java `com.example.backend` → `com.acme.backend` en:
  - `projects/backend/src/main/java/com/example/...` (mover **todo** el árbol de carpetas, incluidas las que solo tienen un `.gitkeep` como `common/constant`, `common/exception`, `common/response`, `common/util`, `features`, `security` — no alcanza con buscar solo archivos `.java`, esas carpetas vacías también hay que moverlas)
  - `projects/backend/src/test/java/com/example/...` (mover carpetas)
  - El `package` / `import` dentro de cada `.java`
- [ ] Renombrá la clase principal `AppBackendApplication` (y su test) por el nombre que prefieras (ej: `AcmeBackendApplication`).
- [ ] `pom.xml`: `groupId` (`com.example` → `com.acme`) y `artifactId` (`app-backend` → `acme-backend`).
- [ ] `application.properties`:
  - `spring.application.name`
  - URL de datasource (`app_db` → tu nombre de base de datos)
  - namespace `app.minio.*` (y actualizá el prefijo si cambiás el nombre)
  - `app.minio.bucket-name`
- [ ] `projects/backend/.env.example`: mismo ajuste de `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`. `CORS_ALLOWED_ORIGINS` ya trae por defecto los orígenes del frontend en desarrollo (`http://localhost:5173` directo, `http://localhost` vía el proxy nginx) — solo hace falta tocarlo si agregás otros orígenes (otro puerto, un dominio de producción, etc.).
- [ ] `src/main/resources/db/migration/V1__init_schema.sql`: cambiá el valor `'App'` de `project_name` (o borrá esa migración de ejemplo cuando tengas tu esquema real).

El scaffold ya trae configurado, sin nada más que hacer:
- **Health check** en `GET /health` (Actuator, base-path movido a la raíz — no es `/actuator/health`).
- **Swagger UI** en `/swagger-ui/index.html` y OpenAPI JSON en `/v3/api-docs` (acceso directo por el puerto 8080, no a través del proxy).
- **Métricas Prometheus** en `GET /prometheus`, ya scrapeadas por el servicio `prometheus` de `docker-compose.yml` y con el datasource de Grafana provisionado.
- **CORS** habilitado vía `SecurityConfig` para los orígenes en `app.cors.allowed-origins` (property `CORS_ALLOWED_ORIGINS`).

Si más adelante exponés otros endpoints de Actuator (`/metrics`, `/env`, `/beans`, etc.), agregalos a `management.endpoints.web.exposure.include` y decidí con cuidado si van en `permitAll()` en `SecurityConfig` — quedarían públicos sin autenticación si los dejás ahí sin querer.

### 4. Frontend (React + Vite)

- [ ] `projects/frontend/package.json`: `"name": "frontend"` → el nombre que quieras.
- [ ] `projects/frontend/index.html`: `<title>frontend</title>`.
- [ ] `projects/frontend/src/App.tsx`: es la pantalla de bienvenida del boilerplate — reemplazala por tu primera vista real.
- [ ] `projects/frontend/.env.example`: revisá `VITE_API_BASE_URL` si cambiás rutas/puertos.

### 5. Mobile (Flutter)

- [ ] `projects/mobile/pubspec.yaml`: `name: mobile` y `description`.
- [ ] Bundle ID / application ID `com.example.mobile` → `com.acme.mobile` en:
  - `android/app/build.gradle.kts` (`namespace`, `applicationId`)
  - `android/app/src/main/kotlin/com/example/mobile/MainActivity.kt` (mover el paquete y actualizar el `package` declarado)
  - `ios/Runner.xcodeproj/project.pbxproj` (`PRODUCT_BUNDLE_IDENTIFIER`, todas las ocurrencias)
  - `macos/Runner.xcodeproj/project.pbxproj` y `macos/Runner/Configs/AppInfo.xcconfig` (`PRODUCT_BUNDLE_IDENTIFIER`, `PRODUCT_NAME`, `PRODUCT_COPYRIGHT`)
  - `linux/CMakeLists.txt` (`APPLICATION_ID`)
  - `windows/runner/Runner.rc` (`CompanyName`, `LegalCopyright`)
- [ ] Íconos y splash (`android/.../mipmap-*`, `ios/Runner/Assets.xcassets`, `macos/Runner/Assets.xcassets`, `web/icons`, `windows/runner/resources`): reemplazalos por los del proyecto real.
- [ ] `lib/core/` y `lib/features/auth/` están vacíos a propósito (solo `.gitkeep`) — son la estructura base para arrancar.
- [ ] `test/widget_test.dart`: tiene `import 'package:mobile/main.dart';` — actualizalo al nuevo nombre de paquete (`name:` de `pubspec.yaml`), por ejemplo `import 'package:acme_mobile/main.dart';`. En Dart el nombre del import depende exclusivamente de ese `name:`, así que si no lo actualizás `flutter analyze`/`flutter test` van a fallar con `uri_does_not_exist`.

> Nota: después de renombrar el bundle ID, corré `flutter pub get` y, si usás Android Studio/Xcode, reabrí el proyecto para que regenere los archivos `ephemeral`/`Generated.xcconfig` (se excluyen del repo a propósito, son generados).

### 6. Infraestructura Docker

- [ ] `infrastructure/nginx/nginx.conf`: `upstream` apunta a `app-frontend` / `app-backend` — actualizá si cambiaste esos nombres de contenedor.
- [ ] `infrastructure/prometheus/prometheus.yml`: `job_name` y `targets: ['app-backend:8080']` (el `metrics_path: '/prometheus'` no lleva el slug, no hace falta tocarlo).
- [ ] `infrastructure/grafana/provisioning/datasources/datasource.yml`: `url: http://app-prometheus:9090`.
- [ ] `infrastructure/minio/create-buckets.sh`: nombre del bucket `app-media`.

### 7. Verificación final

Buscá que no quede ningún rastro del placeholder:

```bash
grep -ril "com\.example\|app-network\|app_db\|app-backend\|app-frontend\|app-storage\|app-media\|app-postgres_data\|app-redis_data\|app-minio_data" . --exclude-dir=.git
```

Si el comando no devuelve nada, ya no queda nada del template. Después corré:

```bash
cp .env.example .env
cp projects/backend/.env.example projects/backend/.env
cp projects/frontend/.env.example projects/frontend/.env
docker compose up
```

para confirmar que el stack completo (proxy, frontend, backend, postgres, redis, minio, prometheus, grafana) levanta correctamente con los nuevos nombres, y confirmá el backend con:

```bash
curl http://localhost:8080/health        # "status":"UP"
curl http://localhost:8080/prometheus | head   # métricas en texto plano
```

## Actualizando versiones (Spring Boot, React, Flutter, Docker)

Importante: la plantilla (`DevelopLayout`) y un proyecto ya generado a partir de ella (ej. renombrado a `Acme`) **no están enlazados**. Una vez que copiás/renombrás el template quedan como repos independientes — actualizar `DevelopLayout` después no le llega solo a un proyecto ya creado. Hay que aplicar el mismo cambio a mano (o vía diff/cherry-pick) en cada proyecto que lo necesite.

### Actualizar un major de framework (poco frecuente)

| Stack | Dónde está pineado | Cómo se actualiza |
|---|---|---|
| Spring Boot | `projects/backend/pom.xml`: `spring-boot-starter-parent` y `java.version` | Subís el `<version>` del parent (y `java.version` si el major lo pide), corrés `mvn dependency:resolve`, y revisás el changelog de Spring Boot por breaking changes. Si subís `java.version`, subí también la imagen `maven:3.9-eclipse-temurin-21-alpine` en `docker-compose.yml` para que coincida. |
| React/Vite | `projects/frontend/package.json`, lockfile en [pnpm-lock.yaml](../pnpm-lock.yaml) (raíz, workspace pnpm) | `pnpm update` (dentro de rango semver) o `pnpm add react@latest` para saltar de major. Como el lockfile vive en la raíz del monorepo, corré los comandos ahí, no dentro de `projects/frontend`. |
| Flutter | `sdk: ^3.12.2` en `projects/mobile/pubspec.yaml` | Cambiás el SDK (con `fvm` o el instalador de Flutter) y corrés `flutter pub upgrade --major-versions`. Este template no commitea `pubspec.lock` (se regenera con `flutter pub get` en cada proyecto nuevo) — a propósito, para que cada proyecto arranque con versiones frescas, pero implica que no hay reproducibilidad de build entre máquinas hasta generar tu propio lock. |
| Infra Docker | Tags de imagen en `docker-compose.yml` (`postgres:18-alpine`, `redis:8-alpine`, `node:22-alpine`, etc.) | Cambiás el tag y corrés `docker compose pull && docker compose up`. `prometheus:latest` y `grafana:latest` quedan sin pin por comodidad en dev — si te importa la reproducibilidad, conviene fijarlos a una versión concreta. |

### Actualizar librerías de rutina (parches/minor) en un proyecto ya generado

- **Backend**: `mvn versions:display-dependency-updates` (requiere agregar el plugin `versions-maven-plugin`, no viene por defecto) lista qué dependencias tienen versión nueva.
- **Frontend**: `pnpm outdated` y `pnpm update` (respeta los rangos del `package.json`); para saltar de major, `pnpm up --latest` paquete por paquete.
- **Mobile**: `flutter pub outdated` y `flutter pub upgrade`.

Estos comandos se corren en el proyecto ya renombrado, no en la plantilla — salvo que quieras que el *próximo* proyecto que arranques desde cero ya venga con esas versiones más nuevas, en cuyo caso el cambio va en `DevelopLayout`.
