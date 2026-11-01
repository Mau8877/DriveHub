# Unidades de trabajo — CU34-MOB

**Caso de uso:** Consultar manuales: la pestaña «Manuales» de «Estudio», con búsqueda, categoría y apertura del PDF en el visor de la app.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU34 - Consultar manuales.md`](../../../CasosDeUso/CU34%20-%20Consultar%20manuales.md)
**Recurso HTTP / contrato:** `/manuales/publicados`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Decisiones de diseño

- **El PDF se abre en el visor de la app** (`VisorPdfScreen`, ruta `/pdf`, de `shared/widgets/`, que también usan los tutoriales): se lee de su dirección pública, sin pasar por el gateway ni enviar el token de la sesión. El visor y la ruta se crean en [CU33-MOB-01](../../CU33%20-%20Gestionar%20tutoriales%20del%20instructor/UT-MOBILE/UT-CU33-MOB-01.md), que también quita la descarga con el visor del teléfono que tenía esta UT.
- **La búsqueda se envía al pulsar «buscar»**, no en cada letra, para no pedir la lista con cada tecla.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU34-MOB-01](./UT-CU34-MOB-01.md) | Pestaña «Manuales» | BE-01 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01, CA02 | Lista, búsqueda y categoría | 01 |
| A1, A2, CA06 a CA08 | Estado vacío, reintento y abrir el PDF | 01 |

## Follow-ups (no se implementan en estas UT)

- «Cargar más» (hoy se trae la página 1 de 50) y lectura sin conexión.

## Qué NO incluyen estas UT

- Los tutoriales (CU35) y la vista del Instructor.
