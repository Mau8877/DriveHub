# UT-CU19-BE-03 — Servicio y reglas de las preguntas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-03` |
| **Nombre** | Servicio y reglas de las preguntas |
| **Caso de uso** | CU19-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU19-BE-01, UT-CU19-BE-02 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (el service es la única capa que loguea y lanza), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Las reglas de una pregunta (opciones según el tipo, estado, aislamiento) tienen que vivir en un solo lugar.

## Objetivo

`PreguntaService` y `ReglasPregunta`: crear, listar, obtener, editar, activar y desactivar.

## Alcance

- DTOs `CrearPreguntaRequest`, `EditarPreguntaRequest`, `OpcionRequest` y `PreguntaResponse` (validaciones de cada campo con mensajes en español).
- `ReglasPregunta`: enunciado de 5 a 500, tema de 2 a 80, categoría `GENERAL|[A-Z]{1,3}`, dificultad de 1 a 3, explicación de hasta 500; opciones de 2 a 6 (2 en verdadero o falso), 1 correcta (o 1 o más en múltiple), sin repetidas, ids A a F por orden. Todo con `ERR_SYS_01`.
- `PreguntaService`: `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`; `crear` (activa, con la escuela y el autor), `listar` (20 por defecto, 100 máximo), `obtener`, `editar` (parcial, revalida el conjunto si cambia el tipo o las opciones), `cambiarActiva` (`ERR_ACA_15` si ya estaba así), `ERR_ACA_14` si no existe en la escuela. `INFO` por cada cambio y `WARN` por cada rechazo, con `module` y `errorCode`.

Archivos esperados (crear/tocar): `dto/*`, `service/PreguntaService.java`, `service/ReglasPregunta.java` y `PreguntaServiceTest.java`.

## Fuera de alcance (no tocar)

- HTTP y lectura del token (UT-04).
- La imagen de la pregunta.

## Resultado esperado

Con el repositorio simulado, el servicio rechaza todo dato u opción inválidos antes de guardar.

## Validación

1. `./mvnw test -Dtest=PreguntaServiceTest`: 11 casos en verde.
2. Una pregunta de opción única con dos correctas, o de verdadero o falso con tres opciones, responde `ERR_SYS_01`.
