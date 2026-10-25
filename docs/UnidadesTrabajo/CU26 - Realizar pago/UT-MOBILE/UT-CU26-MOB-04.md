# UT-CU26-MOB-04 — Pantallas «Comprar horas», resumen y resultado del pago

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-MOB-04` |
| **Nombre** | Pantallas «Comprar horas», resumen y resultado del pago |
| **Caso de uso** | CU26-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU26-MOB-01, UT-CU26-MOB-03 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El estudiante elige un plan, ve cuánto va a pagar, paga y sabe qué pasó.

## Objetivo

Las pantallas de la compra: lista de planes, resumen y resultado.

## Alcance

Con las piezas del tema Cockpit ([WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)): `CockpitScaffold`, `FilaAccion`, `Insignia`, `EstadoVacio`, `AvisoCockpit`, `IndicadorBoton` y `avisar`; nada de `Color(0x...)` sueltos.

- `features/pagos/widgets/mensajes_de_pago.dart`: `mensajeDeErrorDeCompra(error)` traduce un error de la compra a un mensaje en español por su código (`ERR_PAG_05`, `06`, `15`, `17`, `18`), a "no hay conexión" para un error de red y a un mensaje general para el resto; nunca muestra el texto crudo del backend.
- `features/pagos/widgets/formato_dinero.dart`: `formatearBs(double)` → `Bs 1.250,00` y `formatearUsd(double)` → `US$ 179,60` (miles con punto, decimales con coma; sin `intl`, con tests propios, incluidos 0, `0.5` y `1234567.891`).
- `ComprarHorasScreen` (`/payments/plans`): lista los planes (`planesDisponiblesProvider`) como tarjetas con el nombre, las horas, el precio en Bs y en USD y el botón **Comprar**; estado de carga, estado vacío ("Tu escuela todavía no ofrece planes") y error con reintento. Al tocar Comprar guarda el plan en `compraUiProvider` y va al resumen.
- `ResumenCompraScreen` (`/payments/summary`): plan, horas y "Pagarás **US$ X** (≈ Bs Y)"; una nota de que el pago es seguro y se hace con tarjeta en la página de la pasarela (la app no ve datos de tarjeta); botón **Confirmar y pagar** con indicador mientras llama a `compraProvider.iniciar()`; al recibir el `PagoIniciado` abre `urlPago` con `abrirPaginaDePago` (UT 01, a través de `abridorDePaginaProvider`, que los tests sustituyen) y va a `/payments/result`. Si no se pudo abrir la página, avisa y pasa igual al resultado, que permite volver a abrirla. Errores de negocio con su mensaje (`AvisoCockpit`): `ERR_PAG_05` vuelve a la lista con un aviso y la refresca; `ERR_PAG_06` y `ERR_PAG_15` ("No pudimos conectar con el pago. Inténtalo de nuevo más tarde."); `ERR_PAG_17` ("Tu compra ya se está procesando, espera unos segundos"); sin conexión con reintento.
- `PagoResultadoScreen` (`/payments/result`): consulta el estado con `estadoDePagoProvider` y se refresca al volver a la app (`WidgetsBindingObserver`, `AppLifecycleState.resumed`). `PENDIENTE`: "Pago en proceso" con indicador, y los botones **Volver a abrir la página de pago** (mientras no venza) y **Volver al inicio** (no se llama «Cancelar»: salir de la pantalla no cancela nada, el pago sigue su curso); `CONFIRMADO`: "¡Listo! Tus horas ya están disponibles" con **Ir al inicio** (el historial de compras llega con CU28); `FALLIDO` o `REVERTIDO`: "No se pudo completar la compra. Si te cobraron, te devolveremos el dinero" con **Volver a los planes**; sin novedades tras el plazo: "Tu pago sigue en proceso. Todavía no recibimos la confirmación; cuando llegue, tus horas aparecerán en tu cuenta" con **Volver al inicio**. Al terminar (confirmado o fallido) regenera la clave de la compra.
- Mismas reglas que el resto de la app: textos en español, sin términos técnicos, cada estado distinguible también por texto (no solo por color).
- Tests de widget de lo esencial: la lista muestra los planes y su botón; el resumen deshabilita el botón mientras llama y muestra el mensaje de cada error; el resultado muestra cada estado y el botón de reabrir solo en `PENDIENTE`. Test de `formato_dinero`.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/widgets/formato_dinero.dart
- projects/mobile/lib/features/pagos/screens/comprar_horas_screen.dart
- projects/mobile/lib/features/pagos/screens/resumen_compra_screen.dart
- projects/mobile/lib/features/pagos/screens/pago_resultado_screen.dart
- projects/mobile/test/features/pagos/formato_dinero_test.dart
- projects/mobile/test/features/pagos/comprar_horas_screen_test.dart
- projects/mobile/test/features/pagos/resumen_compra_screen_test.dart
- projects/mobile/test/features/pagos/pago_resultado_screen_test.dart

## Fuera de alcance (no tocar)

- Las rutas, el menú y el control de rol (UT 05).
- El historial de pagos (CU28).
- Un formulario de tarjeta propio: se paga en la página de la pasarela.

## Resultado esperado

El estudiante elige un plan, confirma el resumen, paga en la página de la pasarela y ve si el pago se confirmó, sigue en proceso o falló.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con el backend de CU26 y CU27 levantado, una compra con la tarjeta de prueba de Stripe termina en «¡Listo!» sin cerrar la app.
