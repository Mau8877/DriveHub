# Unidades de trabajo — CU25-FE

**Caso de uso:** Gestionar planes de horas (Administrador de escuela): pantalla "Planes de horas" con listado (nombre, horas, costo en Bs y en USD, estado), búsqueda, filtro de estado, creación, edición, desactivar y reactivar.
**Micro-frontend(s):** `pagos` (web, Angular) y `shell` (menú), más el paquete compartido `@drivehub/gestion` y el `docker-compose.yml` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU25 - Gestionar planes de horas`](../../../CasosDeUso/CU25%20-%20Gestionar%20planes%20de%20horas.md)
**Recurso HTTP / contrato:** el de [CU25-BE](../UT-BACKEND/README.md#contrato-de-datos--recurso-http) (`/api/v1/planes-pago`, vía el gateway); pantalla `/pagos/planes`

## Antes de implementar cualquier UT

**Requiere el backend (CU25-BE-05)** para probar la pantalla contra la API real; el esquema y los componentes se pueden hacer antes. Leer [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (en especial "Nada de componentes nativos"), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) y [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Se reutiliza `<dh-gestion>`**: la pantalla es un esquema, no una tabla hecha a mano. Lo que el esquema todavía no sabe expresar se **amplía en el paquete para todos** (regla de GESTION_FRONTEND.md).
2. **Faltan dos cosas en `@drivehub/gestion`** (UT 01): un campo **decimal** (el tipo `numero` solo admite enteros, y el costo lleva centavos) y una columna **moneda** que dibuje `Bs 1.250,00` y `US$ 179,60` sin depender del idioma del navegador.
3. **Las horas** son un campo `numero` (entero de 1 a 500) con `min` y `max`; ya existe y no hace falta ampliarlo.
4. **El dólar solo se muestra**: es una columna de la tabla (`costoUsd`, calculado por el backend) y **no** un campo del formulario. El formulario lo explica con una ayuda bajo el costo ("El equivalente en dólares se calcula solo").
5. **Una sola ruta** `/pagos/planes`: `<dh-gestion>` maneja por dentro el listado y el drawer de crear y editar. El `Inicio` del remoto (`/pagos`) se queda como está.
6. **Sin pantalla de detalle:** el caso de uso no la pide; el formulario de edición ya muestra todos los datos del plan.
7. **El remoto `pagos` no tenía `@drivehub/gestion`** como dependencia (solo estaba en `skip` de la federación) **y el contenedor `frontend` no lo levantaba**: solo corre `shell`, `identidad`, `escuela`, `contenido` y `examenes`. Sin eso, `/pagos` no carga dentro del shell. Las UT 02 y 03 lo arreglan.
8. **Menú:** "Planes de horas" reemplaza a "Plan de pagos y cuotas" dentro de "Gestionar pagos" (mismo icono `receipt`), con la ruta `/pagos/planes`. La opción queda solo para `ADMIN_ESCUELA`.
9. **Errores de campo:** `ERR_PAG_01` → nombre. El resto, aviso general.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU25-FE-01](./UT-CU25-FE-01.md) | Campo decimal y columna moneda en `<dh-gestion>` | — |
| [UT-CU25-FE-02](./UT-CU25-FE-02.md) | Pantalla de planes de horas (esquema y ruta) en `pagos` | UT-CU25-FE-01 |
| [UT-CU25-FE-03](./UT-CU25-FE-03.md) | Menú del shell y `pagos` en el contenedor `frontend` | UT-CU25-FE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Tabla con nombre, horas, Bs y USD; búsqueda; filtro de estado | 01, 02 |
| F1, A1, CA02, CA04 | Crear con validación en el cliente | 01, 02 |
| A2, CA03 | Error de nombre repetido bajo el campo | 02 |
| F2, CA06 | Editar | 02 |
| F3, F4, CA07 | Desactivar con confirmación y reactivar | 02 |
| CA05 | El dólar se muestra, no se escribe | 01, 02 |
| CA11 | Opción de menú solo para el Administrador, dentro de "Gestionar pagos" | 03 |

Los criterios CA08, CA09 y CA10 son del backend ([CU25-BE](../UT-BACKEND/README.md#cobertura-del-caso-de-uso)).

## Follow-ups (no se implementan en estas UT)

- Mostrar el tipo de cambio usado, o poder cambiarlo desde la pantalla.
- Columna con el número de estudiantes que contrataron cada plan (cuando exista la contratación, CU26 y siguientes).
- Ordenar el listado por horas o por costo.

## Qué NO incluyen estas UT

- Cambios en el backend (CU25-BE) ni en el gateway.
- La contratación y el pago de un plan por el estudiante, ni su pantalla móvil.
- Las pantallas de cuotas.
- La app móvil.

## Commits sugeridos

Un commit por proyecto: `gestion` (UT 01), `pagos` (UT 02), `shell` (UT 03) e `INFRA` para `docker-compose.yml` (UT 03); los documentos de esta carpeta, aparte, con `[DOCS]`.
