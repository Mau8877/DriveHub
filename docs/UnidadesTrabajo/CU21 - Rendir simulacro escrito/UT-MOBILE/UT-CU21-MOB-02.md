# UT-CU21-MOB-02 — Pantallas, rutas y sección «Estudio»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-MOB-02` |
| **Nombre** | Pantallas, rutas y sección «Estudio» |
| **Caso de uso** | CU21-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU21-MOB-01 |

## Antes de implementar

[NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit y piezas compartidas).

## Solicitud de negocio

El estudiante practica la teoría desde el teléfono, con tiempo límite y corrección.

## Objetivo

La sección «Estudio» lista simulacros e historial, y permite rendir y ver la corrección.

## Alcance

- `SimulacrosScreen`: simulacros con «Rendir» (aviso de confirmación), historial con insignia (aprobado, no aprobado, sin tiempo), estados vacíos y reintento.
- `RendirSimulacroScreen`: una pregunta por pantalla, cronómetro (rojo el último minuto), anterior y siguiente, finalizar con aviso de preguntas sin responder, envío automático al llegar a cero y confirmación al salir.
- `ResultadoSimulacroScreen`: puntaje, estado y corrección por pregunta con explicación.
- `app_router.dart`: rutas `/study/simulacros/rendir` y `/study/attempts/:codigo`, protegidas para el estudiante; `estudiante_shell.dart`: «Estudio» muestra `SimulacrosScreen`.
- Tests: `simulacros_screens_test.dart`.

Archivos esperados (crear/tocar): las tres pantallas, `app_router.dart`, `estudiante_shell.dart` y el test.

## Fuera de alcance (no tocar)

- Pestañas de manuales y tutoriales y el resto de las secciones del shell.

## Resultado esperado

El estudiante inicia un simulacro, lo responde, lo envía (o se envía solo) y ve su corrección.

## Validación

1. `flutter test` (el de simulacros: 24 casos) y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: rendir un simulacro con un tema sin preguntas suficientes (aviso claro), rendir uno completo, dejar correr el tiempo y comprobar el envío automático.
