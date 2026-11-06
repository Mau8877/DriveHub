# Unidades de trabajo — CU47-BE

**Caso de uso:** Iniciar sesión por WhatsApp: vincular el número de un estudiante a su cuenta con un código enviado al correo, y emitir sesiones cortas para el chat.
**Microservicio(s) / micro-frontend(s):** `identidad` (hexagonal) y `n8n` (componente nuevo, infraestructura) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU47 - Iniciar sesión por WhatsApp.md`](../../../CasosDeUso/CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md)
**Recurso HTTP / contrato:** rutas internas `/internal/v1/whatsapp/*` de Identidad (fuera de `/api/v1`, **no** se registran en el gateway)

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **n8n llama directo a Identidad, sin gateway.** Las rutas son internas, igual que las de Pagos para CU44: prefijo `/internal/`, credencial de servicio en `X-Service-Key`, comparada en tiempo constante. El navegador y la app nunca las alcanzan. Es una copia del patrón de `CredencialServicioFilter` de Pagos, no una librería compartida.
- **El número nunca se guarda en claro.** Se guarda `telefono_hash` = HMAC-SHA256 del número en formato E.164 con una clave secreta del servicio (`WHATSAPP_HASH_SECRET`). Sirve para buscar y no revela el número si se filtra la base.
- **Códigos de 6 dígitos en `desafio_whatsapp`,** con hash, intentos y vencimiento; mismo criterio que los tokens de CU04b.
- **Límite de pedidos de código en la propia Identidad,** porque estas rutas no pasan por el rate limit del gateway: máximo 5 códigos por hora por número y por correo, contado sobre `desafio_whatsapp`. Al superarlo responde 429 `ERR_SYS_02` con el sobre estándar.
  - **Requiere decisión del equipo:** el catálogo cerrado de [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) no tiene una excepción para 429. La UT-03 propone agregar `LimiteExcedidoException` (429, `ERR_SYS_02`) a ese listado antes de usarla.
- **Respuesta uniforme al pedir código:** siempre `202` con el mismo mensaje, exista o no el correo, para no revelar cuentas.
- **La sesión por WhatsApp es un access token común de 5 minutos,** emitido con el mismo `EmisorTokenAccesoPort` y con los roles reales de la cuenta; todos los servicios lo validan igual. No hay refresh token.
- **Solo `ESTUDIANTE` de la escuela del chat.** La escuela es la de la semilla del proyecto, configurada por `WHATSAPP_ESCUELA_CODIGO`.
- **n8n está fuera del mapa de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)** y esa regla pide decidir explícitamente antes de sumarlo: la decisión está tomada por el equipo; la UT-06 registra n8n en la tabla en el mismo cambio.
- **Estado de la conversación** (esperando correo, esperando código): lo guarda n8n en una tabla propia con vencimiento de 24 h. Verificar que la versión de n8n fijada traiga *Data Table*; si no, se usa el almacenamiento de datos estáticos del flujo.
- Los números de error `ERR_IDE_25` a `ERR_IDE_27` son los siguientes libres al escribir esto; confirmar contra `docs/errors/identidad/` al implementar.

## Contrato de datos / recurso HTTP

Todas exigen `X-Service-Key`; sin ella o con una incorrecta responden 401 `ERR_SYS_04`.

| Método | Ruta | Cuerpo | Respuesta |
|--------|------|--------|-----------|
| POST | `/internal/v1/whatsapp/codigos` | `{ telefono, correo }` | 202, `data: null`, mismo mensaje siempre |
| POST | `/internal/v1/whatsapp/vinculos` | `{ telefono, correo, codigo }` | 201, `data: { vinculado: true }`; 400 `ERR_IDE_25` si el código no sirve |
| POST | `/internal/v1/whatsapp/sesiones` | `{ telefono }` | 200, `data: { accessToken, expiraEn }`; 404 `ERR_IDE_26` sin vínculo; 403 `ERR_IDE_27` cuenta no habilitada |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU47-BE-01](./UT-CU47-BE-01.md) | Persistencia del vínculo y de los códigos | — |
| [UT-CU47-BE-02](./UT-CU47-BE-02.md) | Credencial de servicio para rutas internas | — |
| [UT-CU47-BE-03](./UT-CU47-BE-03.md) | Pedir código por correo | 01, 02 |
| [UT-CU47-BE-04](./UT-CU47-BE-04.md) | Confirmar código y vincular el número | 01, 02, 03 |
| [UT-CU47-BE-05](./UT-CU47-BE-05.md) | Emitir la sesión por WhatsApp | 01, 02, 04 |
| [UT-CU47-BE-06](./UT-CU47-BE-06.md) | Servicio n8n y flujo de reconocimiento y login | 03, 04, 05 |

**Cadena:** `01, 02 → 03 → 04 → 05 → 06`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 5 | Enviar el código al correo | 03 |
| Flujo principal 7 | Comprobar el código y vincular | 04 |
| Flujo principal 8, A8 | Sesión corta; cuenta bloqueada | 05 |
| Flujo principal 1 a 4 y 6, A1 | Conversación, reconocimiento del número | 06 |
| A2, A3 | Correo sin cuenta, de otro rol o bloqueada | 03 |
| A5, A6 | Código incorrecto; reemplazo de vínculo | 04 |
| A7, CA10 | Límite de pedidos | 03 |
| CA07 | Credencial de servicio | 02 |

## Follow-ups (no se implementan en estas UT)

- Desvincular un número desde la app o el chat; limpieza programada de códigos vencidos.

## Qué NO incluyen estas UT

- El registro por chat ([CU48](../../CU48%20-%20Registrarse%20por%20WhatsApp/UT-BACKEND/README.md)) ni las herramientas de reservar y comprar ([CU49](../../CU49%20-%20Conversar%20por%20WhatsApp/UT-BACKEND/README.md)).
