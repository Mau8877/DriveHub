# UT-CU02-BE-17 — Prueba de integración del login y la renovación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-17` |
| **Nombre** | Prueba de integración del login y la renovación |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-05, UT-CU02-BE-06, UT-CU02-BE-07, UT-CU02-BE-08, UT-CU02-BE-09, UT-CU02-BE-10, UT-CU02-BE-12 |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Hay que demostrar que las piezas funcionan juntas y que cada criterio de aceptación del backend se cumple contra una base real.

## Objetivo

Una prueba de integración recorre `POST /api/v1/auth/login` y `/refresh` completos con un Escuela falso.

## Alcance

- `AuthIntegrationTest` con contexto completo, `MockMvc`, Postgres real (`PostgresIntegrationTestBase`) y un Escuela falso (como `RegistrarEstudianteIntegrationTest`) que responde la escuela como activa o suspendida según el código; usa los usuarios del seeder `dev` y crea los demás por repositorio.
- Casos (cada método nombra su criterio `CAxx`): CA01 login web de admin con cookie y sin refresh token en el cuerpo, y login móvil de estudiante con refresh token en el cuerpo; CA04 correo inexistente y contraseña incorrecta con el mismo 401; CA06 cinco fallos → 423 y la contraseña correcta sigue en 423; CA07 con `bloqueado_hasta` vencido entra; CA08 cuenta inactiva → 403; CA09 escuela suspendida → 403 y el Super admin entra igual; CA10 estudiante en web y admin en móvil → 403; CA11 segundo login del mismo dispositivo revoca el anterior y el del otro dispositivo sigue; CA15 renovar con token inexistente, revocado y ya rotado → 401; CA16 renovar entrega un token nuevo y el anterior deja de servir; CA17 renovar con cuenta inactiva o escuela suspendida → rechazo; CA19 ninguna respuesta ni log trae la contraseña ni los tokens.
- Fechas relativas al `Clock`/a hoy para no depender del día.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/test/java/.../AuthIntegrationTest.java`

## Fuera de alcance (no tocar)

- Código de producción: si la prueba revela un defecto, se corrige en la UT dueña.
- Rate limit (CA20): lo cubre el gateway.
- Pruebas contra Escuela o gateway reales.

## Resultado esperado

La prueba pasa y cubre CA01, CA04, CA06 a CA11, CA15 a CA17 y CA19 sobre PostgreSQL real.

## Validación

1. `./mvnw test` en `identidad` pasa con Docker disponible (sin Docker queda SKIPPED, no falla).
2. Cada método nombra su criterio `CAxx`.
3. `./mvnw checkstyle:check` sin errores.
