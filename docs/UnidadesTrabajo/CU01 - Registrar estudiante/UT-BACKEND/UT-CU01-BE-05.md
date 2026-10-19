# UT-CU01-BE-05 — Seeder dev de `escuelaexample`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-05` |
| **Nombre** | Seeder dev de `escuelaexample` |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU01-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Para probar el registro del estudiante en desarrollo hace falta una escuela que ya exista. Los datos de ejemplo solo deben aparecer en desarrollo, nunca en producción.

## Objetivo

Al arrancar Escuela con el perfil `dev`, existe la escuela `escuelaexample` con un código fijo.

## Alcance

- Constante del código fijo de la escuela de ejemplo: `0198f0a0-0000-7000-8000-000000000001` (la misma que usará el seeder de Identidad en UT 14; se escribe en los dos servicios porque no comparten código).
- `infrastructure/config/EscuelaSeeder` (`@Component`, `@Profile("dev")`, `ApplicationRunner`): si no existe una escuela con ese `slug`, la crea con `slug = "escuelaexample"`, `nombre = "Escuela Example"`, `estado = ACTIVA` y el código fijo. Es idempotente: ejecutarlo dos veces no duplica ni falla.
- El seeder persiste con `EscuelaJpaRepository` directamente (es infraestructura de desarrollo, no un caso de uso). Registra un `INFO` al crearla.
- Test: con el seeder ejecutado dos veces queda exactamente una escuela; sin el perfil `dev` el bean no existe.

Archivos esperados (crear/tocar):

- `.../infrastructure/config/EscuelaSeeder.java`
- `projects/backend/escuela/src/test/java/.../EscuelaSeederTest.java`

## Fuera de alcance (no tocar)

- Datos de ejemplo de estudiantes, instructores, vehículos o categorías.
- Cualquier seed dentro de una migración Flyway (los datos de ejemplo no van a producción).
- Los usuarios `superadmin` y `adminexample` (viven en Identidad, UT 14).

## Resultado esperado

Con `SPRING_PROFILES_ACTIVE=dev`, la tabla `escuela` tiene una fila `escuelaexample`; sin ese perfil la tabla queda vacía.

## Validación

1. `./mvnw test` en `escuela` pasa.
2. Arrancar dos veces el servicio con perfil `dev`: `SELECT slug, codigo_escuela FROM escuela;` devuelve una sola fila con el código `0198f0a0-0000-7000-8000-000000000001`.
3. Arrancar sin perfil `dev`: la tabla está vacía.
4. `./mvnw checkstyle:check` sin errores.
