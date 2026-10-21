# UT-CU05-FE-05 — Pantalla "Nueva escuela"

> **Reemplazada** por el piloto de `<dh-gestion>` (ver [README, decisión 4](./README.md)): la feature `escuelas` quedó como esquema + pantalla + ruta única. Se conserva como historia.

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-05` |
| **Nombre** | Pantalla "Nueva escuela" |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

Dar de alta una escuela exige crear a la vez a su Administrador.

## Objetivo

Existe el formulario que crea la escuela y su Administrador.

## Alcance

- `screens/escuela-crear.ts` (selector `app-escuela-crear`): Reactive Form tipado (`FormBuilder.nonNullable`) con la escuela (nombre, slug) y el Administrador (nombres, apellidos, fecha de nacimiento, correo, contraseña con botón de mostrar y teléfono opcional). Reglas de la UT 03, mensajes en español **inline** bajo cada campo.
- El slug se **sugiere** con `slugDesdeNombre` mientras la persona no lo haya editado a mano; después ya no se pisa. Al enviar, el botón queda deshabilitado con "Creando…".
- Éxito: vuelve al listado (`/escuelas`) enviando por el estado del `Router` el aviso "Escuela creada correctamente".
- Errores del servidor (`ApiError`): `ERR_ESC_03` bajo el slug ("Ese slug ya está en uso"), `ERR_IDE_01` bajo el correo, `ERR_IDE_02` bajo la fecha de nacimiento; cualquier otro, el mensaje en un aviso del formulario. Nada se pierde del formulario al fallar.
- `tests/escuela-crear.spec.ts`: validaciones por campo y que no se envía un formulario inválido; el slug se sugiere desde el nombre y deja de sugerirse tras editarlo; el cuerpo enviado es el esperado; éxito navega al listado con el aviso; los tres errores de negocio caen en su campo; la contraseña nunca se muestra ni queda en el DOM como texto salvo que se pida verla.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/escuelas/screens/escuela-crear.ts`
- `projects/frontend/escuela/src/app/features/escuelas/tests/escuela-crear.spec.ts`

## Fuera de alcance (no tocar)

- Editar la escuela o al Administrador.
- El montaje de la ruta (UT 07).
- Enviar correo al Administrador: no existe.

## Resultado esperado

El Super admin puede crear una escuela con su Administrador desde la web.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
