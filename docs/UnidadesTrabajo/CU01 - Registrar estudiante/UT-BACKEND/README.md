# Unidades de trabajo — CU01-BE

**Caso de uso:** Registro del estudiante en la plataforma, asociado a una escuela, con rol `ESTUDIANTE` y sin inicio de sesión automático. Incluye la tabla tenant `escuela` y los seeders de desarrollo.
**Microservicio(s):** `escuela` (UT 01 a 05), `gateway` (UT 06), `identidad` (UT 07 a 15) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU01 - Registrar estudiante`](../../../CasosDeUso/CU01%20-%20Registrar%20estudiante.md)
**Recurso HTTP / contrato:** `POST /api/v1/usuarios` (público, Identidad) y `GET /api/v1/escuelas/{slug}` (público, Escuela)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/):

- Todas: [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).
- `escuela`: [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md).
- `identidad`: [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- `gateway`: [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md).

Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **`escuela` es la tabla tenant y vive en el servicio Escuela.** Identidad guarda `usuario.escuela_codigo` (UUID público, sin FK física). Una cuenta pertenece a una sola escuela; el Super admin tiene `escuela_codigo = NULL`.
2. **El estudiante elige escuela por `slug`** (en la web, el enlace `/register/{slug}`; en la app, un campo "código de escuela"). Identidad resuelve el `slug` llamando por REST interno a Escuela (`GET /api/v1/escuelas/{slug}`, URL de Docker, sin pasar por el gateway) y guarda el `codigo` que recibe. Si la escuela no existe o está inactiva, Escuela responde 404 y Identidad responde `ERR_IDE_03`.
3. **El endpoint de Escuela es público** (no hay sesión en el registro). Devuelve solo `codigo`, `slug` y `nombre`. Una escuela inactiva responde 404, igual que una inexistente (CA08).
4. **Registro = `POST /api/v1/usuarios`** (ruta `usuarios` que ya enruta el gateway, público según SEGURIDAD_AUTH_BACKEND.md). La petición **no tiene campo de rol**: el servicio asigna siempre `ESTUDIANTE` (CA09).
5. **Edad mínima y correo:** `app.registro.edad-minima=18` (configurable). El correo se normaliza (`trim` + minúsculas) antes de comprobar unicidad; el `UNIQUE` de la BD cubre la carrera entre dos registros simultáneos.
6. **Contraseña:** mínimo 9 y máximo 64 caracteres (BCrypt corta en 72 bytes). Se guarda con BCrypt detrás de un puerto de salida.
7. **Roles:** la migración renombra `ADMIN` a `ADMIN_ESCUELA` y agrega `SUPER_ADMIN`; los 4 roles quedan como constantes de texto (`RolNombres`).
8. **Seeders solo en perfil `dev`** (los contenedores ya corren con `SPRING_PROFILES_ACTIVE=dev`), idempotentes y con la contraseña `123123123`. La escuela de ejemplo y el administrador comparten un `escuela_codigo` fijo `0198f0a0-0000-7000-8000-000000000001` para quedar enlazados sin llamarse entre servicios. Los seeders **no** son parte del cuerpo del CU: son UT de apoyo para poder probarlo.
9. **Fuera de este CU:** el filtro automático por escuela, `escuela_codigo` en las demás tablas, el perfil `estudiante` (CU08) y el evento `UsuarioRegistrado`.

## Contrato de datos / recurso HTTP

### `GET /api/v1/escuelas/{slug}` (Escuela, público)

| Respuesta | `data` |
|-----------|--------|
| `200` | `{ "codigo": "uuid", "slug": "escuelaexample", "nombre": "Escuela Example" }` |
| `404` | `ERR_ESC_01` |

### `POST /api/v1/usuarios` (Identidad, público)

| Campo | Tipo | Regla |
|-------|------|-------|
| `nombres` | string | obligatorio, máx. 80 |
| `apellidos` | string | obligatorio, máx. 80 |
| `fechaNacimiento` | `yyyy-MM-dd` | obligatoria, en el pasado |
| `email` | string | obligatorio, formato de correo, máx. 120 |
| `password` | string | obligatoria, 9 a 64 caracteres |
| `telefono` | string | opcional, máx. 20 |
| `escuelaSlug` | string | obligatorio |

| Respuesta | `data` |
|-----------|--------|
| `201` | `{ "codigo", "email", "nombres", "apellidos" }` (sin contraseña ni sesión) |
| `400` | `ERR_SYS_01` (validación del cuerpo) o `ERR_IDE_02` (menor de edad) |
| `404` | `ERR_IDE_03` (escuela inexistente o inactiva) |
| `409` | `ERR_IDE_01` (correo en uso) |
| `429` | `ERR_SYS_02` (rate limit del gateway) |

### Códigos de error nuevos

| Código | Servicio | HTTP | Excepción |
|--------|----------|------|-----------|
| `ERR_ESC_01` | Escuela | 404 | `NotFoundException` |
| `ERR_IDE_01` | Identidad | 409 | `ConflictException` |
| `ERR_IDE_02` | Identidad | 400 | `ValidationException` |
| `ERR_IDE_03` | Identidad | 404 | `NotFoundException` |

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU01-BE-01](./UT-CU01-BE-01.md) | Tabla `escuela`: migración, entidad y Model | `escuela` | — |
| [UT-CU01-BE-02](./UT-CU01-BE-02.md) | Repositorio de escuelas | `escuela` | 01 |
| [UT-CU01-BE-03](./UT-CU01-BE-03.md) | Caso de uso `ObtenerEscuelaPorSlug` y `ERR_ESC_01` | `escuela` | 02 |
| [UT-CU01-BE-04](./UT-CU01-BE-04.md) | Endpoint público `GET /api/v1/escuelas/{slug}` | `escuela` | 03 |
| [UT-CU01-BE-05](./UT-CU01-BE-05.md) | Seeder dev de `escuelaexample` | `escuela` | 02 |
| [UT-CU01-BE-06](./UT-CU01-BE-06.md) | Ruta `escuelas` en el gateway | `gateway` | 04 |
| [UT-CU01-BE-07](./UT-CU01-BE-07.md) | Migración de `usuario` y roles | `identidad` | — |
| [UT-CU01-BE-08](./UT-CU01-BE-08.md) | Dominio y Model: fecha de nacimiento, escuela y `RolNombres` | `identidad` | 07 |
| [UT-CU01-BE-09](./UT-CU01-BE-09.md) | Puertos de entrada y salida del registro | `identidad` | 08 |
| [UT-CU01-BE-10](./UT-CU01-BE-10.md) | `RegistrarEstudianteService` y `ERR_IDE_01..03` | `identidad` | 09 |
| [UT-CU01-BE-11](./UT-CU01-BE-11.md) | Adaptador de persistencia de usuarios y roles | `identidad` | 09 |
| [UT-CU01-BE-12](./UT-CU01-BE-12.md) | Adaptadores de salida: Escuela por REST y BCrypt | `identidad` | 09 |
| [UT-CU01-BE-13](./UT-CU01-BE-13.md) | Controller `POST /api/v1/usuarios` y seguridad | `identidad` | 09 |
| [UT-CU01-BE-14](./UT-CU01-BE-14.md) | Seeder dev de `superadmin` y `adminexample` | `identidad` | 11, 12 |
| [UT-CU01-BE-15](./UT-CU01-BE-15.md) | Prueba de integración del registro | `identidad` | 10, 11, 12, 13 |

**Cadena:** `01 → 02 → 03 → 04 → 06` y `02 → 05` (Escuela); `07 → 08 → 09 → 10, 11, 12, 13` y `11 + 12 → 14` (Identidad); `10 + 11 + 12 + 13 → 15`. Las dos ramas (Escuela e Identidad) son independientes hasta la prueba de integración, que usa un Escuela falso.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Registro exitoso: cuenta `ACTIVO`, `ESTUDIANTE`, con su escuela, sin sesión ni contraseña | 08, 10, 11, 13, 15 |
| CA03 / A1 | Campos obligatorios o formato de correo inválidos → 400 | 13, 15 |
| CA04 / A2 | Contraseña corta → 400 | 13, 15 |
| CA05, CA06 / A3 | Menor de 18 → `ERR_IDE_02`; exactamente 18 → acepta | 10, 15 |
| CA07 / A5 | Correo en uso (cualquier escuela) → `ERR_IDE_01` | 10, 11, 15 |
| CA08 / A4 | Escuela inexistente o inactiva → 404 | 01 a 04, 10, 12, 15 |
| CA09 | El rol de la petición se ignora; siempre `ESTUDIANTE` | 10, 13, 15 |
| CA10 | Contraseña con BCrypt y fuera de los logs | 10, 12 |
| CA11 | Rate limit → 429 | ya cubierto por el gateway; sin UT nueva |
| Apoyo | Datos para probar: escuela, super admin y admin de ejemplo | 05, 14 |

CA02 (ir al login tras registrarse) es de frontend y mobile.

## Follow-ups (no se implementan en estas UT)

- Filtro automático por `escuela_codigo` en todos los servicios y diseño del claim `escuela` del JWT (antes de CU07 y CU08).
- Crear/editar escuelas y su Administrador (CU05) y crear Instructores (CU01_2).
- Perfil `estudiante` en Escuela (CU08) y, si hace falta, el evento `UsuarioRegistrado` con outbox.
- Verificación de correo junto con el SMTP (CU04).

## Qué NO incluyen estas UT

- Login, JWT, refresh token ni cierre de sesión (CU02, CU03). El `SecurityConfig` solo abre las dos rutas públicas de este CU.
- Un `escuela_codigo` en las tablas de Escuela u otros servicios.
- Alta de usuarios con rol distinto de `ESTUDIANTE` por la API.
- Las tablas `permiso`, `rol_permiso` y la gestión de roles (sin caso de uso asignado).

## Commits sugeridos

Un commit por servicio y por scope ([CONVENCIONES_GIT.md](../../../../.claude/rules/CONVENCIONES_GIT.md)): las UT de `escuela`, `gateway` e `identidad` van en commits `[BACK]` separados; la edición de `MICROSERVICIOS.md` (UT 06) es `[REPO]`, `docker-compose.yml` (UT 12) es `[INFRA]` y `docs/errors/` (UT 03 y 10) es `[DOCS]`.
