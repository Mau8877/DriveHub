# CI/CD

Aplica a todo el repositorio (el pipeline vive en `.github/workflows/` y
cubre los 8 servicios de backend, las 7 apps web y mobile).

## Qué hay implementado y qué no

- **CI (Integración Continua): sí, implementado.**
  [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) — corre
  lint + tests + build de cada servicio/app que cambió, en cada Pull
  Request contra `main` y en cada push directo a `main`.
- **CD (Entrega/Despliegue Continuo): todavía no.** Ver
  [Por qué CD no está implementado todavía](#por-qué-cd-no-está-implementado-todavía).

## Un job por servicio/app que cambió, no uno solo para todo

El job `changes` (con `dorny/paths-filter`) detecta qué carpetas cambiaron
y arma la lista de servicios/apps afectados; cada stack tiene un job con
**matriz** sobre esa lista:

- Un monorepo con 15 proyectos independientes que no comparten build ni
  runtime: no tiene sentido compilar Flutter ni los 7 micro-frontends si el
  cambio solo tocó una migración de Pagos.
- Los minutos de CI no son gratis.
- Un cambio en `packages/` o en `pnpm-lock.yaml` re-valida todas las apps
  web (y Notificaciones, que también es parte del workspace pnpm).

## Qué corre cada job

Los mismos comandos del flujo local de cada proyecto, sin nada inventado
solo para CI:

| Job | Matriz | Pasos |
| --- | --- | --- |
| `spring` | `gateway`, `identidad`, `escuela`, `pagos`, `academico` | `mvnw checkstyle:check` → `mvnw test` → `mvnw clean package` |
| `notificaciones` | — | `pnpm install --frozen-lockfile` → `lint` (oxlint) → `test` (vitest) → `build` |
| `asistente-ia` | — | `pip install -r requirements-dev.txt` → `ruff check` + `ruff format --check` → `pytest` |
| `contenido` | — | `composer install` → `pint --test` → `php artisan test` |
| `frontend` | `shell`, `identidad`, `escuela`, `pagos`, `examenes`, `contenido`, `asistente-ia` | `pnpm install --frozen-lockfile` → `lint` → `test` → `build` |
| `gestion` | — | `pnpm install --frozen-lockfile --filter ./packages/gestion` → `test` (vitest, jsdom); corre si cambia `packages/gestion/**`, `packages/mf-contract/**` o el lockfile |
| `mobile` | — | `flutter pub get` → `flutter analyze` → `flutter test` |

- El job `spring` levanta `postgres:18-alpine` y `mongo:8` como
  `services:` (mismas versiones que `docker-compose.yml`): los
  `XxxApplicationTests` arrancan el contexto completo (Flyway en los
  servicios Postgres, Mongo en Académico). Cada job de la matriz tiene sus
  propios contenedores.
- El runner de Ubuntu trae Docker nativo: los tests con Testcontainers
  corren de verdad (en Windows local quedan `SKIPPED`, ver
  [TESTING_BACKEND.md](backend/TESTING_BACKEND.md)). Si uno falla en CI pero
  pasaba "SKIPPED" en local, es información real, no un falso positivo.
- El job `frontend` instala con `--filter "{./projects/frontend/<app>}..."` (con llaves y
  `...`): así entran también los paquetes del workspace de los que depende la app y sus
  propias dependencias (`lit` de `@drivehub/gestion`). Con `--filter ./projects/frontend/<app>`
  a secas, falta `lit` y fallan los tests que importan el paquete.
- `pnpm install --frozen-lockfile`: si `pnpm-lock.yaml` no refleja
  exactamente los `package.json`, el job falla en vez de instalar otras
  versiones. La versión de pnpm sale de `packageManager` del `package.json`
  raíz.
- `contenido` no tiene `composer.lock` todavía (se genera con el primer
  `composer install` real y se commitea): hasta entonces CI resuelve las
  versiones en cada corrida.
- Ningún job necesita secretos: build + lint + test contra código propio.

## Por qué CD no está implementado todavía

CD necesita un **destino** (dónde corren los servicios en producción: un
VPS con Docker Compose, un orquestador, un servicio administrado). Ese
destino **no está decidido**: `docker-compose.yml` está pensado para
desarrollo (bind mounts, hot reload, `spring-boot:run`, `ng serve`), no
para producción. Armar CD sin esa decisión sería inventar una plataforma y
rehacerlo después.

### Cómo se vería, en términos generales, una vez que exista un destino

Referencia conceptual, **no algo a implementar todavía**:

1. **Build de artefactos**: cada servicio ya tiene un `Dockerfile` de
   producción (multi-stage, sin root): Spring compila con Maven y corre con
   JRE; NestJS con `pnpm deploy --prod`; FastAPI target `prod`; Laravel
   target `prod` (pendiente pasar de `artisan serve` a php-fpm/FrankenPHP).
   Cada micro-frontend: build estático servido por nginx (falta su
   Dockerfile).
2. **Publicar** cada imagen en un registro (ej. GitHub Container Registry)
   con el SHA del commit como tag.
3. **Desplegar** (lo que depende de la plataforma elegida; necesita
   secretos en GitHub Secrets) y desplegar los manifiestos de federación
   del shell con las URLs reales de cada remoto.
4. **Gate de aprobación** para producción (`environment: production` con
   reviewers).

Cuando se decida el destino, se agrega `.github/workflows/cd.yml` que
reusa el mismo build que valida `ci.yml`.
