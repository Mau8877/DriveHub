---
globs: projects/mobile/**/*
---

# Widgets / UI — Mobile

## Compartidos vs. de una sola feature

Mismo criterio que `common/` en el backend
([CONVENCIONES_JAVA_BACKEND.md](../backend/CONVENCIONES_JAVA_BACKEND.md)) y
`shared/` en el frontend web
([TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)):

- Un widget que usa **una sola feature** vive dentro de esa feature. El
  scaffold actual (`lib/features/auth/{providers,screens,services}`) no
  tiene todavía una subcarpeta `widgets/` — se agrega cuando una feature
  concreta la necesita: `lib/features/{feature}/widgets/`.
- Un widget que usan **dos o más features** va en
  `lib/shared/widgets/` (carpeta nueva, no existe todavía en el scaffold —
  se crea recién cuando aparece el primer widget realmente compartido, no
  de forma anticipada "por si hace falta").

No se mueve un widget a `shared/widgets/` apenas "parece que podría
reusarse" — se mueve cuando una segunda feature efectivamente lo necesita,
igual que el criterio de `shared/api/` del lado del consumo de API (ver
[CONSUMO_API_MOBILE.md](CONSUMO_API_MOBILE.md)).

## Tema (`ThemeData`)

`lib/core/theme/app_theme.dart` define `AppColors`, `AppSpace`, `AppRadius`,
`AppMotion`, `AppSemantic` (tokens sin equivalente en `ColorScheme`, se leen con
`context.semantica`) y `AppTheme.light`/`AppTheme.dark`, que `lib/main.dart`
referencia vía `MaterialApp(theme:, darkTheme:)`. La guía visual completa es
[`projects/mobile/docs/design.md`](../../../projects/mobile/docs/design.md) (propuesta B):
ante una duda de diseño manda ese documento.

- **Ningún color ni medida literal en un widget**: sale del tema o de los tokens.
- **El ámbar es `secondary`, no `primary`**: el botón relleno ya sale ámbar del tema y
  se usa **una sola vez por pantalla**.
- **Cambio de paleta**: se hace en `AppColors` y en el mismo cambio en
  `packages/design-tokens` de la web (espejo manual de la misma paleta, ver el
  comentario en `app_theme.dart`).
- **Tipografía**: Nunito, empaquetada como fuente variable en `assets/fonts/` (licencia
  SIL OFL en el mismo directorio). El peso se fija con el eje `wght`: para cambiar el
  peso de un estilo se usa `AppTheme.conPeso`, no `copyWith(fontWeight: ...)`.
