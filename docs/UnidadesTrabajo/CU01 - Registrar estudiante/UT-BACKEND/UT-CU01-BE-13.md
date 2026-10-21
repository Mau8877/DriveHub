# UT-CU01-BE-13 — Controller `POST /api/v1/usuarios` y seguridad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-13` |
| **Nombre** | Controller `POST /api/v1/usuarios` y seguridad |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-09 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados, `POST` crea), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (endpoints públicos) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (el test verifica la forma del `ApiResponse`).

## Solicitud de negocio

La web y la app registran al estudiante con una sola llamada pública, con validación clara campo por campo.

## Objetivo

`POST /api/v1/usuarios` crea un estudiante y responde `201` con el sobre estándar, sin sesión.

## Alcance

- `infrastructure/adapter/in/web/UsuarioController` (`@RequestMapping("/api/v1/usuarios")`) que depende de `RegistrarEstudianteUseCase` (la interfaz). Un método `POST` con `@Valid @RequestBody RegistrarEstudianteRequest`: arma el comando, llama a `registrar`, responde `201` con `ApiResponse.success(UsuarioRegistradoResponse, "Usuario registrado correctamente")`.
- `RegistrarEstudianteRequest` (`record` con Bean Validation): `nombres` y `apellidos` `@NotBlank @Size(max=80)`; `fechaNacimiento` `@NotNull @Past`; `email` `@NotBlank @Email @Size(max=120)`; `password` `@NotBlank @Size(min=9, max=64)`; `telefono` `@Size(max=20)` opcional; `escuelaSlug` `@NotBlank`. **Sin campo de rol**; una propiedad `rol` en el JSON se ignora.
- `UsuarioRegistradoResponse` (`record`): `codigo`, `email`, `nombres`, `apellidos`. Nunca contraseña, hash ni escuela interna.
- `SecurityConfig`: `requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()`; el resto sigue cerrado.
- `@WebMvcTest` con el puerto mockeado: `201` con la forma del sobre; falta de campo, correo mal formado, contraseña de 8 caracteres y fecha futura → `400` con `ERR_SYS_01`; una excepción `ConflictException`/`ValidationException`/`NotFoundException` del puerto se traduce al status y `error` correctos; un JSON con `"rol": "SUPER_ADMIN"` no cambia el comando enviado al puerto; accesible sin autenticación.

Archivos esperados (crear/tocar):

- `.../infrastructure/adapter/in/web/UsuarioController.java`, `RegistrarEstudianteRequest.java`, `UsuarioRegistradoResponse.java`
- `.../infrastructure/config/SecurityConfig.java`
- `projects/backend/identidad/src/test/java/.../UsuarioControllerTest.java`

## Fuera de alcance (no tocar)

- Listar, obtener, editar o eliminar usuarios; cualquier endpoint de `auth`.
- Las ramas del servicio (ya en UT 10) y los adaptadores de salida (UT 11 y 12).
- Rate limit o CORS (son del gateway).
- Logging en el controller.

## Resultado esperado

Una petición válida a `POST /api/v1/usuarios` devuelve `201` con `data.codigo`; una inválida devuelve `400` con `ERR_SYS_01` y el detalle del campo.

## Validación

1. `./mvnw test` en `identidad` pasa con `UsuarioControllerTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
3. Tras UT 15, el flujo completo funciona de punta a punta.
