# UT-CU12-BE-07 — `PlantillaHorarioService`: crear y listar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-07` |
| **Nombre** | `PlantillaHorarioService`: crear y listar (con `FranjaGeneracionService.generar`) |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-02`, `UT-CU12-BE-03`, `UT-CU12-BE-04`, `UT-CU12-BE-05`, `UT-CU12-BE-06` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el service es la única capa que loguea y lanza excepciones catalogadas), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio) (`@PreAuthorize` en el service), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer las [decisiones de diseño 3 y 4](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El instructor define cuándo atiende y el sistema le arma las clases reservables; también necesita ver los horarios que tiene.

## Objetivo

`PlantillaHorarioService` crea un horario válido con sus franjas y lista los horarios del instructor.

## Alcance

- `features/horarios/service/FranjaGeneracionService` (clase concreta, `@RequiredArgsConstructor`; usa `GeneradorFranjas`, `FranjaRepository`, `Clock` y `HorariosProperties`), con **`int generar(PlantillaHorario horario)`**: calcula las franjas del horario con `GeneradorFranjas` (hora actual del `Clock`, `horizonteDias` de la configuración), las guarda una por una con `insertarSiNoExiste` y devuelve cuántas realmente se insertaron. Sin `@PreAuthorize` (también la usa el sistema, UT 10). La UT 10 le suma el método de generación diaria.
- `features/horarios/service/PlantillaHorarioService` (clase concreta, `@RequiredArgsConstructor`; usa `PlantillaHorarioRepository`, `FranjaGeneracionService`, `Clock`). Ambos métodos con `@PreAuthorize("hasRole('INSTRUCTOR')")`.
- **`crear(UUID instructorCodigo, UUID escuelaCodigo, CrearPlantillaHorarioRequest request)` → `PlantillaHorarioCreadaResponse`**, sin `@Transactional` (decisión 4):
  1. `horaFin` no posterior a `horaInicio` → `ValidationException` `ERR_ACA_01`.
  2. `duracionClaseMin` fuera de 15–240, o `(horaFin − horaInicio)` no múltiplo exacto de ella → `ValidationException` `ERR_ACA_02`.
  3. `vigenteDesde` nulo = hoy (con el `Clock`); `vigenteDesde` anterior a hoy, o `vigenteHasta` anterior a `vigenteDesde` → `ValidationException` `ERR_ACA_03`.
  4. Cruce: con las activas del instructor del mismo `diaSemana`, hay cruce si las ventanas de hora se solapan (`inicio < otroFin && otroInicio < fin`; compartir el borde no cuenta) **y** las vigencias se solapan (`desde <= otroHasta` y `otroDesde <= hasta`, con `hasta` nulo = sin fin) → `ConflictException` `ERR_ACA_04`.
  5. Guarda la `PlantillaHorario` (con `escuelaCodigo`).
  6. Genera las franjas con `FranjaGeneracionService.generar(...)`, que devuelve cuántas realmente se insertaron.
  7. Log `INFO` (`module="horarios"`, `instructorCodigo`, `plantillaCodigo`, `franjasGeneradas`).
  8. Devuelve el horario con `franjasGeneradas`.
- **`listar(UUID instructorCodigo, Boolean activa, int page, int pageSize)` → página de `PlantillaHorarioResponse`** + total, ordenada por `diaSemana` y `horaInicio`. Aplica el tope de `pageSize` del proyecto (ver ENDPOINTS_BACKEND.md; si no hay uno definido, 100). Sin log (es una lectura).
- Tests unitarios de `FranjaGeneracionService` (repositorio y `Clock` fijos): guarda las franjas que calcula `GeneradorFranjas` (con el reloj fijo del test, los mismos bloques que devuelve el cálculo, ni uno más); devuelve solo las realmente insertadas (si un `insertarSiNoExiste` devuelve `false`, no la cuenta).
- Tests unitarios de `PlantillaHorarioService` (repositorios y `FranjaGeneracionService` mockeados, `Clock` fijo): crea y devuelve `franjasGeneradas` con lo que informa el generador; cada error (`ERR_ACA_01` a `04`) no guarda nada; cruce con borde compartido **sí** crea; cruce de horas pero vigencias disjuntas **sí** crea; ignora los horarios inactivos en el cruce; `vigenteDesde` nulo toma hoy; el listado pasa el filtro `activa` y el orden; ningún log contiene datos sensibles.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/FranjaGeneracionService.java`, `PlantillaHorarioService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/FranjaGeneracionServiceTest.java`, `PlantillaHorarioServiceTest.java`

## Fuera de alcance (no tocar)

- Desactivar (UT 08), las franjas (UT 09) y la generación diaria (UT 10).
- El controller y los endpoints (UT 11).
- Editar un horario: no existe (decisión 1).
- Validar que el instructor esté activo en Escuela (CU09).
- Asociar vehículo.

## Resultado esperado

Crear un horario válido deja el horario y sus franjas libres guardados; uno inválido o que se cruza lanza el error catalogado sin guardar nada; el listado devuelve solo lo del instructor.

## Validación

1. `./mvnw test` en `academico` pasa con `PlantillaHorarioServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
