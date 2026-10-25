# UT-CU30-MOB-01 — Descargar y abrir el comprobante en PDF

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-MOB-01` |
| **Nombre** | Descargar y abrir el comprobante en PDF |
| **Caso de uso** | CU30-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El estudiante y el instructor tienen que poder ver el comprobante en el teléfono.

## Objetivo

`ComprobantesService.descargarPdf` baja el PDF y `BotonComprobante` lo abre.

## Alcance

- `core/network/api_client.dart`: el interceptor que desempaqueta el sobre solo actúa si la respuesta es un `Map` (hoy hace `response.data as Map<String, dynamic>` y fallaría con bytes); una respuesta binaria pasa tal cual. Los errores de una descarga siguen llegando con el sobre y como `ApiException`.
- Dependencias `path_provider` y `open_filex` en `pubspec.yaml` (versiones fijadas); `android/app/src/main/AndroidManifest.xml` y `ios/Runner/Info.plist` con lo que pida `open_filex` para abrir PDF.
- `features/pagos/services/comprobantes_service.dart`: `Future<File> descargarPdf(String codigoComprobante)` pide `GET /comprobantes/{codigo}/pdf` con `responseType: ResponseType.bytes`, lo guarda en el directorio temporal (`comprobante-{codigo}.pdf`, se sobrescribe) y devuelve el archivo. `ERR_PAG_14` (409, "en preparación") y `ERR_PAG_13` (404) llegan como `ApiException`.
- `features/pagos/providers/comprobante_provider.dart`: `comprobanteDescargaProvider` (`AsyncNotifierProvider.autoDispose.family` por código) con `descargarYAbrir()`: llama al servicio y abre el archivo con `open_filex`; evita una segunda descarga mientras hay una en curso.
- `features/pagos/widgets/boton_comprobante.dart`: un botón «Comprobante» con icono; mientras descarga muestra `IndicadorBoton` y se deshabilita; `ERR_PAG_14` → aviso "Tu comprobante todavía se está preparando, inténtalo en unos minutos"; `ERR_PAG_13` o cualquier otro error → aviso con el mensaje del backend; si no hay app para abrir PDF, aviso claro. Lo usan [CU28](../../CU28%20-%20Consultar%20historial%20de%20pagos/UT-MOBILE/README.md) y [CU29](../../CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos/UT-MOBILE/README.md) (UT 02).
- Tests: el interceptor deja pasar bytes y sigue desempaquetando JSON y errores; `ComprobantesService` con un adaptador falso (escribe el archivo con los bytes recibidos, `ERR_PAG_14` propagado); el provider no descarga dos veces a la vez; el botón muestra el aviso de cada error (widget test con el servicio y el abridor falsos).

Archivos esperados (crear/tocar):

- projects/mobile/pubspec.yaml
- projects/mobile/lib/core/network/api_client.dart
- projects/mobile/lib/features/pagos/services/comprobantes_service.dart
- projects/mobile/lib/features/pagos/providers/comprobante_provider.dart
- projects/mobile/lib/features/pagos/widgets/boton_comprobante.dart
- projects/mobile/test/core/api_client_bytes_test.dart
- projects/mobile/test/features/pagos/comprobantes_service_test.dart
- projects/mobile/test/features/pagos/boton_comprobante_test.dart

## Fuera de alcance (no tocar)

- Mostrar el botón en las pantallas (UT 02).
- Guardar los PDF para verlos sin conexión.
- Compartir el PDF.

## Resultado esperado

Dado un código de comprobante, la app baja el PDF y lo abre en el visor del teléfono, o explica por qué no pudo.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con el backend de CU30 levantado, el botón abre un PDF real del comprobante.
