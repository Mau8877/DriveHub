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

`lib/core/theme/app_theme.dart` ya define `AppColors`/`AppTheme.light`/`AppTheme.dark`
y `lib/main.dart` los referencia vía `MaterialApp(theme:, darkTheme:)` — pero
con una **paleta neutra de placeholder**, no la identidad de marca real del
proyecto. Mismo criterio que
[TAILWIND_STYLES_FRONTEND.md](../frontend/TAILWIND_STYLES_FRONTEND.md) del
lado web (que define la misma estructura de tokens con valores neutros):
hasta que exista la paleta final del proyecto, no se inventan colores de
marca acá — cuando se defina, se reemplazan los `Color(0x...)` de
`AppColors` por los reales, en el mismo cambio que actualiza `index.css`
del lado web (ambos son espejo manual de la misma paleta, ver el
comentario en `app_theme.dart`).
