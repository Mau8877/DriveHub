# UT-CU11-FE-02 — Pantalla y ruta de disponibilidad en `escuela`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-FE-02` |
| **Nombre** | Pantalla y ruta de disponibilidad en `escuela` |
| **Caso de uso** | CU11-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU11-FE-01 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (mismas carpetas que en React). Como referencia, la feature `asignacion-autos` de CU10: su esquema, su pantalla, sus tests y su ruta.

## Solicitud de negocio

El Administrador necesita una pantalla donde ver qué instructores pueden dar clase un día y con qué autos.

## Objetivo

La pantalla "Disponibilidad" lista a los instructores del día con sus filtros y su búsqueda, en `/escuela/disponibilidad`.

## Alcance

- `features/disponibilidad/schemas/disponibilidad.esquema.ts` (`ESQUEMA_DISPONIBILIDAD`, tipado con `EsquemaGestion`):
  - `recurso: 'disponibilidad-instructores'`, `idCampo: 'instructorCodigo'`, título "Disponibilidad", sin crear, editar ni acciones.
  - Columnas: `nombreCompleto` (Instructor, principal, icono `user`), `disponibilidad` (insignia: `DISPONIBLE` "Disponible" verde, `COMPLETO` "Completo" azul, `SIN_HORARIO` "Sin horario", `NO_DISPONIBLE` "No disponible" ámbar), `libres` ("Libres"), `reservadas` ("Reservadas"), `bloqueadas` ("Bloqueadas"), `autos` (Autos, `tipo: 'etiquetas'`, `etiqueta: 'placa'`, `vacioTexto: 'Sin auto'`) y `estadoAutos` (insignia: `DISPONIBLE` verde, `NO_DISPONIBLE` ámbar, `SIN_AUTO` "Sin auto").
  - Búsqueda: "Buscar por instructor, placa, marca o modelo".
  - Filtros: `filter[fecha]` (`tipo: 'fecha'`, `opciones: []`, `porDefecto: 'hoy'`, `desdeHoy: true`), `filter[momento]` (Mañana, Tarde, Noche; `todas: 'Todo el día'`), `filter[disponibilidad]` y `filter[auto]`, con los valores del contrato.
  - `mensajes`: vacío ("Todavía no hay instructores en tu escuela. Regístralos en Escuela → Instructores"), sin resultados y el error de carga en español.
- `features/disponibilidad/screens/disponibilidad-gestion.ts`: el anfitrión delgado de `<dh-gestion>` (`CUSTOM_ELEMENTS_SCHEMA`, `import '@drivehub/gestion'`).
- `app.routes.ts`: la ruta `disponibilidad` con `loadComponent`, junto a las demás (una sola ruta; el componente maneja el resto).
- Tests, en `tests/` de la feature: `disponibilidad.esquema.spec.ts` (recurso, `idCampo`, columnas y sus insignias, filtros y sus valores, sin acciones), `disponibilidad-gestion.spec.ts` (el anfitrión pasa el esquema a `<dh-gestion>`) y el caso nuevo de `app.routes.spec.ts`.

Archivos esperados (crear/tocar): el esquema, la pantalla, `app.routes.ts` y los tests.

## Fuera de alcance (no tocar)

- El paquete `@drivehub/gestion` (UT 01) y el menú del shell (UT 03).
- Crear, editar o acciones por fila, y el detalle de un instructor.
- Otras pantallas de `escuela` (autos, instructores, asignación de autos).
- `federation.config.mjs`: no cambia.

## Resultado esperado

El Administrador abre `/escuela/disponibilidad`, ve a sus instructores de hoy y puede cambiar el día, el momento y los filtros, y buscar.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `pnpm --dir projects/frontend/escuela lint` en verde.
2. En el navegador, como `adminexample@gmail.com`, con `docker compose up`: la tabla muestra al instructor de ejemplo "Disponible" hoy con franjas libres; cambiar el día a uno sin franjas lo muestra "Sin horario"; filtrar "Sin auto" deja la tabla vacía con su mensaje.
3. Escribir en el buscador la placa de un auto asignado deja solo a su instructor.
