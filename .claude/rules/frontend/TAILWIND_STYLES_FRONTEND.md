---
globs: projects/frontend/**/*
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

## No hay paleta custom todavía

Hoy el proyecto usa el tema **default** de shadcn (`components.json`:
`baseColor: "neutral"`, sin `tailwind.config` custom). **No hay una paleta
de colores propia del proyecto definida todavía** — este archivo es un
placeholder hasta que se defina: cuando exista la paleta final
(colores de marca del proyecto), este documento se actualiza con los
tokens reales y ejemplos concretos de uso. Hasta entonces, no se inventan
colores de marca ni se asume que ya existe una paleta terminada — se sigue
usando el set neutral default de shadcn.

## Agregar componentes vía CLI de shadcn

```bash
pnpm dlx shadcn@latest add <componente>
```

- El output del CLI aterriza en `src/components/ui/` (según
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
