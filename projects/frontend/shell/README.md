# Shell (web)

Host de la web: login (CU02), layout, menú por rol, avisos, rutas de primer nivel y sesión. Carga los 6 remotos.

| | |
| --- | --- |
| Stack | Angular 22, Native Federation, Tailwind 4 + `@drivehub/design-tokens` |
| Rules | [`ARQUITECTURA_FRONTEND.md`](../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`ANGULAR_FRONTEND.md`](../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) |
| Puerto | 4200 |

```bash
pnpm dev      # ng serve
pnpm test
pnpm build
pnpm lint
```
