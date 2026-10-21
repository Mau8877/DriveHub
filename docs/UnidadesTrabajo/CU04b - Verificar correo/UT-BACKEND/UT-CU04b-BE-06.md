# UT-CU04b-BE-06 — `correoVerificado` en la respuesta de login y renovación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-06` |
| **Nombre** | `correoVerificado` en la respuesta de login y renovación |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04b-BE-01 |

## Antes de implementar

[RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Los clientes necesitan saber si el correo está verificado para mostrar u ocultar el botón.

## Objetivo

Login y renovación devuelven `usuario.correoVerificado`.

## Alcance

- `SesionResponse.UsuarioSesionResponse`: agregar `boolean correoVerificado` al final; `AuthController.aUsuarioResponse` lo toma de `Usuario.isCorreoVerificado()`.
- Actualizar el contrato en `docs/UnidadesTrabajo/CU02 - Iniciar sesión/UT-BACKEND/README.md` (campo nuevo de `usuario`).
- Ajustar `AuthControllerTest` y `AuthIntegrationTest` (cuenta sin verificar → `false`; verificada → `true`).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/SesionResponse.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthController.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthControllerTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/AuthIntegrationTest.java`
- `docs/UnidadesTrabajo/CU02 - Iniciar sesión/UT-BACKEND/README.md`

## Fuera de alcance (no tocar)

- Endpoints de verificación (UT 07).
- Cambios en los claims del JWT.

## Resultado esperado

Cada respuesta de sesión indica si el correo de la persona está verificado.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. `./mvnw checkstyle:check` sin errores.
