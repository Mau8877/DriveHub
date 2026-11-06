# UT-CU49-BE-05 — Agente de n8n con OpenRouter

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU49-BE-05` |
| **Nombre** | Agente de n8n con OpenRouter |
| **Caso de uso** | CU49-BE |
| **Microservicio** | `n8n` |
| **Depende de** | UT-CU49-BE-03, UT-CU49-BE-04 (y el flujo base de CU47, ya implementado) |

## Antes de implementar

`infrastructure/n8n/README.md` (CU47/CU48) y [CU49](../../../CasosDeUso/CU49%20-%20Conversar%20por%20WhatsApp.md).

## Solicitud de negocio

Quien ya está reconocido debe poder pedir sus horas, sus clases, reservar y comprar hablando normal.

## Objetivo

El flujo `whatsapp-drivehub.json` tiene la rama del agente y la de confirmación por botón.

## Alcance

- `Agente` (nodo *AI Agent*) con el modelo *OpenRouter Chat Model* (`OPENROUTER_MODEL`, credencial `openRouterApi` con la clave leída de `$env.OPENROUTER_API_KEY`), memoria de ventana corta por número (*Simple Memory*, clave = teléfono) e `returnIntermediateSteps`.
- **Herramientas como nodos *HTTP Request Tool***, una por herramienta del servidor MCP (`consultar_mis_horas`, `listar_planes_de_horas`, `listar_franjas_libres`, `listar_mis_clases`, `preparar_reserva`, `preparar_compra`), que llaman a `POST {ASISTENTE_IA_URL}/mcp/` (JSON-RPC `tools/call`) con el `Authorization: Bearer` de la **sesión corta de esa persona** (`Reconocer número`). Se descartó el nodo *MCP Client Tool* porque solo admite una credencial fija y la sesión cambia por persona y cada 5 minutos. Los argumentos los completa el modelo con `$fromAI`.
- **El agente NO tiene `confirmar_reserva` ni `confirmar_compra`.** `Formatear respuesta` busca en los pasos del agente el `codigoConfirmacion` de `preparar_*` y manda el resumen con los botones «Confirmar» y «Cancelar» (el código viaja en el id del botón: `confirmar|RESERVA|<codigo>`). Al tocar un botón, `¿Botón de confirmación?` desvía el mensaje a `Confirmar`, que llama directo a `confirmar_*` por MCP y cuenta el resultado; «Cancelar» no hace nada. El servidor MCP sigue exigiendo el código (de esa persona, esa acción, vigente y de un solo uso) y aplica el tope diario.
- Prompt del sistema en español (breve, usa solo herramientas, no inventa nada, no pide contraseñas ni tarjeta, explica que cancelar o reprogramar se hace en la app, no dice «reservé» antes de la confirmación). La respuesta se adapta a WhatsApp (negrita con un asterisco, sin títulos de markdown) y se le quitan los códigos internos.
- Fotos, audios y documentos reciben un aviso (A10); más de 12 mensajes por minuto de un número se ignoran.
- `OPENROUTER_API_KEY` y `OPENROUTER_MODEL` en `.env.example` (nombres) y en el entorno de n8n; la clave real vive solo en el `.env` local ignorado por git.
- `codigo/confirmar.js`, `codigo/formatear-respuesta.js` y la definición de los nodos en `construir-flujo.mjs`; `infrastructure/n8n/README.md`.

Archivos esperados (crear/tocar): `infrastructure/n8n/codigo/**`, `infrastructure/n8n/construir-flujo.mjs`, `infrastructure/n8n/flujos/**`, `infrastructure/n8n/README.md`, `.env.example`, `docker-compose.yml`.

## Fuera de alcance (no tocar)

- Las herramientas del servidor (UT 01 a 04) y el login/registro (CU47, CU48).

## Resultado esperado

Una conversación completa: «quiero comprar horas» → planes → resumen con botones → enlace de pago; y «reserva mañana a la tarde» → franjas → resumen con botones → reserva hecha.

## Validación

1. Importar el flujo en n8n y probarlo con el número de pruebas de Meta (o simulando a Meta, ver el README de n8n).
2. Sin tocar el botón no cambia ninguna hora ni se crea ningún pago; tocarlo dos veces ejecuta la acción una sola vez.
3. Un mensaje que pide «confírmalo» por texto no ejecuta nada: el agente remite al botón.
4. `git grep -n "sk-or-"` no encuentra la clave en el repo.
