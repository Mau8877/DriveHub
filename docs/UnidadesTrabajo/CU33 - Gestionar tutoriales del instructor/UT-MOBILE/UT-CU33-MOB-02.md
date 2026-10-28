# UT-CU33-MOB-02 — Mis tutoriales y formulario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU33-MOB-02` |
| **Nombre** | Mis tutoriales y formulario |
| **Caso de uso** | CU33-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU33-MOB-01 |

## Antes de implementar

[NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

El instructor crea y mantiene sus tutoriales desde el teléfono.

## Objetivo

El instructor ve «Mis tutoriales», los filtra por estado, los crea y edita con un formulario, y los publica y archiva.

## Alcance

- `MisTutorialesView` (chips de estado, tarjetas con acciones editar, publicar y archivar con confirmación, estado vacío y reintento) y `TarjetaTutorial` (con `abrirTutorial`).
- `TutorialFormScreen`: título, descripción, categoría, audiencia, etiquetas y contenido (enlace de YouTube o PDF); validaciones inline en español; el PDF se sube al elegirlo.
- `TutorialesInstructorScreen` (pestañas «Mis tutoriales» y «Biblioteca», botón «Nuevo tutorial»); rutas `/tutorials`, `/tutorials/new` y `/tutorials/edit` con el redirect de rol; acceso «Tutoriales» en el Inicio del instructor.
- Tests: `tutoriales_test.dart` (lista, acciones, confirmación, errores, formulario de crear y editar).

Archivos esperados (crear/tocar): los de `lib/features/tutoriales/{screens,widgets}`, `app_router.dart`, `inicio_screen.dart` y el test.

## Fuera de alcance (no tocar)

- La biblioteca de publicados (CU35) y el resto del Inicio.

## Resultado esperado

El instructor abre «Tutoriales > Mis tutoriales», crea un tutorial con un enlace de YouTube o un PDF, lo publica y lo archiva.

## Validación

1. `flutter test` (los de tutoriales: 36 casos) y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: crear un tutorial, publicarlo y verlo en la biblioteca.
