# UT-CU18-MOB-04 — La hoja para ver las pruebas de una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-MOB-04` |
| **Nombre** | La hoja para ver las pruebas de una clase |
| **Caso de uso** | CU18-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU18-MOB-02` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (Cockpit; `Insignia`, `EstadoVacio`, `AvisoCockpit`), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo las hojas de CU17 y el formato de fechas de `formato_clases.dart`. Decisión 7 del [README](./README.md).

## Solicitud de negocio

El instructor quiere repasar cómo le fue a su estudiante en una prueba: el puntaje de cada criterio, el total y lo que anotó.

## Objetivo

`mostrarPruebasDeClase(context, clase)` abre una hoja de solo lectura con las pruebas de la clase.

## Alcance

- `widgets/pruebas_de_clase_sheet.dart` (nuevo): `PruebasDeClaseSheet` (`ConsumerWidget`) y `mostrarPruebasDeClase(BuildContext, ClaseAgenda)`, con `showModalBottomSheet` (`isScrollControlled: true`).
  - Cabecera: el estudiante, la fecha y la hora de la clase.
  - Lee `pruebasDeClaseDetailProvider(claseCodigo)` con el `claseCodigo` de la agenda. Cargando: indicador; error: el mensaje con «Reintentar»; sin pruebas: `EstadoVacio` «Esta clase todavía no tiene pruebas.» (no debería pasar si el servidor ofreció el botón, pero no rompe).
  - Una tarjeta por prueba, en el orden que llegan (el del catálogo): el tipo (`Manejo`, `Estacionamiento` o `Ciudad`), una `Insignia` «Aprobado» o «Reprobado», «Total: 80 / 100» y, debajo, cada criterio con su descripción y «7,5 / 10». Las observaciones, si hay, en un bloque aparte; sin observaciones no se dibuja.
  - Solo lectura: sin «Editar», sin «Borrar». Un solo botón «Cerrar». Esta hoja sí puede cerrarse tocando afuera.
  - Números con coma decimal y sin ceros sobrantes (`80`, `72,5`), igual que en la hoja de evaluar (reutiliza el formateador de `formato_clases.dart` o `validadores_prueba.dart`; si no existe uno, se agrega ahí).
- Tests de widget: una prueba muestra tipo, resultado, total y los criterios con su puntaje; tres pruebas muestran tres tarjetas; las observaciones aparecen solo si existen; aprobada y reprobada muestran la insignia correcta; carga y error con «Reintentar»; lista vacía muestra el estado vacío; no hay botones de editar ni borrar; «Cerrar» cierra la hoja.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/pruebas_de_clase_sheet.dart`
- `projects/mobile/test/features/clases/pruebas_de_clase_sheet_test.dart`

## Fuera de alcance (no tocar)

- Evaluar (UT 03) y el botón de la agenda (UT 05).
- Editar o borrar una prueba.
- Mostrar el puntaje en la tarjeta de la agenda.

## Resultado esperado

El instructor ve, desde su agenda, las pruebas de una clase con todo su detalle.

## Validación

1. `flutter analyze` sin errores.
2. `flutter test test/features/clases/` pasa (en Linux, ver UT-CU18-MOB-01).
