# UT-CU01-BE-15 — Prueba de integración del registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-15` |
| **Nombre** | Prueba de integración del registro |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-10, UT-CU01-BE-11, UT-CU01-BE-12, UT-CU01-BE-13 |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers con `assumeTrue(Docker disponible)`, el test verifica el `ApiResponse`) y [CU01 - Registrar estudiante](../../../CasosDeUso/CU01%20-%20Registrar%20estudiante.md).

## Solicitud de negocio

Hay que demostrar que las piezas del registro funcionan juntas y que cada criterio de aceptación del CU se cumple de verdad contra una base real.

## Objetivo

Una prueba de integración recorre `POST /api/v1/usuarios` completo (controller, servicio, adaptadores, Postgres) con un Escuela falso.

## Alcance

- `RegistrarEstudianteIntegrationTest` con el contexto completo, `MockMvc`, PostgreSQL por Testcontainers (a mano con try-with-resources, `assumeTrue(Docker disponible)`) y un servidor falso de Escuela (`HttpServer` del JDK) que responde 200 para `escuelaexample` y 404 para otro `slug`, apuntado por `app.escuela.url`.
- Casos (uno por criterio): registro exitoso con `201`, fila en `usuario` (estado `ACTIVO`, `escuela_codigo` del falso) y en `usuario_rol` con `ESTUDIANTE`, sin sesión ni contraseña en la respuesta (CA01, CA10); campos faltantes, correo inválido, contraseña de 8 caracteres → `400` (CA03, CA04); 17 años → `400 ERR_IDE_02`, exactamente 18 años → `201` (CA05, CA06); mismo correo con otras mayúsculas → `409 ERR_IDE_01` (CA07); `slug` desconocido → `404 ERR_IDE_03` (CA08); petición con `"rol": "SUPER_ADMIN"` → el usuario queda con `ESTUDIANTE` (CA09).
- La prueba usa un `Clock` fijo o fechas relativas a hoy para no depender del día en que se ejecute.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/test/java/.../RegistrarEstudianteIntegrationTest.java`

## Fuera de alcance (no tocar)

- Código de producción: si la prueba revela un defecto, se corrige en la UT dueña de esa pieza.
- Rate limit (CA11): lo cubre el gateway.
- Pruebas contra el servicio Escuela real.
- Pruebas de rendimiento o concurrencia masiva.

## Resultado esperado

La prueba pasa y cubre CA01, CA03 a CA10 del CU sobre una base PostgreSQL real.

## Validación

1. `./mvnw test` en `identidad` pasa con Docker disponible (sin Docker queda SKIPPED, no falla).
2. Cada caso de la prueba nombra su criterio de aceptación (`CAxx`) en el nombre del método.
3. `./mvnw checkstyle:check` sin errores.
