# Unidades de trabajo — CU14-MOB

**Caso de uso:** Reservar horario en la app móvil: la sección «Clases» del estudiante con las pestañas Mis clases, Reservar e Historial, el filtro de franjas por día, momento del día e instructor, la selección de varias franjas con el indicador de horas, y el resultado de la confirmación.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU14 - Reservar horario`](../../../CasosDeUso/CU14%20-%20Reservar%20horario.md) (absorbe al [CU13](../../../CasosDeUso/CU13%20-%20Solicitar%20clase.md))
**Recurso HTTP / contrato:** `/api/v1/franjas/disponibles`, `/api/v1/franjas/instructores`, `/api/v1/reservas` y `/api/v1/billeteras-horas/mia` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de `models/`), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers y nombres), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit: nada de colores literales ni texto sobre superficies translúcidas), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Tomar como modelo `lib/features/pagos/` (compra de horas) y `lib/features/horarios/` (franjas del instructor).

## Decisiones de diseño

1. **Todo vive en `lib/features/clases/`** (hoy solo tiene `clases_screen.dart`, un estado vacío): `models/`, `services/`, `providers/`, `screens/` y `widgets/`. La sección «Clases» del instructor (su agenda, CU16) no cambia: `ClasesScreen` sigue eligiendo por rol y, para el estudiante, muestra la pantalla nueva.
2. **Solo para el rol `ESTUDIANTE`.** La autorización real sigue en el backend.
3. **Tres pestañas dentro de la pantalla**, con `SegmentedButton` (como `horarios_screen.dart`): `Mis clases`, `Reservar`, `Historial`. Por encima de las tres, siempre visible, el indicador de horas.
4. **El indicador de horas reutiliza `saldoHorasProvider`** (Pagos, CU28) como la fuente del saldo. Mientras el estudiante elige franjas muestra «5 h → 3 h» (disponibles → las que quedarían) calculado en el teléfono; no pide nada ni descuenta. Tras confirmar, `saldoHorasProvider` se invalida y vuelve a mostrar el saldo real. `SaldoHorasChip` (que hoy vive en `pagos/widgets/`) lo usan ahora dos features: pasa a `shared/widgets/` (regla de [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md#compartidos-vs-de-una-sola-feature)).
5. **La selección es estado de UI** (`franjasSeleccionadasUiProvider`: un mapa `codigo → FranjaDisponible`), no se guarda en el servidor. Sobrevive al cambio de filtro y de pestaña, y se vacía al confirmar. No se puede elegir más franjas que horas disponibles ni más de 10.
6. **Filtros:** un día (tira horizontal de los próximos 14 días, hoy por defecto), un momento del día (`Todos`, `Mañana`, `Tarde`, `Noche`) y un instructor (lista desplegable con «Todos»). Se traducen a `filter[fecha]`, `filter[horaDesde]`/`filter[horaHasta]` (`00:00–12:00`, `12:00–18:00`, `18:00` sin límite superior) y `filter[instructor]`. Los corchetes los codifica `Dio` solo.
7. **Sin paginación visible:** la app pide una sola página de 100 franjas (un día de una escuela no pasa de eso). Si el volumen real lo pide, se agrega «Cargar más» como en el historial de pagos (follow-up).
8. **Resultado de confirmar:** una hoja inferior con las clases reservadas y, si las hay, **las que no se pudieron con su fecha, hora, instructor y el motivo en español** (el `mensaje` del backend se muestra tal cual). Las rechazadas por ocupadas (`ERR_ACA_18`) dicen que otro estudiante la tomó. Nunca se da por reservada una clase hasta ver el resultado: si falla la red al confirmar, la app lo informa y refresca «Mis clases».
9. **Fechas y horas:** el backend usa `yyyy-MM-dd` y `HH:mm`; la app los manda y lee así y los muestra en español (`lun 27 oct`, `08:00`). «Hoy» y «ahora» para ocultar lo que ya empezó son los del teléfono (solo UX; el backend rechaza con `ERR_ACA_10`). Reutilizar `formato_fecha.dart` de `pagos/widgets/` si encaja, o moverlo a `shared/` si lo usan las dos features.
10. **Colores solo del tema** (`Theme.of(context).colorScheme`, `context.cockpit`): nada de `Color(0x...)`. Piezas del Cockpit: `CockpitScaffold`, `FilaAccion`, `EstadoVacio`, `Insignia`, `AvisoCockpit`, `IndicadorBoton`.
11. **Rutas en inglés**, como el resto de la app. No se agrega ninguna ruta: las tres pestañas viven dentro de la sección «Clases» del shell, y el resumen y el resultado son hojas inferiores.

## Prerrequisito para la prueba manual

Hace falta (1) una cuenta de estudiante de `escuelaexample` con horas (comprarlas con CU26; para pruebas rápidas, tarjeta `4242 4242 4242 4242`), (2) un instructor con franjas libres (el instructor de ejemplo `instructorexample@gmail.com` crea un horario en CU12) y (3) el backend de estas UT (`UT-BACKEND`, todas las UT) con `academico` y `pagos` levantados.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU14-MOB-01](./UT-CU14-MOB-01.md) | Modelos de franjas disponibles y reservas | — |
| [UT-CU14-MOB-02](./UT-CU14-MOB-02.md) | `ClasesService` | 01 |
| [UT-CU14-MOB-03](./UT-CU14-MOB-03.md) | Providers de franjas, selección y reservas | 01, 02 |
| [UT-CU14-MOB-04](./UT-CU14-MOB-04.md) | Indicador de horas con vista previa | 03 |
| [UT-CU14-MOB-05](./UT-CU14-MOB-05.md) | Pantalla «Clases» del estudiante: pestañas, Mis clases e Historial | 03, 04 |
| [UT-CU14-MOB-06](./UT-CU14-MOB-06.md) | Pestaña «Reservar»: filtros y selección | 03, 04, 05 |
| [UT-CU14-MOB-07](./UT-CU14-MOB-07.md) | Confirmar: resumen y resultado | 03, 06 |

**Cadena:** `01 → 02 → 03 → 04 → 05 → 06 → 07`. La 04 puede hacerse en paralelo con la 05 una vez que existe la 03.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 a 3 | Pestaña Reservar con franjas y filtros por día, momento e instructor | 01, 02, 03, 06 |
| Flujo principal, paso 4 | Elegir franjas y ver las horas que quedarían | 03, 04, 06 |
| Flujo principal, pasos 5 a 8 | Resumen, confirmar y ver el resultado | 01, 02, 03, 07 |
| F1 | Mis clases | 01, 02, 03, 05 |
| F2 | Historial | 01, 02, 03, 05 |
| A1 | Sin franjas: aviso vacío | 06 |
| A2 | Sin horas: no deja elegir y ofrece comprar | 04, 06 |
| A3 a A7 | Franjas rechazadas con su motivo | 07 |
| A12, A13 | Sin conexión o error al listar: aviso y reintentar | 05, 06, 07 |
| CA17 | Indicador de horas con vista previa y saldo real | 03, 04 |

## Follow-ups (no se implementan en estas UT)

- **Cancelar o reprogramar** desde «Mis clases» (CU15).
- **Cargar más** franjas si un día supera las 100.
- **Recordatorio** de la próxima clase en Inicio (hoy `InicioScreen` tiene un estado vacío) y notificaciones (CU37, CU38).

## Qué NO incluyen estas UT

- Cancelación, reprogramación, asistencia ni calendario (CU15, CU16, CU17).
- Compra de horas: ya existe en `features/pagos/` (CU26); acá solo se enlaza a ella.
- La agenda del instructor ni su sección «Clases».
- Ningún cambio en el backend.
