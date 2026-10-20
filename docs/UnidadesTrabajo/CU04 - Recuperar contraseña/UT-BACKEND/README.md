# Unidades de trabajo — CU04-BE

**Caso de uso:** Recuperar la contraseña con un enlace enviado por correo: pedir el enlace (sin revelar cuentas) y restablecer la contraseña, cerrando todas las sesiones.
**Microservicio(s):** `identidad` (UT 01 a 05 y 07) y `gateway` (UT 06) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU04 - Recuperar contraseña`](../../../CasosDeUso/CU04%20-%20Recuperar%20contrase%C3%B1a.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/recuperar` y `POST /api/v1/auth/restablecer` (Identidad, públicos)

## Antes de implementar cualquier UT

**Requiere [CU04b](../../CU04b%20-%20Verificar%20correo/UT-BACKEND/README.md) implementado:** el correo por SMTP, los tokens con `tipo`, `correo_verificado` y `ERR_IDE_11` vienen de ahí. Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Solo cuentas activas con el correo verificado reciben el enlace.** Las demás reciben exactamente la misma respuesta, sin correo (CU04b explica cómo verificar).
2. **Pedir la recuperación nunca falla de forma visible:** ni por cuenta inexistente, inactiva o sin verificar, ni por un fallo del SMTP (se registra `ERROR`). Esto es lo contrario a CU04b, donde la persona tiene sesión y sí debe enterarse.
3. **Enlace de 30 minutos y un solo uso;** pedir otro invalida los anteriores. Un token de verificación no sirve para restablecer y al revés (`tipo`).
4. **Un solo código de error para enlaces malos:** se reutiliza `ERR_IDE_11` (inválido, vencido, usado, reemplazado o de otro tipo); no se agregan códigos nuevos.
5. **Restablecer cierra todas las sesiones** (web y móvil) y desbloquea la cuenta (intentos en 0, sin `bloqueado_hasta`, `BLOQUEADO` → `ACTIVO`). No inicia sesión: la persona entra con la contraseña nueva.
6. **Contraseña:** mismas reglas del registro (mínimo 9). El hash es BCrypt vía `PasswordHasherPort`.
7. **Sin igualar tiempos de respuesta** entre cuentas existentes e inexistentes (el envío ocurre dentro de la petición): queda como seguimiento si se vuelve una preocupación real.

## Contrato de datos / recurso HTTP

| Endpoint | Auth | Cuerpo | Respuesta |
|----------|------|--------|-----------|
| `POST /api/v1/auth/recuperar` | Público | `{ email }` | `200` `data: null`, siempre el mismo mensaje; `400 ERR_SYS_01` correo vacío o mal formado; `429` |
| `POST /api/v1/auth/restablecer` | Público | `{ token, password }` | `200` `data: null`; `400 ERR_IDE_11` enlace inválido; `400 ERR_SYS_01` contraseña inválida; `429` |

Sin códigos de error nuevos.

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU04-BE-01](./UT-CU04-BE-01.md) | Correo de recuperación en el puerto y el adaptador SMTP | `identidad` | — |
| [UT-CU04-BE-02](./UT-CU04-BE-02.md) | Cerrar todas las sesiones de una cuenta | `identidad` | — |
| [UT-CU04-BE-03](./UT-CU04-BE-03.md) | `SolicitarRecuperacionService` | `identidad` | 01 |
| [UT-CU04-BE-04](./UT-CU04-BE-04.md) | `RestablecerContrasenaService` | `identidad` | 02 |
| [UT-CU04-BE-05](./UT-CU04-BE-05.md) | Endpoints `/recuperar` y `/restablecer` | `identidad` | 03, 04 |
| [UT-CU04-BE-06](./UT-CU04-BE-06.md) | Rutas públicas en el gateway | `gateway` | — |
| [UT-CU04-BE-07](./UT-CU04-BE-07.md) | Prueba de integración | `identidad` | 03, 04, 05 |

**Cadena:** `01 → 03`, `02 → 04`, `03 + 04 → 05 → 07`; la 06 es independiente.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Cuenta válida recibe el enlace | 01, 03, 07 |
| CA02, CA03 | Misma respuesta en cuenta inexistente, inactiva, sin verificar o con correo caído | 03, 05, 07 |
| CA04 | Pedir otro enlace invalida los anteriores | 03, 07 |
| CA05 | Contraseña nueva guardada y enlace consumido | 04, 07 |
| CA06, CA07 | Sesiones cerradas y cuenta desbloqueada | 02, 04, 07 |
| CA08 | Enlace inválido, vencido, usado, reemplazado o de verificación | 04, 07 |
| CA09 | Contraseña inválida conserva el enlace | 05, 07 |
| CA11 | Nada sensible en los registros | 03, 04, 07 |
| CA12 | Rate limit | ya cubierto por el gateway; sin UT nueva |

CA10 es de frontend y mobile.

## Follow-ups (no se implementan en estas UT)

- Aviso por correo de que la contraseña cambió.
- Igualar los tiempos de respuesta de `/recuperar`.
- Enviar los correos por Notificaciones (CU36).

## Qué NO incluyen estas UT

- Verificar el correo ([CU04b](../../CU04b%20-%20Verificar%20correo/UT-BACKEND/README.md)).
- Cambiar la contraseña estando con sesión (CU06, gestionar perfil).
- Cambios en el login, la renovación o el registro.

## Commits sugeridos

Un commit `[BACK]` por servicio: `identidad` (UT 01 a 05 y 07) y `gateway` (UT 06). `docs/` es `[DOCS]`.
