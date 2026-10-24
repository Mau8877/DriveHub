# UT-CU08a-BE-06 — Alta del Administrador con su perfil (Identidad)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-06` |
| **Nombre** | Alta del Administrador con su perfil (Identidad) |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08a-BE-02, UT-CU08a-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `CrearAdministradorEscuelaService` y su request (CU05).

## Solicitud de negocio

El Administrador que se crea junto con la escuela también tiene que tener su CI, su sexo y su ubicación.

## Objetivo

`POST /api/v1/usuarios/administradores-escuela` crea la cuenta y el perfil completo del Administrador.

## Alcance

- `CrearAdministradorEscuelaRequest` y `CrearAdministradorEscuelaComando` suman `ci`, `sexo`, `latitud` y `longitud` (obligatorios, mismas validaciones que el registro). La respuesta (`AdministradorEscuelaResponse`) no cambia.
- `CrearAdministradorEscuelaService`: normaliza el CI, comprueba `existePorCi` → `ERR_IDE_19`, calcula la dirección con el geocodificador (vacío = `null`), crea `Usuario` + `Perfil` con `categoriaSigla = null`. Se quitan los valores provisionales de la UT 02. Mantiene la autorización `SUPER_ADMIN` y el resto de las reglas de CU05 (`ERR_IDE_01`, `ERR_IDE_02`, `ERR_IDE_12`).
- `RevertirAdministradorEscuelaService` ya borra la cuenta definitivamente; comprobar (UT 02) que se lleva el perfil y que el CI queda libre para repetir el alta.
- Pruebas: servicio (CI repetido → `ERR_IDE_19` y no se crea nada; la categoría queda `null`; dirección calculada o `null`); `@WebMvcTest` (campos nuevos obligatorios, 400 si faltan); la prueba de integración `AdministradorEscuelaIntegrationTest` se actualiza con los campos nuevos y suma: revertir y volver a crear con el mismo CI funciona.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/CrearAdministradorEscuelaRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CrearAdministradorEscuelaComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/CrearAdministradorEscuelaService.java`
- Los tests de servicio, controller e integración correspondientes
- `docs/errors/identidad/ERRORES_IDENTIDAD.md` (nuevo lugar donde se lanza `ERR_IDE_19`)

## Fuera de alcance (no tocar)

- Quien llama a este endpoint (Escuela, UT 09).
- Cambiar o editar al Administrador después de creado.
- Reglas de edad, correo y unicidad de la escuela, que no cambian.

## Resultado esperado

Identidad crea al Administrador con su perfil completo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el token de `superadmin@gmail.com`: `POST /api/v1/usuarios/administradores-escuela` con los campos nuevos responde 201 y en Postgres hay un `perfil` con `categoria_sigla` nula.
