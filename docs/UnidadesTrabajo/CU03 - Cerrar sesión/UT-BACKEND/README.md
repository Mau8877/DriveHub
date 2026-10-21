# Unidades de trabajo — CU03-BE

**Caso de uso:** Cerrar sesión: revocar la sesión renovable del dispositivo actual, de forma idempotente y pública.
**Microservicio(s):** `identidad` (UT 01, 02, 04) y `gateway` (UT 03) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU03 - Cerrar sesión`](../../../CasosDeUso/CU03%20-%20Cerrar%20sesi%C3%B3n.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/logout` (Identidad, público)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca tokens), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Público e idempotente.** Se identifica por el refresh token, no por el JWT (que puede haber vencido). Un token inexistente, vencido, ya revocado, ausente o de otro dispositivo responde igual que el éxito y no revoca nada: no confirma si un token es válido.
2. **Mismo contrato de entrada que `/refresh`:** cuerpo `{ dispositivo, refreshToken? }`; en `WEB` el token sale de la cookie `refreshToken`, en `MOVIL` del cuerpo.
3. **Solo se revoca ese token** (la sesión de ese dispositivo). No se revocan las demás sesiones del usuario: la del otro dispositivo se conserva y, como hay una activa por dispositivo, ese token es la única de este.
4. **En `WEB` siempre se borra la cookie** (`Max-Age=0`, mismos atributos que al crearla), también en el caso idempotente.
5. **Sin códigos de error nuevos:** el único rechazo posible es `400 ERR_SYS_01` (falta o es inválido `dispositivo`) y `429 ERR_SYS_02` (gateway). No hay nada que agregar a `docs/errors/`.

## Contrato de datos / recurso HTTP

### `POST /api/v1/auth/logout` (Identidad, público)

| Campo | Tipo | Regla |
|-------|------|-------|
| `dispositivo` | `WEB` \| `MOVIL` | obligatorio |
| `refreshToken` | string | solo `MOVIL`; en `WEB` se lee de la cookie |

| Respuesta | `data` / efecto |
|-----------|-----------------|
| `200` | `data: null`, mensaje "Sesión cerrada correctamente". En `WEB`, `Set-Cookie: refreshToken=; Max-Age=0` |
| `400` | `ERR_SYS_01` (`dispositivo` faltante o inválido) |
| `429` | `ERR_SYS_02` (gateway) |

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU03-BE-01](./UT-CU03-BE-01.md) | Puerto y `CerrarSesionService` | `identidad` | — |
| [UT-CU03-BE-02](./UT-CU03-BE-02.md) | `POST /api/v1/auth/logout` en `AuthController` | `identidad` | 01 |
| [UT-CU03-BE-03](./UT-CU03-BE-03.md) | Ruta pública `/auth/logout` en el gateway | `gateway` | — |
| [UT-CU03-BE-04](./UT-CU03-BE-04.md) | Prueba de integración del cierre de sesión | `identidad` | 01, 02 |

**Cadena:** `01 → 02 → 04`; la 03 es independiente.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | La sesión renovable queda revocada | 01, 04 |
| CA02 | Cookie borrada en la web | 02, 04 |
| CA04, A1 | Idempotente, sin revocar otras sesiones | 01, 04 |
| CA05 | La sesión del otro dispositivo se conserva | 01, 04 |
| CA07 | No exige acceso vigente | 02, 03, 04 |
| CA09 | Ningún token en los logs | 01 |
| CA10 | Rate limit | ya cubierto por el gateway; sin UT nueva |

CA03, CA06 y CA08 son de frontend y mobile.

## Follow-ups (no se implementan en estas UT)

- "Cerrar todas las sesiones" y revocar las sesiones al cambiar la contraseña (CU04).
- Invalidar el access token antes de que venza (hoy es stateless a propósito).
- El job que borra sesiones vencidas.

## Qué NO incluyen estas UT

- Cambios en el login ni en la renovación.
- Lógica en el controller ni logging en el controller.

## Commits sugeridos

Un commit `[BACK]` por servicio: `identidad` (UT 01, 02, 04) y `gateway` (UT 03).
