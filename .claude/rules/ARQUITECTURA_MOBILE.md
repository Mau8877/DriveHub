---
globs: projects/mobile/**/*
---

# Arquitectura Mobile

**Feature-based**, mismo criterio conceptual que el frontend web (ver
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md)): el código se
organiza por funcionalidad de negocio (`features/{feature}/`), y lo
transversal a toda la app vive en `core/`.

## Estructura de `lib/`

Ya scaffoldeada — la estructura real que existe hoy en el repo (`core/network`
y `core/theme` ya implementados; `core/routing` todavía vacío; `layouts/`
y `features/health/` como ejemplo de referencia real; `features/auth/`
como ejemplo de referencia vacío):

```
lib/
├── core/
│   ├── network/     # ApiClient (Dio + interceptor de sobre), ya implementado (ver CONSUMO_API_MOBILE.md)
│   ├── routing/     # GoRouter único, con el redirect global -- todavía vacío (ver NAVEGACION_MOBILE.md)
│   └── theme/       # AppColors/AppTheme, ya implementado con paleta neutra de placeholder
├── layouts/
│   └── {rol}/       # Shell por rol (AdminShell, UsuarioShell, ya scaffoldeados como ejemplo)
├── shared/
│   └── widgets/     # Widgets usados por 2+ features (ver WIDGETS_UI_MOBILE.md) -- no existe todavía
├── features/
│   ├── health/      # Feature de diagnóstico end-to-end (service+provider+screen) contra el backend real
│   └── {feature}/   # Un feature de negocio (ver estructura abajo)
└── main.dart         # Entry point, monta MaterialApp + ProviderScope (sin GoRouter todavía)
```

`core/` es la única carpeta de nivel superior con código transversal real
además de `features/` — no hay una carpeta `common/`/`utils/` genérica
todavía; si aparece una necesidad concreta de utilidades puramente
transversales (no ligadas a red, routing ni tema), se define esa carpeta
en su momento, no se anticipa vacía.

`features/health/` no es una feature de negocio real — existe para probar
de punta a punta que `ApiClient` (Dio + interceptor) y Riverpod
(provider + `AsyncValue`) funcionan contra el backend real antes de que
exista la primera feature real. Se borra o se reemplaza el día que haya un
mejor lugar para un chequeo de conectividad, mismo criterio que
`features/home/` en el frontend web.

`layouts/{rol}/` sigue el mismo patrón que
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md#estructura-de-src) del
lado web: un shell delgado por rol (`AdminShell`, `UsuarioShell`, ya
scaffoldeados) que no conoce lógica de negocio, solo la navegación de
nivel superior de ese rol. Ninguno está conectado a `core/routing/`
todavía (esa carpeta sigue vacía) — quedan disponibles para montarse
cuando el proyecto defina su propio `GoRouter`.

## Estructura de un feature (`lib/features/{feature}/`)

Ya scaffoldeada como ejemplo de referencia en `lib/features/auth/` (hoy
vacía, solo `.gitkeep` en cada subcarpeta) — cualquier feature nueva sigue
esta misma forma:

```
lib/features/usuarios/
├── providers/    # Riverpod: AsyncNotifier/FutureProvider (datos) + *UiProvider (estado de UI)
├── screens/      # Pantallas completas que apunta el GoRouter de core/routing/
├── services/     # Llamadas a la API de ESTE feature, vía el ApiClient de core/network/
└── widgets/      # Widgets específicos de esta feature -- se agrega recién cuando hace falta uno
```

Referencia de cada subcarpeta, en el resto de las rules:
[ESTADO_GLOBAL_MOBILE.md](mobile/ESTADO_GLOBAL_MOBILE.md) (`providers/`,
convención de nombres), [CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md)
(`services/`), [NAVEGACION_MOBILE.md](mobile/NAVEGACION_MOBILE.md) (cómo
`screens/` se conecta con el router central), [WIDGETS_UI_MOBILE.md](mobile/WIDGETS_UI_MOBILE.md)
(`widgets/`, y cuándo algo pasa a `shared/widgets/`).

> **Modelos de datos (DTOs Dart) — todavía sin convención definida.** Ni el
> scaffold actual ni ninguna rule dice hoy dónde viven las clases que
> representan el JSON de request/response de la API (¿un `models/` nuevo
> por feature? ¿adentro de `services/`?). No se asume ninguna de las dos —
> se define explícitamente en la rule correspondiente ([CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md))
> antes de escribir el primer `service` real que necesite parsear una
> respuesta.

## Regla de dependencia

`features/{feature}/` puede depender de `core/` y `shared/` — nunca al
revés (`core/`/`shared/` no importan nada de `features/`). Una feature no
depende de otra feature directo salvo que la relación de negocio lo
justifique explícitamente, mismo criterio que
[ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md#regla-de-dependencia).
