# UT-CU09-BE-12 — Endpoints de instructores en Escuela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-12` |
| **Nombre** | Endpoints de instructores en Escuela |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU09-BE-09, UT-CU09-BE-10, UT-CU09-BE-11 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `EscuelaGestionController` y `CategoriaLicenciaController`, y en Identidad `CurrentUserIdArgumentResolver`.

## Solicitud de negocio

La web pide los instructores al recurso `instructores`, sin saber que sus datos viven en dos servicios.

## Objetivo

Existen `GET` y `POST /api/v1/instructores` y `GET` y `PATCH /api/v1/instructores/{codigo}` en Escuela, solo para el Administrador de escuela y aislados por su escuela.

## Alcance

- Argumento `@EscuelaActual UUID` (anotación + `HandlerMethodArgumentResolver` registrado en una configuración MVC): devuelve el claim **`escuela`** del JWT de la petición como `UUID`. Sin sesión con JWT o sin ese claim (el Super admin no lo tiene) lanza `ForbiddenException` `ERR_SYS_05`; un claim que no es un UUID también. Nunca se toma la escuela de la URL ni del cuerpo.
- `presentation/controller/InstructorController` (`/api/v1/instructores`), delgado: arma el input, llama al caso de uso y envuelve en `ApiResponse` + `PageMeta`. `GET` con `page`, `pageSize`, `search` y `filter[estado]` (`EstadoInstructor`; un valor distinto → 400 `ERR_SYS_01`), "Instructores obtenidos correctamente"; `GET /{codigo}` ("Instructor obtenido correctamente"); `POST` → `201`, "Instructor creado correctamente"; `PATCH /{codigo}` → `200`, "Instructor actualizado correctamente".
- Requests y responses (records) en `presentation/controller/`: `CrearInstructorRequest` (nombres y apellidos obligatorios hasta 80; `fechaNacimiento` `@Past`; `sexo`; `ci` de 5 a 15 `[A-Za-z0-9-]`; `telefono` hasta 20; `email` válido hasta 120; `password` de 9 a 64; `latitud` y `longitud` obligatorias y en rango; `nroLicencia` obligatorio de 3 a 30; `licenciaVence` obligatorio; `fechaContratacion` opcional; `categorias` con al menos un elemento de hasta 3 caracteres), `EditarInstructorRequest` (solo `nroLicencia`, `licenciaVence`, `fechaContratacion`, `categorias`, `estado`, `latitud`, `longitud`: **cualquier otro nombre del cuerpo se ignora**), `InstructorResumenResponse` e `InstructorDetalleResponse` según el [contrato](./README.md#contrato-de-datos--recurso-http) (nunca el `usuarioCodigo`, el `escuelaCodigo` ni un id interno).
- `SecurityConfig` **no** suma `permitAll`: exigen token y el rol lo decide el caso de uso.
- `@WebMvcTest` (importando `SecurityConfig` y la configuración MVC, casos de uso simulados): sin token → 401; con sesión → el sobre estándar y `meta`; la escuela del claim llega al caso de uso; sin claim → 403; `pageSize` y `filter%5Bestado%5D` llegan; `filter[estado]=XYZ` y un `codigo` que no es UUID → 400; campos inválidos del `POST` → 400 `ERR_SYS_01` sin llamar al caso de uso; `ERR_IDE_01`, `ERR_IDE_19`, `ERR_IDE_02`, `ERR_ESC_12`, `ERR_ESC_13` y `ERR_ESC_14` llegan con su HTTP; el `PATCH` con `ci`, `email` o `nombres` en el cuerpo no los pasa al input; el cuerpo no contiene `password`, `usuarioCodigo` ni `escuelaCodigo`.
- Prueba de integración de punta a punta (`InstructoresIntegrationTest`, Postgres real, servicios reales, `InstructorCuentaGateway` simulado): **aquí se ejerce el rol y el aislamiento**. `ESTUDIANTE`, `INSTRUCTOR` y `SUPER_ADMIN` → 403; el Administrador de A lista solo a los de A (aunque B tenga más) y recibe 404 `ERR_ESC_13` al pedir o editar uno de B; crea un instructor (la ficha queda con su escuela y la cuenta se pidió con el código guardado); si guardar la ficha falla, se revierte la cuenta; una licencia repetida responde 409 sin llamar a Identidad; lista con búsqueda por nombre (Identidad simulada devuelve los códigos) y con filtro de estado.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/` (`InstructorController`, `CrearInstructorRequest`, `EditarInstructorRequest`, `InstructorResumenResponse`, `InstructorDetalleResponse`, `EscuelaActual`, `EscuelaActualArgumentResolver`)
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/config/WebMvcConfig.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/presentation/controller/InstructorControllerTest.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/InstructoresIntegrationTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio, `try/catch` de excepciones de negocio y logging en el controller.
- La ruta del gateway (UT 13).
- `DELETE` ni cambios de estado fuera del `PATCH`.
- Los endpoints de Identidad (UT 06).

## Resultado esperado

El Administrador de escuela gestiona sus instructores por `/api/v1/instructores`, aislado por escuela.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
2. Con el stack levantado y el token de `adminexample@gmail.com` (directo a Escuela, `:8082`): `POST /api/v1/instructores` crea uno; `GET /api/v1/instructores?search=...&filter%5Bestado%5D=ACTIVO` lo lista con su foto y categorías; `PATCH` cambia su estado; con el token del Super admin, 403.
