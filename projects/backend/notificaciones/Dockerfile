# ==========================================
# ETAPA 1: CONSTRUCCIÓN
# ==========================================
# Contexto de build = raíz del repo (el lockfile de pnpm vive ahí):
#   docker build -f projects/backend/notificaciones/Dockerfile .
FROM node:24-alpine AS builder
WORKDIR /app
RUN corepack enable pnpm
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY projects/backend/notificaciones ./projects/backend/notificaciones
RUN pnpm install --frozen-lockfile --filter ./projects/backend/notificaciones
RUN pnpm --filter ./projects/backend/notificaciones run build
RUN pnpm --filter ./projects/backend/notificaciones deploy --prod --legacy /out

# ==========================================
# ETAPA 2: EJECUCIÓN
# ==========================================
FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
# Práctica de seguridad: no correr como root.
USER node
COPY --from=builder --chown=node:node /out ./
EXPOSE 8086
CMD ["node", "dist/main"]
