# Unidades de trabajo — CU45-MOB

**Caso de uso:** Consultar datos vía GraphQL: la app móvil pide los avisos y las preferencias de notificación por GraphQL, con una bandera para volver a REST.
**Microservicio(s) / micro-frontend(s):** app móvil (Flutter) — consume `notificaciones` por el `gateway`; ver [UT-BACKEND](../UT-BACKEND/README.md)
**Fuente CU:** [`CU45 - Consultar datos vía GraphQL`](../../../CasosDeUso/CU45%20-%20Consultar%20datos%20v%C3%ADa%20GraphQL.md)
**Recurso HTTP / contrato:** `POST /api/v1/graphql` (ver el contrato en el README de UT-BACKEND)

## Antes de implementar cualquier UT

Leer [`.claude/rules/mobile/`](../../../../.claude/rules/mobile/): [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (sección «GraphQL» y «Modo offline»), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) y [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Se migran solo avisos y preferencias** (CU38 y CU37): son lo que el servicio expone por GraphQL. La app no registra el dispositivo push, así que no hay cliente para esas mutaciones. El resto de la app sigue con REST.
2. **Los servicios de GraphQL son subclases de los de REST** (`NotificacionesGraphqlService extends NotificacionesService`, igual con preferencias). Sobrescriben los mismos métodos, así que las pantallas, los providers de datos, los modelos y los tests de pantalla no cambian.
3. **Bandera de vuelta atrás**: `NOTIFICACIONES_TRANSPORTE` en el `.env` (`graphql` por defecto, `rest` para volver) se lee en un provider aparte, que también se puede fijar en los tests.
4. **`ApiClient` no desempaqueta GraphQL.** Hoy desempaqueta cualquier respuesta JSON (devuelve `envelope['data']`): con GraphQL se tragaría los errores (200 con `errors` y `data: null`). Lo marcado con `OpcionesGraphql.clave` pasa tal cual y lo interpreta `GraphqlClient`.
5. **Errores iguales a REST**: `errors[0].extensions.code` → `ApiException` con el mismo `ERR_...`. Los fallos de red se relanzan como `DioException` para que el modo offline y las pantallas los traten igual.
6. **El modo offline guarda las consultas**: una consulta viaja por `POST`, así que la caché (hoy solo de `GET`) se extiende a las marcadas como consulta, con clave ruta + texto + variables. Una mutación nunca se guarda ni se encola. Una respuesta con `errors` no se guarda. Sin esto, pasar a GraphQL habría hecho perder la lectura sin conexión de la bandeja.
7. **Los documentos piden solo los campos que leen los modelos** y viven en `documentos_graphql.dart`. Se comprobó una vez que los seis son válidos contra el esquema real del servicio y que las variables que arma la app se aceptan (incluido el cuerpo exacto de `PreferenciasNotificacion.toJson`).
8. **GraphQL es el transporte por defecto** porque las pruebas pasaron; si el backend no tiene GraphQL encendido, se pone `rest`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU45-MOB-01](./UT-CU45-MOB-01.md) | Cliente GraphQL y caché offline de consultas | UT-CU45-BE-04 |
| [UT-CU45-MOB-02](./UT-CU45-MOB-02.md) | Servicios de avisos y preferencias por GraphQL | 01 |
| [UT-CU45-MOB-03](./UT-CU45-MOB-03.md) | Bandera de transporte y cableado de los providers | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA19 | Mismo comportamiento que con REST, incluidos los códigos de error | 02, 03 |
| CA20 | Un error de negocio con HTTP 200 se muestra como error | 01 |
| CA21 | Sin conexión se ve lo último cargado; una respuesta con errores no se guarda | 01 |
| CA22 | Vuelta a REST con la variable de transporte | 03 |

## Follow-ups (no se implementan en estas UT)

- Que la bandeja pida el contador de no leídos en la misma consulta que la lista, si la pantalla lo necesita junto.
- Registrar el dispositivo push desde la app (`registrarDispositivo`).
- Encolar sin conexión marcar leído y guardar preferencias.
- Una prueba de contrato en CI que valide `documentos_graphql.dart` contra el esquema del backend (hoy se comprobó una vez a mano).

## Qué NO incluyen estas UT

- El panel web: solo registra y quita el dispositivo push y no tiene pantalla de avisos ni de preferencias.
- Cambios en las pantallas, los modelos o los providers de datos.
- Cambios en el backend o en el gateway (son de [UT-BACKEND](../UT-BACKEND/README.md)).
- Borrar los servicios de REST.

## Commits sugeridos

Un commit `[MOBILE]` (UT 01 a 03) y el de `docs/` como `[DOCS]`.
