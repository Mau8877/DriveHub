# Unidades de trabajo — CU05-BE

**Caso de uso:** Gestionar escuelas (Super admin): crear una escuela con su Administrador, listarlas, editar su nombre y desactivarlas o reactivarlas.
**Microservicio(s):** `escuela` (UT 01, 02 y 07 a 12), `identidad` (UT 03 a 06) y `gateway` (UT 13) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU05 - Gestionar escuelas`](../../../CasosDeUso/CU05%20-%20Gestionar%20escuelas.md)
**Recurso HTTP / contrato:** `POST`, `GET`, `PATCH`, `DELETE /api/v1/escuelas` y `POST /api/v1/escuelas/{codigo}/reactivar` (Escuela, solo `SUPER_ADMIN`); `POST` y `DELETE /api/v1/usuarios/administradores-escuela` (Identidad, solo `SUPER_ADMIN`, uso interno de Escuela)

## Antes de implementar cualquier UT

**Requiere CU01 y CU02 implementados** (el JWT con roles, la consulta de la escuela por Identidad y el login que ya la comprueba). Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (Escuela), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (Identidad), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El borrado lógico (`deleted_at`) es la única fuente del estado de la escuela.** Se quita la columna `estado`; `ACTIVA`/`INACTIVA` queda como valor derivado para el listado y el filtro. Como el `@SQLRestriction` del `BaseModel` esconde las filas eliminadas, todo lo que necesita verlas (listado, consulta por código, unicidad del slug, desactivar, reactivar) usa consultas nativas.
2. **Desactivar no toca a Identidad ni a las cuentas.** Identidad ya comprueba la escuela al iniciar y al renovar sesión, así que **todas** las cuentas de una escuela inactiva (Administrador, Instructores y Estudiantes) quedan sin acceso, y lo recuperan al reactivarla. Sin eventos ni escrituras entre servicios. Solo se ajusta el texto del rechazo (UT 06). Una sesión ya abierta sigue válida hasta que vence su token (15 min) o intenta renovarse.
3. **Crear escuela y Administrador es una saga simplificada y sin estado, orquestada por Escuela.** La rule de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) limita la saga formal (estado persistido, outbox, eventos) a CU44, y esa infraestructura no existe. En su lugar, el orden de los pasos hace inofensivos los fallos:
   - Escuela genera el `codigo` de la escuela, guarda la fila en una transacción **todavía abierta** y le pide a Identidad crear al Administrador con ese código.
   - Si Identidad rechaza (correo en uso, menor de edad...), la transacción se deshace: no hay nada que compensar.
   - Si Identidad no responde, vence o hay un fallo al confirmar, el resultado es incierto: Escuela llama a `DELETE .../administradores-escuela?escuelaCodigo=` (idempotente) y relanza el error.
   - Si la compensación también falla, la cuenta sobrante no puede iniciar sesión (su escuela no existe) y queda un `ERROR` en el log para limpiarla.
4. **Revertir al Administrador es un borrado físico, a propósito y acotado.** El correo es `UNIQUE` incluso con borrado lógico, así que un `deleted_at` impediría repetir la creación con el mismo correo. Solo aplica a una cuenta `ADMIN_ESCUELA` que nunca inició sesión (`ERR_IDE_13` si ya se usó).
5. **Escuela llama a Identidad reenviando el token del Super admin.** Identidad vuelve a validar el JWT y el rol (`@PreAuthorize` en el caso de uso). Los errores de negocio de Identidad (`ERR_IDE_01`, `ERR_IDE_02`...) llegan al formulario con su mismo código; una falla de infraestructura cae en el `500` genérico, como dice EXCEPCIONES_BACKEND.md.
6. **El Administrador lleva `fechaNacimiento`** (la columna es `NOT NULL` y el diseño no la relaja) y se le aplica la misma edad mínima del registro. La contraseña inicial la escribe el Super admin.
7. **Dependencia circular entre servicios, aceptada:** Identidad ya consulta a Escuela (CU01, CU02) y ahora Escuela llama a Identidad (CU05). Queda anotada en MICROSERVICIOS.md (UT 07).
8. **En Clean**, los contratos hacia otros servicios van en `domain/gateway/` y sus implementaciones en `infrastructure/client/` (UT 07 lo agrega a ARQUITECTURA_CLEAN_BACKEND.md).
9. **El slug no se edita** y se reserva incluso para escuelas eliminadas. El nombre es lo único editable.
10. **Nuevos códigos de error:** `ERR_ESC_03` a `ERR_ESC_06` (Escuela) y `ERR_IDE_12`, `ERR_IDE_13` (Identidad). `ERR_IDE_07` conserva su código y cambia su mensaje.

## Contrato de datos / recurso HTTP

Escuela (todos exigen `SUPER_ADMIN`):

| Endpoint | Cuerpo / query | Respuesta |
|----------|----------------|-----------|
| `POST /api/v1/escuelas` | `{ nombre, slug, administrador: { nombres, apellidos, fechaNacimiento, email, password, telefono? } }` | `201` `data`: escuela y administrador creados; `409 ERR_ESC_03` slug en uso; `409 ERR_IDE_01` correo en uso; `400 ERR_IDE_02` menor de edad; `400 ERR_SYS_01` datos inválidos |
| `GET /api/v1/escuelas` | `page`, `pageSize`, `search`, `filter[estado]=ACTIVA\|INACTIVA` | `200` `data`: `[{ codigo, slug, nombre, estado, creadoEn }]` + `meta` |
| `PATCH /api/v1/escuelas/{codigo}` | `{ nombre }` | `200` `data`: la escuela; `404 ERR_ESC_02`; `409 ERR_ESC_06` inactiva; `400 ERR_SYS_01` |
| `DELETE /api/v1/escuelas/{codigo}` | — | `200` `data: null`; `404 ERR_ESC_02`; `409 ERR_ESC_04` ya inactiva |
| `POST /api/v1/escuelas/{codigo}/reactivar` | — | `200` `data`: la escuela; `404 ERR_ESC_02`; `409 ERR_ESC_05` ya activa |

Identidad (uso interno de Escuela, exigen `SUPER_ADMIN`):

| Endpoint | Cuerpo / query | Respuesta |
|----------|----------------|-----------|
| `POST /api/v1/usuarios/administradores-escuela` | `{ nombres, apellidos, fechaNacimiento, email, password, telefono?, escuelaCodigo }` | `201` `data`: `{ codigo, email, nombres, apellidos, escuelaCodigo }`; `409 ERR_IDE_01`; `409 ERR_IDE_12` la escuela ya tiene Administrador; `400 ERR_IDE_02` |
| `DELETE /api/v1/usuarios/administradores-escuela?escuelaCodigo=` | — | `200` `data: null` (también si no había nada); `409 ERR_IDE_13` ya inició sesión |

Todos responden `401` sin sesión y `403` sin el rol. La consulta pública `GET /api/v1/escuelas/{slug}` y la interna `GET /api/v1/escuelas/por-codigo/{codigo}` **no cambian**.

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU05-BE-01](./UT-CU05-BE-01.md) | Borrado lógico como único estado de la escuela | `escuela` | — |
| [UT-CU05-BE-02](./UT-CU05-BE-02.md) | Repositorio de escuelas para gestionarlas | `escuela` | 01 |
| [UT-CU05-BE-03](./UT-CU05-BE-03.md) | `CrearAdministradorEscuelaService` | `identidad` | — |
| [UT-CU05-BE-04](./UT-CU05-BE-04.md) | `RevertirAdministradorEscuelaService` | `identidad` | 03 |
| [UT-CU05-BE-05](./UT-CU05-BE-05.md) | Endpoints de administradores de escuela | `identidad` | 03, 04 |
| [UT-CU05-BE-06](./UT-CU05-BE-06.md) | Mensaje de escuela inactiva al iniciar y renovar sesión | `identidad` | — |
| [UT-CU05-BE-07](./UT-CU05-BE-07.md) | Cliente de Identidad para el Administrador de la escuela | `escuela` | 05 |
| [UT-CU05-BE-08](./UT-CU05-BE-08.md) | `CrearEscuelaUseCase` (escuela y Administrador) | `escuela` | 02, 07 |
| [UT-CU05-BE-09](./UT-CU05-BE-09.md) | `ListarEscuelasUseCase` | `escuela` | 02 |
| [UT-CU05-BE-10](./UT-CU05-BE-10.md) | `EditarEscuelaUseCase` | `escuela` | 02 |
| [UT-CU05-BE-11](./UT-CU05-BE-11.md) | `DesactivarEscuelaUseCase` y `ReactivarEscuelaUseCase` | `escuela` | 02 |
| [UT-CU05-BE-12](./UT-CU05-BE-12.md) | Endpoints de gestión de escuelas | `escuela` | 08, 09, 10, 11 |
| [UT-CU05-BE-13](./UT-CU05-BE-13.md) | Pruebas de seguridad de las rutas de gestión en el gateway | `gateway` | 12 |

**Cadena:** `01 → 02 → 09, 10, 11`; `03 → 04 → 05 → 07`; `02 + 07 → 08`; `08 + 09 + 10 + 11 → 12 → 13`. La 06 es independiente (conviene hacerla después de la 01 para probarla de punta a punta).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listado paginado con búsqueda y filtro | 02, 09, 12 |
| F1, CA02 | Crear la escuela y su Administrador | 02, 03, 05, 07, 08, 12 |
| A2, CA03 | Slug en uso | 02, 08, 12 |
| A3, A4, CA04, CA05 | Correo en uso y menor de edad | 03, 05, 07, 08, 12 |
| A5, CA07 | Fallo a medias y compensación | 04, 05, 07, 08 |
| F2, A6, A8, CA08 | Editar el nombre | 10, 12 |
| F3, F4, CA09, CA10 | Desactivar y reactivar; efecto en el login | 01, 06, 11, 12 |
| A7, A9, A10, CA11 | Inexistente, ya inactiva, ya activa | 10, 11, 12 |
| A11, CA12 | Solo Super admin (401 y 403) | 03, 04, 05, 08 a 13 |
| CA14 | Nada sensible en los registros | 03, 07, 08 |
| CA15 | Rate limit | ya cubierto por el gateway; sin UT nueva |

CA06 y CA13 son de frontend.

## Follow-ups (no se implementan en estas UT)

- Limpiar a mano o con una herramienta las cuentas de Administrador huérfanas cuando una compensación falla.
- Cambiar o reemplazar al Administrador de una escuela, y recuperar la contraseña inicial.
- Invitar al Administrador por correo con un enlace para que él elija su contraseña, en lugar de que la escriba el Super admin.
- Cortar las sesiones abiertas de una escuela al desactivarla (hoy duran hasta 15 minutos).
- Migrar la creación a una saga formal con outbox cuando exista esa infraestructura.

## Qué NO incluyen estas UT

- El frontend ([UT-FRONTEND](../UT-FRONTEND/README.md)) ni el mobile: el Super admin usa solo la web.
- Crear Instructores (CU01_2) ni gestionar roles y permisos.
- Cualquier cambio de los endpoints públicos de consulta de la escuela (por slug y por código).

## Commits sugeridos

Un commit `[BACK]` por servicio: `escuela` (UT 01, 02 y 07 a 12), `identidad` (UT 03 a 06) y `gateway` (UT 13). `docs/` es `[DOCS]` y `.claude/` es `[REPO]`.
