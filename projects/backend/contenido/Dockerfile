# ==========================================
# BASE: PHP 8.4 + extensión mongodb + Composer
# ==========================================
FROM php:8.4-cli AS base
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends git unzip libssl-dev \
    && pecl install mongodb \
    && docker-php-ext-enable mongodb \
    && rm -rf /var/lib/apt/lists/*
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

# ==========================================
# DEV: lo usa docker-compose. El código se monta por volumen; al arrancar
# instala dependencias, corre las migraciones (colecciones + índices) y
# levanta el servidor de desarrollo.
# ==========================================
FROM base AS dev
ENV CACHE_STORE=array SESSION_DRIVER=array QUEUE_CONNECTION=sync LOG_CHANNEL=json
CMD ["sh", "-c", "composer install && php artisan migrate --force && php artisan serve --host=0.0.0.0 --port=8085"]

# ==========================================
# PROD: dependencias sin dev, sin root.
# TODO: servir con php-fpm + nginx (o FrankenPHP) cuando se defina el
# TODO: destino de despliegue (ver .claude/rules/CI_CD.md); artisan serve
# TODO: es solo para desarrollo.
# ==========================================
FROM base AS prod
ENV APP_ENV=production APP_DEBUG=false CACHE_STORE=array SESSION_DRIVER=array QUEUE_CONNECTION=sync LOG_CHANNEL=json
COPY . .
RUN composer install --no-dev --optimize-autoloader --no-interaction
RUN useradd --system --no-create-home appuser && chown -R appuser storage bootstrap/cache
USER appuser
EXPOSE 8085
CMD ["php", "artisan", "serve", "--host=0.0.0.0", "--port=8085"]
