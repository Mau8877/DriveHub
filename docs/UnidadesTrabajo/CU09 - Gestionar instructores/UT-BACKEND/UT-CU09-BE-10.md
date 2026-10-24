# UT-CU09-BE-10 — Crear un instructor (saga con compensación)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-10` |
| **Nombre** | Crear un instructor (saga con compensación) |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU09-BE-07, UT-CU09-BE-08, UT-CU09-BE-09 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `CrearEscuelaUseCase` (CU05): misma idea de saga simple y de compensación idempotente.

## Solicitud de negocio

El Administrador da de alta a un instructor con su cuenta y su ficha; o queda todo creado o no queda nada.

## Objetivo

Existe el caso de uso que crea la cuenta del instructor en Identidad y su ficha en Escuela, sin dejar nada a medias.

## Alcance

- `domain/entity/NroLicencia` (utilidad de dominio): `String normalizar(String)` (recorta, sin espacios internos, en mayúsculas) y validez de 3 a 30 caracteres `[A-Z0-9-]`. `Instructor` suma una fábrica `nuevo(...)` que aplica las reglas de la ficha con la fecha de hoy que se le pasa: licencia con formato válido; **vencimiento posterior a hoy**; contratación **no futura** (si no viene, hoy); **una o más categorías** sin repetir; estado inicial `ACTIVO`. Una regla incumplida lanza `ValidationException` `ERR_SYS_01` con un mensaje claro (el dominio puede lanzar las excepciones de `common/exception/`).
- `application/dto/CrearInstructorInput(escuelaCodigo, nombres, apellidos, fechaNacimiento, sexo, ci, telefono, email, password, latitud, longitud, nroLicencia, licenciaVence, fechaContratacion, categorias)`; el `toString` no incluye datos sensibles.
- `application/usecase/CrearInstructorUseCase` (`ejecutar(input)`) con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`, que sigue **este orden** (el orden hace inofensivos casi todos los fallos):
  1. Normaliza y valida la ficha con las reglas del dominio y la fecha de `Clock`.
  2. `existePorNroLicencia` → `ConflictException` `ERR_ESC_12`.
  3. `CategoriaLicenciaRepository.buscarPorSiglas`: si alguna no existe, o es `SIN` ("sin categoría": un instructor enseña categorías reales) → `ValidationException` `ERR_ESC_14`.
  4. Genera el `usuarioCodigo` (`UuidV7`) y llama a `InstructorCuentaGateway.crearCuenta`. Si Identidad **rechaza** por una regla de negocio (`ERR_IDE_01`, `ERR_IDE_02`, `ERR_IDE_19`...), la excepción sube tal cual: Identidad no creó nada, no hay nada que compensar.
  5. Guarda la ficha en una transacción (`TransactionTemplate`) con `escuelaCodigo` del input, `estado` `ACTIVO` y sus categorías.
  6. Si algo falla **después** de pedir la cuenta (Identidad no responde o vence —el resultado es incierto—, o falla guardar la ficha, incluida una carrera por la licencia), llama a `revertirCuenta(usuarioCodigo)` y relanza el error original. `revertirCuenta` nunca lanza: si falla, `ERROR` "Compensación fallida: puede quedar una cuenta sin ficha" con `usuarioCodigo` y el error original es el que importa.
  7. Devuelve el detalle con `InstructorMapeador` y registra `INFO` "Instructor creado" con `instructorCodigo` y `escuelaCodigo`, **nunca** contraseña, CI, correo, teléfono, número de licencia ni coordenadas.
- `EscuelaErrorCodes.CATEGORIA_INVALIDA` = `ERR_ESC_14`. Documentarlo en `docs/errors/escuela/ERRORES_ESCUELA.md` (400, `ValidationException`, "Una categoría no existe o no se puede enseñar", `CrearInstructorUseCase.ejecutar()` y `EditarInstructorUseCase.ejecutar()`) y enlazarlo en `docs/errors/README.md`.
- Pruebas unitarias con los puertos simulados y `InOrder`: el orden de los pasos; licencia vencida, que vence hoy, contratación futura, sin categorías y con repetidas → `ERR_SYS_01` sin llamar a Identidad; licencia en uso → `ERR_ESC_12` sin llamar a Identidad; categoría inexistente o `SIN` → `ERR_ESC_14` sin llamar a Identidad; Identidad rechaza → no se guarda ni se revierte; Identidad no responde → se revierte con el **mismo** `usuarioCodigo` que se mandó; falla guardar → se revierte y se relanza; la compensación fallida no tapa el error original; el `usuarioCodigo` guardado en la ficha es el mismo que se mandó a Identidad; el número de licencia queda normalizado; la contraseña nunca en el log. La autorización por rol **no** se prueba acá: se ejerce en la UT 12.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/entity/NroLicencia.java` e `Instructor.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/CrearInstructorInput.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/CrearInstructorUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- Tests del caso de uso, de `NroLicencia` y de la fábrica de `Instructor`
- `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint (UT 12).
- Editar un instructor (UT 11).
- Enviar un correo de bienvenida o de verificación.
- Una cola de reintentos o la limpieza automática de cuentas sobrantes.
- Asignarle autos o estudiantes.

## Resultado esperado

El Administrador puede crear un instructor con cuenta, perfil y ficha, y un fallo a mitad de camino no deja una cuenta sin ficha.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
