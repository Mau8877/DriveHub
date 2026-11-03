# UT-CU16-BE-08 — Endpoints de la agenda y de cancelar por el instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-08` |
| **Nombre** | Endpoints de la agenda y de cancelar por el instructor |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU16-BE-06`, `UT-CU16-BE-07` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados) (controller delgado; acciones como sub-recurso verbo), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#convenciones) (el test de controller verifica la forma del `ApiResponse`). Tomar como modelo `ReservaController` (UT-CU15-BE-09).

## Solicitud de negocio

La app del instructor necesita una llamada para leer la agenda de un día y otra para cancelar una clase.

## Objetivo

Existen `GET /api/v1/reservas/agenda` y `POST /api/v1/reservas/{codigo}/cancelar-por-instructor`, ambas solo para el rol Instructor.

## Alcance

- `AgendaController` (nuevo, `features/reservas/controller/`, `@RestController`, `@RequestMapping("/api/v1/reservas")`, `@RequiredArgsConstructor`):
  - `GET /agenda` con `@CurrentUserId`, `@CurrentEscuelaId` (se pasa al servicio, que responde `403 ERR_ACA_16` si es nulo, como `reservar`), `BearerActual`, `@RequestParam(name = "filter[fecha]", required = false) String fecha`, `page` (1) y `pageSize` (20); llama a `AgendaService.agenda` y responde `200` con `ApiResponse.successList(...)` y su `PageMeta`, «Agenda obtenida correctamente».
  - `POST /{codigo}/cancelar-por-instructor` con `@CurrentUserId`, `@CurrentEscuelaId`, `BearerActual` y `@PathVariable UUID`; llama a `AgendaService.cancelar` y responde `200` «Clase cancelada correctamente».
  - Sin lógica, sin `try/catch`, sin logging. Un `{codigo}` que no es UUID es `400 ERR_SYS_01` (comprobar que el handler global ya lo cubre).
  - Las rutas literales (`/agenda`) no chocan con las del `ReservaController` (no existe `GET /{codigo}`); comprobar que Spring resuelve cada una al controller correcto.
- Tests con `@WebMvcTest` (servicio simulado): agenda → `200` con la lista, `meta` y los campos de `ClaseAgendaResponse`, sin ids internos; `filter[fecha]` (con los corchetes codificados) llega al servicio; sin parámetros usa los valores por defecto; cancelar → `200` con `DEVUELTA` y con `EN_PROCESO`; `ERR_ACA_35` → 404; `ERR_ACA_36` → 409; `ERR_ACA_17` → 503; `{codigo}` inválido → `400 ERR_SYS_01`; sin sesión → `401 ERR_SYS_04`; sin escuela → `403 ERR_ACA_16`; la forma del sobre (`status`, `data`, `message`, `timestamp`, `error` solo si falló).
- Comprobar `MICROSERVICIOS.md` y el gateway: el recurso `reservas` ya está en las dos partes, **no se agrega fila ni ruta**. Si hay un documento en `docs/DriveHub/` que lista las rutas de `reservas`, agregar estas dos.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/controller/AgendaController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/controller/AgendaControllerTest.java`

## Fuera de alcance (no tocar)

- La lógica de ver y cancelar (UT 06 y 07).
- `ReservaController` y las rutas del estudiante.
- El gateway: `/api/v1/reservas/**` ya enruta a Académico.
- El `403 ERR_SYS_05` por rol: lo decide el `@PreAuthorize` del servicio, que en `@WebMvcTest` está simulado (se comprueba por reflexión en las UT 06 y 07, igual que en CU15).

## Resultado esperado

La app del instructor puede leer su agenda de un día y cancelar una clase con una llamada cada una.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba y el token de un instructor: `GET /api/v1/reservas/agenda?filter%5Bfecha%5D=<fecha con clases>` → `200` con sus clases y los nombres; `POST /api/v1/reservas/<codigo>/cancelar-por-instructor` → `200`, `horaDevuelta: "DEVUELTA"`; la franja queda `BLOQUEADA` en `db.franjas`.
