# DevelopLayout

Plantilla base de arquitectura full-stack. Monorepo con pnpm workspaces
(`pnpm-workspace.yaml`: `projects/*`) que contiene tres
proyectos independientes más la infraestructura de desarrollo:

- **`projects/backend`** — API REST en Java 21 + Spring Boot 4.1 (Maven),
  PostgreSQL + Flyway para persistencia, Redis para caché, MinIO para
  almacenamiento de archivos.
- **`projects/frontend`** — Panel web en React 19 + TypeScript + Vite,
  TanStack (Query/Router/Form/Table), Tailwind CSS 4 + shadcn.
- **`projects/mobile`** — App en Flutter/Dart, Riverpod para estado,
  `go_router` para navegación, `dio` como cliente HTTP.
- **`infrastructure/`** — configuración de Docker Compose: nginx (proxy),
  postgres, redis, minio, prometheus, grafana.

## Comandos básicos

### Backend (`projects/backend`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # correr en modo dev
./mvnw test                                              # tests
./mvnw clean package                                     # build
```

### Frontend (`projects/frontend`)

```bash
pnpm dev       # servidor de desarrollo (Vite, puerto 5173)
pnpm build     # build de producción
pnpm lint      # ESLint
```

### Mobile (`projects/mobile`)

```bash
flutter pub get     # instalar dependencias
flutter run          # correr en un emulador/dispositivo conectado
flutter test          # tests
flutter analyze       # análisis estático
```

### Stack completo (Docker Compose)

Desde la raíz del repo, con los `.env` ya creados a partir de sus
`.env.example` correspondientes (raíz, `projects/backend/`,
`projects/frontend/`):

```bash
docker compose up
```

Levanta proxy (puerto 80), frontend (5173), backend (8080), postgres (5432),
redis (6379), minio (9000/9001), prometheus (9090) y grafana (3000). Ver
[docs/README.md](../docs/README.md) para el detalle completo de variables de
entorno y el checklist de puesta en marcha.

## Reglas del proyecto (`.claude/rules/`)

**Antes de trabajar en cualquier archivo del proyecto, hay que respetar las
reglas correspondientes en `.claude/rules/`** según la carpeta/proyecto en
el que se esté trabajando (backend, frontend, mobile, o transversal a todo
el repo). No es opcional ni una sugerencia — son las decisiones ya tomadas
sobre cómo se construye este proyecto específico, y anulan cualquier
default genérico de "buenas prácticas" cuando entran en conflicto.

El propio frontmatter `globs:` de cada archivo de regla determina en qué
carpetas aplica automáticamente — no hace falta ir a buscarlas a mano, pero
si algo no queda claro para el archivo que se está por tocar, se revisa la
carpeta `.claude/rules/` correspondiente antes de asumir un criterio propio.

## Commits de Git — Claude no commitea el trabajo final, salvo pruebas que deshace

**Claude nunca deja un commit permanente sin que la persona lo haya
pedido.** Decidir cuándo y qué commitear como entrega final del trabajo es
responsabilidad exclusiva de la persona que está usando Claude.

Cuando el trabajo esté listo para commitear, Claude debe:

1. Seguir [CONVENCIONES_GIT.md](rules/CONVENCIONES_GIT.md) al armar el
   mensaje: formato `tipo[SCOPE]: descripción concreta`, y **un commit por
   scope** (si el cambio toca backend y frontend, son dos mensajes
   separados, no uno).
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
echo "feat[BACK]: mensaje de prueba" | pnpm exec commitlint  # valida un mensaje suelto
```
