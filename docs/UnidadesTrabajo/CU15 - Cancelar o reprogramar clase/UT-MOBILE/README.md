# Unidades de trabajo — CU15-MOB

**Caso de uso:** Cancelar o reprogramar clase en la app móvil: desde «Mis clases», el estudiante cancela una clase sabiendo si se le devuelve la hora o la pierde, o la mueve a otra franja libre eligiéndola con los mismos filtros de «Reservar».
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU15 - Cancelar o reprogramar clase`](../../../CasosDeUso/CU15%20-%20Cancelar%20o%20reprogramar%20clase.md)
**Recurso HTTP / contrato:** `POST /api/v1/reservas/{codigo}/cancelar` y `POST /api/v1/reservas/{codigo}/reprogramar`, más el campo `cancelacionATiempo` de `GET /api/v1/reservas` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de `models/`), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers y nombres), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit: nada de colores literales ni texto sobre superficies translúcidas), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Tomar como modelo lo que dejó [CU14-MOB](../../CU14%20-%20Reservar%20horario/UT-MOBILE/README.md) en `lib/features/clases/`.

## Decisiones de diseño

1. **Todo vive en `lib/features/clases/`**, junto a lo de CU14: `models/`, `services/`, `providers/` y `widgets/`. No se agrega ninguna ruta ni pantalla nueva (regla de CU14): la confirmación de cancelar es un **diálogo** (`AlertDialog` del tema) y reprogramar es una **hoja inferior casi a pantalla completa** (`showModalBottomSheet` con `isScrollControlled`).
2. **Solo para el rol `ESTUDIANTE`**, desde «Mis clases». «Historial» no ofrece acciones. La autorización real sigue en el backend.
3. **Las acciones salen de `cancelacionATiempo`, no de un 12 en el teléfono.** El backend manda ese booleano en cada clase de «Mis clases». «Cancelar» se ofrece siempre en una clase que no empezó; «Reprogramar» solo si `cancelacionATiempo` es `true`. Qué clase "ya empezó" se calcula con la fecha y la hora del teléfono (solo UX: el backend manda con `ERR_ACA_33`).
4. **El aviso de cancelar depende de ese mismo booleano:** `true` → «Se te devolverá 1 hora»; `false` → «Cancelas con menos de 12 horas: perderás la hora» (el «12» sale del texto fijo del mensaje; si el plazo cambia en el servidor se ajusta ese texto en un solo archivo, `mensajes_de_cancelacion.dart`). Nunca se llama a cancelar sin que el estudiante vea cuál de los dos le toca.
5. **El resultado de cancelar se muestra con un aviso (`avisar`) y la lista se refresca**, sin hoja de resultado: la clase sale de «Mis clases» y entra en «Historial». El texto depende de `horaDevuelta`: `DEVUELTA` → «Clase cancelada. Se te devolvió 1 hora.»; `PERDIDA` → «Clase cancelada. No se devolvió la hora.»; `EN_PROCESO` → «Clase cancelada. Tu hora se devolverá en unos minutos.».
6. **Reprogramar reutiliza los filtros y las tarjetas de «Reservar»** (`FiltrosReservar`, `FranjaCard`, `franjasDisponiblesListProvider`): los filtros de día, momento e instructor son los mismos que los de la pestaña, así que cambiar el día en la hoja lo cambia también allá. Es una decisión consciente para no duplicar proveedores de filtros; si molesta se separan en un follow-up. La selección es **una sola franja** (`franjaParaReprogramarUiProvider`, estado de UI), no el mapa de varias de CU14, y se vacía al cerrar la hoja.
7. **Reprogramar no toca el indicador de horas:** no pide el saldo ni lo muestra en la hoja.
8. **Errores de negocio con su texto** (el `mensaje` que manda el backend, que ya está en español): `ERR_ACA_18` (otro estudiante la tomó) refresca las franjas y deja la hoja abierta para elegir otra; `ERR_ACA_34`, `ERR_ACA_33` y `ERR_ACA_32` cierran la hoja o el diálogo, muestran el aviso y refrescan «Mis clases». Un error de red no cierra nada y permite reintentar.
9. **Colores solo del tema** (`Theme.of(context).colorScheme`, `context.cockpit`): nada de `Color(0x...)`. Piezas del Cockpit: `AvisoCockpit`, `Insignia`, `IndicadorBoton`, `EstadoVacio`, `avisar`.

## Prerrequisito para la prueba manual

Hace falta (1) una cuenta de estudiante de `escuelaexample` con horas y una clase reservada para dentro de más de 12 horas y otra para dentro de menos de 12 horas, (2) otras franjas libres del instructor de ejemplo (`instructorexample@gmail.com`, CU12) y (3) el backend de [`UT-BACKEND`](../UT-BACKEND/README.md) (todas las UT) con `academico` y `pagos` levantados.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU15-MOB-01](./UT-CU15-MOB-01.md) | Modelos y `ClasesService`: cancelar y reprogramar | — |
| [UT-CU15-MOB-02](./UT-CU15-MOB-02.md) | Provider de cancelar y reprogramar | 01 |
| [UT-CU15-MOB-03](./UT-CU15-MOB-03.md) | «Mis clases»: acciones y cancelar | 02 |
| [UT-CU15-MOB-04](./UT-CU15-MOB-04.md) | Hoja de reprogramar | 02, 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 a 3 | Elegir «Cancelar», ver qué pasa con la hora y confirmar | 01, 02, 03 |
| Flujo principal, paso 5, A8 | Resultado: cancelada, hora devuelta o perdida | 01, 02, 03 |
| F1 | Reprogramar: elegir otra franja y confirmar | 01, 02, 04 |
| A1 | Sin franjas libres para reprogramar: aviso vacío | 04 |
| A2 a A6 | Franja ocupada, ya empezada, solapada, fuera de plazo, clase ajena o no cancelable | 02, 03, 04 |
| A7 | Cancelada con la hora «en proceso» | 03 |
| A11 | Sin conexión: aviso y reintentar, sin dar nada por hecho | 02, 03, 04 |
| CA15, CA17 | La cancelada aparece en el historial; el aviso dice si se devuelve o se pierde la hora | 03 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 04 | **Hechas** (2026-11-01). `flutter analyze` sin errores y la suite móvil completa en verde (904 tests, 131 de `features/clases`) |
| Prueba manual en el teléfono | **Pendiente**: necesita una cuenta de estudiante con horas y una clase a más y a menos de 12 horas (ver el prerrequisito de arriba) |

## Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **`flutter test` no corre en esta máquina Windows**: `pdfrx` necesita compilar un asset nativo y no hay compilador de C. Las pruebas se corrieron en Linux (Docker, Flutter 3.47.4 con `clang`) sobre una copia de `projects/mobile`; el CI de Linux no tiene el problema.
- **No se oculta «la franja actual» en la hoja de reprogramar:** la franja de la clase está `RESERVADA` y nunca aparece entre las libres.
- **Cancelar con un error de red también refresca el saldo** (la cancelación pudo llegar a hacerse y devolver la hora); reprogramar nunca toca el saldo.
- **`Reserva` recibe `cancelacionATiempo` con valor por defecto `false`** en el constructor: no rompe a quien la construye sin él, y un backend viejo no ofrece «Reprogramar».
- **El botón de confirmar del diálogo pasa a «Reintentar»** tras un error de red; con un error de negocio que no cierra la acción (por ejemplo `ERR_ACA_17`) se queda como «Cancelar clase».
- **Los códigos que cierran el diálogo o la hoja** (`ERR_ACA_32`, `33` y `34`) están en `mensajes_de_cancelacion.dart` (`codigosQueCierranLaAccion`).
- **La hoja de reprogramar comparte los filtros con «Reservar»** (decisión 6): cambiar el día allí lo cambia en la pestaña.

## Follow-ups (no se implementan en estas UT)

- **Filtros propios de la hoja de reprogramar**, para no mover los de la pestaña «Reservar».
- **Cancelar o reprogramar desde «Inicio»** (la tarjeta de próxima clase): hoy `InicioScreen` es un estado vacío.
- **Notificar** de un cambio de la clase: CU37 y CU38.

## Qué NO incluyen estas UT

- La pantalla del instructor (calendario y cancelar sus clases): CU16.
- Cambios en el backend.
- Comprar horas: ya existe en `features/pagos/` (CU26).
- Rutas nuevas, ni pantallas completas.
