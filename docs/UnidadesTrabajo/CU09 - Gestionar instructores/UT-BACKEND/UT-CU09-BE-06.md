# UT-CU09-BE-06 — Endpoints internos de instructores en Identidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-06` |
| **Nombre** | Endpoints internos de instructores en Identidad |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU09-BE-03, UT-CU09-BE-04, UT-CU09-BE-05 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `EstudianteController` y `EstudiantesIntegrationTest` de CU08.

## Solicitud de negocio

Escuela le pide a Identidad, por la red interna y con el token del Administrador, todo lo que es de la persona del instructor.

## Objetivo

Existen en Identidad los cinco endpoints de `/api/v1/usuarios/instructores`, solo para el Administrador de escuela y aislados por su escuela.

## Alcance

- `infrastructure/adapter/in/web/InstructorCuentaController` (`/api/v1/usuarios/instructores`), delgado: arma el comando, llama al puerto de entrada y envuelve en `ApiResponse`. El Administrador sale de `@CurrentUserId`; nunca de la URL ni del cuerpo. Cinco operaciones según el [contrato](./README.md#contrato-de-datos--recurso-http): `POST` (201, "Cuenta de instructor creada correctamente"), `POST /consulta`, `GET` (con `page`, `pageSize` y `search`, más `meta`), `PATCH /{codigo}/ubicacion` y `DELETE /{codigo}` (200 con `data: null`).
- Requests y responses (records): `CrearCuentaInstructorRequest` (`codigo` obligatorio y un UUID; mismas reglas de validación que `CrearEstudianteRequest` en lo demás, sin `categoriaSigla`), `ConsultarInstructoresRequest` (`codigos`: de 1 a 100, `@NotEmpty` y `@Size`), `EditarUbicacionRequest` (`latitud` y `longitud` obligatorias y en rango) y `PersonaInstructorResponse` (el contrato de la persona: nunca `passwordHash`, roles ni `escuelaCodigo`).
- `SecurityConfig` **no** suma `permitAll`: `POST /api/v1/usuarios` (registro) sigue siendo la única ruta pública de `usuarios`; estas exigen token y el rol lo decide el servicio.
- `@WebMvcTest` (importando `SecurityConfig` y `WebMvcConfig`, casos de uso simulados): sin token → 401 en cada endpoint; con sesión → el sobre estándar (y `meta` en el `GET`); un `codigo` que no es UUID → 400; `codigos` vacío o con más de 100 → 400 `ERR_SYS_01` sin llamar al caso de uso; campos inválidos del `POST` → 400 sin llamar al caso de uso; `ERR_IDE_01`, `ERR_IDE_19`, `ERR_IDE_02`, `ERR_IDE_22` y `ERR_IDE_23` llegan con su HTTP; el cuerpo no contiene `passwordHash`; un `rol` o una `escuelaCodigo` en el cuerpo del `POST` se ignoran.
- Prueba de integración de punta a punta (`InstructoresCuentaIntegrationTest`, Postgres real y servicios reales): **aquí se ejerce el rol y el aislamiento**. `ESTUDIANTE`, `INSTRUCTOR` y `SUPER_ADMIN` → 403 en los cinco endpoints; el Administrador de A crea una cuenta que luego inicia sesión desde la app móvil, consulta por lote solo a los de A (un código de B y el de un estudiante no vuelven), busca por nombre sin tildes y ve solo a los de A, edita la ubicación (y recibe `ERR_IDE_22` con un instructor de B); revierte una cuenta sin sesión (y la segunda vez también da 200), recibe `ERR_IDE_23` con una que ya inició sesión, y el estudiante de A no se puede revertir por esta vía.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/` (`InstructorCuentaController`, `CrearCuentaInstructorRequest`, `ConsultarInstructoresRequest`, `EditarUbicacionRequest`, `PersonaInstructorResponse`)
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/InstructorCuentaControllerTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/InstructoresCuentaIntegrationTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio, `try/catch` de excepciones de negocio y logging en el controller.
- La ruta del gateway: `usuarios/**` ya llega a Identidad.
- Endpoints para estudiantes, administradores o la ficha profesional.
- Documentar estos endpoints para la web: son solo entre servicios (el frontend usa `/api/v1/instructores`).

## Resultado esperado

Escuela puede crear, consultar, buscar, reubicar y revertir cuentas de instructores en Identidad con el token del Administrador, aislado por escuela.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el stack levantado y el token de `adminexample@gmail.com` (directo a Identidad, `:8081`): `POST /api/v1/usuarios/instructores` crea una cuenta (con un `codigo` elegido) y la persona queda con ese código; `GET /api/v1/usuarios/instructores?search=...` la encuentra; `DELETE` la revierte; con el token del Super admin, 403.
