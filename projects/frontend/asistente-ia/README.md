# Asistente IA (web)

Remoto React: IA local web y administración de agentes n8n (CU41, CU43).

| | |
| --- | --- |
| Stack | React 19, Vite, Module Federation (`@module-federation/vite`), TanStack Router/Query, Tailwind 4 + `@drivehub/design-tokens` |
| Rules | [`ARQUITECTURA_FRONTEND.md`](../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y las de [`frontend/`](../../../.claude/rules/frontend/) |
| Puerto | 4206 |
| Expone | `./mount` (`mount`/`unmount`, ver `src/mount.ts`) |

```bash
pnpm dev      # suelto en el puerto 4206; dentro del shell en /asistente-ia
pnpm test
pnpm build
pnpm lint
```
