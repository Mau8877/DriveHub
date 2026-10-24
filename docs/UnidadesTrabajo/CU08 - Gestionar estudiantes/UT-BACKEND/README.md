# Unidades de trabajo — CU08-BE

**Caso de uso:** Gestionar estudiantes (Administrador de escuela): listar los estudiantes de su escuela con búsqueda por nombre y filtro de estado, ver el detalle, darlos de alta con su cuenta y editar solo su categoría y su ubicación.
**Microservicio(s):** `escuela` (UT 01), `identidad` (UT 02 a 07) y `gateway` (UT 08) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU08 - Gestionar estudiantes`](../../../CasosDeUso/CU08%20-%20Gestionar%20estudiantes.md)
**Recurso HTTP / contrato:** `GET`, `POST` `/api/v1/estudiantes`, `GET`, `PATCH` `/api/v1/estudiantes/{codigo}` (Identidad, solo `ADMIN_ESCUELA`) y `GET /api/v1/categorias-licencia[/{sigla}]` (Escuela, solo `ADMIN_ESCUELA`)

## Antes de implementar cualquier UT

**Requiere CU08a completo** ([CU08a-BE](../../CU08a%20-%20Perfil%20de%20la%20persona/UT-BACKEND/README.md)): la tabla `perfil`, el catálogo de categorías sembrado, la geocodificación y `ERR_IDE_19`. Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (Identidad), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (Escuela), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El recurso `estudiantes` lo atiende Identidad.** Con CU08a un estudiante es una cuenta más su perfil, y Escuela ya no tiene tabla de estudiantes: todos los datos están en Identidad. El gateway enruta `/api/v1/estudiantes` a Identidad (UT 08) y `MICROSERVICIOS.md` mueve el recurso. Lo demás de la escuela (vehículos, instructores, categorías, asignaciones) sigue en Escuela. Cuando exista Pagos, el CU anota la decisión de quién junta el saldo.
2. **Identidad toma la escuela de quien llama, nunca del cliente.** Cada servicio recibe el `codigo` del Administrador (`@CurrentUserId`), carga su cuenta y usa su `escuelaCodigo`. Un estudiante de otra escuela responde igual que uno inexistente (`ERR_IDE_20`).
3. **La categoría es del catálogo de Escuela.** Identidad no tiene el catálogo: para validar la que elige el Administrador le pregunta a Escuela por REST, reenviando el Bearer (patrón de CU05, en sentido contrario). Escuela expone `GET /api/v1/categorias-licencia` (para llenar la lista en la web) y `GET /api/v1/categorias-licencia/{sigla}` (para validar). Si Escuela no responde, el alta o la edición de categoría fallan con el 500 genérico; el listado y el detalle no dependen de Escuela.
4. **El Administrador solo cambia la categoría y la ubicación.** La edición (`PATCH`) acepta únicamente esos campos; cualquier otro nombre en el cuerpo se ignora. Los datos personales son del propio estudiante (CU06, CU08a).
5. **El alta reutiliza la regla del registro.** Para no duplicar validaciones (edad, correo, CI, geocodificación, hash, rol), se extrae de `RegistrarEstudianteService` un componente de aplicación `AltaEstudiante` que usan el registro y el alta por el Administrador. La diferencia: el Administrador elige la categoría (por defecto `SIN`) y la escuela sale de su cuenta, no de un slug.
6. **Búsqueda sin tildes ni mayúsculas con `unaccent`**, habilitado con una migración nueva de Identidad. La búsqueda parte el texto en palabras y exige que **cada** una aparezca en `nombres || ' ' || apellidos` del perfil, así "juan perez" encuentra a "Juan Carlos Pérez".
7. **Estado público `ACTIVO | INACTIVO`.** `BLOQUEADO` (bloqueo temporal por intentos fallidos) cuenta como `ACTIVO`: el filtro `ACTIVO` incluye ambos y el estado que se devuelve nunca es `BLOQUEADO`.
8. **Cálculos en el caso de uso:** `edad` (años cumplidos con el `Clock` inyectado, que Identidad ya tiene), `nombreCompleto` y `fechaIncorporacion` (la fecha de alta de la cuenta). Orden fijo: alta más reciente primero (`created_at DESC`, desempate por `id DESC`); sin parámetro `sort` (ENDPOINTS_BACKEND.md no lo define).
9. **Sin Pagos.** Ningún campo, parámetro ni columna de saldo: el contrato queda como está abajo y se amplía cuando exista Pagos.
10. **Nuevos códigos de error:** `ERR_IDE_20` (el estudiante no existe en la escuela de quien consulta, 404), `ERR_IDE_21` (la categoría no existe, 400) y `ERR_ESC_07` (la categoría no existe, 404, en Escuela).

## Contrato de datos / recurso HTTP

Identidad (todos exigen `ADMIN_ESCUELA`):

| Endpoint | Cuerpo / query | Respuesta |
|----------|----------------|-----------|
| `GET /api/v1/estudiantes` | `page`, `pageSize`, `search`, `filter[estado]=ACTIVO\|INACTIVO` | `200` `data`: `[{ codigo, nombreCompleto, fotoUrl, edad, categoriaSigla, estado, fechaIncorporacion }]` + `meta` |
| `GET /api/v1/estudiantes/{codigo}` | — | `200` `data`: `{ codigo, nombres, apellidos, nombreCompleto, fotoUrl, edad, fechaNacimiento, sexo, ci, email, telefono, direccion, latitud, longitud, categoriaSigla, estado, fechaIncorporacion }`; `404 ERR_IDE_20` |
| `POST /api/v1/estudiantes` | `{ nombres, apellidos, fechaNacimiento, sexo, ci, telefono?, email, password, latitud, longitud, categoriaSigla? }` | `201` `data`: el detalle del estudiante creado; `409 ERR_IDE_01` correo en uso; `409 ERR_IDE_19` CI en uso; `400 ERR_IDE_02` menor de edad; `400 ERR_IDE_21` categoría inexistente; `400 ERR_SYS_01` datos inválidos |
| `PATCH /api/v1/estudiantes/{codigo}` | `{ categoriaSigla?, latitud?, longitud? }` (al menos uno; las coordenadas juntas) | `200` `data`: el detalle actualizado; `404 ERR_IDE_20`; `400 ERR_IDE_21`; `400 ERR_SYS_01` |

`fechaNacimiento` y `fechaIncorporacion` viajan como `yyyy-MM-dd`. `fotoUrl` es la URL prefirmada de lectura de la foto de perfil (vale 60 minutos) o `null` si no tiene foto. El detalle nunca incluye `passwordHash` ni roles.

Escuela (todos exigen `ADMIN_ESCUELA`):

| Endpoint | Respuesta |
|----------|-----------|
| `GET /api/v1/categorias-licencia` (`page`, `pageSize`) | `200` `data`: `[{ sigla, descripcion }]` en el orden del catálogo + `meta` |
| `GET /api/v1/categorias-licencia/{sigla}` | `200` `data`: `{ sigla, descripcion }`; `404 ERR_ESC_07` |

Todos responden `401` sin sesión y `403` sin el rol.

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU08-BE-01](./UT-CU08-BE-01.md) | Consulta de categorías de licencia | `escuela` | — |
| [UT-CU08-BE-02](./UT-CU08-BE-02.md) | Cliente de Escuela para validar categorías | `identidad` | 01 |
| [UT-CU08-BE-03](./UT-CU08-BE-03.md) | Consulta de estudiantes de una escuela (puerto y persistencia) | `identidad` | — |
| [UT-CU08-BE-04](./UT-CU08-BE-04.md) | Casos de uso de listar y obtener estudiantes | `identidad` | 03 |
| [UT-CU08-BE-05](./UT-CU08-BE-05.md) | Alta de un estudiante por el Administrador | `identidad` | 02, 03 |
| [UT-CU08-BE-06](./UT-CU08-BE-06.md) | Edición de categoría y ubicación | `identidad` | 02, 03 |
| [UT-CU08-BE-07](./UT-CU08-BE-07.md) | Endpoints de estudiantes en Identidad | `identidad` | 04, 05, 06 |
| [UT-CU08-BE-08](./UT-CU08-BE-08.md) | Ruta de estudiantes en el gateway | `gateway` | 07 |

**Cadena:** `01 → 02 → 05, 06`; `03 → 04, 05, 06`; `04 + 05 + 06 → 07 → 08`. Las dos ramas iniciales (`01 → 02` y `03`) son independientes.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listado paginado, más reciente primero | 03, 04, 07 |
| Flujo principal pasos 3 y 4, CA02, CA03 | Búsqueda por nombre y filtro de estado | 03, 04, 07 |
| A1, CA04 | Listado vacío sin error | 04, 07 |
| CA05 | Solo estudiantes de su escuela | 03, 04, 05, 06 |
| F1, A2, CA06 | Detalle y `ERR_IDE_20` | 03, 04, 07 |
| F2, A3, A4, CA07, CA08 | Alta con validaciones y errores | 05, 07 |
| A5, CA09 | Categoría inexistente | 01, 02, 05, 06 |
| F3, A6, CA10 | Edición de categoría y ubicación solamente | 06, 07 |
| A7, CA11 | Solo Administrador (401 y 403) | 01, 04 a 08 |
| A8, CA13 | Rate limit | ya cubierto por el gateway; sin UT nueva |
| CA14 | Nada sensible en los registros | 02, 05, 06 |

CA12 es de frontend.

## Follow-ups (no se implementan en estas UT)

- **Pagos** (ver el CU): monto pendiente, filtro por saldo, montos pagados y clases tomadas.
- Activar, desactivar o reenviar la verificación de correo de un estudiante desde la pantalla.
- Filtro por categoría en el listado.
- Parámetro `sort` en los listados, cuando ENDPOINTS_BACKEND.md lo defina.

## Qué NO incluyen estas UT

- El frontend ([UT-FRONTEND](../UT-FRONTEND/README.md)) ni el mobile: el Administrador de escuela usa solo la web.
- El perfil, el catálogo sembrado, la geocodificación y `ERR_IDE_19`: son de [CU08a](../../CU08a%20-%20Perfil%20de%20la%20persona/UT-BACKEND/README.md).
- Que el Administrador cambie nombres, CI, sexo, fecha de nacimiento, teléfono o correo de un estudiante.
- Cualquier dato o filtro de pagos o de clases tomadas.

## Commits sugeridos

Un commit `[BACK]` por servicio: `escuela` (UT 01), `identidad` (UT 02 a 07) y `gateway` (UT 08). `docs/` es `[DOCS]` y `.claude/` es `[REPO]`.
