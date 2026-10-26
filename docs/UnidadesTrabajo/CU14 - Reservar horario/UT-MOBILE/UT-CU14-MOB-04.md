# UT-CU14-MOB-04 — Indicador de horas con vista previa

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-04` |
| **Nombre** | Indicador de horas con vista previa |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU14-MOB-03` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (un widget que usan dos features va en `shared/widgets/`; diseño Cockpit y legibilidad), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Leer `lib/features/pagos/widgets/saldo_horas_chip.dart` y `lib/shared/widgets/`.

## Solicitud de negocio

El estudiante quiere ver, en la pantalla de Clases, cuántas horas tiene y cómo bajan mientras elige franjas, y que al confirmar se gasten de verdad.

## Objetivo

Existe un indicador de horas para la pantalla de Clases, que muestra el saldo y, al elegir franjas, las horas que quedarían.

## Alcance

- Mover `SaldoHorasChip` de `lib/features/pagos/widgets/` a `lib/shared/widgets/` (lo usan ahora el encabezado del shell y la pantalla de Clases) y actualizar sus imports. Su comportamiento no cambia.
- `lib/features/clases/widgets/horas_disponibles_banner.dart`: `HorasDisponiblesBanner` (`ConsumerWidget`), una tarjeta del Cockpit con icono de reloj:
  - Sin franjas elegidas: «Tienes **5 h** disponibles».
  - Con franjas elegidas: «**5 h → 3 h**» y debajo «2 clases elegidas», con la flecha y el valor que quedaría destacados con `context.cockpit.acento` (texto) y el valor actual en el color de texto del tema.
  - Con 0 horas: «No tienes horas disponibles» y un botón de texto «Comprar horas» que lleva a `/payments/plans` (`context.push`).
  - Mientras carga o si falla el saldo muestra «– h» (igual que el chip) y tocarlo vuelve a pedirlo (`ref.invalidate(saldoHorasProvider)`).
  - Una breve animación del número al cambiar (`AnimatedSwitcher`, usando `AppMotion` si existe en el tema); sin colores ni medidas literales.
  - Semántica para lectores de pantalla: «Horas disponibles: 5. Quedarían 3».
- Tests de widget: sin elegir (saldo), eligiendo (de 5 a 3), con 0 horas (aparece «Comprar horas» y navega), carga y error muestran «– h», y el saldo se actualiza tras invalidar el provider.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/shared/widgets/saldo_horas_chip.dart` (movido)
- `projects/mobile/lib/features/pagos/` y `lib/layouts/` (solo los imports del chip)
- `projects/mobile/lib/features/clases/widgets/horas_disponibles_banner.dart`
- `projects/mobile/test/features/clases/horas_disponibles_banner_test.dart`, y los tests existentes del chip movidos a `test/shared/`

## Fuera de alcance (no tocar)

- La pantalla de Clases y sus pestañas (UT 05 a 07).
- `saldoHorasProvider` y el service de Pagos.
- El comportamiento del chip del encabezado.

## Resultado esperado

La pantalla de Clases puede mostrar el saldo de horas y su vista previa al elegir, reutilizando el mismo origen de datos que el resto de la app.

## Validación

1. `flutter test` pasa (incluidos los tests del chip, que siguen en verde tras moverlo).
2. `flutter analyze` sin errores.
