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
  `lib/shared/widgets/` (se crea con el primer widget realmente compartido,
  no de forma anticipada "por si hace falta"; el primero fue `AvatarUsuario`,
  de CU06, que usan el `UsuarioShell` y el perfil).

No se mueve un widget a `shared/widgets/` apenas "parece que podría
reusarse" — se mueve cuando una segunda feature efectivamente lo necesita,
igual que el criterio de `shared/api/` del lado del consumo de API (ver
[CONSUMO_API_MOBILE.md](CONSUMO_API_MOBILE.md)).

## Tema (`ThemeData`): el diseño Cockpit

El diseño **oficial** de la app es el **Cockpit** (mockup
[`projects/mobile/mockup/diseno-3-cockpit`](../../../projects/mobile/mockup/README.md)): fondo con
un resplandor azul y otro ámbar en las esquinas, tarjetas opacas con borde fino, acento ámbar y
pestañas inferiores. Todo vive en `lib/core/theme/app_theme.dart`: `AppColors`, `AppTheme.light` /
`AppTheme.dark` (con los temas de botones, campos, tarjetas, barra inferior, snackbar, diálogos y
hojas ya definidos) y la extensión `CockpitColores` (`context.cockpit`) con lo que `ColorScheme` no
tiene. La paleta sigue siendo espejo manual de `tokens.css` (ver
[TAILWIND_STYLES_FRONTEND.md](../frontend/TAILWIND_STYLES_FRONTEND.md)): un cambio de paleta se hace
en los dos lugares en el mismo cambio.

**Legibilidad, regla estricta** (el primer mockup del Cockpit se leía mal por los degradados):

- El texto **nunca** va sobre un degradado ni sobre una superficie translúcida: las tarjetas, los
  campos, los chips y la barra inferior son opacos. Los resplandores del fondo viven solo en las
  esquinas y son tenues en el modo claro.
- No hay texto con degradado (`ShaderMask`/`background-clip`): los títulos y la hora son del color
  de texto del tema.
- El ámbar puro (`AppColors.ambar`) es para fondos (botón de acción, indicador) y brillos. Un
  **texto** o icono de acento usa `context.cockpit.acento` (ámbar oscuro en claro), y un enlace
  `context.cockpit.enlace`. No se escribe `Color(0x...)` ni `Colors.xxx` en una pantalla.
- `test/core/app_theme_test.dart` exige contraste WCAG AA (4.5:1) de cada combinación de texto, en
  claro y en oscuro, incluso sobre el peor punto del resplandor. Un color nuevo de texto se suma ahí.

**Apariencia**: Sistema, Claro u Oscuro, a elección en Perfil (`temaProvider`, guardada en
`flutter_secure_storage`). Arranca siguiendo al sistema.

### Piezas del diseño (`lib/shared/widgets/`)

Todas las pantallas usan estas piezas en vez de rehacerlas:

| Widget | Para qué |
| --- | --- |
| `CockpitScaffold` / `FondoCockpit` | En lugar de `Scaffold`: el mismo contenido sobre el fondo del Cockpit |
| `LogoDriveHub` / `MarcaDriveHub` | La marca (la "D" con la línea de la ruta) |
| `AvisoCockpit` | Aviso en línea (info, advertencia, error, éxito) |
| `Insignia` | Estado con punto en una píldora (`Verificado`) |
| `FilaAccion` | Fila con ficha de icono, título, detalle y flecha, dentro de una `Card` |
| `EstadoVacio` | Lista o pantalla sin datos |
| `MedidorPassword` | Fuerza de la contraseña (solo ayuda visual; manda el backend) |
| `IndicadorBoton` | Spinner dentro de un botón de acción (toma el color del texto del botón) |
| `AvatarUsuario` | Foto o iniciales |
| `avisar(context, texto)` | Snackbar breve |

Un campo de texto, un botón, una tarjeta o un diálogo **no** necesitan widget propio: salen del tema
(`TextFormField` con `InputDecoration(prefixIcon: ...)`, `FilledButton`, `Card`, `AlertDialog`).
