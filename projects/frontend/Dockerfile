# Este proyecto es un workspace pnpm (ver /pnpm-workspace.yaml) -- el build
# de esta imagen SE HACE CON CONTEXTO EN LA RAÍZ DEL REPO, no en
# projects/frontend, porque `pnpm install` necesita pnpm-workspace.yaml y
# pnpm-lock.yaml de la raíz para instalar con el lockfile real (reproducible)
# en vez de resolver versiones nuevas cada build.
#
#   docker build -f projects/frontend/Dockerfile -t app-frontend .
#
# (la versión vieja de este Dockerfile copiaba solo projects/frontend/package.json
# con contexto en projects/frontend/ -- nunca se probó un build real esta
# sesión porque todo el desarrollo pasa por docker-compose.yml en modo dev,
# y de hecho fallaba: sin el lockfile de la raíz, pnpm no podía resolver el
# workspace correctamente)

# ==========================================
# ETAPA 1: CONSTRUCCIÓN (Builder)
# ==========================================
FROM node:22-alpine AS builder
RUN corepack enable pnpm

WORKDIR /app

# 1. Copiamos primero solo los manifiestos (cache de Docker: si no cambian
# las dependencias, no se reinstala nada aunque cambie el código fuente)
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY projects/frontend/package.json ./projects/frontend/package.json

RUN pnpm install --frozen-lockfile --filter frontend...

# 2. Copiamos el código fuente del frontend y compilamos
COPY projects/frontend ./projects/frontend
RUN pnpm --filter frontend build

# ==========================================
# ETAPA 2: SERVIR CON NGINX (producción)
# ==========================================
FROM nginx:alpine
COPY --from=builder /app/projects/frontend/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
