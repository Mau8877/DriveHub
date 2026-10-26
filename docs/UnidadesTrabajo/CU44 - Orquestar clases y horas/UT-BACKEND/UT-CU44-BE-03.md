# UT-CU44-BE-03 — Pagos: endpoints internos de la billetera

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-03` |
| **Nombre** | Pagos: endpoints internos de la billetera |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `pagos` |
| **Depende de** | `UT-CU44-BE-01`, `UT-CU44-BE-02` |

## Antes de implementar

Las decisiones 3, 4 y 5 y la tabla de contrato del [README](./README.md#contrato-de-datos--recurso-http). [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Tomar de modelo `BilleteraController` (las rutas públicas).

## Solicitud de negocio

El job de Académico necesita dos rutas para descontar y devolver horas sin sesión, que solo él pueda llamar.

## Objetivo

Existen `POST /internal/v1/billeteras-horas/consumos` y `.../devoluciones`, protegidas por la credencial de servicio, que delegan en `consumirDeSistema` y `devolverDeSistema`.

## Alcance

- `BilleteraInternaController` en `features/billetera/controller/`, `@RequestMapping("/internal/v1/billeteras-horas")`:
  - `POST /consumos`: header `Idempotency-Key`, cuerpo `ConsumoInternoRequest { escuelaCodigo, estudianteCodigo, horas, instructorCodigo, claseCodigo }` validado con `@Valid`. Responde `201` con `ConsumoResultado` y el mensaje «Horas descontadas correctamente».
  - `POST /devoluciones`: header `Idempotency-Key`, cuerpo `DevolucionInternaRequest { escuelaCodigo, estudianteCodigo, claseCodigo }`. Responde `200` con `DevolucionResultado` y el mensaje «Horas devueltas correctamente».
  - Controller delgado: valida, llama **un** método del service, envuelve en `ApiResponse`. Sin lógica ni logging.
  - Sin `@CurrentUserId` ni `@CurrentEscuelaId`: no hay token. La escuela y el estudiante vienen del cuerpo.
- DTOs `ConsumoInternoRequest` y `DevolucionInternaRequest` como `record` con `@NotNull`, y `horas` con `@Min(1) @Max(500)`, en `features/billetera/dto/`.
- Tests `@WebMvcTest` del controller con el service simulado y la credencial correcta: `201` y `200` con la forma del sobre; cuerpo incompleto → `400 ERR_SYS_01`; `409 ERR_PAG_09` y `404 ERR_PAG_16` llegan con su código; sin la credencial → `401 ERR_PAG_19`; **con un JWT válido pero sin credencial → `401`** (el JWT no sirve acá).
- Test de integración (patrón `assumeTrue` de Testcontainers) que consume y devuelve contra PostgreSQL real a través del endpoint, con la misma clave dos veces, y comprueba el saldo.

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/controller/BilleteraInternaController.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/ConsumoInternoRequest.java`, `DevolucionInternaRequest.java`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/controller/` (tests)

## Fuera de alcance (no tocar)

- El filtro de credencial y el cambio de `SecurityConfig` (UT 01); el service (UT 02): esta UT solo los usa.
- `BilleteraController` y las rutas `/api/v1/billeteras-horas`: no cambian.
- Registrar la ruta en el gateway: **no se hace** (UT 04 lo verifica).
- Un endpoint de saldo o de consulta internos: el job no los necesita.

## Resultado esperado

Con la credencial correcta, Pagos descuenta y devuelve horas por las rutas internas igual que lo hace por las públicas; sin ella, ninguna petición pasa.

## Validación

1. `./mvnw test` en `pagos` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Manual: `curl` a `http://localhost:8083/internal/v1/billeteras-horas/consumos` sin header → `401 ERR_PAG_19`; con `X-Service-Key` correcta y un cuerpo válido de un estudiante con saldo → `201` y el saldo baja; repetir con la misma `Idempotency-Key` → `201` y el saldo no vuelve a bajar.
