# DriveHub

Plataforma para autoescuelas: gestión escolar (autos, estudiantes,
docentes), clases y horarios, exámenes, pagos, contenido, notificaciones y
asistente con IA. Monorepo de **microservicios** + **micro-frontends** +
app móvil. El diseño funcional y de datos está en
[`docs/DriveHub/`](../docs/DriveHub/) (casos de uso por paquete, diseño de
base de datos, bases de datos y micro-frontends).

## Estructura

```
projects/
├── backend/                 # 8 proyectos independientes
│   ├── gateway/             # API Gateway (Spring Cloud Gateway)      :8080
│   ├── identidad/           # Spring Boot, hexagonal  · PostgreSQL     :8081
│   ├── escuela/             # Spring Boot, clean      · PostgreSQL     :8082
│   ├── pagos/               # Spring Boot, capas      · PostgreSQL     :8083
│   ├── academico/           # Spring Boot, capas      · MongoDB        :8084
│   ├── contenido/           # Laravel, capas          · MongoDB        :8085
│   ├── notificaciones/      # NestJS, capas           · DynamoDB       :8086
│   └── asistente-ia/        # FastAPI, capas          · DynamoDB       :8087
├── frontend/                # 7 micro-frontends web
│   ├── shell/               # Angular, host (Native Federation)        :4200
│   ├── identidad/ escuela/ pagos/            # Angular, remotos        :4201-4203
│   └── examenes/ contenido/ asistente-ia/    # React, remotos (MF Vite) :4204-4206
└── mobile/                  # Flutter (estudiante e instructor)
packages/
├── design-tokens/           # Variables CSS + tema Tailwind comunes a toda la web
├── ui/                      # <dh-shell>, <dh-sidebar>, <dh-header>, <dh-auth-layout> y clases base: el diseño Asfalto
└── mf-contract/             # Contrato shell <-> remotos (sesión, mount/unmount, eventos)
infrastructure/              # nginx, postgres (init), minio, prometheus, grafana
```

Mapa completo (recursos de cada servicio, prefijos de error, casos de uso
de cada micro-frontend): [MICROSERVICIOS.md](rules/MICROSERVICIOS.md).

## Comandos básicos

### Stack completo (Docker Compose)

Desde la raíz, con `.env` creado a partir de `.env.example`:

```bash
docker compose up
```

La infraestructura siempre arranca: Postgres (5432, una base por servicio),
MongoDB replica set (27017), DynamoDB local (8000), RabbitMQ (5672 / consola
15672), MinIO (9000/9001). `COMPOSE_PROFILES` del `.env` agrega `backend`
(los 8 servicios), `frontend` (las 7 apps + nginx en el puerto 80) y
`observabilidad` (Prometheus 9090, Grafana 3000). Detalle en
[docs/INFRAESTRUCTURA.md](../docs/INFRAESTRUCTURA.md).

### Servicios Spring (`gateway`, `identidad`, `escuela`, `pagos`, `academico`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # en la carpeta del servicio
./mvnw test
./mvnw checkstyle:check
```

### Notificaciones (NestJS)

```bash
pnpm --dir projects/backend/notificaciones start:dev
pnpm --dir projects/backend/notificaciones test
```

### Asistente IA (FastAPI)

```bash
pip install -r requirements-dev.txt          # en projects/backend/asistente-ia
uvicorn app.main:app --reload --port 8087
python -m pytest
```

### Contenido (Laravel, requiere PHP 8.4 + ext-mongodb, o Docker)

```bash
composer install && php artisan serve --port=8085   # en projects/backend/contenido
php artisan test
```

### Micro-frontends

```bash
pnpm install                                  # desde la raíz (workspace pnpm)
pnpm --dir projects/frontend/shell dev        # cada app: dev / test / build / lint
pnpm --filter "./projects/frontend/**" --parallel run dev   # las 7 a la vez
```

### Mobile

```bash
flutter pub get && flutter run    # en projects/mobile
flutter test && flutter analyze
```

## Reglas del proyecto (`.claude/rules/`)

**Antes de trabajar en cualquier archivo del proyecto, hay que respetar las
reglas correspondientes en `.claude/rules/`** según el servicio, la app o
el área en la que se esté trabajando. No es opcional ni una sugerencia —
son las decisiones ya tomadas sobre cómo se construye este proyecto, y
anulan cualquier default genérico de "buenas prácticas" cuando entran en
conflicto.

El frontmatter `globs:` de cada rule determina dónde aplica: las
transversales del backend (respuestas, excepciones, logging, endpoints,
persistencia, seguridad) aplican a los 8 servicios; cada arquitectura y
stack tiene la suya (hexagonal → `identidad`, clean → `escuela`, NestJS,
FastAPI, Laravel, gateway, Angular, React). Si algo no queda claro para el
archivo que se está por tocar, se revisa la carpeta `.claude/rules/` antes
de asumir un criterio propio. Puntos que se suelen olvidar:

- **Id interno Long + `codigo_{tabla}` UUID v7** en todas las tablas y
  colecciones; el id Long nunca sale del servicio ([PERSISTENCIA_BD_BACKEND.md](rules/backend/PERSISTENCIA_BD_BACKEND.md)).
- **Una base por servicio**: nadie lee la base de otro.
- **Un recurso HTTP nuevo** se registra en el gateway y en
  [MICROSERVICIOS.md](rules/MICROSERVICIOS.md) en el mismo cambio.
- **No anticipar trabajo de casos de uso**: los servicios y apps tienen hoy
  su estructura, sus Models y sus migraciones; controllers, servicios y
  pantallas se crean con cada caso de uso, a partir de su Historia de
  Usuario y sus Unidades de Trabajo.

## Commits de Git — Claude no commitea el trabajo final, salvo pruebas que deshace

**Claude nunca deja un commit permanente sin que la persona lo haya
pedido.** Decidir cuándo y qué commitear como entrega final del trabajo es
responsabilidad exclusiva de la persona que está usando Claude.

Cuando el trabajo esté listo para commitear, Claude debe:

1. Seguir [CONVENCIONES_GIT.md](rules/CONVENCIONES_GIT.md) al armar el
   mensaje: formato `tipo[SCOPE]: descripción concreta`, **un commit por
   scope**, y dentro de `BACK`/`FRONT` **un commit por microservicio o
   micro-frontend** (la descripción empieza con su nombre:
   `feat[BACK]: pagos -- ...`).
2. Proponerle a la persona el/los mensajes de commit listos para usar
   (idealmente en un bloque de código para copiar/pegar), explicando
   brevemente qué agrupa cada uno si hay más de uno.
3. Dejar que la persona misma corra `git add`/`git commit` — Claude no lo
   ejecuta como entrega final salvo que se lo pidan explícitamente.

**Excepción: probar que un hook de git dispara de verdad.** Claude sí puede
hacer un `git commit` puntual con el único fin de confirmar que
`pre-commit`/`commit-msg` disparan en un commit real (no simulado) —
**siempre y cuando lo deshaga inmediatamente después** con
`git reset --soft HEAD~1` (deja los cambios de vuelta como estaban,
staged, sin perder nada) y avise a la persona que hizo esto. Nunca se deja
como commit definitivo sin que se lo pidan.

Para todo lo demás que no necesite un commit real de por medio (probar que
un mensaje cumple el formato, o que `lint-staged` corre el linter
correcto), usar los mecanismos que ni siquiera tocan el historial de git:

```bash
pnpm exec lint-staged                     # simula el pre-commit sobre lo que esté staged
sh .husky/commit-msg archivo_con_mensaje.txt  # simula el commit-msg sin commitear
echo "feat[BACK]: pagos -- mensaje de prueba" | pnpm exec commitlint  # valida un mensaje suelto
```
