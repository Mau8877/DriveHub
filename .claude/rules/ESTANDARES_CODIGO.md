# Estándares de Código

Aplica a todo el repositorio (backend Java, frontend TypeScript, mobile
Dart). Convenciones de nombrado y comentarios comunes a los tres lenguajes —
las convenciones específicas de arquitectura/librerías de cada uno viven en
sus reglas dedicadas (`.claude/rules/backend/`, `.claude/rules/frontend/`,
`.claude/rules/mobile/`).

## Nombrado

Se sigue el estándar propio de cada lenguaje — no se inventa una convención
distinta cuando el lenguaje ya tiene una establecida:

| | Java | TypeScript | Dart |
| --- | --- | --- | --- |
| Variables / funciones / métodos | `camelCase` | `camelCase` | `camelCase` |
| Clases / tipos / interfaces / enums | `PascalCase` | `PascalCase` | `PascalCase` |
| Constantes | `UPPER_SNAKE_CASE` | `UPPER_SNAKE_CASE` | `UPPER_SNAKE_CASE` (Dart suele usar `lowerCamelCase` para constantes locales, pero las constantes de configuración/catálogo del proyecto — ej. códigos de error, claves de storage — usan `UPPER_SNAKE_CASE` para que se distingan a simple vista de una variable normal) |
| Archivos | `PascalCase.java` (nombre = nombre de la clase pública) | `camelCase.ts`/`PascalCase.tsx` (componentes React en `PascalCase`) | `snake_case.dart` (convención oficial de Dart/Flutter) |

## Enforcement automático (pre-commit)

Estas convenciones no dependen solo de que alguien se acuerde de seguirlas —
`husky` (`.husky/pre-commit`) corre `lint-staged` en cada `git commit`, que
a su vez corre el linter que corresponde según qué archivos quedaron
staged. Si el linter encuentra un error real (no un warning ignorable), el
commit **se cancela**.

| Lenguaje | Herramienta | Config | Alcance |
| --- | --- | --- | --- |
| TypeScript (frontend) | ESLint (`--fix`) | `projects/frontend/eslint.config.js` | `projects/frontend/**/*.{ts,tsx}` |
| Java (backend) | Checkstyle | `projects/backend/checkstyle.xml` | `projects/backend/**/*.java` |
| Dart (mobile) | `flutter analyze` | `projects/mobile/analysis_options.yaml` (`flutter_lints`) | `projects/mobile/**/*.dart` |

**El pre-commit solo lintea, no corre tests.** Se evaluó agregar
`vitest run`/`flutter test`/`mvn test` al mismo hook, pero se decidió que
correr la suite de tests completa es responsabilidad de cada dev **antes**
de commitear (`pnpm test`, `flutter test`, `./mvnw test`), no algo que
bloquee cada commit automáticamente — mantiene el pre-commit rápido. Nada
impide agregarlo después a un hook `pre-push` si en el futuro se decide que
sí hace falta forzarlo.

- La config de Checkstyle es **propia del proyecto** (no la de Google/Sun) —
  cubre indentación (4 espacios, ver `.editorconfig`) y las convenciones de
  nombrado de esta tabla. El `MethodName` usa un patrón relajado
  (`^[a-z][a-zA-Z0-9]*(_[a-zA-Z0-9]+)*$`) para permitir la convención de
  nombres de test de [TESTING_BACKEND.md](backend/TESTING_BACKEND.md)
  (`crearUsuario_conEmailDuplicado_lanzaConflictException`), que de otra
  forma chocaría con el `MethodName` default de Checkstyle.
- Checkstyle **no** está atado a ninguna fase del build de Maven (`mvn test`/
  `package` no lo corren) — se invoca aparte con `mvn checkstyle:check` (o
  `./mvnw checkstyle:check` desde el host, que comparte el mismo `~/.m2`
  que el contenedor vía `docker-compose.yml`), igual que ESLint se invoca
  aparte de `pnpm build`.
- `flutter analyze` no analiza archivo por archivo — corre sobre todo el
  paquete `projects/mobile/` aunque el hook solo dispare por un `.dart`
  puntual modificado. Es el comportamiento normal de la herramienta.

**Testing (Vitest en frontend) está instalado y configurado, pero no
enganchado al pre-commit** — ver [TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md),
[TESTING_BACKEND.md](backend/TESTING_BACKEND.md) y
[TESTING_MOBILE.md](mobile/TESTING_MOBILE.md) para cuándo/cómo escribir
tests en cada proyecto; correrlos antes de commitear (`pnpm test`,
`./mvnw test`, `flutter test`) es criterio de cada dev, no algo que el hook
fuerce.

## Formato del mensaje de commit (`commitlint`)

**Ya implementado.** `commitlint.config.js` (raíz) + hook `.husky/commit-msg`
validan que todo commit cumpla el formato de
[CONVENCIONES_GIT.md](CONVENCIONES_GIT.md) (`tipo[SCOPE]: descripción`) antes
de aceptarlo.

- **No** extiende `@commitlint/config-conventional` — ese preset espera el
  formato estándar `tipo(scope): descripción` (paréntesis), y este proyecto
  usa `tipo[SCOPE]: descripción` (corchetes, scope en mayúsculas). Por eso
  hay un `parserPreset.parserOpts.headerPattern` custom en vez de extender
  el preset de Conventional Commits.
- Reglas activas: `type-enum` (los 10 tipos de la tabla de
  `CONVENCIONES_GIT.md`), `scope-enum` (`BACK`/`FRONT`/`MOBILE`/`INFRA`/`DOCS`/`REPO`),
  `type-empty`/`scope-empty`/`subject-empty` (a diferencia de Conventional
  Commits estándar, acá el scope es **obligatorio**, nunca opcional), y
  `header-max-length` (100).
- Se puede probar sin hacer un commit real: `echo "feat[BACK]: algo" | pnpm exec commitlint`.

## Comentarios

- Un comentario explica el **por qué** de una decisión no obvia (una
  restricción externa, un workaround puntual, un comportamiento que
  sorprendería a quien lo lea) — no repite lo que el código ya dice.
- Si borrar el comentario no genera ninguna duda para quien lea el código
  después, el comentario sobra. Ejemplo de comentario que **no** se escribe:

  ```java
  // suma dos números
  int sumarNumeros(int a, int b) { return a + b; }
  ```

## Formato Better Comments

Dentro de bloques de comentario tipo Javadoc/JSDoc/Dartdoc, se usan estos
prefijos (compatibles con la extensión Better Comments de VSCode) para
marcar el tipo de nota:

| Prefijo | Significado |
| --- | --- |
| `* texto` | Comentario normal/default |
| `* * texto` | Información importante a resaltar |
| `* ! texto` | Alerta, código deprecado o "no usar" |
| `* ? texto` | Pregunta abierta / algo a revisar |
| `* TODO: texto` | Pendiente de hacer |

### Ejemplo — Java (Javadoc)

```java
/**
 * Calcula el precio final aplicando el descuento vigente.
 *
 * * Vive acá porque el service no puede depender del motor de pricing
 * * directamente (evitaría un ciclo de dependencias entre módulos).
 * ! No usar para pedidos ya facturados — el descuento no debe recalcular
 * ! un total histórico.
 * ? ¿Este cálculo debería vivir en el módulo de pagos en vez de acá?
 * TODO: soportar descuentos combinables cuando el módulo de promociones
 * TODO: esté implementado.
 */
BigDecimal calcularPrecioFinal(BigDecimal base) { ... }
```

### Ejemplo — TypeScript (JSDoc)

```ts
/**
 * Normaliza la respuesta paginada del backend a la forma que espera la tabla.
 *
 * * Necesario porque `meta.totalPages` puede venir en 0 cuando la colección
 * * está vacía, y el componente de paginación espera mínimo 1.
 * ? ¿Convendría que esta normalización viva en el interceptor de axios
 * ? en vez de en cada hook que la necesita?
 */
function normalizarMeta(meta: PageMeta): PageMeta { ... }
```

### Ejemplo — Dart (Dartdoc)

```dart
/// Mapea la respuesta cruda del endpoint de login al modelo `Sesion`.
///
/// * Se hace acá y no en el provider porque el mismo mapeo también lo
/// * necesita el flujo de refresh token.
/// ! El campo `refreshToken` nunca se loguea ni se imprime en debug.
/// TODO: manejar el caso de `role` desconocido en vez de asumir `usuario`.
Sesion mapearSesion(Map<String, dynamic> json) { ... }
```
