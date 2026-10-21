# UT-CU06-BE-11 — `fotoUrl` en login y renovación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-11` |
| **Nombre** | `fotoUrl` en login y renovación |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La barra superior de la web y la app tienen que mostrar la foto de la persona apenas inicia sesión, y mantenerla fresca mientras la sesión se renueva.

## Objetivo

La respuesta de `login` y `refresh` incluye `usuario.fotoUrl` (URL prefirmada o `null`).

## Alcance

- `domain/port/in/SesionEmitida`: suma el dato de la foto (la URL ya firmada) y los servicios `IniciarSesionService` y `RenovarSesionService` lo llenan con `AlmacenFotosPort.urlDeLectura(clave)` si la cuenta tiene foto (`null` si no). Si firmar falla, `fotoUrl = null` y un `WARN`: **un problema de MinIO nunca impide iniciar sesión ni renovarla**.
- `infrastructure/adapter/in/web/SesionResponse.UsuarioSesionResponse`: campo nuevo `String fotoUrl` (se omite si es `null`; la clase ya usa `NON_NULL`) y su armado en `AuthController`.
- Tests: login y renovación con foto devuelven la URL; sin foto, el campo no viaja; si `AlmacenFotosPort` falla, el login responde `200` sin `fotoUrl`; los tests existentes de CU02 y CU04b siguen pasando.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/SesionEmitida.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/IniciarSesionService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/RenovarSesionService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/SesionResponse.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthController.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/IniciarSesionServiceTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/RenovarSesionServiceTest.java`

## Fuera de alcance (no tocar)

- El perfil y su endpoint (UT 06 y 12).
- El contenido del JWT (no lleva la foto).
- Cambios de comportamiento del login (bloqueos, plataformas, sesiones).

## Resultado esperado

El cliente recibe la URL de la foto al iniciar y al renovar la sesión, sin depender de una llamada extra.

## Validación

1. `./mvnw test` en `identidad` pasa, incluidos los tests de login y renovación.
2. Manual: con una cuenta con foto, `POST /api/v1/auth/login` devuelve `usuario.fotoUrl` y la URL se abre en el navegador.
3. `./mvnw checkstyle:check` sin errores.
