# UT-CU12-MOB-07 — Sección "Horarios" en el shell y rutas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-07` |
| **Nombre** | Sección "Horarios" en el shell y rutas |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU12-MOB-05`, `UT-CU12-MOB-06` |

## Antes de implementar

[NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (el chequeo vive en el `redirect` global, una sola vez; el árbol de rutas se ensambla en `core/routing/`), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) (`layouts/` no conocen lógica de negocio, `features/` no dependen de otras features). Leer `lib/core/routing/app_router.dart` y `lib/layouts/usuario/usuario_shell.dart` antes de tocarlos.

## Solicitud de negocio

El instructor tiene que llegar a sus horarios desde la app sin que el estudiante vea una opción que no es suya.

## Objetivo

La sección "Horarios" aparece en la navegación del instructor, y sus pantallas están conectadas al router.

## Alcance

- `lib/layouts/usuario/usuario_shell.dart`: agregar la sección **"Horarios"** (icono `Icons.calendar_month_outlined`) a la barra inferior **solo si** `sesionProvider.usuario.roles` incluye `'INSTRUCTOR'`; su cuerpo es `HorariosScreen`. Las secciones que ya existen no cambian, y un estudiante no ve ningún cambio. Cuidar que el índice seleccionado siga siendo válido si los `roles` cambian (por ejemplo, al renovarse la sesión).
- `lib/core/routing/app_router.dart`: agregar `GoRoute` para `/schedules/new` (`HorarioFormScreen`) y `/schedules/slots` (`FranjasScreen`). En el `redirect`, con sesión abierta, si la ruta empieza con `/schedules` y los roles **no** incluyen `INSTRUCTOR`, redirigir a `/inicio`. El resto del `redirect` no cambia.
- Tests: `usuario_shell` con rol instructor muestra "Horarios" y con rol estudiante no la muestra; el `redirect` manda a `/inicio` a un estudiante que pide `/schedules/new` y deja pasar al instructor; sin sesión sigue yendo a `/login`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/test/layouts/` y `projects/mobile/test/core/` (los tests existentes del shell y el router, ampliados)

## Fuera de alcance (no tocar)

- Las pantallas en sí (UT 05 y 06).
- El redirect por sesión (CU02), el login y la renovación.
- `AdminShell` y las demás secciones del `UsuarioShell`.
- Reorganizar la navegación del shell: solo se suma una sección condicional.

## Resultado esperado

Un instructor ve la sección "Horarios", crea horarios y gestiona franjas desde la app; un estudiante no ve nada nuevo ni puede entrar a esas rutas.

## Validación

1. `flutter test` pasa (incluidos los tests nuevos del shell y del router).
2. `flutter analyze` sin errores.
3. **De punta a punta** (con el backend del CU12 corriendo y una cuenta `INSTRUCTOR`, ver el [prerrequisito del README](./README.md#prerrequisito-para-la-prueba-manual)): entrar a la app con esa cuenta → "Horarios" → "Nuevo horario" (lunes 08:00–12:00, 60 min) → se informa cuántas franjas se generaron → "Ver franjas" en un lunes → bloquear una franja libre y desbloquearla → desactivar el horario y comprobar que las franjas futuras desaparecen. Con una cuenta de estudiante, la sección no aparece.
