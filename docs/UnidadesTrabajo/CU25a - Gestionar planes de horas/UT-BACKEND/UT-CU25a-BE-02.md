# UT-CU25a-BE-02 — Errores, escuela del token y tipo de cambio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25a-BE-02` |
| **Nombre** | Errores, escuela del token y tipo de cambio |
| **Caso de uso** | CU25a-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (una copia por servicio, nunca una librería compartida). Como modelo, `academico`: `config/CurrentEscuelaId.java`, `CurrentUserArgumentResolvers.java`, `WebMvcConfig.java` y su `GlobalExceptionHandler`.

## Solicitud de negocio

El servicio necesita saber de qué escuela es quien llama, tener su catálogo de errores y un tipo de cambio configurable para mostrar el costo en dólares; y un pedido mal formado debe responder un error claro, no un 500.

## Objetivo

Pagos tiene su catálogo de códigos de error, entrega la escuela del token a un parámetro de controller, expone el tipo de cambio como configuración validada y responde 400 ante pedidos ilegibles.

## Alcance

- `common/exception/PagosErrorCodes`: `PLAN_NOMBRE_EN_USO = "ERR_PAG_01"`, `PLAN_NO_ENCONTRADO = "ERR_PAG_02"`, `PLAN_YA_EN_ESE_ESTADO = "ERR_PAG_03"`, `TOKEN_SIN_ESCUELA = "ERR_PAG_04"`, y los transversales `VALIDACION = "ERR_SYS_01"` y `SESION_INVALIDA = "ERR_SYS_04"` (sin JWT el resolver responde 401 con este). Constructor privado, comentario de por qué cada número.
- `config/CurrentEscuelaId` (anotación de parámetro) y `config/CurrentUserArgumentResolvers` (entrega el claim `escuela` del `Jwt` como `UUID`; `null` si el token no lo trae; un valor que no sea UUID también da `null`), registrado en `config/WebMvcConfig`. Es la misma pieza que `academico`, copiada.
- `config/PagosProperties` (`@ConfigurationProperties(prefix = "app.pagos")`, `record`): `BigDecimal tipoCambioBsUsd`, con valor por defecto `6.96` y validación `> 0` (`@Validated` + `@Positive`): un valor cero, negativo o no numérico impide que el servicio arranque. `@EnableConfigurationProperties` en una clase de configuración.
- `application.properties`: `app.pagos.tipo-cambio-bs-usd=${TIPO_CAMBIO_BS_USD:6.96}` con un comentario (cambio oficial; para cambiarlo basta la variable de entorno).
- `GlobalExceptionHandler`: 400 `ERR_SYS_01` para `HttpMessageNotReadableException` (cuerpo ilegible, número con formato inválido o valor desconocido) y `MethodArgumentTypeMismatchException` (código que no es UUID, filtro con valor inválido), con un mensaje en español sin detalles técnicos.
- Tests: `CurrentUserArgumentResolversTest` (claim presente, ausente y no UUID), `PagosPropertiesTest` (por defecto 6.96, valor propio, 0 y negativo fallan) y casos nuevos en el test del handler o en un `@WebMvcTest` mínimo: cuerpo JSON roto y UUID inválido responden 400 `ERR_SYS_01`.

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java` y `GlobalExceptionHandler.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/config/CurrentEscuelaId.java`, `CurrentUserArgumentResolvers.java`, `WebMvcConfig.java` y `PagosProperties.java`
- `projects/backend/pagos/src/main/resources/application.properties`
- los tres tests

## Fuera de alcance (no tocar)

- La documentación de los códigos en `docs/errors/` (UT 05, cuando se lanzan por primera vez).
- La conversión a dólares y cualquier servicio de planes (UT 04).
- `CurrentUserId`: ningún caso de uso de Pagos lo necesita todavía.

## Resultado esperado

Un parámetro `@CurrentEscuelaId UUID` de un controller recibe la escuela del token; un pedido mal formado responde 400; el tipo de cambio está disponible como bean validado.

## Validación

1. `./mvnw test` en `pagos` y `./mvnw checkstyle:check` sin errores.
2. Arrancar el servicio con `TIPO_CAMBIO_BS_USD=0`: falla al iniciar con un mensaje claro; sin la variable arranca con 6.96.
