# UT-CU18-BE-05 — `PruebaClaseService`: evaluar una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-05` |
| **Nombre** | `PruebaClaseService`: evaluar una clase |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU18-BE-01`, `UT-CU18-BE-03`, `UT-CU18-BE-04` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el service es la única capa que loguea y decide el rol; un módulo habla con otro por el service del dueño), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio) (`@PreAuthorize` en el service), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca el texto de las observaciones), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md). Tomar como modelo `ClaseService.cerrar` (UT-CU17-BE-06) y `ExamenConduccionService.registrarResultado`. Decisiones 4, 6, 8 y 9 del [README](./README.md).

## Solicitud de negocio

Después de una clase el instructor tiene que poder evaluar al estudiante y que esa nota llegue a su historial, sin que un reintento o dos peticiones a la vez dejen datos duplicados.

## Objetivo

`PruebaClaseService.evaluar` guarda la prueba con su total y su resultado y escribe su nota en `resultados`, y completa la nota si un intento anterior se cortó a medias.

## Alcance

- `CierreDeReservaService` (se edita, módulo `reservas`): `boolean reservaCompletada(Long reservaId)`, verdadero si la reserva existe, no está borrada y su estado es `COMPLETADA`. Sin `@PreAuthorize` (lo llama otro service). Es la única forma en que `clases` pregunta por la reserva.
- `PruebaClaseService` (nueva clase, `features/clases/service/`, `@Service`, `@RequiredArgsConstructor`; usa `ClaseRepository`, `PruebaClaseRepository`, `ResultadoRepository`, `CierreDeReservaService`, `ReglasDePrueba` y el `Clock`):
  - `@PreAuthorize("hasRole('INSTRUCTOR')") PruebaClaseResponse evaluar(UUID instructorCodigo, UUID escuelaCodigo, EvaluarClaseRequest request)`:
    0. `escuelaCodigo` nulo → `ForbiddenException` `ERR_ACA_16`.
    1. Busca la clase con `findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull`; si no está → `NotFoundException` `ERR_ACA_35`.
    2. La clase tiene que tener asistencia `PRESENTE` o `TARDE` y `reservaCompletada(clase.getReservaId())` verdadero; si no → `ConflictException` `ERR_ACA_39`.
    3. `ReglasDePrueba.validarYArmarCriterios` (`ERR_ACA_41`), el total y el resultado. Esto va **antes** de buscar la prueba existente, para que un cuerpo inválido nunca se confunda con un reintento.
    4. Busca la prueba de esa clase y tipo (`findByClaseIdAndTipoAndDeletedAtIsNull`):
       - **existe y su nota existe** (`existsByOrigenAndOrigenId`) → `ConflictException` `ERR_ACA_40`;
       - **existe sin nota** (un intento anterior se cortó) → escribe la nota con los datos **de la prueba guardada** y devuelve esa prueba (los datos del reintento no la cambian);
       - **no existe** → crea la `PruebaClase` (`claseId`, estudiante e instructor de la clase, tipo, criterios, total, resultado, observaciones limpias, `fecha` de la clase) y la guarda; un `DuplicateKeyException` (otra petición la creó a la vez) → `ConflictException` `ERR_ACA_40`.
    5. Escribe la nota: `Resultado` con `escuelaCodigo` del token, `estudianteCodigo`, `origen = PRUEBA_CLASE`, `origenId` = `_id` de la prueba, `titulo = "Prueba de clase: " + etiqueta`, `puntaje` = total, `aprobado`, `fecha = clock.instant()`.
    6. Devuelve `PruebaClaseResponse`.
  - Logs: `INFO` «Prueba de clase registrada» con `pruebaCodigo`, `claseCodigo`, `instructorCodigo`, `tipo` y `resultado`; `INFO` «Nota de prueba de clase completada» cuando se escribe la nota de una prueba que ya existía. Nunca las observaciones ni los puntajes de cada criterio.
  - No llama a Pagos ni a Identidad, no toca la clase, la reserva ni la franja.
- Tests de servicio (repositorios, `CierreDeReservaService` y `Clock` simulados): evaluación válida → guarda la prueba, la nota con el título, el puntaje y `aprobado` correctos, y devuelve la respuesta; con total bajo 70 → `REPROBADO` y `aprobado = false`; observaciones en blanco → `null`; clase inexistente o de otro instructor → `ERR_ACA_35` sin guardar nada; clase `AUSENTE` → `ERR_ACA_39`; `reservaCompletada` falso → `ERR_ACA_39`; criterios inválidos → `ERR_ACA_41` sin guardar nada y sin buscar la prueba; prueba ya existente con nota → `ERR_ACA_40` sin guardar nada; **prueba existente sin nota → escribe solo la nota, no crea otra prueba y devuelve la guardada con sus datos**; `DuplicateKeyException` → `ERR_ACA_40`; otro tipo en la misma clase → se guarda; escuela nula → `ERR_ACA_16`; los logs nunca contienen las observaciones (con el `ListAppender` de Logback, como `ClaseServiceTest`); `@PreAuthorize("hasRole('INSTRUCTOR')")` en `evaluar` (por reflexión).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/PruebaClaseService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/CierreDeReservaService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/PruebaClaseServiceTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/CierreDeReservaServiceTest.java` (se amplía)

## Fuera de alcance (no tocar)

- Consultar pruebas y el catálogo (UT 06), la agenda (UT 07), el controller y la ruta (UT 08).
- Editar o borrar una prueba, y un job que complete las notas pendientes.
- Publicar eventos (`examen.calificado` u otro) y notificar al estudiante.
- Cerrar la clase (CU17) y cualquier cambio de estado de la reserva.

## Resultado esperado

El instructor evalúa una clase suya cerrada: queda la prueba y su nota en el historial; repetir el envío o competir con otra petición no duplica nada, y una nota que faltó se completa sola al reintentar.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. Ningún log contiene el texto de las observaciones.
