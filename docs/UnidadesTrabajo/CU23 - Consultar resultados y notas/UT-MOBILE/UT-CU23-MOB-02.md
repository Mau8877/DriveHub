# UT-CU23-MOB-02 — Notas de mis alumnos (instructor)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-MOB-02` |
| **Nombre** | Notas de mis alumnos (instructor) |
| **Caso de uso** | CU23-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU23-MOB-01, UT-CU23-BE-03 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

El instructor ve las notas de sus alumnos desde el teléfono.

## Objetivo

El instructor abre «Notas de mis alumnos» desde su Inicio y ve las notas de sus alumnos, con el nombre de cada uno y el filtro por tipo.

## Alcance

- `Resultado.estudianteNombre`; `ResultadosService.deMisAlumnos`; `resultadosAlumnosListProvider` (comparte el filtro por tipo con «Mis notas»).
- `ResultadosScreen(deAlumnos: true)`: la misma pantalla de «Mis notas» con el nombre del alumno al inicio de cada fila y sus propios textos de estado vacío y error. Se reutiliza, no se duplica.
- `NotasAlumnosScreen` y la ruta `/grades` (solo Instructor: el redirect global saca a otro rol); acceso «Notas de mis alumnos» en el Inicio del instructor.
- Tests: casos nuevos en `test/features/resultados/resultados_test.dart` (servicio, nombre, filtro, vacío, error y reintento).

Archivos esperados (crear/tocar): los de `lib/features/resultados`, `app_router.dart`, `inicio_screen.dart` y el test.

## Fuera de alcance (no tocar)

- «Mis notas» del estudiante y la vista web del Administrador.

## Resultado esperado

El instructor ve las notas de sus alumnos, cada una con el nombre de quien la obtuvo.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: un alumno termina un simulacro y su nota aparece al instructor.
