# Unidades de trabajo — CU09-BE

**Caso de uso:** Gestionar instructores (Administrador de escuela): listar los instructores de su escuela con búsqueda por nombre y filtro de estado, ver el detalle, darlos de alta con su cuenta y su ficha profesional, y editar su ficha (licencia, contratación, categorías, estado) y su ubicación.
**Microservicio(s):** `identidad` (UT 01 a 06), `escuela` (UT 07 a 12) y `gateway` (UT 13) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU09 - Gestionar instructores`](../../../CasosDeUso/CU09%20-%20Gestionar%20instructores.md)
**Recurso HTTP / contrato:** `GET`, `POST` `/api/v1/instructores` y `GET`, `PATCH` `/api/v1/instructores/{codigo}` (Escuela, solo `ADMIN_ESCUELA`); y, solo entre servicios, `/api/v1/usuarios/instructores/**` (Identidad, solo `ADMIN_ESCUELA`)

## Antes de implementar cualquier UT

**Requiere CU08a y CU08 completos** ([CU08a-BE](../../CU08a%20-%20Perfil%20de%20la%20persona/UT-BACKEND/README.md), [CU08-BE](../../CU08%20-%20Gestionar%20estudiantes/UT-BACKEND/README.md)): el perfil, la geocodificación, `PerfilArmador` (URL prefirmada de la foto), `AltaEstudiante`, la consulta por escuela con `unaccent`, el catálogo de categorías y el cliente entre servicios con Bearer reenviado. También se apoya en el patrón de saga de [CU05](../../CU05%20-%20Gestionar%20escuelas/UT-BACKEND/README.md) (Escuela le pide cosas a Identidad y compensa).

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (Identidad), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (Escuela), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

A diferencia de CU08, un instructor vive en **dos servicios**: la persona (cuenta y perfil) en Identidad y la ficha profesional en Escuela. Casi todo lo difícil de este CU sale de eso.

1. **El recurso `instructores` lo atiende Escuela** (así está en [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) y es la dueña de la tabla `instructor`). La web solo habla con ese recurso; Escuela **orquesta** y le pide a Identidad lo que es de la persona. El gateway ya enruta `/api/v1/instructores` a Escuela y `/api/v1/usuarios/**` a Identidad: no hace falta una ruta nueva.
2. **Identidad suma endpoints internos** bajo `/api/v1/usuarios/instructores` (crear la cuenta, consultar personas por lote, buscar por nombre, cambiar la ubicación, revertir). Exigen `ADMIN_ESCUELA` y toman la escuela **de la cuenta de quien llama**, nunca de un parámetro (igual que CU08). Escuela les **reenvía el Bearer** del Administrador: Identidad vuelve a validar el token y el rol (patrón de CU05 y CU08).
3. **Escuela toma la escuela del claim `escuela` del JWT**, sin llamar a Identidad. Un argumento `@EscuelaActual` lo resuelve; sin claim (Super admin) responde 403.
4. **La ficha guarda `escuela_codigo`** (migración nueva): así se aísla por escuela. `nro_licencia` sigue `UNIQUE` en toda la plataforma. La tabla ya no tiene `ci` (CU08a).
5. **El alta es una saga simple** y su orden hace inofensivos casi todos los fallos: validar lo propio (licencia única, categorías, fechas) → **Identidad crea la cuenta** → **Escuela guarda la ficha** en una transacción → si eso falla, **se revierte la cuenta** (idempotente) y se relanza el error. A diferencia de CU05 (donde Escuela guarda primero en una transacción abierta), aquí la cuenta va primero porque la ficha referencia a la persona. **El código de la persona (`usuarioCodigo`, UUID v7) lo genera Escuela y se lo manda a Identidad al crear la cuenta**: así puede revertirla aunque la respuesta se pierda (un timeout no deja una cuenta que Escuela no sabe cómo identificar). Es el mismo recurso de CU05, donde Escuela genera el código de la escuela y se lo pasa a Identidad.
6. **La reversión es segura**: Identidad solo borra una cuenta `INSTRUCTOR` de la escuela de quien llama y que **nunca inició sesión**; si ya la usó, `409 ERR_IDE_23`. Si ni la compensación funciona, queda un `ERROR` en el registro; la cuenta sobrante no aparece en ningún listado (manda la ficha) pero su correo y su CI quedan ocupados hasta limpiarla. Es una limitación aceptada.
7. **El listado lo manda la ficha (Escuela)**: filtro de estado, orden del alta más reciente a la más antigua y paginación son de Escuela. **Búsqueda por nombre**: Escuela le pide a Identidad los códigos que coinciden (páginas de 100, hasta 5: 500 coincidencias como tope) y filtra su tabla por `usuario_codigo`. Sin búsqueda no se le pide nada para filtrar. Después, **una sola consulta por lote** (hasta 100 códigos) trae nombre, foto y demás datos de las filas de la página. Una persona que Identidad no devuelve deja sus campos en `null` y se registra un `WARN`; no rompe el listado.
8. **La edición** valida todo en Escuela antes de tocar nada (licencia única, categorías, fechas). Si llegan coordenadas, **Identidad actualiza la ubicación** (y recalcula la dirección) y **después** Escuela guarda la ficha. Si guardar la ficha fallara, la ubicación nueva queda: es inofensivo y el reintento lo repite sin problema. No hay compensación.
9. **Reglas de la ficha en el dominio de Escuela** (`Instructor`): número de licencia sin espacios y en mayúsculas, de 3 a 30 caracteres `[A-Z0-9-]`; vencimiento posterior a hoy al dar de alta y solo si cambia al editar; contratación no futura (hoy por defecto); una o más categorías distintas y que no sean `SIN`. Las fechas se calculan con un `Clock` inyectado (Escuela todavía no tiene el bean).
10. **Estados** `ACTIVO`, `LICENCIA` e `INACTIVO` (el enum `EstadoInstructor` ya existe). Ninguno bloquea el inicio de sesión en esta versión.
11. **Nuevos códigos de error.** Escuela: `ERR_ESC_08` (número de licencia en uso, 409), `ERR_ESC_09` (el instructor no existe en la escuela, 404) y `ERR_ESC_10` (categoría inválida, 400). Identidad: `ERR_IDE_22` (el instructor no existe en la escuela, 404) y `ERR_IDE_23` (la cuenta ya se usó y no se revierte, 409). Los errores de Identidad `ERR_IDE_01`, `ERR_IDE_02` y `ERR_IDE_19` llegan al cliente con su **mismo código y mensaje**, traducidos por el cliente de Escuela (como en CU05).
12. **Foto de la persona**: la arma Identidad con la URL prefirmada de lectura (`PerfilArmador`), igual que en estudiantes.
13. **Servicio caído**: si Identidad no responde al crear, consultar o buscar, el cliente recibe el `500` genérico, sin crear nada a medias (la compensación cubre el alta). Mismo criterio que CU08 con Escuela.

## Contrato de datos / recurso HTTP

Escuela (todos exigen `ADMIN_ESCUELA`; `401` sin sesión y `403` sin el rol):

| Endpoint | Cuerpo / query | Respuesta |
|----------|----------------|-----------|
| `GET /api/v1/instructores` | `page`, `pageSize`, `search`, `filter[estado]=ACTIVO\|LICENCIA\|INACTIVO` | `200` `data`: `[{ codigo, nombreCompleto, fotoUrl, nroLicencia, licenciaVence, categorias: [{ sigla, descripcion }], estado }]` + `meta` |
| `GET /api/v1/instructores/{codigo}` | — | `200` `data`: `{ codigo, nombres, apellidos, nombreCompleto, fotoUrl, edad, fechaNacimiento, sexo, ci, email, telefono, direccion, latitud, longitud, nroLicencia, licenciaVence, fechaContratacion, categorias: [{ sigla, descripcion }], estado, fechaAlta }`; `404 ERR_ESC_09` |
| `POST /api/v1/instructores` | `{ nombres, apellidos, fechaNacimiento, sexo, ci, telefono?, email, password, latitud, longitud, nroLicencia, licenciaVence, fechaContratacion?, categorias: [sigla] }` | `201` `data`: el detalle del instructor creado; `409 ERR_IDE_01` correo en uso; `409 ERR_IDE_19` CI en uso; `409 ERR_ESC_08` licencia en uso; `400 ERR_IDE_02` menor de edad; `400 ERR_ESC_10` categoría inválida; `400 ERR_SYS_01` datos inválidos |
| `PATCH /api/v1/instructores/{codigo}` | `{ nroLicencia?, licenciaVence?, fechaContratacion?, categorias?, estado?, latitud?, longitud? }` (al menos uno; las coordenadas juntas) | `200` `data`: el detalle actualizado; `404 ERR_ESC_09`; `409 ERR_ESC_08`; `400 ERR_ESC_10`; `400 ERR_SYS_01` |

Identidad, **solo entre servicios** (todos exigen `ADMIN_ESCUELA`):

| Endpoint | Cuerpo / query | Respuesta |
|----------|----------------|-----------|
| `POST /api/v1/usuarios/instructores` | `{ codigo, nombres, apellidos, fechaNacimiento, sexo, ci, telefono?, email, password, latitud, longitud }` (`codigo`: el UUID v7 que Escuela eligió para la persona) | `201` `data`: la persona (abajo); `409 ERR_IDE_01`, `409 ERR_IDE_19`, `400 ERR_IDE_02`, `400 ERR_SYS_01` (también si el `codigo` ya existe) |
| `POST /api/v1/usuarios/instructores/consulta` | `{ codigos: [uuid] }` (1 a 100) | `200` `data`: `[persona]` de los que existen, son instructores y son de la escuela de quien llama (sin orden garantizado) |
| `GET /api/v1/usuarios/instructores` | `page`, `pageSize`, `search` | `200` `data`: `[persona]` de la escuela, del alta más reciente a la más antigua + `meta` |
| `PATCH /api/v1/usuarios/instructores/{codigo}/ubicacion` | `{ latitud, longitud }` | `200` `data`: la persona; `404 ERR_IDE_22`; `400 ERR_SYS_01` |
| `DELETE /api/v1/usuarios/instructores/{codigo}` | — | `200` `data: null` (idempotente: si no hay nada que borrar, también); `409 ERR_IDE_23` |

**persona:** `{ codigo, nombres, apellidos, nombreCompleto, fotoUrl, edad, fechaNacimiento, sexo, ci, email, telefono, direccion, latitud, longitud, fechaAlta }` (fechas `yyyy-MM-dd`; nunca `passwordHash`, roles ni `escuelaCodigo`).

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU09-BE-01](./UT-CU09-BE-01.md) | Alta de cuenta parametrizada por rol y por código | `identidad` | — |
| [UT-CU09-BE-02](./UT-CU09-BE-02.md) | Consulta de instructores de una escuela (puerto y persistencia) | `identidad` | — |
| [UT-CU09-BE-03](./UT-CU09-BE-03.md) | Crear y revertir la cuenta de un instructor | `identidad` | 01, 02 |
| [UT-CU09-BE-04](./UT-CU09-BE-04.md) | Consultar y buscar personas instructor | `identidad` | 02, 03 |
| [UT-CU09-BE-05](./UT-CU09-BE-05.md) | Editar la ubicación de un instructor | `identidad` | 02, 03 |
| [UT-CU09-BE-06](./UT-CU09-BE-06.md) | Endpoints internos de instructores en Identidad | `identidad` | 03, 04, 05 |
| [UT-CU09-BE-07](./UT-CU09-BE-07.md) | Ficha del instructor: migración, dominio y persistencia | `escuela` | — |
| [UT-CU09-BE-08](./UT-CU09-BE-08.md) | Cliente de Escuela hacia Identidad para instructores | `escuela` | 06 |
| [UT-CU09-BE-09](./UT-CU09-BE-09.md) | Listar y obtener instructores | `escuela` | 07, 08 |
| [UT-CU09-BE-10](./UT-CU09-BE-10.md) | Crear un instructor (saga con compensación) | `escuela` | 07, 08, 09 |
| [UT-CU09-BE-11](./UT-CU09-BE-11.md) | Editar un instructor | `escuela` | 07, 08, 09 |
| [UT-CU09-BE-12](./UT-CU09-BE-12.md) | Endpoints de instructores en Escuela | `escuela` | 09, 10, 11 |
| [UT-CU09-BE-13](./UT-CU09-BE-13.md) | Pruebas de enrutamiento y autenticación en el gateway | `gateway` | 12 |

**Cadena:** `01, 02 → 03 → 04, 05 → 06`; `07` y `08` (esta después de 06) → `09 → 10, 11 → 12 → 13`. Las dos ramas iniciales (Identidad `01, 02` y Escuela `07`) son independientes.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listado paginado, más reciente primero, con foto y categorías | 02, 04, 07, 08, 09, 12 |
| Flujo principal pasos 3 y 4, CA02, CA03 | Búsqueda por nombre y filtro de estado | 02, 04, 07, 09, 12 |
| A1, CA04 | Listado vacío sin error | 09, 12 |
| CA05 | Solo instructores de su escuela | 02, 03, 04, 05, 07, 09, 10, 11 |
| F1, A2, CA06 | Detalle y `ERR_ESC_09` | 07, 08, 09, 12 |
| F2, A3, A4, CA07, CA08 | Alta y errores de la persona | 01, 03, 06, 08, 10, 12 |
| A5, CA09 | Número de licencia en uso | 07, 10, 11 |
| A6, CA10 | Categoría inválida | 10, 11 |
| A3, CA11 | Licencia vencida, contratación futura, sin categorías | 10, 11 |
| A8, CA12 | Alta sin dejar nada a medias | 03, 10 |
| F3, A7, CA13 | Edición de la ficha y la ubicación solamente | 05, 06, 08, 11, 12 |
| A9, CA14 | Solo Administrador (401 y 403) | 03 a 06, 09 a 13 |
| A10, CA16 | Rate limit | ya cubierto por el gateway; sin UT nueva |
| CA17 | Nada sensible en los registros | 03, 05, 08, 10, 11 |
| CA18 | Estudiantes e instructores no se mezclan | 02, 04 |

CA15 es de frontend.

## Follow-ups (no se implementan en estas UT)

- Que `INACTIVO` o `LICENCIA` bloqueen el inicio de sesión, la asignación de autos o alumnos y los horarios (lo decide cada CU que los use).
- Avisar de licencias por vencer o vencidas, y buscar por número de licencia.
- Una ficha para la cuenta de desarrollo `instructorexample@gmail.com`: hoy existe como cuenta pero no tiene ficha, así que no aparece en el listado.
- Limpieza automática de cuentas sobrantes cuando falló la compensación.
- Más de 500 coincidencias en una búsqueda por nombre (hoy se corta en 500).

## Qué NO incluyen estas UT

- El frontend ([UT-FRONTEND](../UT-FRONTEND/README.md)) ni el mobile: el Administrador usa solo la web.
- Que el Administrador cambie nombres, CI, sexo, fecha de nacimiento, teléfono o correo de un instructor.
- Asignar autos o estudiantes (CU10 y siguientes) ni los horarios (CU12).
- Borrar a un instructor: se da de baja con el estado `INACTIVO`.

## Commits sugeridos

Un commit `[BACK]` por servicio: `identidad` (UT 01 a 06), `escuela` (UT 07 a 12) y `gateway` (UT 13). `docs/` es `[DOCS]` y `.claude/` es `[REPO]`.
