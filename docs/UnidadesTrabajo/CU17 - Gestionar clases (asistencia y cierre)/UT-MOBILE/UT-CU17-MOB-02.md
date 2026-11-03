# UT-CU17-MOB-02 — Providers de la clase dictada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-MOB-02` |
| **Nombre** | Providers de la clase dictada |
| **Caso de uso** | CU17-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU17-MOB-01` |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md#convención-de-nombres) (`{feature}DetailProvider` para el detalle de un recurso, `{feature}Provider` para el notifier con mutaciones). Tomar como modelo `gestion_agenda_provider.dart` y `agenda_provider.dart` (UT-CU16-MOB-02). Decisiones 7 y 8 del [README](./README.md).

## Solicitud de negocio

La hoja de editar necesita traer lo guardado de una clase, y las dos acciones (cerrar y corregir) tienen que dejar la agenda al día aunque la llamada falle.

## Objetivo

Hay un provider que trae una clase dictada y otro que la cierra o la corrige y refresca la agenda.

## Alcance

- `providers/clase_dictada_provider.dart`: `claseDictadaDetailProvider` (`FutureProvider.autoDispose.family<ClaseDictada, String>`, la familia es el `codigoClase`) que llama a `ClasesService.claseDictada`; los errores se propagan al `AsyncValue`.
- `providers/gestion_clase_provider.dart`: `GestionClaseNotifier extends Notifier<void>` (`gestionClaseProvider`) con:
  - `Future<ClaseDictada> cerrar(CerrarClaseRequest request)`: llama a `ClasesService.cerrarClase`, y **tanto en éxito como si la llamada falla** invalida `agendaListProvider` (el cierre pudo haberse hecho antes de cortarse).
  - `Future<ClaseDictada> actualizar(String codigoClase, ActualizarClaseRequest request)`: llama a `ClasesService.actualizarClase` e invalida `agendaListProvider` y `claseDictadaDetailProvider(codigoClase)` en éxito y en error.
  - Los errores se propagan: la hoja decide cómo mostrarlos. No toca el saldo de horas (cerrar no mueve horas).
- Tests con `ClasesService` simulado: el detalle pide el código y lo entrega; un error queda en el `AsyncValue`; `cerrar` devuelve la clase e invalida la agenda; con un `ApiException` (`ERR_ACA_37`) y con un error de red relanza e invalida igual; `actualizar` devuelve la clase, invalida la agenda y el detalle de ese código (y no el de otro); `actualizar` con error relanza e invalida igual; ninguna de las dos pide el saldo.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/providers/clase_dictada_provider.dart`, `gestion_clase_provider.dart`
- `projects/mobile/test/features/clases/clase_dictada_provider_test.dart`, `gestion_clase_provider_test.dart`
- `projects/mobile/test/helpers/clases_falsos.dart` (si falta algo del doble)

## Fuera de alcance (no tocar)

- La hoja y los botones (UT 03 y 04).
- Los providers de la agenda, del estudiante y de CU16: no cambian.

## Resultado esperado

La capa de estado de cerrar y corregir una clase está lista y probada con el service simulado.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
