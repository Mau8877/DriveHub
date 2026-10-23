# UT-CU08a-BE-03 — Geocodificación inversa de la ubicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-03` |
| **Nombre** | Geocodificación inversa de la ubicación |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (puerto de salida `...Port`, adaptador `...Adapter`), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo de cliente REST, `EscuelaRestAdapter`.

## Solicitud de negocio

La dirección no la escribe nadie: sale del punto que la persona elige en el mapa, y no puede quedar a merced de lo que mande el cliente.

## Objetivo

Existe un puerto que, dadas una latitud y una longitud, devuelve la dirección, o nada si no puede.

## Alcance

- `domain/port/out/GeocodificadorInversoPort` con `Optional<String> direccionDe(BigDecimal latitud, BigDecimal longitud)`. Documentar en su Javadoc que **nunca lanza**: ante cualquier falla devuelve vacío.
- `infrastructure/adapter/out/geocodificacion/NominatimGeocodificadorAdapter`: `GET {app.geocodificacion.url}/reverse?format=jsonv2&lat=..&lon=..&zoom=18&accept-language=es` con `RestClient` y tiempos de espera cortos (conexión 2 s, lectura `app.geocodificacion.timeout-segundos`, 3 por defecto). Cabecera `User-Agent` con `app.geocodificacion.user-agent` (obligatoria por la política de Nominatim; por defecto `DriveHub/1.0`). Lee `display_name`, lo recorta a 300 caracteres y lo devuelve; un cuerpo con `error` (punto sin dirección), un 4xx/5xx, un timeout o cualquier excepción devuelven vacío y registran un `WARN` **sin las coordenadas** (`module = geocodificacion`).
- **Espaciado:** un candado/semáforo interno garantiza al menos 1 segundo entre llamadas al servidor público, como pide su política; una llamada que tendría que esperar más que el timeout devuelve vacío en vez de bloquear la petición.
- Configuración: `app.geocodificacion.url` (por defecto `https://nominatim.openstreetmap.org`), `.timeout-segundos`, `.user-agent`; variables `GEOCODIFICACION_URL` y `GEOCODIFICACION_USER_AGENT` en `application.properties`, `.env.example` y `docker-compose.yml` (servicio `identidad`).
- Pruebas con un servidor falso (`HttpServer` del JDK o `MockRestServiceServer`): devuelve el `display_name`; lo recorta a 300; envía `User-Agent` y `accept-language=es`; punto sin dirección (`{"error":...}`), 500, timeout y cuerpo no JSON → vacío sin lanzar; dos llamadas seguidas respetan el espaciado; el log de un fallo no contiene latitud ni longitud.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/GeocodificadorInversoPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/geocodificacion/NominatimGeocodificadorAdapter.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/geocodificacion/NominatimGeocodificadorAdapterTest.java`
- `.env.example`, `docker-compose.yml`

## Fuera de alcance (no tocar)

- Usar el puerto en algún caso de uso (UT 04 a 06).
- Caché, reintentos, circuit breaker o una instancia propia de Nominatim.
- Geocodificación directa (de texto a punto): el cliente siempre manda el punto.
- Validar que el punto esté dentro de Bolivia.

## Resultado esperado

Identidad sabe convertir un punto en una dirección sin que una falla externa rompa nada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con internet, un arranque local y una llamada de prueba al adaptador con un punto de La Paz (-16.4897, -68.1193) devuelve una dirección en español.
