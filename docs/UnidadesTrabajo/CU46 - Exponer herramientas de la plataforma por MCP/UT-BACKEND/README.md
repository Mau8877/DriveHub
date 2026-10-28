# Unidades de trabajo — CU46-BE

**Caso de uso:** Exponer herramientas de la plataforma por MCP: un servidor MCP de solo lectura dentro de Asistente IA.
**Microservicio(s) / micro-frontend(s):** `asistente-ia` (FastAPI) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU46 - Exponer herramientas de la plataforma por MCP.md`](../../../CasosDeUso/CU46%20-%20Exponer%20herramientas%20de%20la%20plataforma%20por%20MCP.md)
**Recurso HTTP / contrato:** `/mcp/` (fuera de `/api/v1`, no se enruta por el gateway)

## Antes de implementar cualquier UT

Leer [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), y la tabla de comunicación entre servicios de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Vive en `asistente-ia`** (decisión del equipo), sin servicio nuevo: se monta como una aplicación aparte en `/mcp` del mismo FastAPI y comparte su verificador de JWT.
- **SDK oficial `mcp` fijado en 1.30.0** (`FastMCP`). La 2.x cambió la API (`MCPServer`); se sube a propósito, no por un `pip install` suelto. Transporte HTTP «streamable» **sin estado** y con respuestas JSON (`stateless_http`, `json_response`): cada petición es independiente y no hay sesiones que guardar.
- **Autenticación con un middleware ASGI** (`AutenticacionMcp`) y no una dependencia de FastAPI, porque el servidor MCP es otra aplicación ASGI. Valida el JWT (RS256, emisor), exige un rol permitido y deja la persona y su Bearer en el estado de la petición, de donde los lee cada herramienta. Los rechazos usan el sobre estándar (`ERR_SYS_04`/`ERR_SYS_05`).
- **Sin credenciales propias hacia los datos:** cada herramienta hace un `GET` al servicio dueño (Contenido o Académico, por la red interna) **reenviando el Bearer de la persona**. Los permisos y el aislamiento por escuela los aplica el servicio dueño; el servidor MCP solo filtra por rol para no ofrecer herramientas que no corresponden.
- **Protección contra DNS rebinding del SDK desactivada:** valida el encabezado `Host`, y este servicio se alcanza por nombres de Docker que no se pueden enumerar. El acceso lo controla el JWT.
- **Solo lectura por construcción:** `ClienteServicios` solo tiene `get`; no existe un método para escribir.
- **Errores legibles:** el cliente convierte 401/403, 4xx/5xx, tiempo agotado y respuestas ilegibles en mensajes en español, sin trazas ni cuerpos crudos.
- **Configuración:** `CONTENIDO_URL` y `ACADEMICO_URL` (`Settings`, `.env.example`, `docker-compose.yml`).

## Contrato

Servidor MCP en `http://<asistente-ia>:8087/mcp/` con `Authorization: Bearer <access token>`. Herramientas: `listar_manuales`, `listar_tutoriales`, `consultar_mis_notas` y `consultar_notas_de_mis_alumnos` (ver el CU).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU46-BE-01](./UT-CU46-BE-01.md) | Servidor MCP con herramientas de lectura | CU40-BE-01 (seguridad JWT) |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 5, CA03 a CA06 | Conexión, listado y uso de herramientas | 01 |
| A1 a A5, CA01, CA02, CA07 a CA09 | Token, rol, fallas del servicio y solo lectura | 01 |

## Follow-ups (no se implementan en estas UT)

- Más herramientas de lectura y limitar el uso por persona.

## Qué NO incluyen estas UT

- Herramientas que escriban, el agente web (CU41, CU43) y el enrutamiento por el gateway.
