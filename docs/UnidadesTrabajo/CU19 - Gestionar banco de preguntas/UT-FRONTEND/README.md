# Unidades de trabajo — CU19-FE

**Caso de uso:** Gestionar banco de preguntas: pantalla del banco de la escuela (listado, creación con opciones, edición, desactivar y activar).
**Microservicio(s) / micro-frontend(s):** `examenes` (web, React) y el paquete compartido `@drivehub/gestion` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU19 - Gestionar banco de preguntas.md`](../../../CasosDeUso/CU19%20-%20Gestionar%20banco%20de%20preguntas.md)
**Recurso HTTP / contrato:** pantalla `/examenes`, sobre `/api/v1/preguntas` ([UT-BACKEND](../UT-BACKEND/README.md))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Se reutiliza `<dh-gestion>`**, el componente compartido de `packages/gestion`: la pantalla es un esquema, no una tabla y un formulario nuevos. Lo que faltaba se amplía en el paquete para todos (regla de GESTION_FRONTEND.md), no se hace una excepción local.
- **Tres tipos de campo nuevos en el paquete:** `textarea` (enunciado y explicación), `opciones` (filas repetibles de respuesta con casilla "correcta", de 2 a 6) y la opción `numerico` de `seleccion` (la dificultad viaja como número). Datos booleanos (`activa`) ya funcionaban con insignias, `visibleSi` y filtros sin tocar el paquete.
- **La declaración JSX de `<dh-gestion>` en React deja de copiarse** a cada remoto: vive en `packages/gestion/react.d.ts` (export `@drivehub/gestion/react`) y cada remoto la trae con una línea (`/// <reference types="@drivehub/gestion/react" />`). Para que TypeScript resuelva `react` desde el paquete, este suma `@types/react` como dependencia de desarrollo.
- **Quedan duplicados a propósito** (decisión del equipo) `store/`, `lib/` y `mount*` de los tres remotos React: no se extraen a un paquete nuevo en este caso de uso.
- **Una sola ruta:** la raíz del remoto (`/examenes`) es el banco de preguntas; CU20 y CU22 sumarán las suyas.
- **Los campos de crear y editar se declaran una vez** en el esquema (mismo formulario, mismas reglas).
- **El contenedor `frontend` suma `examenes`** a los dev servers (`docker-compose.yml`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU19-FE-01](./UT-CU19-FE-01.md) | Ampliar `<dh-gestion>` con `textarea`, `opciones` y selección numérica | — |
| [UT-CU19-FE-02](./UT-CU19-FE-02.md) | Declaración JSX compartida de `<dh-gestion>` | — |
| [UT-CU19-FE-03](./UT-CU19-FE-03.md) | Esquema, pantalla y ruta del banco de preguntas | 01, 02, BE-04 |

**Cadena:** `01, 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, F1 a F4 | Listado, creación, edición, desactivar y activar | 01, 03 |
| A1, A2 | Validación de campos y de las opciones en el cliente | 01, 03 |
| CA11 | Agregar y quitar opciones en la pantalla | 01 |

## Follow-ups (no se implementan en estas UT)

- Categorías desde el catálogo de la escuela.
- Imagen de la pregunta.
- Un paquete para el código repetido de los remotos React (`store/`, `lib/`, `mount`).

## Qué NO incluyen estas UT

- Cambios en el shell: `/examenes` ya está montado y el menú ya lo ofrece al Administrador.
- Simulacros y examen de conducción (CU20, CU22).
- Mobile.
