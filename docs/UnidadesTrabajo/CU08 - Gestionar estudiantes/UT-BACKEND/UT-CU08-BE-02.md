# UT-CU08-BE-02 — Cliente de Escuela para validar categorías

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-02` |
| **Nombre** | Cliente de Escuela para validar categorías |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08-BE-01 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (puerto de salida `...Port`, adaptador `...Adapter`), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) (tabla de llamadas REST entre servicios), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `EscuelaRestAdapter` (Identidad ya le consulta a Escuela).

## Solicitud de negocio

Identidad no tiene el catálogo de categorías: antes de guardar la que elige el Administrador, tiene que preguntarle a Escuela si existe.

## Objetivo

Identidad tiene un puerto que responde "esta categoría existe" consultando a Escuela.

## Alcance

- `domain/port/out/CategoriaLicenciaConsultaPort` con `boolean existe(String sigla)`. La sigla `SIN` existe siempre sin consultar a nadie (constante `CategoriaNombres.SIN_CATEGORIA`, de CU08a).
- `infrastructure/adapter/out/escuela/CategoriaLicenciaRestAdapter` (mismo paquete y misma configuración `app.escuela.url` que `EscuelaRestAdapter`): `GET {escuela}/api/v1/categorias-licencia/{sigla}`, **reenviando el Bearer de la sesión** (el Administrador). `200` → existe; `404` → no existe; cualquier otra respuesta, timeout o error de red lanza una excepción propia `EscuelaNoDisponibleException` (el caso de uso la deja subir como 500 `ERR_SYS_00`, sin detalles). Nunca se escribe el Bearer en los registros.
- `.claude/rules/MICROSERVICIOS.md`: agregar la fila Identidad → Escuela "Validar que una categoría de licencia existe (CU08)" a la tabla de llamadas REST entre servicios.
- Pruebas con un servidor falso: 200 → `true`; 404 `ERR_ESC_07` → `false`; 500 y timeout → `EscuelaNoDisponibleException`; el Bearer se reenvía; `SIN` no hace llamada; la sigla se codifica en la URL.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/CategoriaLicenciaConsultaPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/escuela/CategoriaLicenciaRestAdapter.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/escuela/EscuelaNoDisponibleException.java` (o donde viva el equivalente actual)
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/escuela/CategoriaLicenciaRestAdapterTest.java`
- `.claude/rules/MICROSERVICIOS.md`

## Fuera de alcance (no tocar)

- Los casos de uso que lo usan (UT 05 y 06).
- Cambios en `EscuelaRestAdapter` ni en el contrato de `EscuelaConsultaPort`.
- Caché, reintentos o circuit breaker.

## Resultado esperado

Identidad puede comprobar si una categoría existe sin conocer el catálogo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
