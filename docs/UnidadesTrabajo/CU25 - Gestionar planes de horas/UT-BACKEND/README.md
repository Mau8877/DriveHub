# Unidades de trabajo — CU25-BE

**Caso de uso:** Gestionar planes de horas (Administrador de escuela): crear, listar, editar, desactivar y reactivar los planes de horas de su escuela (nombre, horas y costo en bolivianos, con su equivalente en dólares como referencia), aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `pagos` (Spring Boot, capas, PostgreSQL) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU25 - Gestionar planes de horas`](../../../CasosDeUso/CU25%20-%20Gestionar%20planes%20de%20horas.md)
**Recurso HTTP / contrato:** `/api/v1/planes-pago` (ya enrutado por el gateway a `pagos`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (PostgreSQL y Flyway), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El catálogo de planes vive en Pagos**, no en Escuela: es dinero, y `inscripcion_plan` apunta a `plan_pago` con una FK física que solo es posible dentro de la misma base. La tabla `plan_pago` ya existe en el diseño (CU25) y se **reutiliza**: se ajusta, no se crea otra.
2. **`plan_pago` cambia así** (la tabla está vacía en todos los entornos, ningún caso de uso había escrito planes): se quitan `categoria_sigla`, `nro_clases` y `nro_cuotas` (las cuotas son de la contratación del plan, no de un plan de horas); `monto_total` pasa a llamarse `costo_bs`; se suman `horas` y `escuela_codigo`. Queda: `nombre`, `horas`, `costo_bs`, `activo`, `escuela_codigo` y las columnas de `BaseModel`.
3. **Multitenant con una clase abstracta.** `common/model/BaseEscuelaModel extends BaseModel` (`@MappedSuperclass`) trae `escuelaCodigo` (`escuela_codigo UUID NOT NULL`, sin FK: es el `codigo_escuela` de Escuela). Todo Model de Pagos que sea de una escuela la extiende; `PlanPago` es el primero. El filtro por escuela es explícito en cada consulta del repositorio, con la escuela del claim `escuela` del token (no hay filtro automático).
4. **Restricciones en la base, no solo en el servicio:** `CHECK (horas BETWEEN 1 AND 500)`, `CHECK (costo_bs > 0)` y un índice único `uq_plan_pago_escuela_nombre` sobre `(escuela_codigo, lower(nombre))` donde `deleted_at IS NULL`. El servicio comprueba el nombre antes de guardar (para dar un mensaje claro) y la base cierra la carrera entre dos altas simultáneas: esa violación se traduce al mismo `ERR_PAG_01`.
5. **El dólar no se guarda.** `costoUsd` se calcula al responder: `costo_bs ÷ tipo de cambio`, `RoundingMode.HALF_UP`, dos decimales. El tipo de cambio es una configuración de Pagos (`app.pagos.tipo-cambio-bs-usd`, variable de entorno `TIPO_CAMBIO_BS_USD`, hoy 11,77, sin valor por defecto en el código) y el servicio **no arranca** si no es mayor a 0. Sin servicio externo.
6. **Desactivar no es borrar:** el plan pasa a `activo = false` y sigue en el listado. `deleted_at` queda sin usar en este caso de uso, pero `BaseModel` lo filtra igual.
7. **`PATCH` parcial:** lo no enviado no se toca; un cuerpo sin ningún campo responde 400 `ERR_SYS_01`. Cambiar horas o costo no toca `inscripcion_plan.monto_total`: cada contratación guarda su propio monto.
8. **La escuela y el rol salen del token.** `@CurrentEscuelaId` (un resolver por servicio, como en Académico) entrega el claim `escuela`; sin él, 403 `ERR_PAG_04`. El rol se decide con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` en el service.
9. **El `GlobalExceptionHandler` de Pagos aprende a responder 400** ante un cuerpo ilegible, un valor de enumeración o decimal desconocido o un parámetro con formato inválido: hoy caen en el 500 (Académico ya lo hizo en CU19).

## Contrato de datos / recurso HTTP

Todos exigen `ADMIN_ESCUELA` (`401` sin sesión, `403` sin el rol).

| Método | Path | Cuerpo / query | Status | `data` |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/planes-pago` | `{ nombre, horas, costoBs }` | 201 | `PlanPagoResponse` |
| `GET` | `/api/v1/planes-pago` | `page`, `pageSize`, `search`, `filter[activo]=true\|false` | 200 | `PlanPagoResponse[]` + `meta` |
| `GET` | `/api/v1/planes-pago/{codigo}` | — | 200 | `PlanPagoResponse` |
| `PATCH` | `/api/v1/planes-pago/{codigo}` | `{ nombre?, horas?, costoBs? }` (al menos uno) | 200 | `PlanPagoResponse` |
| `POST` | `/api/v1/planes-pago/{codigo}/desactivar` | — | 200 | `PlanPagoResponse` |
| `POST` | `/api/v1/planes-pago/{codigo}/activar` | — | 200 | `PlanPagoResponse` |

`PlanPagoResponse`: `codigo`, `nombre`, `horas`, `costoBs`, `costoUsd`, `activo`, `creadoEn`. El id interno Long y `escuelaCodigo` nunca salen.

Validación de entrada: `nombre` 1 a 80 caracteres (se recorta); `horas` entero de 1 a 500; `costoBs` mayor a 0, hasta 2 decimales y hasta 99 999 999,99. Errores de negocio: `409 ERR_PAG_01` nombre en uso, `404 ERR_PAG_02` plan inexistente o de otra escuela, `409 ERR_PAG_03` ya está en ese estado, `403 ERR_PAG_04` token sin escuela, `400 ERR_SYS_01` datos inválidos. Primeros códigos de Pagos: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU25-BE-01](./UT-CU25-BE-01.md) | Migración y modelo por escuela | — |
| [UT-CU25-BE-02](./UT-CU25-BE-02.md) | Errores, escuela del token y tipo de cambio | — |
| [UT-CU25-BE-03](./UT-CU25-BE-03.md) | Repositorio y listado de planes | 01 |
| [UT-CU25-BE-04](./UT-CU25-BE-04.md) | Servicio y reglas de los planes | 01, 02, 03 |
| [UT-CU25-BE-05](./UT-CU25-BE-05.md) | Controller, errores documentados y seed | 04 |

**Cadena:** `01, 02 → 03 → 04 → 05` (la 02 no depende de la 01 y pueden hacerse en paralelo).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listar con búsqueda, filtro de estado, paginación, costo en Bs y en USD | 03, 04, 05 |
| F1, CA02, CA03 | Crear; nombre único por escuela | 01, 03, 04, 05 |
| A1, CA04 | Datos inválidos | 01, 02, 04, 05 |
| CA05 | Dólar calculado, no guardado | 02, 04 |
| F2, CA06 | Editar sin tocar el estado ni lo contratado | 04, 05 |
| F3, F4, CA07, CA08 | Desactivar y reactivar; estado repetido | 04, 05 |
| A3, CA09 | Plan de otra escuela o inexistente | 03, 04, 05 |
| A5, A6, CA10 | Solo Administrador; token sin escuela | 02, 04, 05 |
| A7 | Rate limit | ya cubierto por el gateway; sin UT nueva |

CA11 es de frontend.

## Follow-ups (no se implementan en estas UT)

- Que la contratación del estudiante (CU26 y siguientes, por documentar) lea los planes activos y guarde el monto en `inscripcion_plan`.
- Búsqueda del nombre sin distinguir tildes (hoy distingue tildes, no mayúsculas).
- Tipo de cambio en vivo desde una fuente externa, y una pantalla para cambiarlo.
- Extender `BaseEscuelaModel` a las demás tablas de Pagos que sean de una escuela (inscripciones, pagos) cuando tengan su caso de uso.

## Qué NO incluyen estas UT

- Cuotas, contratación y pago de un plan (CU26 y siguientes, por documentar).
- Categoría de licencia, descuentos o vigencia de un plan.
- Cambios en el gateway: `planes-pago` ya está enrutado a `pagos`.
- El frontend ([UT-FRONTEND](../UT-FRONTEND/README.md)).

## Commits sugeridos

Un commit `[BACK]` para `pagos` (UT 01 a 05). `docs/` es `[DOCS]` y `.claude/` es `[REPO]`.
