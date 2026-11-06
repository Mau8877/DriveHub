# UT-CU48-BE-03 — Crear la solicitud y su enlace

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-BE-03` |
| **Nombre** | Crear la solicitud y su enlace |
| **Caso de uso** | CU48-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU48-BE-01, UT-CU48-BE-02 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y `SolicitarVerificacionCorreoService` (CU04b) como modelo del enlace de un solo uso.

## Solicitud de negocio

Con todos los datos confirmados, la persona recibe un enlace para fijar su contraseña.

## Objetivo

`POST /internal/v1/whatsapp/registros` guarda la solicitud y devuelve el enlace.

## Alcance

- Puerto `CrearSolicitudRegistroWhatsappUseCase` y servicio (`@Transactional`, loguea):
  1. Revalida **todos** los datos con `ReglasRegistro` y comprueba que el correo y el CI sigan libres.
  2. Reemplaza cualquier solicitud pendiente del mismo número.
  3. Genera el token opaco con `GeneradorTokenOpacoPort`, guarda solo su hash, vence a los 30 minutos.
  4. Devuelve `enlace` = `APP_WEB_URL` + `/account/set-password?token=...` y `expiraEn`.
  5. La dirección se calcula como en CU08a (si no se puede, queda en blanco).
- La escuela **no** viene en el cuerpo: es `WHATSAPP_ESCUELA_CODIGO`; el rol siempre `ESTUDIANTE`.
- Controller: `POST /internal/v1/whatsapp/registros` con `CrearRegistroWhatsappRequest`; `201` con `data: { enlace, expiraEn }`.
- Logs: INFO con el código de la solicitud; nunca el enlace, el token, el CI, el correo ni el número.
- Tests: crea y devuelve enlace, dato inválido, correo o CI duplicado, reemplazo de la solicitud previa del número, vencimiento a 30 minutos, y que el cuerpo con rol o escuela se ignora.

Archivos esperados (crear/tocar): puerto y servicio nuevos, `CrearRegistroWhatsappRequest.java`, controller, propiedad de la escuela y de la URL web, y los tests.

## Fuera de alcance (no tocar)

- Crear la cuenta (UT-04) y la página web (FE-01).

## Resultado esperado

Tras confirmar, existe una solicitud con vencimiento y la persona tiene un enlace de un solo uso; todavía no hay cuenta.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. `POST /internal/v1/whatsapp/registros` con datos válidos → 201 con el enlace; la tabla `usuario` no cambió; repetirlo con el mismo número deja una sola solicitud pendiente.
