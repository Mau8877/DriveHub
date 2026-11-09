# Unidades de trabajo — CU45-BE

**Caso de uso:** Consultar datos vía GraphQL: ofrecer GraphQL sobre los avisos, las preferencias y los dispositivos push de la persona, al lado de las rutas REST de siempre.
**Microservicio(s) / micro-frontend(s):** `notificaciones` (NestJS, capas, DynamoDB) + `gateway` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU45 - Consultar datos vía GraphQL`](../../../CasosDeUso/CU45%20-%20Consultar%20datos%20v%C3%ADa%20GraphQL.md)
**Recurso HTTP / contrato:** `POST /api/v1/graphql` (GraphQL, sin sobre estándar)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) (sección «GraphQL»), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **GraphQL se suma a REST, no lo reemplaza.** Los controllers y las rutas de `notificaciones` no se tocan: móvil y web siguen funcionando igual. Lo único existente que se modifica es código compartido (guards, decorador y filtro), y los tests originales de REST son la red de seguridad.
2. **Código primero** (`@nestjs/graphql` 14 + `@nestjs/apollo` 14, Apollo Server 5 sobre Express 5): el esquema se arma en memoria desde los decoradores (`autoSchemaFile: true`), sin generar archivos. Las versiones se eligieron porque declaran soporte para NestJS 12 y TypeScript 6 (verificado en el registro de npm).
3. **Un resolver por feature, que llama al mismo service que el controller.** La lógica, el logging y los `BusinessException` quedan en el service; los resolvers solo traducen entrada y salida.
4. **Los inputs repiten las reglas de su DTO de REST** (no se decora el DTO). Un test compara mensajes con los mismos datos en ambos lados para que no se desalineen.
5. **Quién es la persona sale del token.** Ninguna operación recibe el código de una persona.
6. **Los guards y `@UsuarioActual` leen el request con `requestDe(context)`**, que resuelve el contexto HTTP o el de GraphQL. El `GlobalExceptionFilter` separa «traducir la excepción» de «cómo se responde».
7. **Todo error lleva un `ERR_...` en `extensions.code`.** Apollo reemplaza el código de los errores de validación de la consulta por `GRAPHQL_VALIDATION_FAILED`; `formatError` los pasa a `ERR_SYS_01` y cualquier error sin catalogar a `ERR_SYS_00` (regla estricta de [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)).
8. **GraphQL no usa el sobre estándar** (`{ data, errors }`): excepción documentada en RESPONSES_BACKEND.md. Los errores de negocio responden 200 con el error en el cuerpo; una consulta inválida, 400.
9. **Límites contra el abuso** (el rate limit del gateway cuenta peticiones, no consultas): profundidad 6, 10 campos en la raíz, sin lotes, introspección solo en desarrollo.
10. **Interruptor** `GRAPHQL_ENABLED=false`: quita el módulo y deja el servicio solo con REST.
11. **Se expone solo lo que REST ya expone**: sin listado de dispositivos (no existe en REST) y sin el token de Firebase. Las operaciones de mutación se llaman por lo que hacen (`guardarPreferencias` reemplaza la configuración completa, como el `PUT`).
12. **El contador y «leer todas» devuelven un entero** (`Int`), no el objeto `{ noLeidas }` / `{ marcadas }` de REST: es más cómodo para el cliente y no pierde información.

## Contrato

| Operación | Tipo | Roles | Devuelve |
| --- | --- | --- | --- |
| `notificaciones(page, pageSize, leida, tipo)` | Consulta | `ESTUDIANTE`, `INSTRUCTOR` | `PaginaNotificaciones { items, meta }` |
| `conteoNoLeidas` | Consulta | `ESTUDIANTE`, `INSTRUCTOR` | `Int` |
| `preferencias` | Consulta | `ESTUDIANTE`, `INSTRUCTOR` | `Preferencias` |
| `marcarLeida(codigo)` | Mutación | `ESTUDIANTE`, `INSTRUCTOR` | `Notificacion` |
| `marcarTodasLeidas` | Mutación | `ESTUDIANTE`, `INSTRUCTOR` | `Int` (cuántas) |
| `guardarPreferencias(entrada)` | Mutación | `ESTUDIANTE`, `INSTRUCTOR` | `Preferencias` |
| `registrarDispositivo(entrada)` | Mutación | cualquier sesión válida | `Dispositivo` |
| `quitarDispositivo(codigo)` | Mutación | cualquier sesión válida | `Boolean` |

Errores: `ERR_SYS_04` sin sesión, `ERR_SYS_05` rol no permitido, `ERR_SYS_01` consulta o datos inválidos, `ERR_NOT_02` / `ERR_NOT_03` / `ERR_NOT_04` los de negocio de siempre, `ERR_SYS_00` inesperado. Catálogo: [docs/errors](../../../errors/README.md) (no hay códigos nuevos).

Variables: `GRAPHQL_ENABLED`, `GRAPHQL_INTROSPECTION`, `GRAPHQL_MAX_DEPTH`, `GRAPHQL_MAX_ROOT_FIELDS` (en `.env.example`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU45-BE-01](./UT-CU45-BE-01.md) | Base de GraphQL en `notificaciones` (módulo, interruptor, esquema oculto) | — |
| [UT-CU45-BE-02](./UT-CU45-BE-02.md) | Guards, decorador y filtro de errores para REST y GraphQL | 01 |
| [UT-CU45-BE-03](./UT-CU45-BE-03.md) | Límites de consulta contra el abuso | 01 |
| [UT-CU45-BE-04](./UT-CU45-BE-04.md) | Resolvers de avisos, preferencias y dispositivos | 02, 03 |
| [UT-CU45-BE-05](./UT-CU45-BE-05.md) | Ruta de GraphQL en el gateway y documentación | 04 |

**Cadena:** `01 → 02, 03 → 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Consultar, CA01, CA02 | Bandeja, contador y preferencias en una respuesta | 04 |
| Cambiar, CA03 a CA07 | Marcar leídos, guardar preferencias, dispositivos | 04 |
| A1, CA08, CA09 | Sesión, rol y solo lo propio | 02, 04 |
| A2, A3, CA10, CA11, CA12 | Consulta inválida, límites y lotes | 03 |
| A4 a A7, CA14, CA15, CA16 | Errores catalogados y mismas validaciones que REST | 02, 04 |
| CA13 | Esquema oculto en producción | 01 |
| CA17 | Interruptor de apagado | 01 |
| CA18, A8 | Gateway | 05 |

## Follow-ups (no se implementan en estas UT)

- **Consultas que juntan datos de varios servicios** (detalle de un alumno del instructor: Escuela + Identidad + Pagos + Académico). Es el uso que el diseño original le da a GraphQL; necesita decidir dónde vive el agregador (servicio propio o uno existente) y se documenta en un caso de uso nuevo.
- La app móvil ya consume avisos y preferencias por GraphQL: ver [UT-MOBILE](../UT-MOBILE/README.md). El panel web solo registra y quita el dispositivo push, y también lo hace por GraphQL (`GraphqlClient` del shell).
- Límite de complejidad por costo de campo, si el esquema crece.
- Acceso del Administrador cuando exista un dato suyo que exponer.

## Qué NO incluyen estas UT

- Cambios en los controllers de REST ni en el panel web. La app móvil es de [UT-MOBILE](../UT-MOBILE/README.md).
- Borrar las rutas REST.
- Un servicio nuevo ni cambios en `docker-compose.yml`: GraphQL vive dentro de `notificaciones`.
- Códigos de error nuevos.

## Commits sugeridos

Un commit `[BACK]` para `notificaciones` (UT 01 a 04), otro `[BACK]` para `gateway` (UT 05), y los de `docs/` como `[DOCS]` y de `.claude/` como `[REPO]`.
