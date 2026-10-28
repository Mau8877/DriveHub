# Unidades de trabajo — CU22-FE

**Caso de uso:** Gestionar examen de conducción: pantalla de exámenes de la escuela (listado, programar, reprogramar, cancelar, no asistió y registrar resultado).
**Microservicio(s) / micro-frontend(s):** `examenes` (web, React), el paquete `@drivehub/gestion` y el menú del `shell` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU22 - Gestionar examen de conducción.md`](../../../CasosDeUso/CU22%20-%20Gestionar%20examen%20de%20conducci%C3%B3n.md)
**Recurso HTTP / contrato:** pantalla `/examenes/conduccion`, sobre `/api/v1/examenes-conduccion` ([UT-BACKEND](../UT-BACKEND/README.md))

## Antes de implementar cualquier UT

Leer [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Se reutiliza `<dh-gestion>`.** Lo que faltaba se amplía en el paquete: una **acción con formulario** (`accion.formulario`), que abre el mismo drawer de crear y editar y envía su cuerpo a la ruta de la acción. Lo necesita "Registrar resultado" (puntaje y observaciones), que no es crear ni editar.
- **Estudiante, evaluador y vehículo** se eligen con listas traídas de `estudiantes`, `instructores` y `vehiculos` (`opcionesDe`, valor `codigo`).
- **Fecha y hora** son dos campos: la fecha con `<dh-fecha>` (posterior a hoy) y la hora de texto `HH:mm` (todavía no existe un `<dh-hora>`).
- **Editar es reprogramar:** solo aparece en exámenes programados y sin campos obligatorios. Las tres acciones también solo aparecen en un examen programado.
- **Menú:** la opción "Examen de conducción" en "Gestionar exámenes".

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU22-FE-01](./UT-CU22-FE-01.md) | Acción con formulario en `<dh-gestion>` | — |
| [UT-CU22-FE-02](./UT-CU22-FE-02.md) | Esquema, pantalla y ruta de examen de conducción | 01, BE-03 |
| [UT-CU22-FE-03](./UT-CU22-FE-03.md) | Opción "Examen de conducción" en el menú del shell | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, F1 a F4 | Listado, programar, reprogramar, cancelar, no asistió y resultado | 01, 02 |
| A1, A2, A3, A4 | Validación en el cliente y errores del backend bajo su campo | 02 |
| CA10 | La opción se ve solo para el Administrador | 03 |

## Follow-ups (no se implementan en estas UT)

- `<dh-hora>` propio de `@drivehub/ui`.
- Mostrar evaluador y vehículo con su nombre en la tabla.

## Qué NO incluyen estas UT

- Resultados para estudiante e instructor (CU23) y mobile.
