# UT-CU35-MOB-01 — Biblioteca de tutoriales

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU35-MOB-01` |
| **Nombre** | Biblioteca de tutoriales |
| **Caso de uso** | CU35-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU35-BE-01, UT-CU33-MOB-01 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

Estudiantes e instructores aprenden con los tutoriales de su escuela desde el teléfono.

## Objetivo

La pestaña «Tutoriales» de «Estudio» y la «Biblioteca» del instructor listan los tutoriales publicados para su audiencia, con búsqueda y tipo, y los abren en la app.

## Alcance

- `bibliotecaFiltroUiProvider` y `bibliotecaTutorialesProvider`; `TutorialesService.publicados`.
- `BibliotecaTutorialesView` (búsqueda, chips «Todos», «Videos» y «PDF», tarjetas, estado vacío y reintento).
- `estudio_screen.dart`: la pestaña «Tutoriales» y la barra desplazable.
- Tests: casos de la biblioteca en `tutoriales_test.dart` (lista, vacío, filtros, error, abrir PDF y video).

Archivos esperados (crear/tocar): `biblioteca_tutoriales_view.dart`, `tutoriales_providers.dart`, `estudio_screen.dart` y el test.

## Fuera de alcance (no tocar)

- «Mis tutoriales» y el formulario (CU33), los manuales y las notas.

## Resultado esperado

El estudiante abre «Estudio > Tutoriales», busca y abre un video o un PDF dentro de la app.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: un tutorial publicado por un instructor aparece al estudiante y se reproduce.
