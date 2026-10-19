# UT-CU01-FE-01 — Modelos y reglas de validación del registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-01` |
| **Nombre** | Modelos y reglas de validación del registro |
| **Caso de uso** | CU01-FE |
| **Microservicio** | `identidad` (micro-frontend Angular) |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (estructura de un remoto, formularios con mensajes en español, tests Vitest) y [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (las reglas deben coincidir con mobile y backend).

## Solicitud de negocio

Las reglas de cada campo (edad mínima, longitud de contraseña) deben ser las mismas en web, mobile y backend, y estar escritas en un solo lugar para que cambiarlas no obligue a buscarlas por la pantalla.

## Objetivo

Existen los tipos del registro y las funciones de validación puras, con sus tests.

## Alcance

- `src/app/features/registro/models/registro.models.ts`: `RegistroEstudianteRequest` (`nombres`, `apellidos`, `fechaNacimiento` como `yyyy-MM-dd`, `email`, `password`, `telefono?`, `escuelaSlug`), `UsuarioRegistrado` (`codigo`, `email`, `nombres`, `apellidos`) y `EscuelaPublica` (`codigo`, `slug`, `nombre`).
- `src/app/features/registro/registro.reglas.ts`: constantes `EDAD_MINIMA_REGISTRO = 18`, `PASSWORD_MIN = 9`, `PASSWORD_MAX = 64`, longitudes máximas (80, 80, 120, 20), y validadores de Reactive Forms (`ValidatorFn`) más una función pura `edadEnAnios(fechaNacimiento, hoy)`:
  - `fechaNoFutura` (la fecha debe estar en el pasado) y `edadMinima` (≥ 18 años cumplidos a la fecha de hoy; el día en que cumple 18 es válido).
- `src/app/features/registro/registro.mensajes.ts`: mapa de claves de error a mensajes **en español** (obligatorio, formato de correo, longitud mínima/máxima de contraseña, fecha futura, menor de edad).
- Tests `registro.reglas.spec.ts`: 17 años y 364 días → inválido; exactamente 18 años hoy → válido; fecha futura → inválido; contraseña de 8 → inválida, de 9 → válida, de 65 → inválida.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/registro/models/registro.models.ts`
- `projects/frontend/identidad/src/app/features/registro/registro.reglas.ts`, `registro.mensajes.ts`, `registro.reglas.spec.ts`

## Fuera de alcance (no tocar)

- El service, la pantalla y las rutas (UT 02 a 04).
- Estado global o NgRx: no se agrega.
- Validaciones de formularios de otros CU.

## Resultado esperado

Las reglas del formulario existen en un solo archivo, probadas, listas para que la pantalla las use.

## Validación

1. `pnpm --dir projects/frontend/identidad test` pasa.
2. `pnpm --dir projects/frontend/identidad lint` pasa.
