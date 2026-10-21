# UT-CU06-MOB-03 — Provider del perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-03` |
| **Nombre** | Provider del perfil |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (feature `perfil`) |
| **Depende de** | UT-CU06-MOB-01, UT-CU06-MOB-02 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Las pantallas del perfil comparten los mismos datos y se enteran de los cambios sin pedir todo otra vez.

## Objetivo

Un `AsyncNotifier` con el perfil y sus acciones, que mantiene la sesión sincronizada.

## Alcance

- `lib/features/perfil/providers/perfil_provider.dart`:
  - `perfilServiceProvider` (`Provider<PerfilService>`).
  - `PerfilNotifier extends AsyncNotifier<Perfil>` (`perfilProvider`): `build()` pide `obtener()`; métodos `editar(EditarPerfilRequest)`, `subirFoto(...)`, `quitarFoto()` (cada uno guarda en el estado el `Perfil` devuelto), `cambiarContrasena(...)` y `solicitarCambioCorreo(...)` (no cambian el perfil), y `recargar()` (vuelve a pedirlo, para cuando se confirma el correo fuera de la app).
  - Tras `editar`, `subirFoto` y `quitarFoto`, llama a `SesionNotifier.actualizarUsuario(...)` con los nombres y la `fotoUrl` devueltos, para que el avatar y la barra cambien al instante.
  - Los `ApiException` se relanzan para que la pantalla muestre su mensaje; no quedan como error del estado cuando solo falló una acción (el estado conserva el último perfil bueno).
- Convención de nombres de [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md): `perfilProvider` (notifier con mutaciones).
- Tests: con el servicio mockeado, `build` carga el perfil; `editar` actualiza el estado y llama a `actualizarUsuario`; `subirFoto`/`quitarFoto` igual con la `fotoUrl`; un `ApiException` al editar se relanza y el estado conserva el perfil anterior; `recargar` vuelve a pedirlo.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/perfil/providers/perfil_provider.dart`
- `projects/mobile/test/features/perfil/perfil_provider_test.dart`

## Fuera de alcance (no tocar)

- Las pantallas (UT 04 a 07) y la selección de la foto.
- Lógica de red nueva: se usa el `PerfilService` de la UT 01.

## Resultado esperado

El perfil se carga, se edita y mantiene la sesión sincronizada desde un solo provider.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
