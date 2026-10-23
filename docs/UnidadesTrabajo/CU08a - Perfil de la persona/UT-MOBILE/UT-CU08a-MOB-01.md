# UT-CU08a-MOB-01 — `SelectorUbicacion` (mapa)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-MOB-01` |
| **Nombre** | `SelectorUbicacion` (mapa) |
| **Caso de uso** | CU08a-MOB |
| **Plataforma** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (widgets compartidos, tema Cockpit, nada de `Color(0x...)` en una pantalla, legibilidad), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

La persona tiene que poder marcar dónde vive tocando un mapa, en el registro y en el perfil.

## Objetivo

Existe `SelectorUbicacion`, un widget de mapa reutilizable que devuelve latitud y longitud.

## Alcance

- `pubspec.yaml`: `flutter_map`, `latlong2` y `geolocator`, con versiones fijas. Android: permisos `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` y acceso a internet en `AndroidManifest.xml`; iOS: `NSLocationWhenInUseUsageDescription` en `Info.plist` con el motivo en español ("Para marcar dónde vives en tu perfil").
- `lib/shared/widgets/selector_ubicacion.dart`: `SelectorUbicacion` con `latitud`, `longitud` (`double?`), `direccion` (`String?`, de solo lectura), `habilitado`, `onCambio(double latitud, double longitud)` y `centroInicial` (por defecto La Paz, -16.4897, -68.1193; si ya hay punto, centra en él). Mosaicos de OpenStreetMap con su atribución visible, un marcador con el color de acento del tema (`context.cockpit.acento`) y zoom limitado. Un toque coloca o mueve el marcador y llama a `onCambio` con 6 decimales; botón "Mi ubicación" que usa `geolocator` y, si el permiso se niega o el GPS está apagado, muestra un aviso breve (`avisar`) y el mapa sigue funcionando. Si hay `direccion`, se muestra debajo con `Text` del color de texto del tema.
- Tamaño fijo y alto razonable dentro de un `Form`; no captura el gesto de scroll de la pantalla salvo cuando se interactúa con el mapa.
- `proveedorTeselasProvider` (Riverpod, `null` por defecto) en el mismo archivo: los tests de las pantallas lo sustituyen por mosaicos falsos (`test/helpers/teselas_falsas.dart`) para no depender de la red. Android declara además `INTERNET`.
- Tests (`test/shared/widgets/`): el widget se construye con y sin punto; un toque llama a `onCambio` con los decimales correctos (con un controlador de mapa simulado); `habilitado: false` no llama; la dirección se muestra; el botón "Mi ubicación" con el permiso denegado muestra el aviso (con `geolocator` simulado).

Archivos esperados (crear/tocar):

- `projects/mobile/pubspec.yaml` y `pubspec.lock`
- `projects/mobile/android/app/src/main/AndroidManifest.xml`
- `projects/mobile/ios/Runner/Info.plist`
- `projects/mobile/lib/shared/widgets/selector_ubicacion.dart`
- `projects/mobile/test/shared/widgets/selector_ubicacion_test.dart`

## Fuera de alcance (no tocar)

- Calcular la dirección: la calcula el servidor.
- Usarlo en alguna pantalla (UT 02 y 03).
- Búsqueda por texto, rutas o varios marcadores.
- Cambios al tema ni a los demás widgets compartidos.

## Resultado esperado

Cualquier pantalla de la app puede mostrar un mapa que devuelve un punto.

## Validación

1. `flutter pub get`, `flutter analyze` y `flutter test` sin errores.
2. En un emulador, el widget muestra el mapa de La Paz con su atribución y el marcador se mueve al tocar.
