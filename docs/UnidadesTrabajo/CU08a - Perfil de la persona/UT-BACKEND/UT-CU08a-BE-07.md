# UT-CU08a-BE-07 — Datos de ejemplo de Identidad con perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-07` |
| **Nombre** | Datos de ejemplo de Identidad con perfil |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08a-BE-02 |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `IdentidadSeeder`.

## Solicitud de negocio

Quien levante el entorno de desarrollo necesita cuentas de ejemplo con un perfil real y reconocible, no con valores de relleno.

## Objetivo

El seeder crea las cuentas de ejemplo con su perfil completo y es idempotente.

## Alcance

- `IdentidadSeeder`: cada cuenta (`superadmin@gmail.com`, `adminexample@gmail.com`, `instructorexample@gmail.com`) se crea con su `Perfil`: nombres y apellidos que ya tenía, `fechaNacimiento` 1990-01-01, un `ci` propio y distinto por cuenta, `sexo`, y —solo el Administrador y el Instructor— una ubicación fija de La Paz (-16.4897, -68.1193) con `direccion` escrita en el propio seeder, **sin llamar al geocodificador** (el arranque no debe depender de internet). El Super admin no lleva dirección ni coordenadas. Se quitan los valores provisionales de la UT 02 en este archivo.
- Los CI de ejemplo son constantes públicas del seeder (para que los tests del resto de los servicios y la documentación los puedan nombrar).
- Sigue siendo idempotente: si la cuenta ya existe, no se toca. Una cuenta que existe sin perfil (no debería pasar tras la UT 01) se completa solo si falta.
- Pruebas: correr el seeder dos veces deja una sola cuenta y un solo perfil por correo; el Super admin sin ubicación; los CI son únicos entre sí.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/IdentidadSeeder.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/config/IdentidadSeederTest.java`

## Fuera de alcance (no tocar)

- Crear estudiantes de ejemplo (si hacen falta, los pide su propio CU).
- Cambiar los correos ni las contraseñas de ejemplo.
- El seeder de Escuela.

## Resultado esperado

Las cuentas de ejemplo tienen un perfil reconocible.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con una base vacía y el stack levantado, `GET /api/v1/usuarios/me` con el token de `adminexample@gmail.com` devuelve su CI, su sexo y su dirección.
