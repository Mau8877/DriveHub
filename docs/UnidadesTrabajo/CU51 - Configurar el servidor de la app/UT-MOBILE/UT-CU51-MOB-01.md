# UT-CU51-MOB-01 — Dirección del servidor: validación, almacenamiento y cambio en caliente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU51-MOB-01` |
| **Nombre** | Dirección del servidor: validación, almacenamiento y cambio en caliente del cliente HTTP |
| **Caso de uso** | CU51-MOB |
| **Microservicio** | app móvil |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (cliente único) y [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md).

## Solicitud de negocio

La app tiene que poder hablar con el servidor que la persona elija, y cambiarlo sin reiniciar.

## Objetivo

Existen la validación de la dirección, su almacenamiento en el teléfono y un `ApiClient` que cambia de servidor en el momento.

## Alcance

- `features/servidor/validators/servidor_validators.dart`: `normalizarDireccionServidor` (agrega `http://` y `/api/v1`, quita la barra final, `/api` → `/api/v1`, IPv6 con corchetes), `validarDireccionServidor` (mensajes en español) y `direccionParaMostrar`. Rechaza espacios y caracteres no válidos en el host, usuario, parámetros, `#` y otros protocolos.
- `features/servidor/services/servidor_storage.dart`: `ServidorStorage` y su implementación sobre `flutter_secure_storage`, con `servidorStorageProvider` para sustituirla en los tests.
- `core/network/api_client.dart`: `baseUrlActual` (lo elegido, o `API_BASE_URL`, o la del emulador) y `configurarBaseUrl`, que actualiza el MISMO `Dio` si ya existe. `core/offline/sondeo_servidor.dart` usa la misma dirección.
- `features/servidor/providers/servidor_provider.dart`: `EstadoServidor`, `servidorInicialProvider` (por defecto, configurado) y `ServidorNotifier.guardar`.
- Tests: `servidor_validators_test.dart` y `servidor_provider_test.dart`.

**Archivos esperados (crear/tocar):** los de arriba y sus tests.

## Fuera de alcance (no tocar)

- La pantalla (UT 02), el router, el login y `main()` (UT 03).
- Más de un servidor guardado.

## Resultado esperado

Una dirección escrita de cualquier forma razonable queda normalizada, guardada y en uso; las llamadas siguientes salen al servidor nuevo.

## Validación

1. `flutter test test/features/servidor/servidor_validators_test.dart test/features/servidor/servidor_provider_test.dart` pasa.
2. `hola mundo` y `http://host :8080` se rechazan (no se guardan codificados).
3. Tras guardar, `ApiClient.instance` es el mismo objeto y su `options.baseUrl` es el nuevo.
