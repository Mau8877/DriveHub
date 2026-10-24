# UT-CU08a-FE-03 — Registro con CI, sexo y ubicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-FE-03` |
| **Nombre** | Registro con CI, sexo y ubicación |
| **Caso de uso** | CU08a-FE |
| **Microservicio** | `identidad` (web) |
| **Depende de** | UT-CU08a-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (Reactive Forms tipados, mensajes en español bajo el campo, reglas iguales a backend y mobile, `CUSTOM_ELEMENTS_SCHEMA` con `@drivehub/ui`), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (clases `dh-*`, nada de utilidades de Tailwind en remotos Angular), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md). Como base, la feature `registro/`.

## Solicitud de negocio

El estudiante que se registra desde el navegador tiene que dar su CI, su sexo y su ubicación.

## Objetivo

La página `/register/:escuelaSlug` pide los datos nuevos y los envía; los errores del CI salen bajo su campo.

## Alcance

- `registro.types.ts`: `RegistroRequest` suma `ci`, `sexo` (`'MASCULINO' | 'FEMENINO'`), `latitud` y `longitud`.
- `registro.reglas.ts`: constantes (`CI_MIN = 5`, `CI_MAX = 15`, `CI_PATRON = /^[A-Za-z0-9-]+$/`) y validador de coordenadas; comentario de que tienen que seguir iguales a las de backend y mobile. `registro.mensajes.ts`: mensajes en español ("El CI es obligatorio", "El CI debe tener entre 5 y 15 caracteres: letras, números y guion", "Elige tu sexo", "Elige tu ubicación en el mapa").
- `registro.ts`: campos `ci` (texto) y `sexo` (dos opciones con las clases base), y `<dh-mapa-ubicacion>` en una sección "Tu ubicación" que llena `latitud` y `longitud` del formulario; el formulario no se envía sin punto. El error `ERR_IDE_19` del backend se muestra bajo el CI ("Este CI ya está registrado"), como hoy `ERR_IDE_01` bajo el correo.
- `registro.service.ts`: el `POST` incluye los campos nuevos. La pantalla de éxito no cambia.
- Tests (`tests/`): reglas de CI y coordenadas en sus límites; el servicio envía el cuerpo completo; la pantalla no envía sin CI, sin sexo o sin punto; al elegir un punto en el mapa se llenan las coordenadas; `ERR_IDE_19` se muestra bajo el CI. Los tests existentes del registro se ajustan.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/registro/types/registro.types.ts`
- `projects/frontend/identidad/src/app/features/registro/schemas/registro.reglas.ts` y `registro.mensajes.ts`
- `projects/frontend/identidad/src/app/features/registro/api/registro.service.ts`
- `projects/frontend/identidad/src/app/features/registro/screens/registro.ts`
- `projects/frontend/identidad/src/app/features/registro/tests/` (los specs de reglas, servicio y pantalla)

## Fuera de alcance (no tocar)

- El mapa (UT 01) y el backend.
- Pedir la dirección escrita o la categoría: la dirección la calcula el servidor y la categoría arranca en "sin categoría".
- La app móvil.
- Las demás pantallas de `identidad` (cuenta, perfil).

## Resultado esperado

Quien se registra en la web da su CI, su sexo y su ubicación.

## Validación

1. `pnpm --dir projects/frontend/identidad test` y `lint` sin errores.
2. Con todo levantado: abrir `/register/{slug}`, completar el formulario con un punto en el mapa y registrarse; repetir el CI muestra el error bajo ese campo.
