# UT-CU36-MOB-03 — Conexión con la sesión y los avisos en primer plano

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-MOB-03` |
| **Nombre** | Conexión con la sesión y los avisos en primer plano |
| **Caso de uso** | CU36-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU36-MOB-02 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (la sesión es la fuente de verdad).

## Solicitud de negocio

Que registrar y quitar el teléfono ocurra solo al iniciar y cerrar sesión, y que un aviso que llega con la app abierta se vea.

## Objetivo

Iniciar sesión registra el teléfono, cerrar sesión lo quita, y un aviso en primer plano refresca la bandeja.

## Alcance

- `features/auth/providers/sesion_provider.dart`: el gancho `antesDeCerrarSesion` de `cerrarSesion` (máximo 5 segundos; un fallo no impide salir; perder la sesión por el servidor no lo dispara).
- `features/push/providers/arranque_push.dart`: `conectarPush` escucha la sesión (cada cambio intenta registrar; si ya está registrado no hace nada), conecta el gancho y, ante un aviso en primer plano, invalida el contador y la bandeja y muestra un aviso breve.
- `shared/widgets/mensajero_global.dart` y `scaffoldMessengerKey` en `MaterialApp`: mostrar un aviso sin `BuildContext`.
- `lib/main.dart`: llama a `conectarPush`.
- Tests: `arranque_push_test.dart` y el grupo `antesDeCerrarSesion` de `sesion_provider_test.dart`.

**Archivos esperados (crear/tocar):** los de arriba.

## Fuera de alcance (no tocar)

- Abrir la pantalla relacionada al tocar el aviso.
- Aviso del sistema con la app abierta.

## Resultado esperado

Con sesión iniciada el teléfono queda registrado; al cerrar sesión, quitado; con la app abierta, un aviso actualiza el contador y se ve.

## Validación

1. `flutter test` completo pasa y `flutter analyze` no tiene hallazgos.
2. En un teléfono real con el backend levantado: iniciar sesión y comprobar en la tabla de dispositivos del servicio que aparece uno `ANDROID`; cerrar sesión y comprobar que se quita.
3. Un aviso enviado a esa persona llega: con la app cerrada, como notificación del sistema; con la app abierta, como aviso breve y con el contador actualizado.
