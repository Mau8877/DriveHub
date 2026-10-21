# UT-CU06-MOB-05 — Foto de perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-05` |
| **Nombre** | Foto de perfil |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (feature `perfil`) |
| **Depende de** | UT-CU06-MOB-03, UT-CU06-MOB-04 |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona sube, cambia o quita su foto desde el teléfono y la ve en la barra de la app.

## Objetivo

La sección de la foto (flujo F2) dentro de la pantalla de perfil.

## Alcance

- `pubspec.yaml`: dependencia `image_picker` (versión fijada). iOS: `NSPhotoLibraryUsageDescription` y `NSCameraUsageDescription` en `ios/Runner/Info.plist` con textos en español ("Para elegir tu foto de perfil", "Para sacarte una foto de perfil"). Android no necesita permisos extra con el selector del sistema.
- `lib/features/perfil/widgets/foto_perfil_seccion.dart`: `AvatarUsuario` grande y botones "Cambiar foto" (o "Subir foto") que abre una hoja con "Galería" y "Cámara", y "Quitar foto" (solo si hay foto, con confirmación "¿Quitar tu foto?").
- **Revisión previa en el cliente** (feedback inmediato; el backend decide) en `lib/features/perfil/validators/foto_validators.dart`: formato JPG, PNG o WebP (por extensión y tipo), tamaño ≤ 5 MB (`File.length()`) y dimensiones ≤ 4000×4000 px (se leen del encabezado con `decodeImageFromList` o `instantiateImageCodec`). Mensajes: "Solo se admiten imágenes JPG, PNG o WebP", "La imagen no puede superar 5 MB", "La imagen no puede superar 4000×4000 píxeles". Un archivo rechazado no se envía. Se pide `maxWidth`/`maxHeight` de 4000 al selector para que la cámara no genere imágenes mayores.
- Con una imagen válida muestra vista previa con "Guardar foto" y "Cancelar"; al guardar llama a `perfilProvider.subirFoto` (que actualiza la sesión y el avatar de la barra) con indicador de progreso y botones deshabilitados. Éxito: `SnackBar` "Foto actualizada". Errores del backend: `ERR_IDE_16` y `ERR_IDE_17` con su mensaje en español, sin perder la foto actual. Quitar: `perfilProvider.quitarFoto` y `SnackBar` "Foto eliminada".
- Se agrega a `PerfilScreen` (UT 04).
- Tests: `foto_validators` (formato, 5 MB justo y 5 MB + 1 byte, dimensiones en el límite y pasadas); el mapeo de `ERR_IDE_16`/`ERR_IDE_17` a su mensaje.

Archivos esperados (crear/tocar):

- `projects/mobile/pubspec.yaml`
- `projects/mobile/ios/Runner/Info.plist`
- `projects/mobile/lib/features/perfil/widgets/foto_perfil_seccion.dart`
- `projects/mobile/lib/features/perfil/validators/foto_validators.dart`
- `projects/mobile/lib/features/perfil/screens/perfil_screen.dart`
- `projects/mobile/test/features/perfil/foto_validators_test.dart`

## Fuera de alcance (no tocar)

- Recortar o comprimir la imagen.
- Caché local de la foto.
- El avatar de la barra (UT 04): solo se actualiza por la sesión.

## Resultado esperado

La persona sube una foto válida desde galería o cámara y la ve en la barra y en el perfil; las inválidas se rechazan con un mensaje claro.

## Validación

1. `flutter pub get`, `flutter test` y `flutter analyze` en `projects/mobile` pasan.
2. Manual (con el backend de CU06 y `MINIO_PUBLIC_URL` con la IP de la PC): subir una foto de la galería y verla en el avatar; probar un archivo de más de 5 MB; quitar la foto.
