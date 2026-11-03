# Unidades de trabajo — CU16-MOB

**Caso de uso:** Consultar horario de clases en la app móvil: el instructor ve su agenda por día en la pestaña «Agenda» (hoy un estado vacío) y cancela una clase sabiendo que al estudiante se le devolverá la hora y que la franja quedará bloqueada.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU16 - Consultar horario de clases`](../../../CasosDeUso/CU16%20-%20Consultar%20horario%20de%20clases.md)
**Recurso HTTP / contrato:** `GET /api/v1/reservas/agenda` y `POST /api/v1/reservas/{codigo}/cancelar-por-instructor` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de `models/`), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers y nombres), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit: nada de colores literales ni texto sobre superficies translúcidas), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Tomar como modelo lo que dejaron [CU14-MOB](../../CU14%20-%20Reservar%20horario/UT-MOBILE/README.md) y [CU15-MOB](../../CU15%20-%20Cancelar%20o%20reprogramar%20clase/UT-MOBILE/README.md) en `lib/features/clases/`.

## Decisiones de diseño

1. **Todo vive en `lib/features/clases/`**, junto a lo del estudiante: la sección «Clases» ya es una sola para los dos roles y `ClasesScreen` elige por rol. El instructor reemplaza su estado vacío por la pantalla nueva. **No se agrega ninguna ruta ni pantalla completa:** la confirmación de cancelar es un `AlertDialog`, como en CU15. Los nombres llevan `agenda` (`ClaseAgenda`, `agendaListProvider`...) para no confundirse con `Reserva` y `reservasListProvider` del estudiante.
2. **Solo para el rol `INSTRUCTOR`.** La autorización real sigue en el backend.
3. **Un día a la vez, con la fecha como estado de UI.** `agendaFechaUiProvider` guarda el día elegido (hoy al abrir). Se mueve con «día anterior», «día siguiente», «Hoy» y tocando la fecha del encabezado, que abre el selector de fecha de Material (`showDatePicker`; la regla de «nada de controles nativos» es de la web, mobile usa sus propios widgets). El encabezado muestra el día completo («Hoy», «Mañana» o la fecha larga), reutilizando lo que ya haya en `formato_clases.dart`.
4. **La app no repite reglas:** «Cancelar» se ofrece solo si `ClaseAgenda.cancelable` (que decide el servidor). Una clase `terminada` sin cerrar se rotula «Pendiente de cierre» (el cierre es de CU17).
5. **El aviso de cancelar es fijo:** «Se le devolverá 1 hora al estudiante» y «La franja quedará bloqueada». No depende de ningún plazo. Nunca se llama a cancelar sin que el instructor lo haya visto.
6. **El resultado de cancelar se muestra con `avisar` y la agenda se refresca:** `DEVUELTA` → «Clase cancelada. Se devolvió la hora al estudiante.»; `EN_PROCESO` → «Clase cancelada. La hora del estudiante se devolverá en unos minutos.». La clase queda en su día con la insignia «Cancelada».
7. **Errores de negocio con su texto** (el `mensaje` del backend, que ya está en español): `ERR_ACA_35` y `ERR_ACA_36` cierran el diálogo, muestran el aviso y refrescan la agenda. `ERR_ACA_17` al leer la agenda es un error recuperable con «Reintentar». Un error de red no cierra nada y permite reintentar.
8. **Colores solo del tema** (`Theme.of(context).colorScheme`, `context.cockpit`): nada de `Color(0x...)`. Piezas del Cockpit: `Insignia`, `AvisoCockpit`, `IndicadorBoton`, `EstadoVacio`, `avisar`.
9. **El indicador de horas no aparece:** la billetera es del estudiante.

## Prerrequisito para la prueba manual

Hace falta (1) una cuenta de instructor (`instructorexample@gmail.com`, CU12) con franjas y clases reservadas por un estudiante con horas (CU14), una para dentro de más de 12 horas y otra para dentro de menos, y (2) el backend de [`UT-BACKEND`](../UT-BACKEND/README.md) (todas las UT) con `academico`, `pagos` e `identidad` levantados.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU16-MOB-01](./UT-CU16-MOB-01.md) | Modelos y `ClasesService`: agenda y cancelar por el instructor | — |
| [UT-CU16-MOB-02](./UT-CU16-MOB-02.md) | Providers de la agenda | 01 |
| [UT-CU16-MOB-03](./UT-CU16-MOB-03.md) | Pantalla «Agenda» del instructor | 02 |
| [UT-CU16-MOB-04](./UT-CU16-MOB-04.md) | Cancelar una clase desde la agenda | 02, 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Abrir la agenda, ver las clases del día y cambiar de día | 01, 02, 03 |
| A1 | Día sin clases: aviso vacío | 03 |
| F1 | Cancelar: resumen, confirmar y resultado | 01, 02, 04 |
| A2, A3 | Clase no cancelable, ajena o inexistente | 02, 04 |
| A4 | Hora «en proceso»; Identidad caída al ver la agenda | 03, 04 |
| A7 | Sin conexión: aviso y reintentar, sin dar nada por hecho | 02, 03, 04 |
| CA01, CA02, CA12 | Agenda ordenada, día vacío, aviso previo de cancelar | 03, 04 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 04 | Hechas: `flutter analyze` sin errores y toda la suite en verde (960 tests) |
| Prueba manual en el teléfono | Pendiente (ver el prerrequisito de arriba; también depende de UT-CU16-BE-09) |

### Notas de implementación

- **Dónde quedaron los tests:** los de la feature `clases` viven todos en `test/features/clases/` (sin subcarpetas `models/`, `services/`, `screens/`, `widgets/`, `providers/` como decían las fichas), igual que los de CU14 y CU15. Los de modelos y service se sumaron a `clases_models_test.dart` y `clases_service_test.dart`; el doble `ClasesServiceFalso` de `test/helpers/clases_falsos.dart` ganó `agenda` y `cancelarComoInstructor`, y `claseDeAgendaDePrueba`.
- **El hueco de la tarjeta:** `ClaseAgendaCard` no terminó con un `Widget? acciones` genérico. Como `AccionesReserva` del estudiante, recibe `alCancelar` y dibuja «Cancelar» solo si `clase.cancelable` y se le pasó qué hacer; `ListaDeAgenda` le pasa la apertura del diálogo.
- **Texto de «Completada»:** la agenda del instructor rotula `Completada` (como pide la ficha), mientras que el historial del estudiante dice `Realizada` (CU14). Son dos funciones distintas en `formato_clases.dart` (`etiquetaDeEstadoAgenda` y `etiquetaDeEstado`).
- **El estado vacío del instructor cambió:** de «No tienes clases hoy» a «No tienes clases este día», porque ahora se puede mirar cualquier día; el test del instructor en `clases_estudiante_screen_test.dart` se actualizó.
- **`flutter test` no corre en esta máquina Windows** (`pdfrx` necesita compilar un asset nativo y no hay compilador de C), igual que en CU15: las pruebas se corrieron en Linux (imagen `drivehub-flutter:test`) sobre una copia de `projects/mobile`. `flutter analyze` sí corre aquí.

## Follow-ups (no se implementan en estas UT)

- **Vista semanal o mensual** de la agenda del instructor.
- **Mostrar la agenda en «Inicio»** (próxima clase del instructor): hoy `InicioScreen` es un estado vacío.
- **Cerrar la clase** desde la agenda (asistencia y notas): CU17.
- **Notificar** al estudiante: CU37 y CU38.

## Qué NO incluyen estas UT

- Cambios en el backend (están en [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Una vista de calendario para el estudiante: su «Mis clases» e «Historial» son de CU14.
- Reprogramar una clase por el instructor.
- Rutas nuevas, ni pantallas completas.
