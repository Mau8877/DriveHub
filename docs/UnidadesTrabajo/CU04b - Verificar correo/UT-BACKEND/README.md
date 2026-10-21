# Unidades de trabajo — CU04b-BE

**Caso de uso:** Verificar el correo: pedir un enlace de un solo uso por correo (SMTP) y confirmarlo, dejando `correo_verificado` en la cuenta.
**Microservicio(s):** `identidad` (UT 01 a 07 y 09) y `gateway` (UT 08) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU04b - Verificar correo`](../../../CasosDeUso/CU04b%20-%20Verificar%20correo.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/verificar-correo` (con sesión) y `POST /api/v1/auth/verificar-correo/confirmar` (público)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) y [INFRAESTRUCTURA.md](../../../INFRAESTRUCTURA.md) (Mailpit). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La verificación no bloquea el login;** solo la exige la recuperación de contraseña (CU04). Todas las cuentas empiezan en `correo_verificado = false`, incluidas las existentes y las del seeder.
2. **Una sola tabla de tokens:** `token_recuperacion` gana la columna `tipo` (`RECUPERACION` | `VERIFICACION`). Un token de un tipo no sirve para el otro.
3. **Enlace, no código:** el correo lleva `${APP_WEB_URL}/account/verify-email?token=…`, una página pública del shell (remoto `identidad`) que funciona también para quien solo usa la app. 24 horas de vigencia; solo se guarda el hash.
4. **Pedir exige sesión; confirmar es público.** El correo se envía siempre a la dirección de la cuenta (el cliente no la manda).
5. **SMTP por variables:** `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `APP_WEB_URL`. En desarrollo apuntan a Mailpit; en producción, a un proveedor, sin tocar código. El envío vive tras `EnviadorCorreoPort`.
6. **Si el envío falla al verificar, el error se ve** (500 genérico) y el token se revierte, porque la persona está presente y puede reintentar. En CU04 se silencia, por otra razón (no revelar cuentas).
7. **`management.health.mail.enabled=false`:** el SMTP caído no debe poner `/health` en `DOWN`.
8. **Sin límite de reenvíos propio:** lo cubre el límite de peticiones del gateway.

## Contrato de datos / recurso HTTP

| Endpoint | Auth | Cuerpo | Respuesta |
|----------|------|--------|-----------|
| `POST /api/v1/auth/verificar-correo` | JWT | — | `200` `data: null`; `409 ERR_IDE_10` ya verificado; `401 ERR_SYS_04`; `500` si falla el correo |
| `POST /api/v1/auth/verificar-correo/confirmar` | Público | `{ token }` | `200` `data: null`; `400 ERR_IDE_11` enlace inválido, vencido, usado o de otro tipo; `400 ERR_SYS_01` token vacío |

Además, `usuario.correoVerificado` (boolean) se suma a la respuesta de `login` y `refresh`.

### Códigos de error nuevos

| Código | HTTP | Excepción |
|--------|------|-----------|
| `ERR_IDE_10` | 409 | `ConflictException` |
| `ERR_IDE_11` | 400 | `ValidationException` |

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU04b-BE-01](./UT-CU04b-BE-01.md) | Migración y dominio | `identidad` | — |
| [UT-CU04b-BE-02](./UT-CU04b-BE-02.md) | Persistencia de los tokens de correo | `identidad` | 01 |
| [UT-CU04b-BE-03](./UT-CU04b-BE-03.md) | Envío de correo por SMTP | `identidad` | — |
| [UT-CU04b-BE-04](./UT-CU04b-BE-04.md) | `SolicitarVerificacionCorreoService` y `ERR_IDE_10` | `identidad` | 01, 02, 03 |
| [UT-CU04b-BE-05](./UT-CU04b-BE-05.md) | `ConfirmarCorreoService` y `ERR_IDE_11` | `identidad` | 01, 02 |
| [UT-CU04b-BE-06](./UT-CU04b-BE-06.md) | `correoVerificado` en login y renovación | `identidad` | 01 |
| [UT-CU04b-BE-07](./UT-CU04b-BE-07.md) | `@CurrentUserId` y endpoints | `identidad` | 04, 05 |
| [UT-CU04b-BE-08](./UT-CU04b-BE-08.md) | Ruta pública en el gateway | `gateway` | — |
| [UT-CU04b-BE-09](./UT-CU04b-BE-09.md) | Prueba de integración | `identidad` | 04, 05, 06, 07 |

**Cadena:** `01 → 02 → 04, 05 → 07 → 09`; `01 → 06 → 09`; la 03 alimenta la 04; la 08 es independiente.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA02, CA05 | Correo con enlace a la cuenta; enlaces anteriores invalidados | 03, 04, 09 |
| CA03, CA04 | Enlace válido verifica; inválido, vencido, usado o reemplazado → 400 | 02, 05, 09 |
| CA06 | Ya verificado → 409 | 04, 07, 09 |
| CA08 | La cuenta sin verificar usa la plataforma con normalidad | 09 |
| CA09 | El correo falla → 500 genérico, sin enlace entregado | 03, 04 |
| CA10 | Ningún enlace ni token en los registros | 03, 04, 05, 09 |
| CA11 | Rate limit | ya cubierto por el gateway; sin UT nueva |
| CA01, CA07 | Botón y actualización de la sesión | 06 (dato) y las UT de frontend y mobile |

## Follow-ups (no se implementan en estas UT)

- Reiniciar la verificación al cambiar el correo (CU06).
- Job que borra tokens de correo vencidos.
- Enviar los correos por Notificaciones (CU36) en lugar de SMTP directo.

## Qué NO incluyen estas UT

- Recuperar la contraseña y el correo de recuperación ([CU04](../../CU04%20-%20Recuperar%20contrase%C3%B1a/UT-BACKEND/README.md)).
- Exigir la verificación para iniciar sesión.
- Cambios en el registro (CU01).

## Commits sugeridos

Un commit `[BACK]` por servicio: `identidad` (UT 01 a 07 y 09) y `gateway` (UT 08). `docker-compose.yml` y `.env.example` son `[INFRA]`; `docs/` y `docs/errors/` son `[DOCS]`.
