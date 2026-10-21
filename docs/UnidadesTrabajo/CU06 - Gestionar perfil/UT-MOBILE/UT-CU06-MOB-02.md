# UT-CU06-MOB-02 — Sesión con `fotoUrl` y `actualizarUsuario`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-02` |
| **Nombre** | Sesión con `fotoUrl` y `actualizarUsuario` |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (feature `auth`) |
| **Depende de** | — |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La barra de la app debe mostrar la foto de la persona desde que inicia sesión, y reflejar al instante los cambios que haga en su perfil.

## Objetivo

La sesión de la app conoce la `fotoUrl` y puede actualizar los datos visibles del usuario sin volver a pedirla al servidor.

## Alcance

- `features/auth/models/usuario_sesion.dart`: campo `String? fotoUrl` (opcional); `fromJson` lo lee como `null` si falta (sesiones ya guardadas en el teléfono); `toJson` lo incluye.
- `features/auth/providers/sesion_provider.dart`: método `actualizarUsuario({String? nombres, String? apellidos, String? fotoUrl, bool quitarFoto = false})` que reemplaza el usuario del estado `conSesion` (y lo guarda con `SesionStorage`) sin tocar tokens ni temporizadores. Sin sesión, no hace nada.
- Tests: `UsuarioSesion.fromJson` sin el campo da `fotoUrl == null`, con el campo lo conserva y `toJson` lo devuelve; `actualizarUsuario` cambia solo lo indicado, puede quitar la foto y no hace nada sin sesión.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/models/usuario_sesion.dart`
- `projects/mobile/lib/features/auth/providers/sesion_provider.dart`
- `projects/mobile/test/features/auth/usuario_sesion_test.dart`
- `projects/mobile/test/features/auth/sesion_provider_test.dart`

## Fuera de alcance (no tocar)

- El perfil, el avatar y las pantallas (UT 03 a 07).
- Cómo se renueva la sesión o se guardan los tokens.

## Resultado esperado

`UsuarioSesion` lleva la `fotoUrl` del backend y el provider de sesión puede corregir nombre y foto al instante.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
2. Manual (con el backend de CU06): una sesión guardada antes del cambio sigue abriendo sin cerrar sesión.
