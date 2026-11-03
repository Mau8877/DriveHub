# Unidades de trabajo — CU37-BE

**Caso de uso:** Configurar preferencias de notificación: consultar y guardar los canales, los tipos de aviso y las horas de silencio de la persona.
**Microservicio(s) / micro-frontend(s):** `notificaciones` (NestJS, capas, DynamoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU37 - Configurar preferencias de notificación`](../../../CasosDeUso/CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)
**Recurso HTTP / contrato:** `/api/v1/preferencias-notificacion` (ya enrutado por el gateway a `notificaciones`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Un recurso singular, sin código en la URL.** Cada persona tiene una sola configuración, y quién es sale del token (`sub`). Por eso `GET` y `PUT /api/v1/preferencias-notificacion`, sin `{codigo}`.
2. **`PUT`, no `PATCH`** ([ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): la app manda siempre la configuración completa y a propósito; lo omitido (las horas de silencio, el correo) se interpreta como «vaciar».
3. **`GET` sin configuración guardada devuelve los valores por defecto sin escribirlos.** La constante `PREFERENCIAS_POR_DEFECTO` es la misma que usa la política de envío de CU36, para que lo que se muestra y lo que se aplica no se separen.
4. **Reglas de la franja en el servicio** (`ERR_NOT_03`): las dos horas o ninguna, `HH:mm` de `00:00` a `23:59`, y no iguales. El formato lo valida el DTO (`ERR_SYS_01`).
5. **El correo lo manda la app** con cada guardado (el del perfil, CU06). El servicio no puede verificarlo: solo valida el formato. Mantenerlo al día por un evento de Identidad queda como follow-up.
6. **Solo `ESTUDIANTE` e `INSTRUCTOR`**, por un guard declarativo de roles (CU36 solo exigía sesión).
7. **Guardar conserva el `codigoPreferenciasNotificacion`** si ya existía; si no, crea uno nuevo (UUID v7).

## Contrato de datos / recurso HTTP

| Método | Path | Rol | Status | `data` |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/preferencias-notificacion` | `ESTUDIANTE`, `INSTRUCTOR` | 200 | `PreferenciasResponse` |
| `PUT` | `/api/v1/preferencias-notificacion` | `ESTUDIANTE`, `INSTRUCTOR` | 200 | `PreferenciasResponse` |

`PreferenciasRequest` (`PUT`): `canales { email, push, inApp }` (boolean, obligatorios), `tipos { RESERVA, PAGO, EXAMEN, RECORDATORIO }` (boolean, obligatorios), `silencioDesde?` y `silencioHasta?` (`HH:mm`) y `email?` (correo).
`PreferenciasResponse`: lo mismo más `actualizadoEn` (ISO, `null` si nunca se guardó); `silencioDesde`, `silencioHasta` y `email` en `null` cuando no están. El código interno de la configuración no sale.

Errores: `400 ERR_NOT_03` franja de silencio no válida, `400 ERR_SYS_01` formato, `401 ERR_SYS_04`, `403 ERR_SYS_05`. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU37-BE-01](./UT-CU37-BE-01.md) | Guard de roles y error de franja no válida | — |
| [UT-CU37-BE-02](./UT-CU37-BE-02.md) | Guardar y consultar las preferencias | 01 |
| [UT-CU37-BE-03](./UT-CU37-BE-03.md) | Endpoints `GET` y `PUT` | 01, 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Paso 2, CA01 | Consultar, con valores por defecto | 02, 03 |
| Pasos 3 a 6, CA02, CA07 | Guardar la configuración completa | 02, 03 |
| A3, CA06 | Franja de silencio no válida | 01, 02 |
| A5, CA10 | Correo con formato inválido | 02 |
| A6, CA09 | Roles y sesión | 01, 03 |
| CA08 | Solo las propias | 03 |
| CA03, CA04, CA05 | Aplicar lo elegido | ya lo hace CU36 (`decidirCanales`): no se reconstruye |

La pantalla de la app está en [UT-MOBILE](../UT-MOBILE/README.md).

## Follow-ups (no se implementan en estas UT)

- Actualizar el correo de las preferencias con un evento de Identidad cuando la persona lo cambia (CU06).
- Elegir canal por cada tipo de aviso.
- Preferencias desde la web.

## Qué NO incluyen estas UT

- El historial de avisos (CU38).
- Enviar avisos y aplicar las preferencias (CU36).
- Cambios en el gateway: el recurso ya está enrutado.
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Commits sugeridos

Un commit `[BACK]` para `notificaciones` (UT 01 a 03). Los de `docs/` son `[DOCS]`.
