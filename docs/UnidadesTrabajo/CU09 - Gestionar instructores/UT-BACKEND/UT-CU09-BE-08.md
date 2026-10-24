# UT-CU09-BE-08 — Cliente de Escuela hacia Identidad para instructores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-08` |
| **Nombre** | Cliente de Escuela hacia Identidad para instructores |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU09-BE-06 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (el contrato hacia otro servicio vive en `domain/gateway/` y su implementación en `infrastructure/client/`), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) (tabla de llamadas REST entre servicios), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `AdministradorEscuelaGateway` y `AdministradorEscuelaRestClient` (CU05).

## Solicitud de negocio

Escuela no tiene los datos de la persona del instructor: tiene que pedírselos a Identidad, con el permiso del Administrador que está operando.

## Objetivo

Escuela tiene un contrato y un cliente REST que crean, consultan, buscan, reubican y revierten cuentas de instructores en Identidad.

## Alcance

- `domain/gateway/InstructorCuentaGateway`, con Javadoc:
  - `PersonaInstructor crearCuenta(DatosCuentaInstructor datos)`: crea la cuenta con el `codigo` de persona que trae `datos`.
  - `void revertirCuenta(UUID usuarioCodigo)`: idempotente; compensación de `crearCuenta`.
  - `List<PersonaInstructor> consultar(Collection<UUID> usuarioCodigos)`: parte la lista en lotes de **100** y junta los resultados; vacía no llama a nadie.
  - `List<UUID> buscarCodigos(String busqueda)`: pide páginas de **100** hasta agotarlas o llegar a **5** páginas (tope de 500 coincidencias; si se corta, `WARN`).
  - `PersonaInstructor cambiarUbicacion(UUID usuarioCodigo, BigDecimal latitud, BigDecimal longitud)`.
- Tipos en `domain/gateway/`: `PersonaInstructor` (los campos de la persona del [contrato](./README.md#contrato-de-datos--recurso-http), fechas como `LocalDate`) y `DatosCuentaInstructor(usuarioCodigo, nombres, apellidos, fechaNacimiento, sexo, ci, telefono, email, password, latitud, longitud)` cuyo `toString` no incluye la contraseña, el CI, el correo ni las coordenadas. Se reutiliza `IdentidadNoDisponibleException`.
- `infrastructure/client/InstructorCuentaRestClient` (misma configuración `app.identidad.url` y `app.identidad.timeout-segundos` que el cliente de CU05; sin pasar por el gateway):
  - `POST /api/v1/usuarios/instructores` (con `codigo` en el cuerpo), `POST .../consulta`, `GET ...?search&page&pageSize`, `PATCH .../{codigo}/ubicacion` y `DELETE .../{codigo}`.
  - **Reenvía el Bearer** de la persona que hizo la petición (el Administrador): Identidad vuelve a validar el token y el rol.
  - Un 4xx de negocio de Identidad se relanza como la excepción equivalente con su **mismo código y mensaje** (`ERR_IDE_01`, `ERR_IDE_02`, `ERR_IDE_19`, `ERR_IDE_22`, `ERR_IDE_23`); un 5xx, `429`, un timeout o un error de red lanzan `IdentidadNoDisponibleException` (el resultado allá es incierto y el caso de uso compensa).
  - La traducción de errores se **extrae a una clase compartida** (`TraductorDeErroresDeIdentidad`, package-private) que usan este cliente y `AdministradorEscuelaRestClient`, sin cambiar el comportamiento de este último.
- `.claude/rules/MICROSERVICIOS.md`: sumar la fila Escuela → Identidad "Crear, consultar, buscar, reubicar y revertir la cuenta de un instructor (reenvía el Bearer del Administrador)" (CU09) a la tabla de llamadas REST entre servicios.
- Pruebas con un Identidad falso (`HttpServer` del JDK): cada operación pega a la ruta y con el método correctos y reenvía el Bearer; `crearCuenta` manda el `codigo` y nunca escribe la contraseña en un registro; un 409 `ERR_IDE_01` llega como `ConflictException` con ese código y mensaje (y los demás códigos de la lista); 500, `429` y timeout → `IdentidadNoDisponibleException`; `consultar` con 250 códigos hace 3 llamadas; `buscarCodigos` recorre varias páginas y se detiene en 5; `revertirCuenta` ante 200 no falla. Las pruebas de `AdministradorEscuelaRestClient` siguen en verde.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/gateway/` (`InstructorCuentaGateway`, `PersonaInstructor`, `DatosCuentaInstructor`)
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/client/InstructorCuentaRestClient.java` y `TraductorDeErroresDeIdentidad.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/client/AdministradorEscuelaRestClient.java` (solo para usar el traductor)
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/infrastructure/client/InstructorCuentaRestClientTest.java`
- `.claude/rules/MICROSERVICIOS.md`

## Fuera de alcance (no tocar)

- Los casos de uso que lo usan (UT 09 a 11).
- Reintentos, caché o circuit breaker.
- Los endpoints de Identidad (UT 06).
- El contrato de `AdministradorEscuelaGateway`.

## Resultado esperado

Escuela puede pedirle a Identidad todo lo que es de la persona del instructor, con el token del Administrador y con los errores de negocio intactos.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
