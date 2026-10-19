# UT-CU01-MOB-05 — Provider del registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-05` |
| **Nombre** | Provider del registro |
| **Caso de uso** | CU01-MOB |
| **Aplicación** | `mobile` |
| **Depende de** | UT-CU01-MOB-04 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers de datos de API vs. estado de UI, convención de nombres) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (la lógica dentro de un `AsyncNotifier` es de las que sí se prueban).

## Solicitud de negocio

La pantalla necesita enviar el registro, saber si está en curso o falló y mapear cada error del backend al campo correcto.

## Objetivo

Un provider expone la acción "registrar" y su estado, y traduce los errores de negocio.

## Alcance

- `lib/features/auth/providers/registro_provider.dart`:
  - `registroServiceProvider` (`Provider<RegistroService>`) para poder sustituirlo en tests.
  - `registroProvider`: `AsyncNotifierProvider` cuyo estado es `AsyncValue<UsuarioRegistrado?>` (`null` inicial, sin llamada al construirse) con un método `registrar(RegistroEstudianteRequest)` que pasa por `AsyncLoading` y termina en `AsyncData` o `AsyncError`.
  - Una función pura `errorDeRegistro(Object error)` que clasifica el error en un tipo (`correoEnUso` para `ERR_IDE_01`, `menorDeEdad` para `ERR_IDE_02`, `escuelaNoDisponible` para `ERR_IDE_03`, `validacion` para `ERR_SYS_01` y `general` para cualquier otro) para que la pantalla muestre cada mensaje bajo su campo.
- No guarda ningún token ni sesión.
- Tests con `ProviderContainer` y el service sustituido: éxito → `AsyncData` con el usuario; `ApiException('ERR_IDE_01')` → `AsyncError` clasificado como `correoEnUso`; los otros tres códigos y el error genérico se clasifican bien.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/providers/registro_provider.dart`
- `projects/mobile/test/features/auth/registro_provider_test.dart`

## Fuera de alcance (no tocar)

- Provider de sesión/autenticación y cualquier uso de `flutter_secure_storage` (CU02).
- Widgets, pantallas y navegación.
- Reintentos automáticos.

## Resultado esperado

La pantalla puede llamar `ref.read(registroProvider.notifier).registrar(...)`, observar el estado y clasificar el error.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
