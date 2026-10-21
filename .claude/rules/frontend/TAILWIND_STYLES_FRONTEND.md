---
globs: projects/frontend/{examenes,contenido,asistente-ia}/**/*, packages/design-tokens/**/*
---

# Tailwind / Estilos — Frontend

## Siempre variables de tema, nunca valores arbitrarios

Se usan las clases de tema que ya trae shadcn/Tailwind 4
(`bg-primary`, `text-foreground`, `border-border`, `bg-muted`,
`text-destructive`, etc. — el set completo lo define
`src/index.css`/`components.json`). **No se usan valores arbitrarios**
(`w-[123px]`, `bg-[#ff0000]`, `text-[15px]`) salvo una excepción puntual y
justificada, y en ese caso el propio código lleva un comentario explicando
por qué no había alternativa dentro del sistema de diseño:

```tsx
// * Ancho fijo del ícono del proveedor externo (SVG con viewBox no estándar,
// * no hay token de tamaño del sistema que lo represente sin distorsión)
<img src={providerIcon} className="w-[37px]" />
```

Un valor arbitrario sin ese comentario es una señal de que se está
esquivando el sistema de diseño en vez de extenderlo — si hace falta un
tamaño/color nuevo con frecuencia, se agrega como token al tema, no se
repite el valor arbitrario en cada lugar que lo necesita.

## Tokens compartidos: `@drivehub/design-tokens`

Los colores, radios y tipografía de **toda la web** (shell y los 6
remotos, Angular y React) vienen de un único paquete,
[`packages/design-tokens`](../../../packages/design-tokens/):

- `tokens.css`: las variables CSS (`--primary`, `--background`,
  `--radius`...), claro y oscuro (`.dark`).
- `theme.css`: el mapeo a Tailwind 4 (`@theme inline`) que habilita
  `bg-primary`, `text-foreground`, `rounded-lg`, etc.

Cada app los importa en su CSS de entrada, después de `@import 'tailwindcss'`.
**Nunca** se define un color en el CSS de una app: si falta un token, se
agrega en `tokens.css` y su mapeo en `theme.css`.

La paleta de marca está definida:

| Color | Hex | Uso |
| --- | --- | --- |
| Gris Asfalto (primario) | `#1E293B` | Estructura principal, navegación (sidebar, cabecera), tipografía |
| Ámbar Vial (acento) | `#F59E0B` | Botones de acción clave, resaltados, insignias |
| Azul Técnico (soporte) | `#0284C7` | Indicadores de estado, horas programadas |
| Gris Claro Neutral | `#F1F5F9` | Fondo |
| Blanco Puro | `#FFFFFF` | Tarjetas y contenedores |
| Gris Medio | `#475569` | Textos secundarios y bordes suaves |

Se declaran como variables y se asignan a los tokens semánticos
(`--primary` = ámbar con texto asfalto, `--sidebar` = asfalto, `--info` =
azul técnico...) solo en `tokens.css`. Un cambio de paleta se hace ahí y en
el mismo cambio en `projects/mobile/lib/core/theme/app_theme.dart` (espejo
manual de la misma paleta). No se agregan colores fuera de esta paleta.

## Agregar componentes vía CLI de shadcn

```bash
pnpm dlx shadcn@latest add <componente>
```

- Se corre dentro de la carpeta del remoto (`projects/frontend/{remoto}`).
  El output del CLI aterriza en `src/components/ui/` de ese remoto (según
  `components.json` → `aliases.ui`).
- **No se edita a mano el output del CLI** salvo una necesidad puntual real
  (ej. un fix de accesibilidad que el componente base no cubre). Si se edita,
  se dejar constancia en un comentario arriba del cambio — de otra forma, la
  próxima vez que alguien re-corra el CLI para actualizar ese componente,
  pisa el cambio manual sin que nadie se dé cuenta de que existía.
- Componentes específicos de una sola feature (que no son de uso general del
  sistema de diseño) van en `src/features/{feature}/components/`, no en
  `src/components/ui/` — esa carpeta es exclusivamente para lo que el CLI de
  shadcn genera o lo que extiende directamente ese sistema de diseño
  compartido.
