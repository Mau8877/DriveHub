# UT-CU01-MOB-01 — Convención de DTOs Dart

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-01` |
| **Nombre** | Convención de DTOs Dart |
| **Caso de uso** | CU01-MOB |
| **Aplicación** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) (nota "Modelos de datos: todavía sin convención definida") y [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md).

## Solicitud de negocio

Las rules dicen que antes de escribir el primer `service` real hay que definir dónde viven y cómo se escriben las clases del JSON de la API. Este CU escribe el primero.

## Objetivo

La convención de DTOs Dart queda escrita en las rules y se ejemplifica con la estructura de la feature `auth`.

## Alcance

- Editar `.claude/rules/ARQUITECTURA_MOBILE.md`: reemplazar la nota de "todavía sin convención" por la decisión (carpeta `models/` por feature, `features/{feature}/models/`, agregada al esquema de estructura de un feature) y crear `lib/features/auth/models/.gitkeep`.
- Editar `.claude/rules/mobile/CONSUMO_API_MOBILE.md`: sección "Modelos de datos (DTOs)" con las reglas:
  - Una clase por DTO en `models/`, nombre `{Recurso}Request` / `{Recurso}` (o `{Recurso}Response` si hace falta distinguir).
  - Clases inmutables (`final`), constructor `const`, `toJson()` para requests y `factory fromJson(Map<String, dynamic>)` para respuestas, escritos a mano (sin `json_serializable` ni generación de código mientras no se decida).
  - Los campos van en `camelCase`, igual que el JSON del backend.
  - Solo el `service` de la feature construye y parsea DTOs; los providers y pantallas reciben objetos ya tipados.
  - El parseo se prueba con un test unitario cuando el mapeo no es trivial (TESTING_MOBILE.md).

Archivos esperados (crear/tocar):

- `.claude/rules/ARQUITECTURA_MOBILE.md`
- `.claude/rules/mobile/CONSUMO_API_MOBILE.md`
- `projects/mobile/lib/features/auth/models/.gitkeep`

## Fuera de alcance (no tocar)

- Los DTOs reales del registro (UT 04).
- Elegir `freezed`, `json_serializable` u otra librería de generación.
- Otras reglas de mobile (estado, navegación, widgets).

## Resultado esperado

Las rules describen sin ambigüedad dónde y cómo se escriben los DTOs, y `lib/features/auth/models/` existe.

## Validación

1. Ya no aparece la nota "todavía sin convención definida" en `ARQUITECTURA_MOBILE.md`.
2. `CONSUMO_API_MOBILE.md` contiene la sección de DTOs con las cinco reglas.
3. `flutter analyze` en `projects/mobile` sigue sin problemas.
