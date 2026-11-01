# Unidades de trabajo — CU22-BE

**Caso de uso:** Gestionar examen de conducción: programar, listar, reprogramar, cancelar, marcar inasistencia y registrar el resultado, aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `academico` (Spring Boot, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU22 - Gestionar examen de conducción.md`](../../../CasosDeUso/CU22%20-%20Gestionar%20examen%20de%20conducci%C3%B3n.md)
**Recurso HTTP / contrato:** `/api/v1/examenes-conduccion` (ya enrutado por el gateway a `academico`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Exámenes por escuela:** `ExamenConduccion` suma `escuelaCodigo` y `creadoPorCodigo`; índice `idx_examen_escuela_estudiante_categoria`.
- **Estados:** un examen `PROGRAMADO` termina en `CANCELADO`, `NO_ASISTIO` o `REALIZADO`. Cualquier cambio sobre uno que ya no está programado es 409 `ERR_ACA_25`.
- **Intento calculado** por el servicio: exámenes del estudiante en la categoría que no están cancelados, más uno. Dos programados a la vez en una categoría: 409 `ERR_ACA_26`.
- **Fecha y hora separadas** en la API (`fecha` `yyyy-MM-dd` y `hora` `HH:mm`, en la zona de la escuela, la del `Clock`), porque así las edita la pantalla; se guardan como un instante.
- **Nombre del estudiante:** Académico no lo guarda. `IdentidadClient.nombresDeEstudiantes` llama a `POST /api/v1/estudiantes/consulta` con el Bearer del Administrador: sirve para validar que el estudiante es de la escuela (`ERR_ACA_27`) y para unir el nombre al listado en una sola consulta por página. Si Identidad falla, 503 `ERR_ACA_17`. Las respuestas de cancelar, inasistencia, reprogramar y resultado no traen el nombre (no llaman a Identidad).
- **Resultado:** el servicio decide aprobado desde 70 puntos y escribe también la nota consolidada en `resultados` (`origen = EXAMEN_CONDUCCION`), que lee CU23. `Resultado` suma `escuelaCodigo` y setters.
- **Evaluador y vehículo no se validan** contra Escuela: seguimiento declarado en el CU.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `POST` | `/api/v1/examenes-conduccion` | 201 | `ExamenResponse` |
| `GET` | `/api/v1/examenes-conduccion?page&pageSize&filter[categoria]&filter[estado]&filter[estudianteCodigo]` | 200 | `ExamenResponse[]` + `meta` |
| `GET` | `/api/v1/examenes-conduccion/{codigo}` | 200 | `ExamenResponse` |
| `PATCH` | `/api/v1/examenes-conduccion/{codigo}` | 200 | `ExamenResponse` |
| `POST` | `/api/v1/examenes-conduccion/{codigo}/cancelar` | 200 | `ExamenResponse` |
| `POST` | `/api/v1/examenes-conduccion/{codigo}/no-asistio` | 200 | `ExamenResponse` |
| `POST` | `/api/v1/examenes-conduccion/{codigo}/registrar-resultado` | 200 | `ExamenResponse` |

`ExamenResponse`: `codigo`, `estudianteCodigo`, `estudianteNombre`, `categoria`, `fechaProgramada`, `fecha`, `hora`, `evaluadorCodigo`, `vehiculoCodigo`, `intento`, `estado`, `puntaje`, `resultado`, `observaciones`.

Códigos de error nuevos: `ERR_ACA_24` a `ERR_ACA_27`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU22-BE-01](./UT-CU22-BE-01.md) | Modelos, cliente de estudiantes y códigos de error | — |
| [UT-CU22-BE-02](./UT-CU22-BE-02.md) | Repositorios y servicio de exámenes | 01 |
| [UT-CU22-BE-03](./UT-CU22-BE-03.md) | Controller, errores documentados y diseño de BD | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar con nombres y filtros | 02, 03 |
| F1 a F4 | Programar, reprogramar, cancelar o inasistencia, resultado | 02, 03 |
| A1 a A8 | Validación, fecha pasada, estudiante, estados, Identidad, rol | 01, 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Validar evaluador y vehículo contra Escuela e Identidad, y cruce de horarios del vehículo.
- Test de integración con MongoDB real.
- Aviso al estudiante por Notificaciones.

## Qué NO incluyen estas UT

- CU23 (consulta de resultados) y CU21.
- Cambios en el gateway: `examenes-conduccion` ya está enrutado.
