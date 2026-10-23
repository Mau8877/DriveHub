# UT-CU08a-BE-09 — Alta de escuela con el perfil del Administrador (Escuela)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-09` |
| **Nombre** | Alta de escuela con el perfil del Administrador (Escuela) |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU08a-BE-06 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `CrearEscuelaUseCase`, `CrearEscuelaRequest` y `AdministradorEscuelaRestClient` (CU05).

## Solicitud de negocio

Al dar de alta una escuela, el Super admin tiene que indicar también el CI, el sexo y la ubicación del Administrador.

## Objetivo

`POST /api/v1/escuelas` recibe y reenvía a Identidad los datos nuevos del Administrador.

## Alcance

- `CrearEscuelaRequest.AdministradorRequest` suma `ci`, `sexo` (texto `MASCULINO`/`FEMENINO`, validado con `@Pattern` para no depender del enum de Identidad), `latitud` y `longitud` (obligatorios, mismas validaciones y rangos que Identidad). `DatosAdministrador` (`domain/gateway/`) suma los mismos cuatro campos; `CrearEscuelaInput` los lleva.
- `AdministradorEscuelaRestClient`: el cuerpo de `POST .../administradores-escuela` los incluye (`PeticionCrear`). El `toString` de la petición sigue sin mostrar la contraseña y ahora tampoco el CI ni las coordenadas.
- `ERR_IDE_19` (CI en uso) llega al formulario con su mismo código, como ya pasa con `ERR_IDE_01` y `ERR_IDE_02`: el cliente REST ya traduce los 409 de negocio; comprobarlo con una prueba. Si el alta falla, la compensación de CU05 sigue siendo la misma.
- Pruebas: `@WebMvcTest` (campos obligatorios; sexo inválido y coordenadas fuera de rango → 400 `ERR_SYS_01`); el cliente con un servidor falso (el cuerpo trae los campos nuevos; un 409 `ERR_IDE_19` se relanza como `ConflictException` con ese código); el caso de uso (el input llega completo al gateway); las pruebas existentes del alta se ajustan.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/CrearEscuelaRequest.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/gateway/DatosAdministrador.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/client/AdministradorEscuelaRestClient.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/EscuelaGestionController.java` (solo el armado del input)
- Tests de controller, cliente y caso de uso

## Fuera de alcance (no tocar)

- Identidad (UT 06) y la pantalla (el esquema web de escuelas es [UT-CU08a-FE-05](../UT-FRONTEND/UT-CU08a-FE-05.md)).
- Editar al Administrador después de creado.
- La saga de creación y compensación: no cambia.

## Resultado esperado

Una escuela se crea con su Administrador completo, perfil incluido.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
2. Con el stack levantado y el token de `superadmin@gmail.com`: `POST /api/v1/escuelas` con los datos nuevos del Administrador responde 201 y su perfil existe en Identidad; repetir el CI responde 409 `ERR_IDE_19` y no queda la escuela.
