# Notificaciones

Historial de notificaciones (correo, push, in-app), preferencias por usuario y dispositivos push. CU36 a CU38.

| | |
| --- | --- |
| Stack | NestJS 12, Node 24, TypeScript (ESM) |
| Arquitectura | capas por feature — ver [`NESTJS_BACKEND.md`](../../../.claude/rules/backend/NESTJS_BACKEND.md) |
| Base de datos | DynamoDB (tablas creadas al arrancar, ver `src/common/dynamodb/definicion-tablas.ts`) |
| Puerto | 8086 |

## Correr

```bash
pnpm install                 # desde la raíz del repo (workspace pnpm)
pnpm start:dev
pnpm test
pnpm lint
```

Variables: [`.env.example`](.env.example). `GET /health` para verificar (necesita DynamoDB local en :8000).
