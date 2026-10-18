# Contenido (web)

Remoto React: Manuales de la escuela (CU32).

| | |
| --- | --- |
| Stack | React 19, Vite, Module Federation (`@module-federation/vite`), TanStack Router/Query, Tailwind 4 + `@drivehub/design-tokens` |
| Rules | [`ARQUITECTURA_FRONTEND.md`](../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y las de [`frontend/`](../../../.claude/rules/frontend/) |
| Puerto | 4205 |
| Expone | `./mount` (`mount`/`unmount`, ver `src/mount.ts`) |

```bash
pnpm dev      # suelto en el puerto 4205; dentro del shell en /contenido
pnpm test
pnpm build
pnpm lint
```
