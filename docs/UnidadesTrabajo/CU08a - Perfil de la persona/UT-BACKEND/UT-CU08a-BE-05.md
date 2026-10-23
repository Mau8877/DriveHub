# UT-CU08a-BE-05 — Perfil propio con CI, sexo y ubicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-05` |
| **Nombre** | Perfil propio con CI, sexo y ubicación |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08a-BE-02, UT-CU08a-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (`PATCH` como edición parcial), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `EditarPerfilService`, `EditarPerfilRequest` y `PerfilArmador` (CU06).

## Solicitud de negocio

Cada persona tiene que poder ver y corregir todos sus datos, salvo la categoría, que la fija la escuela.

## Objetivo

`GET` y `PATCH /api/v1/usuarios/me` leen y editan el CI, el sexo y la ubicación; la categoría solo se lee.

## Alcance

- `PerfilUsuario` (vista del perfil) y `PerfilResponse` suman `ci`, `sexo`, `direccion`, `latitud`, `longitud` y `categoriaSigla`; `PerfilArmador` los toma del `Perfil`. La respuesta de login y renovación no cambia.
- `EditarPerfilRequest` suma `ci` (mismas reglas que el registro), `sexo`, `latitud` y `longitud`. Todo sigue siendo opcional (PATCH). Las coordenadas deben venir **juntas**: una sola → 400 `ERR_SYS_01`. **No** se agregan `direccion` ni `categoriaSigla`: lo que llegue con esos nombres se ignora.
- `EditarPerfilService`: si cambia el CI (normalizado), comprueba `existePorCiDeOtro` → `ConflictException` `ERR_IDE_19`; si llegan coordenadas **distintas** a las guardadas, recalcula la dirección con `GeocodificadorInversoPort` (vacío = se guarda `null`, `WARN`); si son iguales, no llama al geocodificador. Un Administrador, Instructor o Estudiante no puede quedar sin ubicación: el PATCH no tiene forma de borrarla. No toca `categoriaSigla` ni los datos de acceso.
- Reutiliza `ERR_IDE_19` (UT 04); agregar a su sección de `docs/errors/` el nuevo lugar donde se lanza (`EditarPerfilService.editar()`).
- Pruebas: servicio (CI repetido de otro → 409; el CI propio sin cambios no choca consigo mismo; coordenadas nuevas recalculan la dirección, las iguales no; categoría intacta aunque el cuerpo la traiga); `@WebMvcTest` (respuesta con los campos nuevos y `categoriaSigla` nula para el Super admin; una sola coordenada → 400; `ERR_IDE_19` → 409); integración con Postgres real y la seguridad real (cada persona edita su perfil, no el de otra).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/PerfilUsuario.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/PerfilArmador.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/EditarPerfilService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/PerfilResponse.java` y `EditarPerfilRequest.java`
- Los tests de esos tres, y `docs/errors/identidad/ERRORES_IDENTIDAD.md`

## Fuera de alcance (no tocar)

- Foto, contraseña y correo: no cambian.
- Cambiar la categoría (la cambia la escuela, [CU08](../../CU08%20-%20Gestionar%20estudiantes/UT-BACKEND/README.md)).
- Edad mínima: CU06 ya la valida al cambiar la fecha de nacimiento, y no cambia.
- Pantallas.

## Resultado esperado

La persona ve y edita sus datos nuevos y el servidor mantiene la dirección al día.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el stack levantado y un token: `PATCH /api/v1/usuarios/me` con nuevas coordenadas devuelve la dirección recalculada; con un CI de otra cuenta, 409 `ERR_IDE_19`; con `categoriaSigla` en el cuerpo, la categoría sigue igual.
