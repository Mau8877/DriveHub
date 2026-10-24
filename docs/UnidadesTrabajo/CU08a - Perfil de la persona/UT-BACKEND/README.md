# Unidades de trabajo — CU08a-BE

**Caso de uso:** Perfil de la persona: separar `perfil` de `usuario` en Identidad, sumar CI, sexo y ubicación (con la dirección calculada por el servidor), eliminar la tabla de estudiantes de Escuela y cargar el catálogo de categorías de licencia.
**Microservicio(s):** `identidad` (UT 01 a 07) y `escuela` (UT 08 y 09) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU08a - Perfil de la persona`](../../../CasosDeUso/CU08a%20-%20Perfil%20de%20la%20persona.md)
**Recurso HTTP / contrato:** `POST /api/v1/usuarios` (registro), `GET` y `PATCH /api/v1/usuarios/me` (perfil propio), `POST /api/v1/usuarios/administradores-escuela` (Identidad) y `POST /api/v1/escuelas` (Escuela). Ningún recurso nuevo; cambian los cuerpos y las respuestas.

## Antes de implementar cualquier UT

**Requiere CU01, CU02, CU05 y CU06 implementados**: este CU los modifica. Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones Flyway, id Long + `codigo` UUID v7, y que todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **`perfil` vive en Identidad, junto a `usuario`**, 1 a 1 (`perfil.usuario_id` único, con FK porque es la misma base). Las cinco columnas que salen de `usuario` (`nombres`, `apellidos`, `telefono`, `foto_url`, `fecha_nacimiento`) pasan a `perfil`. Como toda tabla del proyecto, `perfil` lleva su `id` Long interno y su `codigo_perfil` UUID v7, pero la persona sigue identificándose por `codigo_usuario`: el `codigo_perfil` no sale del servicio.
2. **Columnas de `perfil`:** `nombres`, `apellidos`, `telefono`, `foto_url`, `fecha_nacimiento` (las de antes), más `ci` (`VARCHAR(15) NOT NULL UNIQUE`), `sexo` (`VARCHAR(10) NOT NULL`, `CHECK IN ('MASCULINO','FEMENINO')`), `direccion` (`VARCHAR(300)`), `latitud` y `longitud` (`NUMERIC(9,6)`, con `CHECK` de que van juntas o ninguna y dentro de rango) y `categoria_sigla` (`VARCHAR(3)`, referencia por código a `escuela.categoria_licencia.sigla`, sin FK). Más `created_at`, `updated_at` y `deleted_at`.
3. **`NOT NULL` y el Super admin.** `ci` y `sexo` son `NOT NULL` en la base para todos. `direccion`, `latitud`, `longitud` y `categoria_sigla` son nulables en la base (el Super admin no tiene ubicación; solo el estudiante tiene categoría) y el caso de uso las **exige** al crear un Administrador, un Instructor o un Estudiante. La categoría del estudiante nace en `SIN`.
4. **La dirección la calcula el servidor** con geocodificación inversa (OpenStreetMap Nominatim) detrás de un puerto de salida; el cliente solo manda latitud y longitud. Si el servicio no responde, se guarda el punto con la dirección en blanco: el registro no falla por un servicio de mapas. El servidor público de Nominatim tiene una política de uso (1 consulta por segundo, `User-Agent` identificable): sirve para desarrollo y poco volumen; una instancia propia es un follow-up.
5. **Datos existentes.** Es un entorno de desarrollo, y `ci` y `sexo` son `NOT NULL`: la migración rellena las cuentas existentes con `ci = 'X' + id con ceros` (único), `sexo = 'MASCULINO'`, `direccion = 'X'` y, para los estudiantes, `categoria_sigla = 'SIN'`. Los seeders se actualizan con datos reales de ejemplo.
6. **Dominio:** `Usuario` pasa a contener un `Perfil` (nuevo modelo de dominio). `PerfilUsuario` (el objeto que arma `PerfilArmador` para CU06) conserva su nombre: es la vista del perfil para el cliente, no el modelo. La persistencia guarda y lee `usuario` y `perfil` siempre juntos, en una transacción.
7. **El estudiante deja de existir como tabla en Escuela.** Se eliminan `estudiante` y su Model/entidad; `asignacion_alumno` pasa a guardar `estudiante_codigo` (el `codigo_usuario` de la persona, sin FK, como el resto de las referencias entre servicios); `instructor.ci` se quita porque pasa a `perfil`. Ninguna de esas tablas tiene casos de uso hechos: el costo es solo de migración. El saldo y los pagos viven en Pagos y referencian a la persona por su código.
8. **Catálogo de categorías de licencia** sembrado por migración en Escuela: `P`, `M`, `A`, `B`, `C` y `SIN` ("sin categoría"). **Hay que confirmar con el equipo la lista, las descripciones y las edades mínimas** antes de implementar: la UT las deja con las descripciones propuestas y `edad_minima` y `clases_minimas` en `NULL`.
9. **Nuevo código de error:** `ERR_IDE_19` (el CI ya está registrado, 409).

## Contrato de datos / recurso HTTP

Cambian así (todo lo demás de cada endpoint queda igual):

| Endpoint | Cambio |
|----------|--------|
| `POST /api/v1/usuarios` (registro) | El cuerpo suma `ci`, `sexo`, `latitud` y `longitud` (obligatorios). `409 ERR_IDE_19` si el CI ya existe. Sin `direccion`: la calcula el servidor |
| `GET /api/v1/usuarios/me` | La respuesta suma `ci`, `sexo`, `direccion`, `latitud`, `longitud` y `categoriaSigla` (esta última `null` si no es estudiante) |
| `PATCH /api/v1/usuarios/me` | El cuerpo acepta además `ci`, `sexo` y el par `latitud` + `longitud` (se recalcula la dirección). **No** acepta `categoriaSigla` ni `direccion`. `409 ERR_IDE_19` |
| `POST /api/v1/usuarios/administradores-escuela` | El cuerpo suma `ci`, `sexo`, `latitud` y `longitud` (obligatorios) |
| `POST /api/v1/escuelas` | `administrador` suma `ci`, `sexo`, `latitud` y `longitud` (obligatorios) |

Valores: `sexo` ∈ `MASCULINO` \| `FEMENINO`; `ci` de 5 a 15 caracteres `[A-Za-z0-9-]`, guardado en mayúsculas y sin espacios; `latitud` ∈ [-90, 90] y `longitud` ∈ [-180, 180].

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU08a-BE-01](./UT-CU08a-BE-01.md) | Tabla `perfil` y migración de datos | `identidad` | — |
| [UT-CU08a-BE-02](./UT-CU08a-BE-02.md) | Dominio y persistencia de `Perfil` dentro de `Usuario` | `identidad` | 01 |
| [UT-CU08a-BE-03](./UT-CU08a-BE-03.md) | Geocodificación inversa de la ubicación | `identidad` | — |
| [UT-CU08a-BE-04](./UT-CU08a-BE-04.md) | Registro del estudiante con su perfil | `identidad` | 02, 03 |
| [UT-CU08a-BE-05](./UT-CU08a-BE-05.md) | Perfil propio con CI, sexo y ubicación | `identidad` | 02, 03 |
| [UT-CU08a-BE-06](./UT-CU08a-BE-06.md) | Alta del Administrador con su perfil (Identidad) | `identidad` | 02, 03 |
| [UT-CU08a-BE-07](./UT-CU08a-BE-07.md) | Datos de ejemplo de Identidad con perfil | `identidad` | 02 |
| [UT-CU08a-BE-08](./UT-CU08a-BE-08.md) | Escuela sin tabla de estudiantes y catálogo de categorías | `escuela` | — |
| [UT-CU08a-BE-09](./UT-CU08a-BE-09.md) | Alta de escuela con el perfil del Administrador (Escuela) | `escuela` | 06 |

**Cadena:** `01 → 02`; `02 + 03 → 04, 05, 06`; `02 → 07`; `06 → 09`; la `08` y la `03` son independientes. La 02 es la grande y la más delicada: hasta que no pase toda la suite de Identidad, no se sigue.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Registro con perfil y categoría `SIN` | 01, 02, 04 |
| F1, A4, CA03, CA04 | Ubicación y dirección calculada, con o sin geocodificador | 03, 04, 05, 06 |
| F2, CA06 | Editar perfil (todo menos la categoría) | 05 |
| F3, CA07, CA08 | Alta del Administrador con perfil; sin ubicación para el Super admin | 06, 07, 09 |
| A1, CA05 | Validaciones de CI, sexo y coordenadas | 04, 05, 06 |
| A2, CA02 | CI duplicado (`ERR_IDE_19`) | 01, 04, 05, 06 |
| CA09 | Conservar los datos existentes | 01, 02 |
| CA10 | Catálogo de categorías | 08 |
| CA11 | Rate limit | ya cubierto por el gateway; sin UT nueva |
| CA12 | Nada sensible en los registros | 03, 04, 05, 06 |

## Follow-ups (no se implementan en estas UT)

- Una instancia propia de Nominatim (o un proveedor de pago) para producción.
- Acotar las coordenadas a Bolivia.
- Que `perfil` guarde el CI del instructor al crearse (CU09) y su categoría de licencia (`instructor_categoria` ya existe en Escuela).
- Reintentar el cálculo de la dirección cuando quedó en blanco.

## Qué NO incluyen estas UT

- El frontend ([UT-FRONTEND](../UT-FRONTEND/README.md)) ni el mobile ([UT-MOBILE](../UT-MOBILE/README.md)).
- Listar, ver, crear o editar estudiantes desde el Administrador, ni el endpoint de categorías: es [CU08](../../CU08%20-%20Gestionar%20estudiantes/UT-BACKEND/README.md).
- Cambios en el gateway: ningún recurso cambia de ruta.
- Cualquier cosa de Pagos o de Académico.

## Commits sugeridos

Un commit `[BACK]` por servicio: `identidad` (UT 01 a 07) y `escuela` (UT 08 y 09). `docs/` es `[DOCS]` y `.claude/` es `[REPO]`.
