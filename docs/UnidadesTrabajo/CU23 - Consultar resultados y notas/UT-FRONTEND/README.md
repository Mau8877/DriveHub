# Unidades de trabajo — CU23-FE

**Caso de uso:** Consultar resultados y notas: pantalla de resultados de la escuela para el Administrador.
**Microservicio(s) / micro-frontend(s):** `examenes` (web, React) y el menú del `shell` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU23 - Consultar resultados y notas.md`](../../../CasosDeUso/CU23%20-%20Consultar%20resultados%20y%20notas.md)
**Recurso HTTP / contrato:** pantalla `/examenes/resultados`, sobre `/api/v1/resultados` ([UT-BACKEND](../UT-BACKEND/README.md))

## Antes de implementar cualquier UT

Leer [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Se reutiliza `<dh-gestion>` sin crear ni editar:** un esquema con solo columnas y un filtro es una pantalla de solo lectura (no hace falta ampliar el paquete).
- **La columna de fecha usa `dia`** (la fecha en la hora de la escuela) y no el instante, para que una nota de la noche no aparezca con la fecha del día siguiente.
- **Menú:** la opción «Resultados» en «Gestionar exámenes».

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU23-FE-01](./UT-CU23-FE-01.md) | Esquema, pantalla, ruta y menú de resultados | BE-02 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| F1 | Notas de la escuela con filtro por tipo | 01 |
| CA07 | Solo lectura y solo para el Administrador | 01 |

## Follow-ups (no se implementan en estas UT)

- Filtro por estudiante en la pantalla (la API ya lo admite).

## Qué NO incluyen estas UT

- La vista del instructor y mobile ([UT-MOBILE](../UT-MOBILE/README.md)).
