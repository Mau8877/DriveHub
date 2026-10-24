# UT-CU08-BE-05 — Alta de un estudiante por el Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-05` |
| **Nombre** | Alta de un estudiante por el Administrador |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08-BE-02, UT-CU08-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `RegistrarEstudianteService` tal como la dejó UT-CU08a-BE-04.

## Solicitud de negocio

El Administrador puede dar de alta a un estudiante con su cuenta, su contraseña inicial, su ubicación y su categoría.

## Objetivo

Existe el caso de uso que crea la cuenta y el perfil de un estudiante en la escuela del Administrador.

## Alcance

- **Extraer la regla compartida** de `RegistrarEstudianteService` a un componente de aplicación `AltaEstudiante` (`application/service/`): valida la edad mínima (`app.registro.edad-minima`), comprueba correo (`ERR_IDE_01`) y CI (`ERR_IDE_19`), normaliza, hashea la contraseña, calcula la dirección con `GeocodificadorInversoPort`, crea `Usuario` (rol `ESTUDIANTE`, estado `ACTIVO`, correo sin verificar) + `Perfil` y los guarda. Recibe la escuela y la categoría ya resueltas. `RegistrarEstudianteService` pasa a usarlo **sin cambiar su comportamiento ni su orden de validaciones** (sus pruebas existentes siguen en verde).
- Puerto `domain/port/in/CrearEstudianteUseCase` (`EstudianteDetalle crear(CrearEstudianteComando)`) con `CrearEstudianteComando(administradorCodigo, nombres, apellidos, fechaNacimiento, sexo, ci, telefono, email, password, latitud, longitud, categoriaSigla)`; `CrearEstudianteService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional`:
  - Toma la `escuelaCodigo` de la cuenta del Administrador (`ForbiddenException` si no tiene).
  - `categoriaSigla` nula o vacía = `SIN`; cualquier otra se valida con `CategoriaLicenciaConsultaPort.existe` (UT 02) → `ValidationException` `ERR_IDE_21` si no existe. Una `EscuelaNoDisponibleException` sube como 500 sin detalles.
  - Llama a `AltaEstudiante` y devuelve `EstudianteDetalle` con el mapeador de la UT 04.
  - Registra `INFO` "Estudiante creado por el administrador" con `usuarioCodigo` y `administradorCodigo`, **nunca** contraseña, CI, correo, teléfono ni coordenadas.
- `IdentidadErrorCodes.CATEGORIA_NO_EXISTE` = `ERR_IDE_21`. Documentarlo en `docs/errors/identidad/ERRORES_IDENTIDAD.md` (400, `ValidationException`, "La categoría de licencia no existe", `CrearEstudianteService.crear()`) y enlazarlo en `docs/errors/README.md`; sumar `CrearEstudianteService.crear()` a los lugares donde se lanzan `ERR_IDE_01`, `ERR_IDE_02` y `ERR_IDE_19`.
- Pruebas unitarias con los puertos simulados: alta feliz (escuela del Administrador, `SIN` por defecto, estado `ACTIVO`, correo sin verificar); categoría válida y categoría inexistente (`ERR_IDE_21`, sin guardar); correo y CI repetidos y menor de edad (sin guardar); Escuela caída; la contraseña nunca en el log. El registro de CU08a conserva sus pruebas. Integración con Postgres real y la seguridad real: **aquí se ejerce el rol** (`ESTUDIANTE`, `INSTRUCTOR` y `SUPER_ADMIN` → 403 y no se crea nada).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/AltaEstudiante.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/RegistrarEstudianteService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/CrearEstudianteService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CrearEstudianteUseCase.java` y `CrearEstudianteComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- Tests de `AltaEstudiante`, `CrearEstudianteService` y la integración
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint (UT 07).
- Enviar un correo de bienvenida o de verificación: el correo queda sin verificar y la persona lo verifica como siempre.
- Cambiar las reglas de contraseña, edad o CI.
- Crear Instructores (CU09).

## Resultado esperado

El Administrador puede crear estudiantes con su perfil completo, sin duplicar las reglas del registro.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
