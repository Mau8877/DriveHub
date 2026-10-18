---
globs: projects/frontend/**/*
---

# Formularios (Zod + TanStack Form) — Frontend

## Un solo schema, dos usos

El schema de Zod de un recurso vive en `schemas/` dentro de la feature
(`src/features/{feature}/schemas/{recurso}Schema.ts`) y se reutiliza para:

1. Validar el formulario en el cliente, vía `@tanstack/react-form`.
2. Tipar y validar el payload que se manda a la API (el `Request` DTO del
   lado del frontend, inferido del schema con `z.infer<typeof schema>`).

**No se duplica** la validación en dos lugares (un schema para el form y un
`type CrearUsuarioRequest = {...}` escrito a mano aparte) — el segundo se
deriva del primero:

```ts
// src/features/usuarios/schemas/usuarioSchema.ts
import { z } from 'zod'

export const crearUsuarioSchema = z.object({
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  email: z.string().min(1, 'El correo es obligatorio').email('El correo no es válido'),
})

export type CrearUsuarioRequest = z.infer<typeof crearUsuarioSchema>
```

Si el schema de creación y el de edición difieren (ej. edición no pide
password), se definen como schemas separados pero uno puede derivar del otro
con `.partial()`/`.omit()`/`.extend()` en vez de copiar los campos a mano.

## Mensajes en español

El proyecto no es multi-idioma — todos los mensajes de error de Zod se
escriben en español directo en el schema (`.min(1, "El correo es obligatorio")`),
no se arma una capa de i18n para esto. Se muestran **inline, debajo del
campo correspondiente**, no en un toast/alert genérico que no indica qué
campo falló.

## Integración con TanStack Form

Las versiones instaladas (`@tanstack/react-form` ^1.33, `zod` ^4.4) soportan
pasar el schema de Zod **directamente** como validador (Zod 4 implementa el
protocolo Standard Schema, que TanStack Form v1 consume nativamente, sin
paquete adapter intermedio):

```tsx
const form = useForm({
  defaultValues: { nombre: '', email: '' },
  validators: {
    onChange: crearUsuarioSchema,
  },
  onSubmit: async ({ value }) => {
    // value ya viene tipado como CrearUsuarioRequest
  },
})
```

> Este patrón (schema directo, sin adapter) corresponde a las versiones
> exactas instaladas hoy en `package.json`. Si en algún momento se
> actualiza `@tanstack/react-form` o `zod` a un major distinto, **verificar
> primero** en el changelog de esa versión si el soporte de Standard Schema
> sigue funcionando igual antes de asumir que este ejemplo sigue vigente —
> es una integración que evoluciona rápido entre versiones de ambas
> librerías.

Se prefiere `validators.onChange` (valida mientras el usuario escribe) sobre
solo validar en `onSubmit`, para que el error aparezca inline apenas el
campo es inválido, no recién al intentar enviar.
