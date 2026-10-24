# UT-CU09-BE-03 — Crear y revertir la cuenta de un instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-03` |
| **Nombre** | Crear y revertir la cuenta de un instructor |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU09-BE-01, UT-CU09-BE-02 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `CrearEstudianteService`, `RevertirAdministradorEscuelaService` y `EstudianteMapeador`.

## Solicitud de negocio

Cuando el Administrador da de alta a un instructor, su cuenta y su perfil tienen que nacer en la escuela del Administrador; y si la ficha no se puede guardar, esa cuenta tiene que poder deshacerse sin dejar rastro.

## Objetivo

Existen los casos de uso que crean la cuenta con su perfil (rol `INSTRUCTOR`) y que la revierten, ambos para la escuela de quien llama.

## Alcance

- `domain/port/in/PersonaInstructor` (record de lectura): `codigo, nombres, apellidos, nombreCompleto, fotoUrl, edad, fechaNacimiento, sexo, ci, email, telefono, direccion, latitud, longitud, fechaAlta`. Sin `passwordHash`, roles ni escuela.
- `application/service/PersonaInstructorMapeador` (`@Component`, con el `Clock` y `PerfilArmador`): arma la `PersonaInstructor` desde `Usuario`+`Perfil` (edad al momento, `nombreCompleto` = nombres + apellidos, `fechaAlta` = fecha de `creadoEn` en la zona del reloj, `fotoUrl` firmada o `null`). Lo reutilizan las UT 04 y 05.
- **Crear:** `domain/port/in/CrearCuentaInstructorUseCase` (`PersonaInstructor crear(CrearCuentaInstructorComando)`), `CrearCuentaInstructorComando(administradorCodigo, codigo, nombres, apellidos, fechaNacimiento, sexo, ci, telefono, email, password, latitud, longitud)` y `CrearCuentaInstructorService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional`: toma la escuela de la cuenta del Administrador con `EscuelaDelAdministrador` (`ForbiddenException` si no tiene) y llama a `AltaCuenta` (UT 01) con `RolNombres.INSTRUCTOR`, categoría nula y el `codigo` que trae el comando (el que eligió Escuela). Registra `INFO` "Cuenta de instructor creada" con `usuarioCodigo` y `administradorCodigo`, **nunca** contraseña, CI, correo, teléfono ni coordenadas.
- **Revertir:** `domain/port/in/RevertirCuentaInstructorUseCase` (`void revertir(UUID administradorCodigo, UUID instructorCodigo)`) y `RevertirCuentaInstructorService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional`: busca con `buscarInstructorDeEscuela`; si **no existe**, termina sin error (idempotente); si la cuenta **ya inició sesión** (`ultimoLogin` no nulo), `ConflictException` con `ERR_IDE_23`; si no, `eliminarDefinitivamente` (borra roles, sesiones, enlaces de correo, perfil y cuenta). `WARN` "Cuenta de instructor revertida" con `usuarioCodigo`.
- `IdentidadErrorCodes.CUENTA_YA_USADA` = `ERR_IDE_23`. Documentarlo en `docs/errors/identidad/ERRORES_IDENTIDAD.md` (409, `ConflictException`, "La cuenta del instructor ya se usó y no se puede revertir", `RevertirCuentaInstructorService.revertir()`) y enlazarlo en `docs/errors/README.md`. Sumar `CrearCuentaInstructorService.crear()` a los lugares donde se lanzan `ERR_IDE_01`, `ERR_IDE_02` y `ERR_IDE_19`.
- Pruebas unitarias con los puertos simulados: alta feliz (escuela del Administrador, rol `INSTRUCTOR`, estado `ACTIVO`, correo sin verificar, sin categoría, con el código pedido); un código que ya existe → `ERR_SYS_01` sin guardar; correo repetido, CI repetido y menor de edad (sin guardar); la escuela sale de la cuenta del Administrador y no del comando; Administrador sin escuela → `ForbiddenException`; el mapeador (edad en el día exacto del cumpleaños y el anterior, fecha de alta en la zona del reloj, foto firmada y sin foto); revertir: borra una cuenta sin sesión, no hace nada si no existe o es de otra escuela, `ERR_IDE_23` si ya inició sesión y no borra; la contraseña nunca en el log. La autorización por rol **no** se prueba acá: se ejerce en la UT 06.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/` (`PersonaInstructor`, `CrearCuentaInstructorUseCase`, `CrearCuentaInstructorComando`, `RevertirCuentaInstructorUseCase`)
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/` (`PersonaInstructorMapeador`, `CrearCuentaInstructorService`, `RevertirCuentaInstructorService`)
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- Tests de los dos servicios y del mapeador
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Los endpoints (UT 06) y las consultas y la ubicación (UT 04 y 05).
- La ficha profesional (licencia, categorías, estado): es de Escuela.
- Enviar un correo de bienvenida o de verificación: el correo queda sin verificar.
- Cambiar `eliminarDefinitivamente` ni su orden de borrado.

## Resultado esperado

Identidad crea la cuenta de un instructor en la escuela de quien llama y sabe deshacerla mientras nadie la haya usado.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
