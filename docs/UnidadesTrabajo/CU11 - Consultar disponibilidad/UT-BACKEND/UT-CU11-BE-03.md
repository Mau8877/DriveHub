# UT-CU11-BE-03 — Cliente de Académico y error `ERR_ESC_20`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-03` |
| **Nombre** | Cliente de Académico y error `ERR_ESC_20` |
| **Caso de uso** | CU11-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU11-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md#comunicación). Como referencia, `InstructorCuentaRestClient` (cliente REST con el Bearer del Administrador) y su test contra un servidor falso.

## Solicitud de negocio

Escuela necesita las franjas de los instructores, que son de Académico, y debe avisar con claridad cuando Académico no responde.

## Objetivo

Escuela puede pedir a Académico el resumen de franjas de un día, detrás de una interfaz del dominio, y una falla de Académico responde 503 `ERR_ESC_20`.

## Alcance

- `domain/gateway/ResumenFranjas` (`total`, `libres`, `reservadas`, `bloqueadas`) y `domain/gateway/HorariosGateway` con `Map<UUID, ResumenFranjas> resumenDelDia(LocalDate fecha, MomentoDelDia momento)`: la clave es el `codigo_usuario` del instructor; un instructor sin franjas no viene en el mapa. Un `momento` nulo es todo el día.
- `infrastructure/client/AcademicoRestClient implements HorariosGateway`: `GET {app.academico.url}/api/v1/franjas/resumen` con `filter[fecha]`, `filter[horaDesde]` y `filter[horaHasta]` (los corchetes viajan codificados; `horaDesde` y `horaHasta` salen del `MomentoDelDia`). Reenvía el Bearer de la sesión (el del Administrador; Académico vuelve a validar el rol y toma la escuela del token). Timeout configurable. Cualquier falla (5xx, 4xx, timeout, corte de red, respuesta sin `data`) es `ServicioNoDisponibleException` con `ERR_ESC_20`, registrada con un `WARN` sin el token.
- `common/exception/ServicioNoDisponibleException` (`503`, `BusinessException`) y `EscuelaErrorCodes.HORARIOS_NO_DISPONIBLES = "ERR_ESC_20"`.
- `application.properties`: `app.academico.url=${ACADEMICO_URL:http://localhost:8084}` y `app.academico.timeout-segundos=${ACADEMICO_TIMEOUT_SEGUNDOS:5}`; `docker-compose.yml`: `ACADEMICO_URL=http://drivehub-academico:8084` en el servicio `escuela`.
- **Documentación, en el mismo cambio** ([GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)): la sección `ERR_ESC_20` en [`docs/errors/escuela/ERRORES_ESCUELA.md`](../../../errors/escuela/ERRORES_ESCUELA.md) y su link en [`docs/errors/README.md`](../../../errors/README.md).
- Tests: `AcademicoRestClientTest` contra un Académico falso (`HttpServer` del JDK): arma la ruta y los filtros codificados, manda el Bearer, lee el resumen en un mapa por instructor, y responde `ERR_ESC_20` ante `500`, `403`, cuerpo vacío y timeout.

Archivos esperados (crear/tocar): `ResumenFranjas`, `HorariosGateway`, `AcademicoRestClient`, `ServicioNoDisponibleException`, `EscuelaErrorCodes`, `application.properties`, `docker-compose.yml`, `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md` y el test.

## Fuera de alcance (no tocar)

- El caso de uso y el controller (UT 04 y 05).
- `MICROSERVICIOS.md` y el gateway (UT 06).
- El cliente de Identidad y su traductor de errores.
- Reintentos o caché del resumen.

## Resultado esperado

Escuela obtiene el resumen de franjas de Académico con la sesión del Administrador, y si Académico no responde, lo dice con un 503 y un código documentado.

## Validación

1. `./mvnw test -Dtest=AcademicoRestClientTest` en verde y `./mvnw checkstyle:check` sin errores.
2. Un `500` o un corte de Académico lanza `ServicioNoDisponibleException` con `ERR_ESC_20` (503).
3. `ERR_ESC_20` aparece en `docs/errors/` y en el índice.
