# UT-CU05-BE-07 — Cliente de Identidad para el Administrador de la escuela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-07` |
| **Nombre** | Cliente de Identidad para el Administrador de la escuela |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-05 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Y la sección de comunicación de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md).

## Solicitud de negocio

Escuela debe pedirle a Identidad que cree (y si hace falta revierta) al Administrador, que es dueño de las cuentas.

## Objetivo

Escuela tiene un puerto de dominio y un cliente REST para hablar con los dos endpoints de Identidad.

## Alcance

- `domain/gateway/AdministradorEscuelaGateway` (interfaz; en Clean los contratos hacia otros servicios viven en `domain/gateway/`, igual que los de persistencia en `domain/repository/`): `AdministradorCreado crearAdministrador(UUID escuelaCodigo, DatosAdministrador datos)` y `void revertirAdministrador(UUID escuelaCodigo)`. Records `DatosAdministrador` (nombres, apellidos, fechaNacimiento, email, password, telefono) y `AdministradorCreado` (codigo, email, nombres, apellidos).
- `domain/gateway/IdentidadNoDisponibleException` (`RuntimeException` propia, sin Spring): Identidad no respondió, venció el tiempo de espera o contestó 5xx. El caso de uso la usa para decidir si compensa.
- `infrastructure/client/AdministradorEscuelaRestClient` con `RestClient`: URL base `app.identidad.url` (`${IDENTIDAD_URL:http://localhost:8081}`, la misma variable que ya usa el JWKS; `docker-compose.yml` ya la define para `escuela`), tiempos de espera de conexión 2 s y de lectura 5 s (`app.identidad.timeout-segundos`). Reenvía el `Authorization: Bearer` de la persona que hizo la petición (sale del `SecurityContext`); Identidad vuelve a validar el token y el rol.
- Desempaqueta el sobre estándar. Una respuesta 4xx de Identidad se convierte en la excepción de negocio equivalente **con el mismo código y mensaje** (400 `ValidationException`, 403 `ForbiddenException`, 409 `ConflictException`, etc.), para que el formulario muestre el motivo real (`ERR_IDE_01`, `ERR_IDE_02`...). Un 5xx, un timeout o un error de red lanzan `IdentidadNoDisponibleException`.
- **Nunca** loguea la contraseña ni el cuerpo de la petición.
- `.claude/rules/MICROSERVICIOS.md` (sección Comunicación) anota las llamadas REST entre servicios que ya existen: Identidad → Escuela (CU01, CU02) y Escuela → Identidad (CU05).
- `.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md`: sumar `domain/gateway/` (interfaces hacia otros servicios) e `infrastructure/client/` (sus implementaciones) a la estructura de paquetes.
- Tests con `MockRestServiceServer`: envía el Bearer y el cuerpo esperado; 201 → `AdministradorCreado`; 409 `ERR_IDE_01` → `ConflictException` con ese código; 500 y timeout → `IdentidadNoDisponibleException`; `revertir` con 200 (también cuando no había nada) y con 409 `ERR_IDE_13`.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/gateway/AdministradorEscuelaGateway.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/gateway/DatosAdministrador.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/gateway/AdministradorCreado.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/gateway/IdentidadNoDisponibleException.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/client/AdministradorEscuelaRestClient.java`
- `projects/backend/escuela/src/main/resources/application.properties`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/infrastructure/client/AdministradorEscuelaRestClientTest.java`
- `.claude/rules/MICROSERVICIOS.md`
- `.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md`

## Fuera de alcance (no tocar)

- El caso de uso que orquesta la creación (UT 08).
- Reintentos automáticos o circuit breaker.
- Eventos o saga formal: no existe la infraestructura y la rule de MICROSERVICIOS.md limita la saga a CU44.

## Resultado esperado

Escuela puede crear y revertir al Administrador en Identidad, con los errores traducidos.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
