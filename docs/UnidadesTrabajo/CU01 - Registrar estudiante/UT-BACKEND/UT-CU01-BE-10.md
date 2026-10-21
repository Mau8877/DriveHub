# UT-CU01-BE-10 — `RegistrarEstudianteService` y `ERR_IDE_01..03`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-10` |
| **Nombre** | `RegistrarEstudianteService` y `ERR_IDE_01..03` |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-09 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca loguear contraseñas) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Es la lógica del registro: validar la edad, confirmar la escuela, evitar correos repetidos y crear la cuenta de estudiante.

## Objetivo

`RegistrarEstudianteService` implementa `RegistrarEstudianteUseCase` con todas las reglas del CU, probado con los puertos de salida mockeados.

## Alcance

- `common/exception/IdentidadErrorCodes` (un solo lugar): `CORREO_EN_USO = "ERR_IDE_01"`, `MENOR_DE_EDAD = "ERR_IDE_02"`, `ESCUELA_NO_DISPONIBLE = "ERR_IDE_03"`.
- `application/service/RegistrarEstudianteService` (`@Service`, `@Transactional`, `@RequiredArgsConstructor`), en este orden:
  1. Normaliza el correo (`trim` + minúsculas) y los nombres (`trim`).
  2. Calcula la edad con un `Clock` inyectado a la fecha de hoy; si es menor que `app.registro.edad-minima` (inyectada con `@Value`, default 18) lanza `ValidationException` `ERR_IDE_02`. Exactamente 18 años cumplidos hoy es válido.
  3. Resuelve la escuela con `EscuelaConsultaPort`; si no hay resultado lanza `NotFoundException` `ERR_IDE_03`.
  4. Si `existePorEmail`, lanza `ConflictException` `ERR_IDE_01`.
  5. Busca el rol `RolNombres.ESTUDIANTE` (si no existe es un error de configuración: `IllegalStateException`, cae en el 500 genérico).
  6. Crea el `Usuario` con `estado = ACTIVO`, `intentosFallidos = 0`, `escuelaCodigo` de la escuela, rol `ESTUDIANTE` (siempre, no viene del comando) y `passwordHash` del `PasswordHasherPort`; lo guarda y lo devuelve.
- Logging solo en el servicio: `INFO` al registrar (campo `module = "usuarios"`, `usuarioCodigo`), `WARN` con `errorCode` en los tres rechazos. Nunca la contraseña ni el hash.
- `app.registro.edad-minima=18` en `application.properties`.
- Documentación: crear `docs/errors/identidad/ERRORES_IDENTIDAD.md` con las secciones de `ERR_IDE_01`, `ERR_IDE_02` y `ERR_IDE_03` en el formato de GUIA_ERRORES_BACKEND.md y sumar sus links (y la fila de `identidad/` en la tabla) a `docs/errors/README.md`.
- Tests unitarios (puertos mockeados, `Clock` fijo): registro exitoso (verifica rol `ESTUDIANTE`, estado `ACTIVO`, escuela y que el hash guardado no es la contraseña), menor de 18, exactamente 18, escuela inexistente, correo repetido (con el correo escrito en otras mayúsculas), y que no se guarda nada cuando falla una regla.

Archivos esperados (crear/tocar):

- `.../common/exception/IdentidadErrorCodes.java`
- `.../application/service/RegistrarEstudianteService.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/test/java/.../RegistrarEstudianteServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Implementaciones de los puertos de salida y el controller (UT 11 a 13).
- Validación de formato del cuerpo (`@Valid`): es del controller.
- Verificación de correo, evento `UsuarioRegistrado` y creación del perfil `estudiante`.
- Emitir tokens o iniciar sesión.

## Resultado esperado

Con puertos de prueba, el servicio crea siempre un estudiante `ACTIVO` con rol `ESTUDIANTE` y su escuela, y rechaza con el código correcto cada caso inválido.

## Validación

1. `./mvnw test` en `identidad` pasa con `RegistrarEstudianteServiceTest` en verde.
2. `ERRORES_IDENTIDAD.md` tiene las tres secciones con HTTP, excepción, significado y dónde se lanza (`RegistrarEstudianteService.registrar()`), y el índice del README las enlaza.
3. Ningún log del test contiene la contraseña ni el hash.
4. `./mvnw checkstyle:check` sin errores.
