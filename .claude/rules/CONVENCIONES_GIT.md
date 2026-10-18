# Convenciones de Git

Aplica a todo el repositorio (backend, frontend, mobile, infra, docs).

## Formato de commit

```
tipo[SCOPE]: descripción concreta
```

**Este formato se valida automáticamente** — `commitlint` (hook
`.husky/commit-msg`) rechaza el commit si no cumple el patrón, el tipo no
está en la lista de abajo, o falta el `[SCOPE]`. Ver detalle de la config en
[ESTANDARES_CODIGO.md](ESTANDARES_CODIGO.md#formato-del-mensaje-de-commit-commitlint).

Ejemplos:

```
feat[BACK]: Creación de model Usuario
fix[FRONT]: Corregir invalidación de queryKey en listado de usuarios
refactor[MOBILE]: Extraer cliente HTTP a core/network
chore[REPO]: Actualizar dependencias del workspace
```

### Tipos (Conventional Commits)

| Tipo | Uso |
| --- | --- |
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un bug |
| `refactor` | Cambio de código que no altera comportamiento externo |
| `docs` | Documentación (README, comentarios, `.claude/rules/`, etc.) |
| `test` | Agregar o corregir tests, sin cambiar código de producción |
| `chore` | Tareas de mantenimiento (dependencias, config, limpieza) |
| `style` | Formato/estilo sin cambio de lógica (espacios, imports, etc.) |
| `perf` | Mejora de performance |
| `build` | Cambios al sistema de build (Maven, Vite, pubspec, Docker build) |
| `ci` | Cambios a pipelines/integración continua |

### Scopes válidos

| Scope | Cuándo |
| --- | --- |
| `BACK` | Cambios dentro de `projects/backend/` |
| `FRONT` | Cambios dentro de `projects/frontend/` |
| `MOBILE` | Cambios dentro de `projects/mobile/` |
| `INFRA` | Cambios dentro de `infrastructure/`, `docker-compose.yml` |
| `DOCS` | Cambios dentro de `docs/`, o documentación suelta en cualquier proyecto |
| `REPO` | Cambios que afectan a todo el repositorio (config raíz: `package.json`, `pnpm-workspace.yaml`, `.editorconfig`, `.prettierrc`, `.claude/`, etc.) |

No hay scope libre fuera de esta lista — un cambio que no encaja
claramente en ninguno se piensa dos veces antes de forzarlo; si de verdad no
encaja, se marca `REPO` en vez de inventar un scope nuevo sin definirlo acá
primero.

## Regla estricta: un commit, un scope

Un commit **nunca** toca más de un scope a la vez. Si un cambio afecta
backend y frontend a la vez (ej. agregar un endpoint nuevo + el hook que lo
consume), son **dos commits separados**, uno `[BACK]` y otro `[FRONT]`, aunque
se hagan en la misma sesión de trabajo y terminen en el mismo PR. Esto
mantiene el historial legible por módulo y hace posible revertir un lado sin
tocar el otro.

## Ramas

- Convención confirmada: `feature/{modulo}` (ej. `feature/usuarios`,
  `feature/auth`).
- Convención para `fix`/`refactor`/otros tipos de rama: **pendiente de
  definir** — no se inventa un patrón (`fix/...`, `hotfix/...`, etc.) hasta
  que se decida explícitamente. Hasta entonces, cualquier trabajo que no sea
  una feature nueva puede seguir usando `feature/{modulo}` como default
  provisorio.
