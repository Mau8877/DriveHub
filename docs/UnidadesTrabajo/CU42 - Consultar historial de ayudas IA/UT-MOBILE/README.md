# Unidades de trabajo — CU42-MOB

**Caso de uso:** Consultar historial de ayudas IA: la pantalla «Historial de ayudas», con detalle y opinión.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU42 - Consultar historial de ayudas IA.md`](../../../CasosDeUso/CU42%20-%20Consultar%20historial%20de%20ayudas%20IA.md)
**Recurso HTTP / contrato:** `/ayudas-tablero`

## Antes de implementar cualquier UT

Leer [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Decisiones de diseño

- **Misma feature que CU40** (`features/tablero`): el modelo `AyudaTablero`, el servicio y los providers ya existen; el historial suma la pantalla y dos llamadas (`historial`, `marcarUtil`).
- **Pantalla aparte** (`/study/tablero/historial`) a la que se llega con «Ver mi historial de ayudas» desde la pestaña «Tablero»; el redirect global ya deja `/study/**` solo para estudiantes.
- **Detalle en una hoja inferior**, sin ruta propia: no necesita su propia navegación y vuelve al listado al cerrarla.
- **El historial se vuelve a pedir** cada vez que se abre (`autoDispose`) y al guardar una ayuda nueva o marcar una opinión, así lo recién guardado aparece sin recargar nada.
- **Miniatura tolerante:** si la imagen no carga se muestra un icono.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU42-MOB-01](./UT-CU42-MOB-01.md) | Pantalla «Historial de ayudas» | CU40-MOB-02, CU42-BE-01 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01, CA03, CA09 | Lista y detalle | 01 |
| A1 a A4, CA05, CA08 | Vacío, reintento, miniatura rota y opinión | 01 |

## Follow-ups (no se implementan en estas UT)

- «Cargar más» (hoy se trae la página 1 de 50), buscar y filtrar.

## Qué NO incluyen estas UT

- Analizar la foto y guardar la ayuda (CU40).
