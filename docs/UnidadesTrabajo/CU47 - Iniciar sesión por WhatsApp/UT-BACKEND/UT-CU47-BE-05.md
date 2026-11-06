# UT-CU47-BE-05 — Emitir la sesión por WhatsApp

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU47-BE-05` |
| **Nombre** | Emitir la sesión por WhatsApp |
| **Caso de uso** | CU47-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU47-BE-01, UT-CU47-BE-02, UT-CU47-BE-04 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), `EmisorTokenAccesoPort` y `JwtEmisorAdapter` existentes.

## Solicitud de negocio

n8n necesita actuar como el estudiante, con sus permisos reales, durante unos minutos, sin conocer su contraseña.

## Objetivo

`POST /internal/v1/whatsapp/sesiones` entrega un access token de 5 minutos de la cuenta vinculada al número.

## Alcance

- Puerto `EmitirSesionWhatsappUseCase` y servicio `EmitirSesionWhatsappService` (loguea INFO con el `codigo_usuario`):
  1. Busca el vínculo por el hash del número; si no existe, `NotFoundException` `ERR_IDE_26` (404).
  2. Carga la cuenta: debe estar `ACTIVO`, ser `ESTUDIANTE` y de la escuela `WHATSAPP_ESCUELA_CODIGO`; si no, `ForbiddenException` `ERR_IDE_27` (403).
  3. Emite el access token con los claims normales (`sub` = `codigo_usuario`, roles, escuela) y **vigencia de 5 minutos**, sin refresh token.
- `EmisorTokenAccesoPort`: método o parámetro para pedir una vigencia distinta (`app.whatsapp.sesion-segundos`, 300 por defecto); el emisor de la sesión normal no cambia.
- Controller: `POST /internal/v1/whatsapp/sesiones` en `WhatsappController` con `SesionWhatsappRequest` (`telefono`); `200` con `data: { accessToken, expiraEn }`. El token nunca se loguea.
- Errores `ERR_IDE_26` y `ERR_IDE_27`: constantes y secciones en `docs/errors/` con su link en el índice.
- Tests: emite con vínculo y cuenta válidos (verificar vigencia y roles del token), sin vínculo, cuenta bloqueada, cuenta de otro rol, cuenta de otra escuela, y que dos llamadas dan tokens distintos.

Archivos esperados (crear/tocar): `domain/port/in/EmitirSesionWhatsapp*.java`, `application/service/EmitirSesionWhatsappService.java`, `EmisorTokenAccesoPort.java`, `JwtEmisorAdapter.java`, `SesionWhatsappRequest.java`, `WhatsappController.java`, `IdentidadErrorCodes.java`, `docs/errors/**` y los tests.

## Fuera de alcance (no tocar)

- Refresh de la sesión web o móvil (CU02) y cualquier cambio en el login normal.

## Resultado esperado

n8n obtiene un token que Pagos y Académico aceptan como el del estudiante y que vence a los 5 minutos.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. Tras vincular un número, `POST /internal/v1/whatsapp/sesiones` devuelve un token; usado en `GET /api/v1/billeteras-horas/mia` del gateway responde con las horas del estudiante.
3. Un número sin vínculo → 404 `ERR_IDE_26`; con la cuenta bloqueada → 403 `ERR_IDE_27`.
