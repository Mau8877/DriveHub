# UT-CU05-BE-05 — Endpoints de administradores de escuela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-05` |
| **Nombre** | Endpoints de administradores de escuela |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU05-BE-03, UT-CU05-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Escuela necesita pedirle a Identidad, por la red interna, que cree y que revierta al Administrador.

## Objetivo

Existen `POST` y `DELETE /api/v1/usuarios/administradores-escuela`, solo para el Super admin.

## Alcance

- `UsuarioController` (recurso `usuarios`) suma `POST /api/v1/usuarios/administradores-escuela` → `201`, `data`: `AdministradorEscuelaResponse` (`codigo`, `email`, `nombres`, `apellidos`, `escuelaCodigo`); y `DELETE /api/v1/usuarios/administradores-escuela?escuelaCodigo={uuid}` → `200`, `data: null`, mensaje "Administrador revertido correctamente" (también cuando no había nada que revertir).
- `CrearAdministradorEscuelaRequest`: mismas validaciones que `RegistrarEstudianteRequest` (obligatorios, correo válido, contraseña de 9 caracteres como mínimo, longitudes) más `escuelaCodigo` (`@NotNull`). La contraseña no se devuelve nunca.
- `SecurityConfig` **no** suma `permitAll`: ambos endpoints exigen token y la autorización por rol queda en los casos de uso (UT 03 y 04).
- `GlobalExceptionHandler`: un parámetro de la URL que falta o no es un UUID (`MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`) responde 400 `ERR_SYS_01` y no 500.
- `@WebMvcTest` (importando `SecurityConfig`, casos de uso simulados): sin token → 401; con sesión → 201 y el sobre estándar sin contraseña; campos inválidos → 400 `ERR_SYS_01` sin llamar al caso de uso; `ERR_IDE_01`, `ERR_IDE_02`, `ERR_IDE_12` y `ERR_IDE_13` llegan con su HTTP; `DELETE` sin `escuelaCodigo` o con uno que no es UUID → 400.
- Prueba de integración con los servicios reales (`AdministradorEscuelaIntegrationTest`, Postgres real): **aquí se ejerce el rol**, porque el `@PreAuthorize` vive en el caso de uso y un caso de uso simulado no lo ejecuta: sin token → 401; `ADMIN_ESCUELA` y `ESTUDIANTE` → 403 y no se crea nada; `SUPER_ADMIN` crea, revierte (idempotente) y recibe `ERR_IDE_13` si el Administrador ya inició sesión.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/UsuarioController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/CrearAdministradorEscuelaRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/AdministradorEscuelaResponse.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/UsuarioControllerTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/AdministradorEscuelaIntegrationTest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/GlobalExceptionHandler.java`

## Fuera de alcance (no tocar)

- Lógica de negocio, `try/catch` de excepciones de negocio y logging en el controller.
- La ruta del gateway: `usuarios` ya está registrado y `POST /api/v1/usuarios` (registro) sigue siendo la única ruta pública; esta es otra ruta y exige token.
- Quien consume estos endpoints (UT 07).

## Resultado esperado

Identidad expone los dos endpoints protegidos para crear y revertir al Administrador.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el stack levantado y el token de `superadmin@gmail.com`: `POST /api/v1/usuarios/administradores-escuela` crea la cuenta (`ERR_IDE_01` si se repite el correo) y `DELETE ...?escuelaCodigo=` la borra; con el token de `adminexample@gmail.com` ambos dan 403.
