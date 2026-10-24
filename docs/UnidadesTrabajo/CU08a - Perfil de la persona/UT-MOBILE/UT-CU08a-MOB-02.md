# UT-CU08a-MOB-02 — Registro con CI, sexo y ubicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-MOB-02` |
| **Nombre** | Registro con CI, sexo y ubicación |
| **Caso de uso** | CU08a-MOB |
| **Plataforma** | `mobile` |
| **Depende de** | UT-CU08a-MOB-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Como base, `features/auth/` (registro de CU01).

## Solicitud de negocio

El estudiante que se registra desde la app tiene que dar su CI, su sexo y su ubicación.

## Objetivo

La pantalla de registro de la app pide los datos nuevos y los envía; el error del CI sale bajo su campo.

## Alcance

- `RegistroEstudianteRequest` suma `ci`, `sexo` (`'MASCULINO'`/`'FEMENINO'`), `latitud` y `longitud` en `toJson()`.
- `registro_validators.dart`: `validarCi` (obligatorio, 5 a 15, solo letras, números y guion), `validarSexo` y `validarUbicacion`; mensajes en español iguales a los de la web ("El CI es obligatorio", "El CI debe tener entre 5 y 15 caracteres: letras, números y guion", "Elige tu sexo", "Elige tu ubicación en el mapa"). Comentario de que tienen que seguir iguales a los del backend y la web.
- `registro_screen.dart` y `registro_provider.dart`: campo CI (`TextFormField`), selector de sexo (dos opciones del tema) y `SelectorUbicacion` en una sección "Tu ubicación"; el estado del punto vive en el provider de UI del registro (`registroUiProvider`) y el formulario no se envía sin punto. `ERR_IDE_19` se muestra bajo el CI ("Este CI ya está registrado"), como hoy `ERR_IDE_01` bajo el correo.
- `registro_service.dart`: sin cambios más que el cuerpo; el `ApiClient` ya traduce el error de negocio a `ApiException`.
- Tests (`test/features/auth/`): los validadores en sus límites; `toJson()` con los campos nuevos; el provider traduce `ERR_IDE_19` al campo CI. Una prueba de widget de que el formulario no envía sin punto (la pantalla tiene lógica nueva).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/models/registro_estudiante_request.dart`
- `projects/mobile/lib/features/auth/validators/registro_validators.dart`
- `projects/mobile/lib/features/auth/providers/registro_provider.dart`
- `projects/mobile/lib/features/auth/screens/registro_screen.dart`
- `projects/mobile/test/features/auth/` (validadores, modelo, provider, pantalla)

## Fuera de alcance (no tocar)

- El widget de mapa (UT 01) y el backend.
- Pedir dirección escrita o categoría.
- El resto de las pantallas de auth.

## Resultado esperado

El estudiante se registra desde la app con su CI, su sexo y su ubicación.

## Validación

1. `flutter analyze` y `flutter test` sin errores.
2. Con el stack levantado y la app en un emulador: registrarse con un punto en el mapa; repetir el CI muestra el error bajo ese campo.
