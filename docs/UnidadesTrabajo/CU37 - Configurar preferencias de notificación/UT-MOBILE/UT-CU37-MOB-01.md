# UT-CU37-MOB-01 — Modelos, servicio y providers de las preferencias

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU37-MOB-01` |
| **Nombre** | Modelos, servicio y providers de las preferencias |
| **Caso de uso** | CU37-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU37-BE-03 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

La app tiene que traer y guardar lo que la persona elige, y no dejar guardar una configuración que el servicio va a rechazar.

## Objetivo

La feature `notificaciones` sabe leer y guardar las preferencias y mantiene el borrador de la pantalla.

## Alcance

- `lib/features/notificaciones/models/preferencias_notificacion.dart`: `PreferenciasNotificacion` (canales, tipos, `silencioDesde`/`silencioHasta` como `String?` en `HH:mm`) con `fromJson`, `toJson` (sin `actualizadoEn`; con el `email`) y `copyWith`; `TipoNotificacion` (`reserva`, `pago`, `examen`, `recordatorio`) con su nombre del JSON y su texto en español («Clases y reservas», «Pagos», «Exámenes», «Recordatorios»).
- `lib/features/notificaciones/services/preferencias_service.dart`: `obtener()` (`GET /preferencias-notificacion`) y `guardar(PreferenciasNotificacion, {required String email})` (`PUT`) con el `ApiClient` compartido.
- `lib/features/notificaciones/validators/franja_silencio_validator.dart`: `String? validarFranja(String? desde, String? hasta)` (las dos o ninguna, no iguales), con los mensajes en español.
- `lib/features/notificaciones/providers/notificaciones_providers.dart`: `preferenciasNotificacionProvider` (`AsyncNotifierProvider`, con `guardar` que reemplaza el valor guardado con la respuesta del servicio) y `preferenciasFormUiProvider` (borrador; `hayCambios` frente a lo guardado). El `email` sale de `perfilProvider`.
- Tests: `fromJson`/`toJson` (incluida la franja ausente → `null` y el nombre exacto de los tipos), el validador de la franja, y el provider con un servicio falso (carga, guarda y actualiza, error al guardar conserva el borrador, `hayCambios`).

Archivos esperados (crear/tocar): los de `lib/features/notificaciones/{models,services,validators,providers}/` y `test/features/notificaciones/`.

## Fuera de alcance (no tocar)

- La pantalla y la ruta (UT 02).
- El historial de avisos (CU38) y el token de Firebase (CU36).
- `perfil/`: solo se lee `perfilProvider`.

## Resultado esperado

Un test de provider con el servicio falso guarda un borrador y el estado guardado queda igual a lo que respondió el servicio.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` (los tests móviles se corren en la imagen de Linux, no en el host de Windows).
