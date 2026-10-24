# UT-CU08-BE-07 — Endpoints de estudiantes en Identidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-07` |
| **Nombre** | Endpoints de estudiantes en Identidad |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08-BE-04, UT-CU08-BE-05, UT-CU08-BE-06 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La web pide los estudiantes al recurso `estudiantes`, sin saber qué servicio los guarda.

## Objetivo

Existen `GET` y `POST /api/v1/estudiantes` y `GET` y `PATCH /api/v1/estudiantes/{codigo}` en Identidad, solo para el Administrador de escuela.

## Alcance

- `infrastructure/adapter/in/web/EstudianteController` (`/api/v1/estudiantes`), delgado: arma el comando, llama al puerto de entrada y envuelve en `ApiResponse`. El `codigo` del Administrador sale de `@CurrentUserId`; nunca de la URL ni del cuerpo.
  - `GET` con `page`, `pageSize`, `search` y `filter[estado]` (`EstadoFiltroEstudiante`; un valor distinto → 400 `ERR_SYS_01`). `200`, `data`: lista de `EstudianteResumenResponse` + `meta` (`PageMeta`), "Estudiantes obtenidos correctamente".
  - `GET /{codigo}` → `200`, `data`: `EstudianteDetalleResponse`, "Estudiante obtenido correctamente".
  - `POST` → `201`, `data`: `EstudianteDetalleResponse`, "Estudiante creado correctamente". `CrearEstudianteRequest`: `nombres`, `apellidos` (obligatorios, hasta 80), `fechaNacimiento` (`@Past`), `sexo`, `ci` (reglas de CU08a), `telefono` (opcional, hasta 20), `email` (válido, hasta 120), `password` (9 a 64), `latitud` y `longitud` (obligatorias, en rango) y `categoriaSigla` (opcional, hasta 3).
  - `PATCH /{codigo}` → `200`, `data`: `EstudianteDetalleResponse`, "Estudiante actualizado correctamente". `EditarEstudianteRequest`: solo `categoriaSigla`, `latitud` y `longitud`; **cualquier otro nombre en el cuerpo se ignora**.
- `EstudianteResumenResponse` y `EstudianteDetalleResponse` (records) según el [contrato](./README.md#contrato-de-datos--recurso-http): nunca `passwordHash`, roles ni `escuelaCodigo`.
- `SecurityConfig` **no** suma `permitAll`: exigen token; el rol lo decide el servicio.
- `@WebMvcTest` (importando `SecurityConfig`, casos de uso simulados): sin token → 401; con sesión → el sobre estándar y `meta`; `pageSize` y `filter%5Bestado%5D` llegan al caso de uso; `filter[estado]=XYZ` y un `codigo` que no es UUID → 400; campos inválidos del `POST` → 400 `ERR_SYS_01` sin llamar al caso de uso; `ERR_IDE_01`, `ERR_IDE_19`, `ERR_IDE_02`, `ERR_IDE_20` y `ERR_IDE_21` llegan con su HTTP; el `PATCH` con `ci` en el cuerpo no lo pasa al comando; el cuerpo no contiene `passwordHash`.
- Prueba de integración de punta a punta (Postgres real, servicios reales, `CategoriaLicenciaConsultaPort` simulado): **aquí se ejerce el rol y el aislamiento**. `ESTUDIANTE`, `INSTRUCTOR` y `SUPER_ADMIN` → 403; el `ADMIN_ESCUELA` de A lista solo a los de A (aunque B tenga más) y recibe 404 `ERR_IDE_20` al pedir o editar uno de B; crea un estudiante que luego inicia sesión; lista con búsqueda sin tildes y con filtro de estado.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/EstudianteController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/` (`CrearEstudianteRequest`, `EditarEstudianteRequest`, `EstudianteResumenResponse`, `EstudianteDetalleResponse`)
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/EstudianteControllerTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/EstudiantesIntegrationTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio, `try/catch` de excepciones de negocio y logging en el controller.
- La ruta del gateway (UT 08).
- `DELETE` ni cambios de estado del estudiante.
- Pagos.

## Resultado esperado

El Administrador de escuela gestiona sus estudiantes por `/api/v1/estudiantes`, aislado por escuela.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el stack levantado y el token de `adminexample@gmail.com` (directo a Identidad, `:8081`): `GET /api/v1/estudiantes?search=ana&filter%5Bestado%5D=ACTIVO` devuelve la página con `meta`; `POST` crea un estudiante; `PATCH` cambia su categoría; con el token del Super admin, 403.
