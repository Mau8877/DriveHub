# Unidades de trabajo — CU23-MOB

**Caso de uso:** Consultar resultados y notas: «Mis notas» del estudiante, dentro de la sección «Estudio», y «Notas de mis alumnos» del instructor.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU23 - Consultar resultados y notas.md`](../../../CasosDeUso/CU23%20-%20Consultar%20resultados%20y%20notas.md)
**Recurso HTTP / contrato:** `/resultados/mios`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Decisiones de diseño

- **Feature `resultados/`** con la forma de siempre, y una feature `estudio/` que solo agrupa en pestañas lo que va en la sección «Estudio»: «Simulacros» (CU21) y «Mis notas» (CU23). CU34 y CU35 sumarán las suyas.
- **Filtro por tipo** como chips (`resultadosFiltroUiProvider`); cambiar de filtro vuelve a pedir la lista.
- **La app solo muestra lo que informa el backend:** no calcula puntajes ni si aprobó.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU23-MOB-01](./UT-CU23-MOB-01.md) | Mis notas y pestañas de «Estudio» | BE-02 |
| [UT-CU23-MOB-02](./UT-CU23-MOB-02.md) | Notas de mis alumnos (instructor) | MOB-01, BE-03 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Notas del estudiante con filtro por tipo | 01 |
| A5, CA06 | Estado vacío y reintento sin conexión | 01 |
| F2, CA08 a CA10 | Notas de los alumnos del instructor | 02 |

## Follow-ups (no se implementan en estas UT)

- «Cargar más» (hoy se trae la página 1 de 50).

## Qué NO incluyen estas UT

- La vista web del Administrador ([UT-FRONTEND](../UT-FRONTEND/README.md)).
