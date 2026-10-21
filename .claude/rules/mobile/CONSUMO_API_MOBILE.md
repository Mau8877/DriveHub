---
globs: projects/mobile/**/*
---

# Consumo de API — Mobile

## Una sola URL: el API Gateway

La app habla **solo con el gateway** (`API_BASE_URL`, ej.
`http://10.0.2.2:8080/api/v1` en el emulador de Android), nunca con un
microservicio directo: el gateway enruta cada recurso al servicio que
corresponde (ver [MICROSERVICIOS.md](../MICROSERVICIOS.md#recursos-http-de-cada-servicio)).
Todos los servicios responden con el mismo sobre, sin importar su stack.
El refresh token en mobile viaja en el body y se guarda en
`flutter_secure_storage` (no hay cookies, ver
[SEGURIDAD_AUTH_BACKEND.md](../backend/SEGURIDAD_AUTH_BACKEND.md)).

## Cliente `dio` centralizado (prerequisito)

`dio` está en `pubspec.yaml`, pero **no existe todavía** ningún cliente
configurado — `lib/core/network/` solo tiene un `.gitkeep`. Antes de
escribir el primer `service` que llame al backend, hay que crear:

- `lib/core/network/api_client.dart`: una instancia única de `Dio` con
  `baseUrl` apuntando a la API (leído de configuración de entorno —
  `flutter_dotenv` está instalado en `pubspec.yaml` pero tampoco está
  wireado todavía, es otro prerequisito ligado a este mismo punto).
- Un `Interceptor` que desempaqueta el sobre estándar del backend
  (`{status, data, message, error, timestamp, meta?}`, mismo contrato que
  [RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)):
  - Si `status == "success"`, el interceptor deja pasar `data` (y `meta`
    cuando exista) como el resultado real de la llamada — los `services` no
    desestructuran el sobre a mano en cada método.
  - Si `status == "failed"`, el interceptor lanza una excepción tipada
    propia (ej. `ApiException(code: error, message: message)`) en vez de
    dejar pasar una respuesta 2xx con `status: "failed"` como si fuera
    éxito, o dejar que el `DioException` crudo (sin el código de negocio)
    se propague tal cual hasta la UI.

## Servicios por feature

Los servicios que llaman a la API viven en `services/` dentro de cada
feature, siguiendo el scaffold ya creado (`lib/features/auth/services/`):

```
lib/features/usuarios/
├── services/
│   └── usuarios_service.dart   # usa el ApiClient de core/network/
├── providers/
└── screens/
```

Un `service` recibe el `ApiClient` compartido (no crea su propia instancia
de `Dio`), y solo conoce endpoints de **su** feature.

## Modelos de datos (DTOs)

Las clases que representan el JSON de la API viven en `models/` dentro de
la feature (`lib/features/{feature}/models/`):

- Una clase por DTO, nombre `{Recurso}Request` para lo que se envía y
  `{Recurso}` (o `{Recurso}Response` si hace falta distinguir) para lo que
  se recibe.
- Clases inmutables: campos `final` y constructor `const`. `toJson()` en los
  requests y `factory fromJson(Map<String, dynamic>)` en las respuestas,
  escritos a mano (sin `json_serializable` ni generación de código mientras
  no se decida lo contrario).
- Los campos van en `camelCase`, igual que el JSON del backend.
- Solo el `service` de la feature construye y parsea DTOs; los providers y
  las pantallas reciben objetos ya tipados.
- El parseo se prueba con un test unitario cuando el mapeo no es trivial
  (ver [TESTING_MOBILE.md](TESTING_MOBILE.md)).

### Endpoints compartidos entre features

Mismo criterio que en el frontend web (ver
[TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)): si un
endpoint se consume desde más de una feature, su lógica va en
`lib/core/network/` (no en `features/{feature}/services/` de ninguna de las
dos, y no duplicado en ambas). Se mueve ahí recién cuando aparece el segundo
consumidor real, no de forma anticipada.
