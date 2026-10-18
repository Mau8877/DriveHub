---
globs: projects/mobile/**/*
---

# Testing — Mobile

## Diferencia deliberada respecto a `TESTING_BACKEND.md`

En el backend, todo cambio que altera comportamiento existente **debe**
actualizar sus tests (regla estricta, ver
[TESTING_BACKEND.md](../backend/TESTING_BACKEND.md)). **En mobile esa regla
estricta no aplica** — es una decisión deliberada, no un descuido ni una
omisión por symmetry con el backend.

## Criterio: caso a caso, no cobertura exhaustiva

Se testea cuando la lógica es crítica o compleja:

- Lógica de negocio dentro de un `AsyncNotifier`/provider (ej. cálculo,
  transformación de datos antes de mostrarlos).
- Parsing/mapeo de respuestas de API (que el `Interceptor` de
  [CONSUMO_API_MOBILE.md](CONSUMO_API_MOBILE.md) desempaquete bien el sobre
  estándar, que un DTO mapee bien sus campos).
- Cualquier función pura con reglas de negocio no triviales (validaciones
  compartidas con el backend, cálculos de fechas/montos, etc.).

**No se exige** cobertura de widgets triviales de UI (un botón, un layout
sin lógica, una pantalla que solo arma un formulario y llama a un provider
ya testeado). Escribir un widget test para cada pantalla del CRUD estándar
no agrega valor proporcional al tiempo que toma, para el alcance de este
proyecto.

## Cuando sí se escribe un test

Se mantiene en verde — la diferencia con backend es **cuándo se exige
escribir uno**, no el estándar de calidad una vez que existe. Un test que
se agrega y después queda roto sin arreglarse es peor que no haberlo
escrito (desinforma en vez de dar confianza).
