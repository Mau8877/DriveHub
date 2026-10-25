# UT-CU25-BE-04 — Servicio y reglas de los planes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-BE-04` |
| **Nombre** | Servicio y reglas de los planes |
| **Caso de uso** | CU25-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU25-BE-01, UT-CU25-BE-02, UT-CU25-BE-03 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (el service es la única capa con lógica, transacciones y logs), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`, sin interfaz `IXxx`), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El Administrador crea, corrige, desactiva y reactiva los planes de su escuela, ve el costo también en dólares, y ninguna escuela toca los planes de otra.

## Objetivo

`PlanPagoService` implementa todos los flujos del caso de uso con sus reglas, y devuelve el plan con su costo en dólares calculado.

## Alcance

- `features/planes/dto/` (todos `record`):
  - `CrearPlanPagoRequest(nombre, horas, costoBs)`: `@NotBlank` y `@Size(max = 80)` en el nombre; `@NotNull`, `@Min(1)` y `@Max(500)` en las horas; `@NotNull`, `@DecimalMin("0.01")`, `@DecimalMax("99999999.99")` y `@Digits(integer = 8, fraction = 2)` en el costo. Mensajes en español.
  - `EditarPlanPagoRequest(nombre, horas, costoBs)`: las mismas reglas, pero cada campo es opcional (nulo = no tocar).
  - `PlanPagoResponse(codigo, nombre, horas, costoBs, costoUsd, activo, creadoEn)`.
- `features/planes/service/ConversionMoneda` (`@Component`, recibe `PagosProperties`): `BigDecimal aDolares(BigDecimal bolivianos)` = `bolivianos ÷ tipoCambioBsUsd`, escala 2 y `RoundingMode.HALF_UP`.
- `features/planes/service/PlanPagoService` (clase concreta, `@RequiredArgsConstructor`), todos los métodos con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y la escuela como parámetro (la del token):
  - `crear(escuelaCodigo, request)` (`@Transactional`): recorta el nombre; si `existsByEscuelaCodigoAndNombreIgnoreCase` → `ConflictException` `ERR_PAG_01`; guarda con `saveAndFlush` y, si aun así la base rechaza por el índice único (carrera entre dos altas), traduce esa violación al mismo `ERR_PAG_01`. El plan queda activo.
  - `listar(escuelaCodigo, page, pageSize, search, activo)` (`@Transactional(readOnly = true)`): `page` por defecto 1, `pageSize` por defecto 20 y máximo 100; la búsqueda se recorta y en blanco = sin búsqueda; orden `createdAt DESC, id DESC`; devuelve la página de `PlanPagoResponse` y el `PageMeta`.
  - `obtener(escuelaCodigo, codigo)`: `NotFoundException` `ERR_PAG_02` si no existe o es de otra escuela (no se distinguen).
  - `editar(escuelaCodigo, codigo, request)` (`@Transactional`): `ERR_PAG_02` si no existe o es de otra; un cuerpo sin ningún campo → `ValidationException` `ERR_SYS_01`; si cambia el nombre, comprueba que ningún **otro** plan de la escuela lo tenga (`ERR_PAG_01`, también con la violación del índice); cambia solo los campos enviados y **no** toca `activo`.
  - `desactivar(escuelaCodigo, codigo)` y `activar(escuelaCodigo, codigo)` (`@Transactional`): `ERR_PAG_02` si no existe o es de otra; `ConflictException` `ERR_PAG_03` si ya está en ese estado.
  - Cada operación que cambia datos registra `INFO` (`module = planes`, `planCodigo`, `escuelaCodigo`); nunca el nombre ni el costo, y el resto de las lecturas no se registran.
- El mapeo `PlanPago → PlanPagoResponse` va inline en el service (es 1:1; solo `costoUsd` sale de `ConversionMoneda`).
- Tests de service con el repositorio mockeado (`PlanPagoServiceTest`): crear (nombre recortado, plan activo, `ERR_PAG_01` con otra capitalización, `ERR_PAG_01` si la base rechaza, mismo nombre en otra escuela se acepta porque el `exists` se pregunta con la escuela); listar (valores por defecto, tope de 100, búsqueda en blanco); obtener y editar de otra escuela → `ERR_PAG_02`; editar solo horas deja el resto igual y no cambia `activo`; editar con cuerpo vacío → `ERR_SYS_01`; renombrar a su propio nombre no choca; desactivar y activar y sus `ERR_PAG_03`; el log no contiene el nombre ni el costo. `ConversionMonedaTest`: 1177 Bs → 100,00 USD con 11,77; 100 Bs → 8,50; redondeo hacia arriba en el medio centavo; con otro tipo de cambio cambia el resultado; costo con dos decimales. Un `PlanPagoServiceAutorizacionTest` (`@SpringBootTest` o `@WithMockUser`) comprueba que sin el rol responde `AccessDenied` (el handler lo traduce a 403).

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/dto/CrearPlanPagoRequest.java`, `EditarPlanPagoRequest.java` y `PlanPagoResponse.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/service/PlanPagoService.java` y `ConversionMoneda.java`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/features/planes/PlanPagoServiceTest.java`, `ConversionMonedaTest.java` y `PlanPagoServiceAutorizacionTest.java`

## Fuera de alcance (no tocar)

- El controller y la documentación de los errores (UT 05).
- Actualizar inscripciones que ya contrataron el plan: no hay ninguna aún, y cada una guardará su propio monto.
- Guardar el costo en dólares o llamar a un servicio de cambio.
- `inscripcion_plan`, cuotas y pagos.

## Resultado esperado

Con el repositorio real, el service cubre cada flujo y cada excepción del caso de uso; solo el Administrador de la escuela dueña puede tocar un plan.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con un solo plan, desactivarlo dos veces: la primera lo deja `activo = false` y la segunda responde `ERR_PAG_03`.
