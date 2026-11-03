# UT-CU37-MOB-02 — Pantalla «Notificaciones» y acceso desde el Perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU37-MOB-02` |
| **Nombre** | Pantalla «Notificaciones» y acceso desde el Perfil |
| **Caso de uso** | CU37-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU37-MOB-01 |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (piezas del Cockpit: `CockpitScaffold`, `FilaAccion`, `AvisoCockpit`, `EstadoVacio`, `IndicadorBoton`, `avisar`), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md).

## Solicitud de negocio

Estudiantes e instructores deciden qué avisos reciben y cuándo no quieren que les suene el teléfono.

## Objetivo

Desde el Perfil se abre una pantalla donde la persona cambia canales, tipos de aviso y horas de silencio, y las guarda.

## Alcance

- `lib/features/notificaciones/screens/preferencias_notificacion_screen.dart`, con las piezas del Cockpit:
  - Sección «Canales»: interruptores «Correo», «Notificaciones push» y «Dentro de la app».
  - Sección «Tipos de aviso»: un interruptor por tipo, con su texto en español.
  - Sección «Horas de silencio»: un interruptor; al encenderlo se proponen 22:00 a 06:00 (así nunca queda una sola hora) y aparecen dos filas «Desde» y «Hasta» que abren `showTimePicker` en 24 h, y una nota que dice que solo silencia las notificaciones push. Si las dos horas quedan iguales, el motivo se muestra en línea.
  - «Guardar» (`IndicadorBoton` mientras guarda), deshabilitado sin cambios o con la franja inválida; al guardar bien, `avisar(context, 'Preferencias guardadas')`; si falla, `AvisoCockpit` de error y se conserva el borrador.
  - Mientras carga, un indicador; si la carga falla, `EstadoVacio` con «Reintentar» (sin interruptores).
- `lib/core/routing/app_router.dart`: la ruta `/perfil/notificaciones`.
- `lib/features/perfil/screens/perfil_screen.dart`: una fila «Notificaciones» (`FilaAccion`, detalle «Elige qué avisos recibes») que navega a esa ruta, junto a «Cambiar contraseña» y «Cambiar correo».
- Tests (`test/features/notificaciones/`): muestra los valores por defecto cuando el servicio los devuelve; tocar un interruptor habilita «Guardar»; guardar envía la configuración completa y el correo del perfil; apagar el silencio manda sin franja; al encender el silencio propone 22:00 a 06:00; dos horas iguales muestran el motivo y dejan «Guardar» deshabilitado; el error de carga ofrece «Reintentar»; el error al guardar conserva lo tocado.

Archivos esperados (crear/tocar): `preferencias_notificacion_screen.dart`, `app_router.dart`, `perfil_screen.dart` y los tests.

## Fuera de alcance (no tocar)

- Pedir el permiso de notificaciones del teléfono y el token de Firebase.
- El historial de avisos (CU38) y cualquier otra pantalla del Perfil.
- Cambios de tema o de piezas compartidas: si falta un widget, se resuelve con el tema del Cockpit.

## Resultado esperado

En «Perfil > Notificaciones» la persona apaga «Pagos», activa el silencio de 22:00 a 06:00, guarda, y al volver a entrar ve lo mismo.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` (en Linux, ver las notas del equipo).
2. En un emulador contra el stack: guardar, cerrar la app, volver a abrir y comprobar que se mantiene; un aviso de tipo `PAGO` enviado a esa persona por `POST /internal/v1/notificaciones` responde `OMITIDA`.
