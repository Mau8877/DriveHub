# Unidades de trabajo — CU49-BE

**Caso de uso:** Conversar por WhatsApp: herramientas MCP para consultar horas, planes, franjas y clases, y para reservar y comprar con confirmación en dos pasos; más el agente de n8n que las usa.
**Microservicio(s) / micro-frontend(s):** `asistente-ia` (FastAPI) y `n8n` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU49 - Conversar por WhatsApp.md`](../../../CasosDeUso/CU49%20-%20Conversar%20por%20WhatsApp.md)
**Recurso HTTP / contrato:** servidor MCP `/mcp/` (ya existe por CU46; se suman herramientas)

## Antes de implementar cualquier UT

Leer [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (sección DynamoDB), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) y la tabla de comunicación de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Prerrequisito: CU46 y CU47 implementados** (servidor MCP, servicio n8n y sesión corta por WhatsApp).
- **Reglas de CU46 que se mantienen:** el MCP no tiene credenciales de datos propias, reenvía el Bearer de la persona y cada servicio aplica sus reglas. Lo que cambia es que `ClienteServicios` suma `post`; las lecturas siguen siendo `GET`.
- **Confirmación en dos pasos en el servidor, no en el modelo.** `preparar_*` guarda en DynamoDB (`ConfirmacionesIA`) una confirmación ligada a la persona y a los datos exactos, con código aleatorio, vencimiento de 10 minutos (TTL) y estado de un solo uso; `confirmar_*` solo ejecuta con ese código. El código lo recibe el flujo de n8n, que lo asocia al botón «Confirmar» de WhatsApp, de modo que lo que el modelo escribe no basta para ejecutar.
- **Tope diario** de confirmaciones ejecutadas por persona (`TOPE_ESCRITURAS_DIARIAS`, 10 por defecto), contado en la misma tabla.
- **Idempotencia:** `confirmar_reserva` y `confirmar_compra` derivan su clave del código de confirmación; ejecutar dos veces la misma confirmación devuelve el resultado ya guardado.
- **Sin hora por las dudas:** `preparar_reserva` consulta las horas (Pagos) y rechaza si no alcanzan; el descuento real lo hace Académico al reservar, como en CU14.
- **n8n:** el modelo es OpenRouter (`OPENROUTER_API_KEY`, modelo configurable por `OPENROUTER_MODEL`, uno con soporte de herramientas); el agente usa el nodo *MCP Client Tool* contra `http://asistente-ia:8087/mcp/` con la sesión de 5 minutos como Bearer.

## Contrato

Herramientas nuevas (ver el CU): `consultar_mis_horas`, `listar_planes_de_horas`, `listar_franjas_libres`, `listar_mis_clases`, `preparar_reserva`, `confirmar_reserva`, `preparar_compra`, `confirmar_compra`. Todas solo para el rol `ESTUDIANTE`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU49-BE-01](./UT-CU49-BE-01.md) | Cliente con escritura y herramientas de lectura | — |
| [UT-CU49-BE-02](./UT-CU49-BE-02.md) | Confirmaciones y tope diario | — |
| [UT-CU49-BE-03](./UT-CU49-BE-03.md) | Reservar clases con confirmación | 01, 02 |
| [UT-CU49-BE-04](./UT-CU49-BE-04.md) | Comprar horas con confirmación | 01, 02 |
| [UT-CU49-BE-05](./UT-CU49-BE-05.md) | Agente de n8n con OpenRouter | 03, 04 |

**Cadena:** `01, 02 → 03, 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 3, CA02, CA11 | Consultas | 01 |
| Flujo principal 5 y 6, A3, A6, A7, CA03, CA04, CA07, CA10 | Preparar, confirmar, tope | 02 |
| Flujo principal 7, A2, A4, A5, CA05, CA08 | Reservar | 03 |
| Flujo principal 7 y 8, CA06 | Comprar | 04 |
| Flujo principal 1 a 6, A1, A8, A9, A10, CA01, CA09, CA12 | Conversación y agente | 05 |

## Follow-ups (no se implementan en estas UT)

- Avisos proactivos por WhatsApp; cancelar y reprogramar por chat.

## Qué NO incluyen estas UT

- El login y el registro por chat (CU47, CU48) ni cambios en Pagos, Académico o Identidad.
