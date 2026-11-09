# UT-CU51-MOB-03 — Primera vez en el router, «Cambiar servidor» en el login y arranque

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU51-MOB-03` |
| **Nombre** | Primera vez en el router, «Cambiar servidor» en el login y arranque en `main()` |
| **Caso de uso** | CU51-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU51-MOB-02 |

## Antes de implementar

[NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md): el chequeo vive una sola vez en el `redirect` global del router.

## Solicitud de negocio

La primera vez la app debe pedir el servidor antes que nada, y las siguientes arrancar directo como siempre.

## Objetivo

Sin servidor guardado el router solo deja `/servidor`; con uno guardado, la app se comporta como siempre.

## Alcance

- `core/routing/app_router.dart`: `/servidor` en las rutas públicas y en las rutas; el `redirect` global pregunta primero `servidorProvider.configurado` (si no, solo `/servidor`) y recién después la sesión; el router vuelve a evaluar cuando cambia el servidor.
- `features/auth/screens/login_screen.dart`: «Cambiar servidor» (`botonCambiarServidor`) que va a `/servidor`.
- `lib/main.dart`: lee el servidor guardado ANTES de crear la sesión y el modo offline, lo fija en el `ApiClient` y sustituye `servidorInicialProvider` con lo leído.
- `projects/mobile/.env.example`: `API_BASE_URL` aclarado como la dirección sugerida.
- Tests: en `servidor_screen_test.dart`, los grupos «la primera vez que se abre la app» y «las veces siguientes»; los tests existentes de router, login y app pasan sin tocarse.

**Archivos esperados (crear/tocar):** `app_router.dart`, `login_screen.dart`, `main.dart`, `.env.example` y los tests.

## Fuera de alcance (no tocar)

- La pantalla y la validación (UT 01 y 02).
- Cambiar el servidor con sesión iniciada.

## Resultado esperado

Primera vez: servidor y luego login. Después: login directo.

## Validación

1. `flutter test` completo pasa y `flutter analyze` no tiene hallazgos.
2. Sin servidor guardado, cualquier ruta (`/login`, `/register`, `/inicio`) termina en `/servidor`, incluso mientras la sesión se recupera.
3. Con sesión iniciada, `/servidor` lleva al inicio.
