# UT-CU17-BE-04 — DTOs y reglas de los datos de la clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-04` |
| **Nombre** | DTOs y reglas de los datos de la clase |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-03` |

## Antes de implementar

[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#estilo-general) (los DTOs son `record`, sin Lombok), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) (`ValidationException` con código catalogado). Tomar como modelo `ReservarFranjasRequest`, `ReprogramarReservaRequest` y `ClaseAgendaResponse`. Decisiones 7 y 8 del [README](./README.md).

## Solicitud de negocio

Lo que el instructor escribe al cerrar o corregir una clase tiene que revisarse con las mismas reglas en los dos casos, y lo que se devuelve no debe mostrar ids internos.

## Objetivo

Existen los DTOs de entrada y de salida de la clase y las reglas de negocio de sus datos, en un solo lugar.

## Alcance

- `features/clases/dto/` (todos `record`):
  - `CerrarClaseRequest(UUID reservaCodigo, Asistencia asistencia, String tema, String observaciones, Integer kmInicio, Integer kmFin)`: `@NotNull` en `reservaCodigo` y `asistencia`; `@Size(max = 120)` en `tema`; `@Size(max = 500)` en `observaciones`; `@Min(0)` y `@Max(9999999)` en cada kilometraje.
  - `ActualizarClaseRequest(String tema, String observaciones, Integer kmInicio, Integer kmFin)`: las mismas restricciones de tamaño y rango, todo opcional.
  - `ClaseResponse(UUID codigo, LocalDate fecha, UUID estudianteCodigo, String estudianteNombre, Asistencia asistencia, String tema, String observaciones, Integer kmInicio, Integer kmFin, EstadoClase estado)` con `static de(Clase, String estudianteNombre)`. Sin ids internos, sin `reservaId` ni vehículo.
- `features/clases/service/ReglasDeClase` (clase con métodos estáticos, o un `@Component` sin estado), que lanza `ValidationException` `ERR_ACA_38` con mensajes en español, sin detalles técnicos:
  - `validarCierre(Asistencia, String tema, Integer kmInicio, Integer kmFin)`: con `AUSENTE`, `tema` o cualquier kilometraje no nulos y no en blanco → error; si vienen los dos kilometrajes y `kmFin < kmInicio` → error.
  - `validarCorreccion(Clase clase, ActualizarClaseRequest request)`: con la clase `AUSENTE`, `tema` o kilómetros enviados → error; el kilometraje inicial y final **resultantes** (lo enviado, o lo que ya tenía la clase si no se envió) cumplen `kmFin >= kmInicio`.
  - `static String limpiar(String texto)`: recorta los espacios y devuelve `null` si queda en blanco (así «   » borra el texto).
- Tests: Bean Validation de cada request (por ejemplo con `Validator`): `asistencia` y `reservaCodigo` obligatorios, 121 caracteres de tema y 501 de observaciones fallan y 120 y 500 pasan, kilometraje −1 y 10 000 000 fallan y 0 y 9 999 999 pasan; `ClaseResponse.de` copia los campos y no expone ids internos; `ReglasDeClase`: cierre `AUSENTE` con tema o km falla y solo con observaciones pasa, final menor que inicial falla, solo un kilometraje pasa, iguales pasan; corrección que combina con lo guardado (se envía solo `kmFin` menor al `kmInicio` ya guardado falla), corrección de una clase `AUSENTE` con tema falla y con observaciones pasa; `limpiar` recorta, devuelve `null` con blancos y con `null`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/dto/CerrarClaseRequest.java`, `ActualizarClaseRequest.java`, `ClaseResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/ReglasDeClase.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/dto/ClaseDtoTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/ReglasDeClaseTest.java`

## Fuera de alcance (no tocar)

- Guardar nada o consultar la base (UT 06 y 07).
- El modelo `Clase` y sus enums: no cambian.
- El controller y la ruta (UT 09).

## Resultado esperado

Los datos de una clase se validan igual al cerrar y al corregir, y la respuesta tiene la forma pública acordada.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
