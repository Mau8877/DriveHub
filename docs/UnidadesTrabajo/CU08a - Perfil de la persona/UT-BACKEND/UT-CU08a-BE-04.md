# UT-CU08a-BE-04 — Registro del estudiante con su perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-04` |
| **Nombre** | Registro del estudiante con su perfil |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08a-BE-02, UT-CU08a-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `RegistrarEstudianteService` (CU01).

## Solicitud de negocio

El estudiante tiene que quedar registrado con su documento, su sexo y su ubicación, y con la categoría "sin categoría".

## Objetivo

`POST /api/v1/usuarios` crea la cuenta y el perfil juntos con los datos nuevos.

## Alcance

- `RegistrarEstudianteRequest` suma `ci` (`@NotBlank`, `@Size(5..15)`, `@Pattern("[A-Za-z0-9-]+")`), `sexo` (`@NotNull`, `SexoPersona`), `latitud` y `longitud` (`@NotNull`, `@DecimalMin/@DecimalMax` de rango). Sin campo `direccion`: si llega, se ignora.
- `RegistrarEstudianteComando` y `RegistrarEstudianteService`: normalizan el CI (sin espacios, en mayúsculas); validan en este orden edad → escuela → correo → **CI** (`existePorCi` en el puerto, nuevo); calculan la dirección con `GeocodificadorInversoPort` (vacío = dirección `null`, `WARN`, sin error); crean `Usuario` + `Perfil` con `categoriaSigla = "SIN"` (constante `CategoriaNombres.SIN_CATEGORIA`) y la ubicación; todo en la misma transacción. Se quitan los valores provisionales que dejó la UT 02 en este servicio.
- `UsuarioRepositoryPort.existePorCi(String ci)` y, para el cambio de perfil de la UT 05, `existePorCiDeOtro(String ci, UUID usuarioCodigo)`; implementados en el adaptador.
- `IdentidadErrorCodes.CI_EN_USO` = `ERR_IDE_19` (`ConflictException`, "Ya existe una cuenta con ese CI"). Documentarlo en `docs/errors/identidad/ERRORES_IDENTIDAD.md` (409, "El CI ya pertenece a otra cuenta", `RegistrarEstudianteService.registrar()`; luego lo comparten otras UT) y en `docs/errors/README.md`.
- El `GlobalExceptionHandler`/`AuthExceptionHandler` ya traduce `ConflictException` y la validación (`ERR_SYS_01`): comprobar que el `400` por `sexo` con un valor fuera del enum responde `ERR_SYS_01` y no `500` (`HttpMessageNotReadableException`).
- Logs: nunca `ci`, correo, teléfono ni coordenadas; sí `usuarioCodigo`.
- Pruebas: servicio con puertos simulados (CI repetido → `ERR_IDE_19` sin guardar; el orden de validaciones; categoría `SIN` y estado `ACTIVO`; dirección del geocodificador guardada; geocodificador vacío → se guarda igual con dirección `null`; CI normalizado); `@WebMvcTest` (campos nuevos obligatorios, `sexo` inválido → 400 `ERR_SYS_01`, coordenadas fuera de rango o solo una → 400, `ERR_IDE_19` llega como 409); integración con Postgres real (se crean `usuario` y `perfil`, dos registros con el mismo CI → el segundo 409 y no queda nada a medias).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/RegistrarEstudianteRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/RegistrarEstudianteComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/RegistrarEstudianteService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/UsuarioRepositoryPort.java` y su adaptador
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/CategoriaNombres.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- Tests de servicio, controller e integración del registro
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Editar el perfil (UT 05) y el alta del Administrador (UT 06).
- Validar la categoría contra Escuela: el registro siempre usa `SIN`.
- Cambios en el login o en la verificación del correo.
- Enviar la dirección desde el cliente.

## Resultado esperado

Un estudiante se registra con CI, sexo y ubicación y queda con su perfil completo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el stack levantado: `POST /api/v1/usuarios` con `ci`, `sexo`, `latitud` y `longitud` responde 201; repetir el CI responde 409 `ERR_IDE_19`; en Postgres, `perfil.categoria_sigla = 'SIN'` y `perfil.direccion` con la dirección calculada.
