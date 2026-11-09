---
globs: projects/mobile/**/*
---

# Navegación — Mobile

## `go_router` con redirect global

Un único `GoRouter` configurado en `lib/core/routing/` (scaffold ya creado,
hoy solo tiene `.gitkeep` — es un prerequisito armarlo antes de la primera
pantalla real). El chequeo de sesión **no se repite pantalla por pantalla**:
se resuelve una sola vez en el callback `redirect` de la configuración raíz
del router:

```dart
// lib/core/routing/app_router.dart
final router = GoRouter(
  redirect: (context, state) {
    final isLoggedIn = /* leer estado de sesión, ej. desde un provider de auth */;
    final isLoggingIn = state.matchedLocation == '/login';

    if (!isLoggedIn && !isLoggingIn) return '/login';
    if (isLoggedIn && isLoggingIn) return '/';
    return null; // sin redirect
  },
  routes: [ /* ... */ ],
);
```

Mismo concepto que el guard de rutas del frontend web (ver
[RUTAS_NAVEGACION_FRONTEND.md](../frontend/RUTAS_NAVEGACION_FRONTEND.md)):
la lógica de "¿puede entrar acá?" vive en **un solo lugar**, no se
reimplementa un `if` de sesión al principio de cada pantalla.

## Primero el servidor (CU51)

Antes que la sesión, el `redirect` global pregunta si la app ya sabe con qué backend habla
(`servidorProvider.configurado`). La primera vez que se abre la app no lo sabe y **solo deja `/servidor`**: ni el login ni el
registro ni la recuperación de contraseña tienen a quién llamar. Con un servidor guardado (lo lee `main()` del teléfono
antes de crear la sesión) el flujo es el de siempre. `/servidor` también es una ruta pública: el login ofrece «Cambiar
servidor» y, con sesión iniciada, vuelve al inicio.

- El estado inicial por defecto es «configurado»: los tests de las demás pantallas no pasan por la configuración. `main()` lo
  sustituye con lo leído del teléfono, y los tests del flujo lo fijan en `configurado: false`.

## Fuente del estado de sesión

El `redirect` necesita leer si hay sesión válida — esto se resuelve contra
el provider de autenticación de `features/auth/providers/` (ver
[ESTADO_GLOBAL_MOBILE.md](ESTADO_GLOBAL_MOBILE.md)), no leyendo
`flutter_secure_storage` directo desde el router. El router depende del
provider de auth, no al revés.

## Estructura

- Definición de rutas y del `GoRouter` en `lib/core/routing/`.
- Cada feature expone sus rutas (o al menos las pantallas a las que el
  router apunta) desde `features/{feature}/screens/`, pero el árbol de
  rutas completo se ensambla en un solo lugar dentro de `core/routing/` —
  no queda esparcido un `GoRoute` por archivo suelto en cada feature sin un
  punto central que las junte.
