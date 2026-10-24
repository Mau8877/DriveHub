# Unidades de trabajo — CU07-FE

**Caso de uso:** Gestionar autos: pantalla de la flota de la escuela (listado, registro, edición y cambio de estado).
**Microservicio(s) / micro-frontend(s):** `escuela` (web, Angular) y el paquete compartido `@drivehub/gestion` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU07 - Gestionar autos.md`](../../../CasosDeUso/CU07%20-%20Gestionar%20autos.md)
**Recurso HTTP / contrato:** pantalla `/escuela/autos`, sobre `/api/v1/vehiculos` ([UT-BACKEND](../UT-BACKEND/README.md))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Se amplía `<dh-gestion>`, no se hace una excepción local.** El formulario de autos necesita un desplegable (transmisión) y un campo numérico (año, kilometraje), que el esquema no expresaba. Siguiendo GESTION_FRONTEND.md ("se amplía el esquema para todos"), se suman `tipo: 'seleccion'` (con `opciones`) y `tipo: 'numero'` (entero; `min`/`max` son el rango del valor, y viaja como número en el cuerpo).
- **Una sola ruta** `/escuela/autos` hacia la pantalla; la raíz `/escuela` pasa a ser una puerta con un enlace a Autos.
- **El menú no cambia:** el Administrador de escuela ya ve "Escuela" (`/escuela`) y el Super admin no.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU07-FE-01](./UT-CU07-FE-01.md) | Ampliar `<dh-gestion>` con `numero` y `seleccion` | — |
| [UT-CU07-FE-02](./UT-CU07-FE-02.md) | Esquema, pantalla y ruta de autos | 01 |
| [UT-CU07-FE-03](./UT-CU07-FE-03.md) | Foto del auto: campo y columna de imagen | 02, BE-06 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, F1 a F5 | Listado, registro, edición y acciones de estado | 01, 02 |
| A1 | Validación en el cliente | 01, 02 |
| A2 | Placa en uso bajo el campo | 02 |

## Follow-ups (no se implementan en estas UT)

- Mobile: el CU07 es solo web.
- Campo de categoría de licencia, cuando exista su catálogo.

## Qué NO incluyen estas UT

- Cambios en el shell: `/escuela` ya está montado y protegido por rol.
- Pantallas de estudiantes, docentes o asignaciones (CU08 a CU11).
