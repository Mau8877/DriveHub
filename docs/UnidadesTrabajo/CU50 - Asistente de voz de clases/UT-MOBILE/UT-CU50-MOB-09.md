# UT-CU50-MOB-09 — Permisos y modelos ausentes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-09` |
| **Nombre** | Permisos y modelos ausentes |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-05, UT-CU50-MOB-06, UT-CU50-MOB-08 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

Si falta el permiso o los modelos, la persona tiene que enterarse y seguir usando la pantalla como siempre.

## Objetivo

El asistente pide el micrófono, y si no hay permiso o faltan modelos se desactiva con un aviso, sin cerrar la app.

## Alcance

- `AndroidManifest.xml`: permiso de micrófono (`RECORD_AUDIO`), declarado con su comentario del CU como los demás (el plugin `record` también lo aporta al Manifest final).
- Petición del permiso la primera vez que se pulsa el botón (A11): la hace el grabador (`record`, `hasPermission`). Si se niega, el panel lo explica y dice que se puede conceder en los ajustes de la app; el botón se puede volver a pulsar. Un botón que abra los ajustes directamente necesita un paquete de permisos (`permission_handler`) que hoy no está en el proyecto: queda como follow-up.
- Si `ModeloNoDisponible` o `ModeloDeVozNoDisponible` (A12), el botón avisa «El asistente no está disponible» y no escucha; el resto de la pantalla sigue igual.
- Tests: `test/features/asistente/disponibilidad_test.dart`: modelo de acciones ausente con el motor real (sin el archivo, nunca toca el plugin) y volver a intentar; permiso negado y concedido después; modelo de voz ausente.

Archivos esperados (crear/tocar): `AndroidManifest.xml`, el botón y el test.

## Fuera de alcance (no tocar)

- Descargar o empaquetar los modelos (follow-up).

## Resultado esperado

Sin permiso o sin modelos, el asistente lo dice y la pantalla de Clases funciona con sus controles normales.

## Validación

1. `flutter test test/features/asistente/disponibilidad_test.dart` y `flutter analyze`.
2. En un Android real: negar el permiso y comprobar el aviso; borrar la carpeta `ia/` y comprobar que no se cierra.
