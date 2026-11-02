# UT-CU17-BE-06 — `ClaseService`: cerrar una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-06` |
| **Nombre** | `ClaseService`: cerrar una clase |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-02`, `UT-CU17-BE-04`, `UT-CU17-BE-05` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el service es la única capa que loguea y decide el rol), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio) (`@PreAuthorize` en el service), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca el texto de las observaciones), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md). Tomar como modelo `AgendaService.cancelar` (UT-CU16-BE-07) y cómo resuelve el nombre del estudiante. Decisiones 3, 4, 10 y 13 del [README](./README.md).

## Solicitud de negocio

El instructor que terminó una clase tiene que poder dejarla registrada: si el estudiante vino, qué se vio y los kilómetros, y que la reserva quede cerrada.

## Objetivo

`ClaseService.cerrar` guarda la clase dictada y deja la reserva `COMPLETADA` o `NO_ASISTIO`, sin duplicar la clase si se repite y sin dejar una clase huérfana si otra petición ganó.

## Alcance

- `ClaseService` (nueva clase, `features/clases/service/`, `@Service`, `@RequiredArgsConstructor`; usa `ClaseRepository`, `CierreDeReservaService`, `IdentidadClient`, `ReglasDeClase` y el `Clock`):
  - `@PreAuthorize("hasRole('INSTRUCTOR')") ClaseResponse cerrar(UUID instructorCodigo, UUID escuelaCodigo, String bearer, CerrarClaseRequest request)`:
    0. `escuelaCodigo` nulo → `ForbiddenException` `ERR_ACA_16` (igual que `AgendaService`).
    1. `ReglasDeClase.validarCierre(...)` (`ERR_ACA_38`), antes de tocar la base.
    2. `cierre.buscarCerrable(instructorCodigo, request.reservaCodigo())` (`ERR_ACA_35` o `ERR_ACA_37`, UT 05).
    3. Busca la clase de esa reserva (`findByReservaIdAndDeletedAtIsNull`). Si **existe** (cierre interrumpido, S1) la reutiliza y la actualiza con lo nuevo; si no, crea una `Clase` con `reservaId`, `estudianteCodigo`, `instructorCodigo`, `fecha` (los de la reserva), `estado = CERRADA`, la asistencia y los datos limpios (`ReglasDeClase.limpiar`). `vehiculoCodigo`, `inicioReal` y `finReal` quedan vacíos. La guarda. Un `DuplicateKeyException` (otra petición creó la clase de esa reserva a la vez) → `ConflictException` `ERR_ACA_37`.
    4. `cierre.marcarCerrada(...)` (UT 05). Si devuelve **vacío**: si la clase la creó esta petición, la **borra físicamente** (`ClaseRepository.delete`); en cualquier caso → `ConflictException` `ERR_ACA_37`.
    5. Arma la `ClaseResponse`. El nombre del estudiante sale de `IdentidadClient.nombresDeEstudiantes` con el Bearer del instructor; si Identidad falla o no lo devuelve → `Estudiante`, **sin error** (el cierre ya se hizo).
  - Logs: `INFO` «Clase cerrada» con `claseCodigo`, `reservaCodigo`, `instructorCodigo` y `asistencia`; `INFO` «Cierre de clase retomado» si se reutilizó una clase; `WARN` si hay que borrar la clase por conflicto. Nunca `tema` ni `observaciones`.
  - No llama a Pagos y no toca la franja.
- Tests de servicio (repositorios, `CierreDeReservaService`, `IdentidadClient` y `Clock` simulados): cierre con `PRESENTE` y con `TARDE` → guarda la clase con los datos y pide `COMPLETADA`; con `AUSENTE` → pide `NO_ASISTIO` y guarda solo observaciones; solo con asistencia (sin tema, observaciones ni km) → cierra; los blancos se guardan `null`; **cierre repetido**: ya existía la clase → no crea otra, la actualiza y termina el cierre; **conflicto**: `marcarCerrada` vacío tras crear → borra la clase y `ERR_ACA_37`; vacío tras reutilizar → no la borra y `ERR_ACA_37`; `DuplicateKeyException` → `ERR_ACA_37`; `ERR_ACA_35` y `ERR_ACA_37` de `buscarCerrable` salen tal cual y no se guarda nada; datos inválidos → `ERR_ACA_38` sin tocar la base ni buscar la reserva; Identidad caída → `Estudiante` y sin excepción; escuela nula → `ERR_ACA_16`; no se llama a Pagos; la anotación `@PreAuthorize("hasRole('INSTRUCTOR')")` está en `cerrar` (por reflexión, como en `AgendaServiceTest`).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/ClaseService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/ClaseServiceTest.java`

## Fuera de alcance (no tocar)

- Ver y corregir una clase (UT 07), la agenda (UT 08), el controller y la ruta (UT 09).
- Un job que cierre solo las clases sin cerrar.
- La prueba de clase (CU18).
- Pagos, Identidad y la franja: no cambian.

## Resultado esperado

El instructor cierra una clase suya que ya empezó: queda guardada con sus datos y la reserva `COMPLETADA` o `NO_ASISTIO`; repetir el cierre o competir con una cancelación no deja datos duplicados ni huérfanos.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
