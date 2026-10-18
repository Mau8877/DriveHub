# CI/CD

Aplica a todo el repositorio (el pipeline vive en `.github/workflows/` y
cubre backend, frontend y mobile a la vez, con un job independiente por
proyecto).

## Qué hay implementado y qué no

- **CI (Integración Continua): sí, implementado.**
  [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) — corre
  lint + tests + build de cada proyecto en cada Pull Request contra
  `main` y en cada push directo a `main`.
- **CD (Entrega/Despliegue Continuo): todavía no.** No existe ningún job
  de deploy, ni de build/push de imágenes Docker a un registro. Ver
  [Por qué CD no está implementado todavía](#por-qué-cd-no-está-implementado-todavía)
  más abajo — no es un olvido, es una decisión pendiente de otra decisión
  que este repo no tomó todavía.

## Un job por proyecto, no uno solo para todo

El pipeline usa un job `changes` (con la acción `dorny/paths-filter`) que
detecta qué carpetas cambiaron en el PR/push, y cada job de proyecto
(`backend`, `frontend`, `mobile`) solo corre si `changes` marcó que su
carpeta tuvo cambios. Esto es deliberado por dos razones:

- **Este es un monorepo con 3 proyectos independientes** (ver
  [`README.md`](../../README.md)) que no comparten build ni runtime — no
  tiene sentido esperar a que Flutter compile si el cambio solo tocó una
  migración de Flyway en el backend.
- **Minutos de CI no son gratis** (tienen un límite mensual incluso en
  cuentas gratuitas de GitHub) — correr los 3 jobs completos por cada
  cambio, sin importar qué tocó, desperdicia esa cuota sin necesidad.

Si un cambio toca más de un proyecto a la vez (lo cual, según
[CONVENCIONES_GIT.md](CONVENCIONES_GIT.md#regla-estricta-un-commit-un-scope),
va a venir en commits separados por scope de todos modos), simplemente
corren los jobs de ambos proyectos en paralelo — no hay orden de
dependencia entre `backend`, `frontend` y `mobile`.

## Qué corre cada job

Los mismos comandos que ya están documentados como el flujo local de cada
proyecto (ver [`README.md`](../../README.md#puesta-en-marcha-rápida)),
sin nada adicional inventado solo para CI:

| Job | Pasos |
| --- | --- |
| `backend` | `mvnw checkstyle:check` → `mvnw test` → `mvnw clean package` |
| `frontend` | `pnpm install --frozen-lockfile` (desde la raíz, ver [docs/README.md](../../docs/README.md)) → `pnpm lint` → `pnpm test` → `pnpm build` |
| `mobile` | `flutter pub get` → `flutter analyze` → `flutter test` |

- El job de `backend` levanta `postgres:18-alpine` y `redis:8-alpine` como
  `services:` del job (mismas versiones que `docker-compose.yml`) y le pasa
  `SPRING_DATASOURCE_*`/`SPRING_DATA_REDIS_HOST` al paso de tests: el test
  `@SpringBootTest` (`AppBackendApplicationTests`) arranca el contexto completo,
  y sin base de datos ni Redis Flyway/el health check lo harían fallar. Si
  se renombra la base (`POSTGRES_DB`), se actualiza también en `ci.yml`.
- El job de `backend` corre en un runner de Ubuntu, que trae Docker nativo
  disponible — a diferencia del setup local en Windows (ver
  [TESTING_BACKEND.md](backend/TESTING_BACKEND.md)), acá los tests que
  usan Testcontainers corren de verdad en vez de quedar `SKIPPED`. Si un
  test con Testcontainers falla en CI pero pasaba "SKIPPED" en local, es
  información real sobre ese test, no un falso positivo del pipeline.
- `pnpm install` se corre con `--frozen-lockfile`: si `pnpm-lock.yaml` no
  refleja exactamente lo que pide algún `package.json` del workspace, el
  job falla en vez de instalar versiones distintas a las que se usan en
  desarrollo — evita el clásico "en mi máquina funciona" por un lockfile
  desactualizado que nadie commiteó.
- Ninguno de los tres jobs necesita secretos/credenciales — son build +
  lint + test contra código propio, sin tocar servicios externos reales
  (Postgres/Redis/MinIO de verdad, un backend desplegado, etc.).

## Por qué CD no está implementado todavía

CD (llevar el build a un servidor real, staging o producción) necesita un
**destino**: dónde corre el backend/frontend en producción — un VPS
propio, un servicio administrado (ECS, Cloud Run, un droplet con Docker
Compose, etc.), o alguna otra plataforma. Ese destino **no está decidido
todavía** en este proyecto — hoy `docker-compose.yml` está pensado
explícitamente para desarrollo local (bind mounts del código fuente, hot
reload, `spring-boot:run` en vez de un JAR empaquetado), no para correr en
producción tal cual.

Armar un job de CD sin esa decisión tomada significaría inventar una
plataforma de destino de forma especulativa y después tener que
rehacerlo cuando se decida la real — exactamente el tipo de anticipación
que este proyecto evita en otras áreas (ver el criterio de "no crear
`shared/` hasta que haga falta" en
[TANSTACK_QUERY_FRONTEND.md](frontend/TANSTACK_QUERY_FRONTEND.md)). Por
eso CD queda pendiente de una decisión de infraestructura explícita, no
de una tarea de pipeline.

### Cómo se vería, en términos generales, una vez que exista un destino

Documentado acá como referencia conceptual, **no como algo a implementar
todavía**:

1. **Build de artefacto desplegable.** Backend: una imagen Docker
   construida con un `Dockerfile` multi-stage (compila con Maven, corre
   con solo el JRE — no la imagen `maven:...` de desarrollo que usa
   `docker-compose.yml` hoy). Frontend: un build estático de Vite
   (`pnpm build`) servido por nginx, tampoco la imagen `node:22-alpine`
   con `pnpm run dev` que se usa en desarrollo.
2. **Publicar el artefacto.** Subir la imagen a un registro de
   contenedores (ej. GitHub Container Registry, gratis para repos de
   GitHub) con un tag identificable (el SHA del commit, o la versión).
3. **Desplegar.** El paso que de verdad depende de la plataforma elegida:
   desde un simple `ssh` a un VPS que hace `docker compose pull && docker
   compose up -d`, hasta una acción específica de un proveedor cloud
   (ej. `aws-actions/amazon-ecs-deploy-task-definition` para ECS en AWS,
   o el CLI de Cloud Run en GCP). Esta pieza **sí** necesita secretos
   (credenciales del proveedor cloud, guardados como GitHub Secrets, no
   en el repo) — algo que ningún job de CI actual necesita.
4. **Gate de aprobación (opcional pero recomendado para producción).** Que
   el job de deploy a producción no dispare solo con cualquier push a
   `main`, sino que requiera una aprobación manual en GitHub
   (`environment: production` con reviewers configurados) — así un merge
   no despliega solo a nadie mirando.

Cuando se tome la decisión de dónde se despliega este proyecto, se agrega
un job de deploy nuevo (o un workflow separado,
`.github/workflows/cd.yml`) que reusa el mismo build que ya valida `ci.yml`
— no se duplica la lógica de build entre los dos workflows.
