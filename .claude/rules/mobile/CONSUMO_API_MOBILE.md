---
globs: projects/mobile/**/*
---

# Consumo de API — Mobile

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

### Endpoints compartidos entre features

Mismo criterio que en el frontend web (ver
[TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)): si un
endpoint se consume desde más de una feature, su lógica va en
`lib/core/network/` (no en `features/{feature}/services/` de ninguna de las
dos, y no duplicado en ambas). Se mueve ahí recién cuando aparece el segundo
consumidor real, no de forma anticipada.
