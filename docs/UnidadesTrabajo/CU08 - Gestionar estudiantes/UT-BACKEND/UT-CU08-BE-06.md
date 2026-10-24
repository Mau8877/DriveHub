# UT-CU08-BE-06 — Edición de categoría y ubicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-06` |
| **Nombre** | Edición de categoría y ubicación |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08-BE-02, UT-CU08-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (`PATCH` como edición parcial), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `EditarPerfilService` (UT-CU08a-BE-05).

## Solicitud de negocio

La escuela asigna la categoría de cada estudiante y puede corregir su ubicación; nada más de su ficha le pertenece.

## Objetivo

Existe el caso de uso que cambia únicamente la categoría y/o la ubicación de un estudiante de la escuela.

## Alcance

- Puerto `domain/port/in/EditarEstudianteUseCase` (`EstudianteDetalle editar(EditarEstudianteComando)`) con `EditarEstudianteComando(administradorCodigo, estudianteCodigo, categoriaSigla, latitud, longitud)` (los tres últimos opcionales; las coordenadas juntas o ninguna). `EditarEstudianteService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional`:
  - Toma la escuela de la cuenta del Administrador y busca al estudiante con `buscarEstudianteDeEscuela` → `ERR_IDE_20` si no existe o es de otra escuela.
  - Si no llega ningún campo → `ValidationException` (`ERR_SYS_01`, "No hay nada que cambiar"). Si llega solo una coordenada → `ValidationException`.
  - Categoría: `SIN` sin consultar; otra se valida con `CategoriaLicenciaConsultaPort` → `ERR_IDE_21` si no existe.
  - Ubicación: solo si las coordenadas son distintas de las guardadas se recalcula la dirección con `GeocodificadorInversoPort` (vacío = `null`, `WARN`).
  - Modifica **solo** `categoriaSigla`, `latitud`, `longitud` y `direccion` del `Perfil` y guarda con `actualizar`; nunca toca nombres, CI, sexo, fecha de nacimiento, teléfono, correo, estado ni la escuela. Devuelve el `EstudianteDetalle` actualizado.
  - `INFO` "Estudiante actualizado por el administrador" con `usuarioCodigo` y `administradorCodigo`, sin coordenadas.
- Pruebas unitarias con los puertos simulados: cambia la categoría; cambia la ubicación y recalcula la dirección; coordenadas iguales no llaman al geocodificador; `ERR_IDE_20` (inexistente y de otra escuela); `ERR_IDE_21`; cuerpo sin campos; una sola coordenada; los datos personales nunca cambian aunque el comando los trajera (no existen en el comando: la prueba comprueba el `Perfil` guardado). Integración con Postgres real y la seguridad real: roles distintos a `ADMIN_ESCUELA` → 403 y nada cambia; el Administrador de la escuela A no puede editar a un estudiante de la B.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/EditarEstudianteUseCase.java` y `EditarEstudianteComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/EditarEstudianteService.java`
- Tests del servicio y de la integración
- `docs/errors/identidad/ERRORES_IDENTIDAD.md` (nuevos lugares donde se lanzan `ERR_IDE_20` y `ERR_IDE_21`)

## Fuera de alcance (no tocar)

- El endpoint (UT 07).
- Cambiar el estado de la cuenta (activar/desactivar) ni cualquier dato personal.
- Que el propio estudiante edite su perfil (CU08a, UT 05).

## Resultado esperado

El Administrador puede corregir la categoría y la ubicación de un estudiante y nada más.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
