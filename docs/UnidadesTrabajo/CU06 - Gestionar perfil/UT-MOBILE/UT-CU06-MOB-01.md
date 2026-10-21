# UT-CU06-MOB-01 — Modelos y servicio de perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-01` |
| **Nombre** | Modelos y servicio de perfil |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (feature `perfil`) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La app necesita hablar con los endpoints del perfil con datos tipados.

## Objetivo

La feature `perfil` tiene sus DTOs y un `PerfilService` que usa el `ApiClient`.

## Alcance

- `lib/features/perfil/models/` (clases inmutables, `final` y `const`, JSON escrito a mano, campos en `camelCase`):
  - `perfil.dart`: `Perfil` (`codigo`, `nombres`, `apellidos`, `fechaNacimiento` (`DateTime`/`yyyy-MM-dd`), `telefono?`, `email`, `correoVerificado`, `roles`, `escuelaCodigo?`, `escuelaNombre?`, `fotoUrl?`) con `fromJson`.
  - `editar_perfil_request.dart`: campos opcionales; `toJson` solo incluye los presentes y, para borrar el teléfono, manda `telefono: null` explícito.
  - `cambiar_contrasena_request.dart` (`passwordActual`, `passwordNueva`, `dispositivo: 'MOVIL'` fijo) y `cambiar_correo_request.dart` (`passwordActual`, `correoNuevo`).
- `lib/features/perfil/services/perfil_service.dart` (recibe el `ApiClient`/`Dio` compartido, sin crear el suyo): `obtener()`, `editar(request)`, `subirFoto(File/bytes, nombre)` (`FormData.fromMap({'archivo': MultipartFile.fromBytes(..., filename:, contentType:)})` con `PUT /usuarios/me/foto`), `quitarFoto()`, `cambiarContrasena(request)`, `solicitarCambioCorreo(request)`. Los rechazos del backend llegan como `ApiException` con su código; el servicio no los captura.
- Tests: `Perfil.fromJson` con y sin escuela y foto; `EditarPerfilRequest.toJson` solo incluye los campos presentes y el `null` explícito del teléfono; `CambiarContrasenaRequest.toJson` lleva `dispositivo: 'MOVIL'`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/perfil/models/perfil.dart`
- `projects/mobile/lib/features/perfil/models/editar_perfil_request.dart`
- `projects/mobile/lib/features/perfil/models/cambiar_contrasena_request.dart`
- `projects/mobile/lib/features/perfil/models/cambiar_correo_request.dart`
- `projects/mobile/lib/features/perfil/services/perfil_service.dart`
- `projects/mobile/test/features/perfil/models/perfil_models_test.dart`

## Fuera de alcance (no tocar)

- Providers y pantallas (UT 03 a 07) y la selección de la foto (UT 05).
- Cambios en `ApiClient` (las rutas `/usuarios/me/...` ya llevan el Bearer).
- `json_serializable` o generación de código.

## Resultado esperado

El servicio llama a los seis endpoints del perfil y devuelve objetos tipados.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
