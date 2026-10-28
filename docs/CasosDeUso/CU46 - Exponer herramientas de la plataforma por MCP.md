# Caso de uso

| Id   | Nombre                                           | Paquete       | Prioridad | Estado |
| ---- | ------------------------------------------------ | ------------- | --------- | ------ |
| CU46 | Exponer herramientas de la plataforma por MCP    | P9. Plataforma | Media    | Hecho  |

**Actor principal:** Sistema (un cliente MCP o un agente de IA que actúa en nombre de una persona)
**Actores secundarios:** Estudiante e Instructor (la persona en cuyo nombre se consulta); Contenido y Académico (dueños de los datos)
**Objetivo:** Que un asistente o agente de IA compatible con el protocolo MCP (Model Context Protocol) pueda consultar información de DriveHub con los mismos permisos de la persona que lo conectó.
**Plataforma:** Backend (sin interfaz)

**Descripción:**
`asistente-ia` expone un servidor MCP en `/mcp/` (URL `http://<asistente-ia>:8087/mcp/`). Un cliente MCP se conecta con el access token de una persona (`Authorization: Bearer <token>`), descubre las herramientas disponibles y las usa para consultar manuales, tutoriales y notas. **Todas las herramientas son de solo lectura**: ninguna crea, cambia ni borra datos. Cada herramienta pregunta al servicio dueño del dato reenviando el token de la persona, así que ve exactamente lo que esa persona vería en la app: un estudiante lo suyo, un instructor lo de sus alumnos, y nada de otra escuela.

---

## Precondiciones

- La persona tiene un access token vigente ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) con rol Estudiante, Instructor o Administrador de escuela.
- Los servicios de Contenido y Académico están disponibles (si no, la herramienta lo avisa).

## Flujo principal

1. El cliente MCP se conecta a `/mcp/` con el Bearer de la persona.
2. El sistema valida el token (firma, vencimiento y emisor) y el rol.
3. El cliente pide la lista de herramientas y recibe sus nombres, descripciones y parámetros.
4. El cliente invoca una herramienta con sus parámetros.
5. El sistema comprueba que el rol de la persona puede usar esa herramienta, consulta al servicio dueño del dato con el token de la persona y devuelve el resultado resumido.

## Herramientas

| Herramienta | Rol | Qué devuelve | Servicio dueño |
| --- | --- | --- | --- |
| `listar_manuales` (`buscar`, `categoria`) | Estudiante | Manuales publicados de su escuela: título, categoría, tema, descripción, etiquetas y enlace al PDF | Contenido |
| `listar_tutoriales` (`buscar`, `tipo`) | Estudiante, Instructor | Tutoriales publicados para su audiencia: título, tipo, categoría, descripción, etiquetas y recurso | Contenido |
| `consultar_mis_notas` (`tipo`) | Estudiante | Sus notas: evaluación, tipo, puntaje, si aprobó y día | Académico |
| `consultar_notas_de_mis_alumnos` (`tipo`) | Instructor | Las notas de sus alumnos, con el nombre de cada uno | Académico |

Cada herramienta devuelve como máximo 20 resultados, los más recientes primero.

## Flujos alternativos y excepciones

- **A1 (paso 2):** Sin token, token inválido o vencido → 401 `ERR_SYS_04`; no se llega a ninguna herramienta.
- **A2 (paso 2):** La persona no tiene ninguno de los roles permitidos (por ejemplo, Super administrador) → 403 `ERR_SYS_05`.
- **A3 (paso 5):** El rol de la persona no puede usar esa herramienta (un instructor pidiendo `listar_manuales`) → la herramienta responde con un error «no está disponible para tu rol» y no se consulta nada.
- **A4 (paso 5):** El servicio dueño del dato no responde o falla → la herramienta responde con un mensaje legible («no responde ahora» o «no pudo atender la consulta»), sin detalles técnicos ni la respuesta cruda.
- **A5 (paso 5):** El servicio dueño del dato rechaza el token (403) → la herramienta responde «tu cuenta no tiene permiso para consultar esto».

## Reglas de negocio

- **Solo lectura:** ninguna herramienta escribe. Todas las llamadas a otros servicios son `GET`. Agregar una herramienta que escriba es una decisión nueva, no se hace desde este caso de uso.
- **Mismos permisos que en la app:** el servidor MCP no tiene credenciales propias hacia los datos: reenvía el Bearer de la persona y cada servicio aplica sus reglas (rol y escuela). Nunca devuelve datos de otra escuela ni de otro estudiante.
- **El token se valida acá y en el servicio dueño:** firma RS256 contra las claves públicas de Identidad y emisor `drivehub-identidad`; no se confía en que la petición venga del gateway ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Resultados resumidos:** se devuelven solo los campos útiles para un asistente; nunca ids internos, códigos de otras personas ni claves de archivo.
- **Sin estado:** cada petición es independiente (transporte HTTP «streamable» sin sesiones, respuestas JSON); no se guarda nada de la conversación del cliente.
- **Fuera del prefijo `/api/v1`:** `/mcp/` no es API de negocio ni se enruta por el gateway; se alcanza directo en `asistente-ia` (red interna o puerto 8087 en desarrollo).

## Postcondiciones

- Ninguna: es solo lectura. No cambia ningún dato.

## Criterios de aceptación

- **CA01:** Dado un token inválido, vencido o ausente, el sistema debe responder 401 `ERR_SYS_04` sin ejecutar ninguna herramienta.
- **CA02:** Dado un token de un rol sin acceso (por ejemplo, Super administrador), el sistema debe responder 403 `ERR_SYS_05`.
- **CA03:** Dado un cliente autenticado, el sistema debe listar las cuatro herramientas con su descripción y parámetros.
- **CA04:** Dado un estudiante, `listar_manuales` debe consultar a Contenido con el token del estudiante y devolver los manuales publicados con su enlace al PDF.
- **CA05:** Dados un estudiante o un instructor, `listar_tutoriales` debe devolver los tutoriales publicados para su audiencia.
- **CA06:** Dado un estudiante, `consultar_mis_notas` debe devolver sus notas; dado un instructor, `consultar_notas_de_mis_alumnos` debe devolver las notas de sus alumnos con el nombre de cada uno, y no deben salir códigos de personas.
- **CA07:** Dada una herramienta que el rol de la persona no puede usar, el sistema debe responder con un error de herramienta y no consultar a ningún servicio.
- **CA08:** Dado un servicio dueño del dato caído, con error o con una respuesta ilegible, la herramienta debe responder con un mensaje legible sin detalles técnicos.
- **CA09:** Dadas todas las herramientas, el sistema debe hacer únicamente consultas `GET` a los otros servicios.

## Fuera de alcance

- **Herramientas que escriban** (reservar una clase, comprar horas, publicar contenido).
- **Más herramientas de lectura** (clases, pagos, historial del tablero): se suman con su caso de uso.
- **Autenticación OAuth del propio protocolo MCP:** se usa el access token que ya emite Identidad.
- **Enrutar `/mcp/` por el gateway** y limitar su uso por persona.
- **El agente web (CU41, CU43)**, que es de otro responsable.

## Desarrollador

Otsubo
