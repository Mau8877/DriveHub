# Exámenes (web)

Remoto React: Banco de preguntas, simulacros, examen de conducción y notas (CU19, CU20, CU22, CU23).

| | |
| --- | --- |
| Stack | React 19, Vite, Module Federation (`@module-federation/vite`), TanStack Router/Query, Tailwind 4 + `@drivehub/design-tokens` |
| Rules | [`ARQUITECTURA_FRONTEND.md`](../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y las de [`frontend/`](../../../.claude/rules/frontend/) |
| Puerto | 4204 |
| Expone | `./mount` (`mount`/`unmount`, ver `src/mount.ts`) |

```bash
pnpm dev      # suelto en el puerto 4204; dentro del shell en /examenes
pnpm test
pnpm build
pnpm lint
```
