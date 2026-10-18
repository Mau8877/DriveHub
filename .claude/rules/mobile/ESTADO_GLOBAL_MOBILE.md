---
globs: projects/mobile/**/*
---

# Estado Global — Mobile

## Riverpod no separa "cache de servidor" de "estado de UI" como dos
## librerías distintas — pero la separación conceptual se mantiene igual

A diferencia del frontend web (Zustand para UI vs. TanStack Query para
datos de servidor, ver
[ESTADO_GLOBAL_FRONTEND.md](../frontend/ESTADO_GLOBAL_FRONTEND.md) y
[TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)), acá
todo es Riverpod. Pero la distinción entre "esto envuelve datos que vienen
del backend" y "esto es puramente estado de la UI" sigue existiendo, y se
marca por **convención de nombre y ubicación**, no se mezcla todo en un
único tipo de provider indistinguible.

## Providers que envuelven datos de API

Viven en `providers/` de cada feature, junto al `service` que consumen
(scaffold ya creado: `lib/features/auth/providers/`):

```
lib/features/usuarios/
├── services/
│   └── usuarios_service.dart
└── providers/
    └── usuarios_provider.dart   # FutureProvider / AsyncNotifier sobre usuarios_service
```

- Listado: `FutureProvider`/`AsyncNotifierProvider` que llama al `service` y
  expone un `AsyncValue<List<Usuario>>` — es el equivalente conceptual a un
  `useQuery` de listado en TanStack Query.
- Detalle: `FutureProvider.family`/`AsyncNotifierProvider.family` parametrizado
  por `id`.
- Mutaciones (crear/editar/eliminar): un método en el mismo
  `AsyncNotifier` que, al completar, invalida/refresca el provider de
  listado correspondiente (`ref.invalidate(usuariosListProvider)`) —
  equivalente conceptual a `invalidateQueries` en TanStack Query.

## Providers de estado puro de UI

Mismo lugar (`providers/` de la feature) cuando el estado es específico de
esa feature (ej. el paso actual de un formulario multi-paso dentro de
`features/usuarios/`), pero se distinguen por **nombre**: terminan en
`UiProvider` en vez de `Provider`/`ListProvider`/`DetailProvider`, para que
al leer el nombre quede claro si el provider envuelve datos del backend o
es estado local de pantalla.

## Convención de nombres

| Tipo | Convención | Ejemplo |
| --- | --- | --- |
| Listado de un recurso | `{feature}ListProvider` | `usuariosListProvider` |
| Detalle de un recurso | `{feature}DetailProvider` (family por `id`) | `usuarioDetailProvider` |
| Notifier con mutaciones | `{feature}Provider` | `usuariosProvider` |
| Estado puro de UI de una feature | `{feature}UiProvider` | `usuarioFormUiProvider` |

No se usa un nombre genérico como `provider` o `state` sin el prefijo de la
feature — con varias features activas, un nombre así genérico hace
imposible saber de dónde viene solo con autocompletado/búsqueda.
