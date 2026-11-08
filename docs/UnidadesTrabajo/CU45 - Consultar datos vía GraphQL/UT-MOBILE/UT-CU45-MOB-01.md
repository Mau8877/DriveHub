# UT-CU45-MOB-01 — Cliente GraphQL y caché offline de consultas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-MOB-01` |
| **Nombre** | Cliente GraphQL y caché offline de consultas |
| **Caso de uso** | CU45-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU45-BE-04 (el endpoint existe) |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (cliente único, sobre estándar, modo offline).

## Solicitud de negocio

La app tiene que poder hablar GraphQL sin perder lo que ya hace con REST: errores con su código, sesión que se renueva y lectura sin conexión.

## Objetivo

Existe un `GraphqlClient` que se usa como el resto de los servicios, y la caché del modo offline también guarda las consultas de GraphQL.

## Alcance

- `lib/core/network/graphql_client.dart`: `GraphqlClient` con `consultar` y `mutar` (`POST /graphql` con `{ query, variables }`) sobre el `ApiClient`; revisa `errors` además del estado HTTP y los traduce a `ApiException` con el código de `extensions.code` (`ERR_SYS_00` si no hay); relanza el fallo de red como `DioException`. `OpcionesGraphql.clave`.
- `ApiClient`: el interceptor de desempaquetado deja pasar sin tocar las peticiones marcadas con `OpcionesGraphql.clave`, en la respuesta y en el error.
- `core/offline/interceptor_offline.dart`: `OpcionesOffline.claveConsultaGraphql`; una consulta (POST marcado) es cacheable, con clave ruta + texto + variables; una respuesta con `errors` no se guarda; una mutación nunca.
- Tests: `test/core/graphql_client_test.dart` (lo que envía, lo que devuelve, errores, sesión) y `test/core/offline/graphql_offline_test.dart`.

**Archivos esperados (crear/tocar):** `graphql_client.dart`, `api_client.dart`, `interceptor_offline.dart` y los dos archivos de test.

## Fuera de alcance (no tocar)

- Los servicios de avisos y preferencias (UT 02).
- Encolar mutaciones sin conexión.
- El comportamiento de REST: los tests de `api_client_*` y de offline originales pasan sin cambios.

## Resultado esperado

Una consulta se hace, se guarda y se responde sin conexión; un error de negocio llega como `ApiException`; REST se comporta igual que antes.

## Validación

1. `flutter test test/core/graphql_client_test.dart test/core/offline/graphql_offline_test.dart` pasa.
2. Un error de GraphQL con HTTP 200 y `data: null` lanza `ApiException` y no se toma por una respuesta vacía.
3. `flutter analyze` sin hallazgos y toda la suite en verde.
