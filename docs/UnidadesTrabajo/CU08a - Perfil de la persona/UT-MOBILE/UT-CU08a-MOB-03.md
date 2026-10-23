# UT-CU08a-MOB-03 — Perfil con CI, sexo, ubicación y categoría

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-MOB-03` |
| **Nombre** | Perfil con CI, sexo, ubicación y categoría |
| **Caso de uso** | CU08a-MOB |
| **Plataforma** | `mobile` |
| **Depende de** | UT-CU08a-MOB-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Como base, `features/perfil/` (CU06) y los validadores de CI de la UT 02.

## Solicitud de negocio

El estudiante y el instructor tienen que ver y corregir su CI, su sexo y su ubicación, y ver su categoría sin poder cambiarla.

## Objetivo

La pestaña "Perfil" muestra y edita los datos nuevos; la categoría aparece solo como lectura.

## Alcance

- `perfil.dart` suma `ci`, `sexo`, `direccion`, `latitud`, `longitud` y `categoriaSigla` (nulable) en `fromJson`; `editar_perfil_request.dart` suma `ci`, `sexo`, `latitud` y `longitud` (solo los que cambiaron; **sin** `direccion` ni `categoriaSigla`).
- `perfil_validators.dart`: reutiliza las reglas de CI y sexo (mismo origen que el registro: las reglas de `auth` se exponen para que `perfil` las importe, o se duplican con un comentario; la regla del proyecto es que una feature solo importa de otra con justificación de negocio, y aquí la hay: es la misma regla del mismo campo).
- `perfil_screen.dart` y `perfil_provider.dart`: campos CI y sexo editables; `SelectorUbicacion` precargado con el punto guardado y, debajo, la `direccion` calculada por el servidor (o "Dirección pendiente de calcular"); si el punto cambia, al guardar se envía el par y el provider refresca la dirección con la respuesta. Una fila "Categoría" de solo lectura con `categoriaSigla` y la ayuda "La asigna tu escuela", solo si no es nula. `ERR_IDE_19` bajo el CI.
- Tests (`test/features/perfil/`): `Perfil.fromJson` con y sin ubicación y categoría; `EditarPerfilRequest.toJson` sin campos que no cambiaron y con el par de coordenadas cuando cambia; el provider traduce `ERR_IDE_19`; prueba de widget de que la categoría no es editable.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/perfil/models/perfil.dart` y `editar_perfil_request.dart`
- `projects/mobile/lib/features/perfil/validators/perfil_validators.dart`
- `projects/mobile/lib/features/perfil/providers/perfil_provider.dart`
- `projects/mobile/lib/features/perfil/screens/perfil_screen.dart`
- `projects/mobile/test/features/perfil/`

## Fuera de alcance (no tocar)

- Foto, contraseña y correo.
- Editar la categoría.
- El widget de mapa (UT 01) y el registro (UT 02).
- El backend.

## Resultado esperado

El perfil de la app muestra y edita los datos nuevos, con la categoría solo de lectura.

## Validación

1. `flutter analyze` y `flutter test` sin errores.
2. Con el stack levantado y la app en un emulador: abrir "Perfil", mover el punto del mapa y guardar; la dirección cambia. La categoría no es editable.
