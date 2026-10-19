# UT-CU01-BE-14 — Seeder dev de `superadmin` y `adminexample`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-14` |
| **Nombre** | Seeder dev de `superadmin` y `adminexample` |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-11, UT-CU01-BE-12 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (no loguear contraseñas).

## Solicitud de negocio

El Super admin se crea con un seeder (no hay API para crearlo) y hace falta un Administrador de escuela de ejemplo para tener los roles de la plataforma disponibles en desarrollo.

## Objetivo

Al arrancar Identidad con el perfil `dev` existen los usuarios `superadmin@gmail.com` (`SUPER_ADMIN`) y `adminexample@gmail.com` (`ADMIN_ESCUELA` de `escuelaexample`).

## Alcance

- `infrastructure/config/IdentidadSeeder` (`@Component`, `@Profile("dev")`, `ApplicationRunner`) que, por cada usuario, solo lo crea si su correo no existe (idempotente):
  - `superadmin@gmail.com`, contraseña `123123123`, nombres `Super`, apellidos `Admin`, fecha de nacimiento `1990-01-01`, rol `SUPER_ADMIN`, `escuelaCodigo = null`.
  - `adminexample@gmail.com`, contraseña `123123123`, nombres `Admin`, apellidos `Example`, fecha de nacimiento `1990-01-01`, rol `ADMIN_ESCUELA`, `escuelaCodigo = 0198f0a0-0000-7000-8000-000000000001` (el mismo código fijo de UT-CU01-BE-05).
- Usa `UsuarioRepositoryPort`, `RolRepositoryPort` y `PasswordHasherPort` (la contraseña se hashea con BCrypt, nunca se guarda en claro).
- Logs `INFO` por cada usuario creado, sin contraseña. Un comentario en el seeder aclara que las credenciales son solo para desarrollo.
- Test: ejecutar el seeder dos veces deja exactamente dos usuarios, con los roles y escuelas indicados y el hash distinto de `123123123`; sin el perfil `dev` el bean no existe.

Archivos esperados (crear/tocar):

- `.../infrastructure/config/IdentidadSeeder.java`
- `projects/backend/identidad/src/test/java/.../IdentidadSeederTest.java`

## Fuera de alcance (no tocar)

- Un camino de API para crear al Super admin o al Administrador de escuela (CU01_1 y CU01_2).
- Seeds dentro de migraciones Flyway.
- Instructores o estudiantes de ejemplo.
- Permisos o `rol_permiso` (CU05).
- Validar que la escuela exista en Escuela (el código fijo se comparte por convención).

## Resultado esperado

Con `SPRING_PROFILES_ACTIVE=dev`, la base de Identidad tiene 2 usuarios sembrados; sin ese perfil no se crea ninguno.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. Arrancar dos veces con perfil `dev`: `SELECT email FROM usuario;` devuelve exactamente `superadmin@gmail.com` y `adminexample@gmail.com`; `password_hash` empieza con `$2`.
3. `./mvnw checkstyle:check` sin errores.
