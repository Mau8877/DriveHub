# Unidades de trabajo — CU20-FE

**Caso de uso:** Configurar simulacro escrito: pantalla de simulacros de la escuela (listado, creación con reglas por tema, edición, desactivar y activar).
**Microservicio(s) / micro-frontend(s):** `examenes` (web, React), el paquete compartido `@drivehub/gestion` y el menú del `shell` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU20 - Configurar simulacro escrito.md`](../../../CasosDeUso/CU20%20-%20Configurar%20simulacro%20escrito.md)
**Recurso HTTP / contrato:** pantalla `/examenes/simulacros`, sobre `/api/v1/simulacros` ([UT-BACKEND](../UT-BACKEND/README.md))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Se reutiliza `<dh-gestion>`:** la pantalla es un esquema. Lo que faltaba se amplía en el paquete para todos, no se hace una excepción local.
- **Un tipo de campo nuevo en el paquete: `reglas`**, filas repetibles de tema (texto) y cantidad (entero de 1 a 100), de `min` a `max` filas (1 y 10 por defecto) y sin temas repetidos. Viaja como arreglo de `{ tema, cantidad }`. Que las cantidades sumen el número de preguntas es una regla que cruza campos: la valida solo el backend (`ERR_SYS_01`).
- **El nombre repetido** (`ERR_ACA_23`) se muestra bajo el campo nombre con `erroresDeCampo`.
- **Una ruta más en el remoto:** `/examenes/simulacros` (la raíz sigue siendo el banco de preguntas).
- **Menú:** una opción nueva "Simulacros" dentro de "Gestionar exámenes". Con `/examenes` y `/examenes/simulacros` coincidiendo con la ruta, las migas de pan eligen la opción de la ruta más específica.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU20-FE-01](./UT-CU20-FE-01.md) | Campo `reglas` en `<dh-gestion>` | — |
| [UT-CU20-FE-02](./UT-CU20-FE-02.md) | Esquema, pantalla y ruta de simulacros | 01, BE-04 |
| [UT-CU20-FE-03](./UT-CU20-FE-03.md) | Opción "Simulacros" en el menú del shell | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, F1 a F4 | Listado, creación, edición, desactivar y activar | 01, 02 |
| A1, A2, A3 | Validación de campos, de las reglas y nombre repetido | 01, 02 |
| CA10 | La opción se ve solo para el Administrador | 03 |
| CA11 | Agregar y quitar temas en la pantalla | 01 |

## Follow-ups (no se implementan en estas UT)

- Mostrar la suma de las cantidades mientras se escribe.
- Categorías desde el catálogo de la escuela.

## Qué NO incluyen estas UT

- Rendir simulacros (CU21) y examen de conducción (CU22).
- Mobile.
