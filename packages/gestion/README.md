# @drivehub/gestion

`<dh-gestion>`: Web Component (Lit 3, JS plano + `.d.ts`, sin build) que dibuja una pantalla de
gestión completa —listado con búsqueda, filtros y paginación, crear, editar y acciones por fila—
a partir de un **esquema**. Lo usan los remotos Angular y React por igual.

```ts
import '@drivehub/gestion'
import type { EsquemaGestion } from '@drivehub/gestion'

// Angular: <dh-gestion [esquema]="esquema">  (con CUSTOM_ELEMENTS_SCHEMA)
// React 19: <dh-gestion esquema={esquema} />
```

- Llama al backend por el contexto del shell (`@drivehub/mf-contract`): token, URL base y renovación
  de sesión. Desempaqueta el sobre estándar y mapea los errores de negocio a campos.
- Solo usa variables de `@drivehub/design-tokens`.
- Ejemplo real: `projects/frontend/escuela/src/app/features/escuelas/schemas/escuelas.esquema.ts`.
- Reglas de uso: [GESTION_FRONTEND.md](../../.claude/rules/frontend/GESTION_FRONTEND.md).

```bash
pnpm --dir packages/gestion test
```
