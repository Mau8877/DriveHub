# UT-CU29-MOB-02 — Providers de «Mis alumnos»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-MOB-02` |
| **Nombre** | Providers de «Mis alumnos» |
| **Caso de uso** | CU29-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU29-MOB-01 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El instructor busca a sus alumnos por nombre y abre el detalle de cada uno.

## Objetivo

Providers del listado con búsqueda y del detalle.

## Alcance

- `alumnosBusquedaUiProvider` (estado de UI): el texto de búsqueda.
- `misAlumnosProvider` (`AsyncNotifierProvider`): carga la primera página (20) y expone `cargarMas()` que agrega la siguiente hasta `totalPages`; cambiar la búsqueda reinicia la lista, con una **espera de 400 ms** desde la última tecla antes de pedir (para no llamar en cada letra); `refrescar()`.
- `alumnoDetalleProvider` (`FutureProvider.autoDispose.family` por código de estudiante).
- Tests con servicio falso y reloj controlado: la búsqueda espera 400 ms y pide una sola vez aunque se escriban varias letras; `cargarMas` agrega sin duplicar; el detalle por código; un error de red permite reintentar sin perder lo cargado.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/alumnos/providers/alumnos_busqueda_provider.dart
- projects/mobile/lib/features/alumnos/providers/mis_alumnos_provider.dart
- projects/mobile/lib/features/alumnos/providers/alumno_detalle_provider.dart
- projects/mobile/test/features/alumnos/mis_alumnos_provider_test.dart

## Fuera de alcance (no tocar)

- Pantallas y rutas (UT 03).
- Búsqueda del lado del dispositivo: la hace el backend.

## Resultado esperado

La lista de alumnos se busca y se pagina sin llamadas de más.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
