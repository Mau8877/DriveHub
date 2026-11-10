# ==========================================
# Imagen de producción de la web: las 7 apps (shell + 6 remotos) compiladas y servidas por nginx.
# Contexto de build = raíz del repo (el lockfile y los paquetes de packages/ viven ahí):
#   docker build -f projects/frontend/Dockerfile -t drivehub-web .
# En desarrollo no se usa: docker-compose corre los 7 dev servers.
# ==========================================

# ==========================================
# ETAPA 1: CONSTRUCCIÓN
# ==========================================
FROM node:24-alpine AS builder
WORKDIR /app
RUN corepack enable pnpm
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY packages ./packages
COPY projects/frontend ./projects/frontend
# Con llaves y "...": entran también los paquetes del workspace de los que dependen las apps (ver CI_CD.md).
RUN pnpm install --frozen-lockfile --filter "{./projects/frontend/*}..."

# Un RUN por app: si una falla, el error dice cuál, y Docker reaprovecha las que ya compilaron.
RUN pnpm --dir projects/frontend/shell run build
RUN pnpm --dir projects/frontend/identidad run build
RUN pnpm --dir projects/frontend/escuela run build
RUN pnpm --dir projects/frontend/pagos run build
# Los remotos React se compilan para servirse bajo /remotos/{app}/ (DH_BASE, ver su vite.config.ts).
RUN DH_BASE=/remotos/examenes/ pnpm --dir projects/frontend/examenes run build
RUN DH_BASE=/remotos/contenido/ pnpm --dir projects/frontend/contenido run build
RUN DH_BASE=/remotos/asistente-ia/ pnpm --dir projects/frontend/asistente-ia run build

# ==========================================
# ETAPA 2: EJECUCIÓN (nginx sin root, puerto 8080)
# ==========================================
FROM nginxinc/nginx-unprivileged:1.28-alpine
COPY projects/frontend/despliegue/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/projects/frontend/shell/dist/shell/browser /usr/share/nginx/html
COPY --from=builder /app/projects/frontend/identidad/dist/identidad/browser /usr/share/nginx/html/remotos/identidad
COPY --from=builder /app/projects/frontend/escuela/dist/escuela/browser /usr/share/nginx/html/remotos/escuela
COPY --from=builder /app/projects/frontend/pagos/dist/pagos/browser /usr/share/nginx/html/remotos/pagos
COPY --from=builder /app/projects/frontend/examenes/dist /usr/share/nginx/html/remotos/examenes
COPY --from=builder /app/projects/frontend/contenido/dist /usr/share/nginx/html/remotos/contenido
COPY --from=builder /app/projects/frontend/asistente-ia/dist /usr/share/nginx/html/remotos/asistente-ia
# Manifiestos de producción: todas las apps en el mismo origen, así que valen igual en cualquier entorno.
# Pisan los de desarrollo (localhost:420x) que trae el build del shell.
COPY projects/frontend/despliegue/federation.manifest.json projects/frontend/despliegue/remotos-react.manifest.json /usr/share/nginx/html/
EXPOSE 8080
