# Unidades de trabajo — CU06-MOB

**Caso de uso:** Gestionar el perfil desde la app móvil (estudiante e instructor): pestaña "Perfil" con los datos personales, la foto, la contraseña y el correo.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU06 - Gestionar perfil`](../../../CasosDeUso/CU06%20-%20Gestionar%20perfil.md)
**Recurso HTTP / contrato:** `GET`/`PATCH /usuarios/me`, `PUT`/`DELETE /usuarios/me/foto`, `POST /usuarios/me/cambiar-contrasena` y `POST /usuarios/me/cambiar-correo` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La entrada es la pestaña "Perfil" del `UsuarioShell`** (hoy un texto de prueba): su contenido pasa a ser la pantalla de perfil. Cambiar la contraseña y cambiar el correo son pantallas aparte (`/perfil/contrasena` y `/perfil/correo`) que se abren desde ese perfil. Los administradores usan la web (el `AdminShell` no se toca).
2. **Una feature nueva `features/perfil/`** con `models/`, `providers/`, `screens/`, `services/`, `validators/` y `widgets/`. Reutiliza las reglas de campo de `features/auth/validators/registro_validators.dart` (importar de otra feature está justificado: son las mismas reglas del registro, [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)).
3. **El avatar es el primer widget compartido** (`lib/shared/widgets/avatar_usuario.dart`): lo usan el `UsuarioShell` y la pantalla de perfil, o sea dos features.
4. **Foto con `image_picker`** (dependencia nueva): galería y cámara; Android usa el selector del sistema (sin permiso extra) e iOS necesita los textos de uso en `Info.plist`. El archivo sube por `multipart/form-data` con la parte `archivo` (`FormData` de `dio`).
5. **`fotoUrl` viaja en la sesión** (login y renovación): `UsuarioSesion` la guarda y, como la URL prefirmada se renueva con cada renovación de sesión, el avatar nunca queda con una URL vencida. Las sesiones ya guardadas no la traen: el campo se lee como `null`.
6. **El perfil actualiza la sesión sin pedirla de nuevo:** tras editar o cambiar la foto, el provider llama a `SesionNotifier.actualizarUsuario(...)` para que la barra y el avatar cambien al instante.
7. **Cambiar contraseña manda `dispositivo: 'MOVIL'`** y conserva la sesión del teléfono; la de la web se cierra. El `ApiClient` ya manda el Bearer a `/usuarios/me/...` (solo las rutas `/auth/` quedan afuera), así que no cambia.
8. **El enlace del cambio de correo se abre en el navegador del teléfono,** en la página pública de la web (`/account/change-email`); la app no necesita deep links. Al volver (`resumed`) con una solicitud pendiente, la app vuelve a pedir el perfil.
9. **Tests solo donde hay lógica** ([TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)): validadores, parseo de modelos y los providers; no hay widget tests de pantallas triviales.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU06-MOB-01](./UT-CU06-MOB-01.md) | Modelos y servicio de perfil | — |
| [UT-CU06-MOB-02](./UT-CU06-MOB-02.md) | Sesión con `fotoUrl` y `actualizarUsuario` | — |
| [UT-CU06-MOB-03](./UT-CU06-MOB-03.md) | Provider del perfil | 01, 02 |
| [UT-CU06-MOB-04](./UT-CU06-MOB-04.md) | Avatar compartido y pantalla de perfil (datos personales) | 03 |
| [UT-CU06-MOB-05](./UT-CU06-MOB-05.md) | Foto de perfil | 03, 04 |
| [UT-CU06-MOB-06](./UT-CU06-MOB-06.md) | Cambiar la contraseña | 03, 04 |
| [UT-CU06-MOB-07](./UT-CU06-MOB-07.md) | Cambiar el correo | 03, 04 |

**Cadena:** `01, 02 → 03 → 04 → 05, 06, 07`. Necesita el backend de CU06 solo para la validación manual.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Entrada al perfil desde la app | 04 |
| CA02, CA03 | Editar datos personales y errores de campo | 01, 03, 04 |
| CA04 a CA07, CA09 | Subir, rechazar y quitar la foto | 05 |
| CA10, CA11, CA12 | Cambiar la contraseña y sus errores | 06 |
| CA13, CA14, CA17, CA18 | Pedir el cambio de correo, errores y ver el correo nuevo | 07 |
| Avatar en la barra | Foto o iniciales, actualizados al instante | 02, 04, 05 |

## Follow-ups (no se implementan en estas UT)

- Abrir el enlace del cambio de correo directamente en la app (deep links / App Links).
- Recortar la foto antes de subirla.
- Caché local de la foto (hoy `Image.network` usa la caché por URL).

## Qué NO incluyen estas UT

- La web ([UT-FRONTEND](../UT-FRONTEND/README.md)) ni el backend.
- Cambios en el `AdminShell` ni en el login.
- Recuperar contraseña y verificar el correo (CU04, CU04b).

## Commits sugeridos

Un commit `[MOBILE]` para todo CU06-MOB.
