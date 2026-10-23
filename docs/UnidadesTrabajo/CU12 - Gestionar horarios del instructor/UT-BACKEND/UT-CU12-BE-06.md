# UT-CU12-BE-06 — DTOs de horarios y franjas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-06` |
| **Nombre** | DTOs de horarios y franjas |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#nombres-de-clases) (DTOs como `record`, nunca un Model expuesto), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Bean Validation en DTOs). Contrato exacto en el [README](./README.md#contrato-de-datos--recurso-http).

## Solicitud de negocio

El cliente móvil necesita un contrato claro de lo que envía para crear un horario y de lo que recibe de horarios y franjas, sin ver identificadores internos.

## Objetivo

Existen los `record` de request y response del CU12, con las validaciones de formato en el request.

## Alcance

- `features/horarios/dto/CrearPlantillaHorarioRequest(int diaSemana, String horaInicio, String horaFin, Integer duracionClaseMin, LocalDate vigenteDesde, LocalDate vigenteHasta)`:
  - `diaSemana`: `@NotNull`, `@Min(1)`, `@Max(7)` (usar `Integer`).
  - `horaInicio`, `horaFin`: `@NotBlank` y `@Pattern` de `HH:mm` en 24 horas (`^([01]\d|2[0-3]):[0-5]\d$`).
  - `duracionClaseMin`: `@NotNull` (el rango 15–240 es de negocio, `ERR_ACA_02`, lo valida el servicio).
  - `vigenteDesde`, `vigenteHasta`: opcionales.
  - Mensajes de validación en español.
- `features/horarios/dto/PlantillaHorarioResponse(UUID codigo, int diaSemana, String horaInicio, String horaFin, int duracionClaseMin, LocalDate vigenteDesde, LocalDate vigenteHasta, boolean activa)`.
- `features/horarios/dto/PlantillaHorarioCreadaResponse`: los mismos campos más `int franjasGeneradas`.
- `features/horarios/dto/FranjaResponse(UUID codigo, LocalDate fecha, String horaInicio, String horaFin, EstadoFranja estado)`.
- Mapeo Model → response inline en el service (es 1:1, no hace falta `Mapper`, ver ARQUITECTURA_BACKEND.md), por lo que acá no hay mapper.
- Tests unitarios del request con un `Validator`: válido; `diaSemana` 0 y 8; hora `24:00`, `9:00`, `08:60` y vacía; duración nula; vigencias opcionales.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/dto/CrearPlantillaHorarioRequest.java`, `PlantillaHorarioResponse.java`, `PlantillaHorarioCreadaResponse.java`, `FranjaResponse.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/dto/CrearPlantillaHorarioRequestTest.java`

## Fuera de alcance (no tocar)

- Las reglas de negocio (hora fin posterior, duración, vigencia): son del servicio (UT 07).
- Exponer `vehiculoCodigo`, `escuelaCodigo` ni ningún id interno.
- DTOs de otros módulos de Académico.

## Resultado esperado

El contrato HTTP del CU12 está definido en `record` y el request rechaza el formato inválido antes de llegar al servicio.

## Validación

1. `./mvnw test` en `academico` pasa con `CrearPlantillaHorarioRequestTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
