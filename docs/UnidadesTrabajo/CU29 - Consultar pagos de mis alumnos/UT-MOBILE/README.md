# Unidades de trabajo — CU29-MOB

**Caso de uso:** Consultar pagos de mis alumnos en la app móvil (Instructor): ver a los alumnos asignados con sus horas compradas, las gastadas con él y las que les quedan, y el detalle con el tiempo y los pagos.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU29 - Consultar pagos de mis alumnos`](../../../CasosDeUso/CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos.md)
**Recurso HTTP / contrato:** `/api/v1/horas-alumnos` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Una feature nueva: `lib/features/alumnos/`** (models, services, providers, screens). Reutiliza `EstadoPagoVisible` e `Insignia` de `pagos` porque la relación de negocio lo justifica.
2. **Solo para el rol `INSTRUCTOR`:** la sección y la ruta `/students/:codigo`. La sección «Pagos» del instructor se renombra **«Alumnos»**: lo que muestra es a sus alumnos y su tiempo, no solo pagos.
3. **Solo lectura:** ninguna acción sobre el pago ni sobre el alumno desde esta pantalla.
4. **La búsqueda la hace el backend** (con espera de 400 ms para no llamar en cada tecla); la app no filtra del lado del dispositivo.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU29-MOB-01](./UT-CU29-MOB-01.md) | Modelos y servicio de las horas de los alumnos | — |
| [UT-CU29-MOB-02](./UT-CU29-MOB-02.md) | Providers de «Mis alumnos» | 01 |
| [UT-CU29-MOB-03](./UT-CU29-MOB-03.md) | Pantallas «Mis alumnos» y detalle del alumno, y sección del instructor | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 a 3, CA01, CA02 | Lista con horas y búsqueda por nombre | 01, 02, 03 |
| F1, CA03, CA04 | Detalle con el tiempo conmigo y los pagos | 01, 03 |
| A1, CA07 | Sin alumnos asignados | 03 |
| A2, A3, CA05 | Alumno no asignado; sin clases | 03 |
| A5, CA08 | Solo instructor | 03 |

El comprobante (F2, A4, CA06) es de CU30-MOB. Necesita el backend de [UT-BACKEND](../UT-BACKEND/README.md), con alumnos asignados (CU10) y consumos con `instructor_codigo` de Académico para ver datos reales.

## Follow-ups (no se implementan en estas UT)

- Scroll infinito.
- Ver la fecha y hora reales de cada clase.

## Qué NO incluyen estas UT

- El comprobante (CU30-MOB).
- Asignar alumnos (CU10).
- El backend ([UT-BACKEND](../UT-BACKEND/README.md)).

## Commits sugeridos

Un commit `[MOBILE]` para todo CU29-MOB.
