# UT-CU05-FE-06 — Pantalla "Editar escuela"

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-06` |
| **Nombre** | Pantalla "Editar escuela" |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

Lo único que el Super admin puede corregir de una escuela es su nombre.

## Objetivo

Existe el formulario que cambia el nombre de una escuela activa.

## Alcance

- `screens/escuela-editar.ts` (selector `app-escuela-editar`): lee `:codigo` de la ruta, carga la escuela con `EscuelasService.obtener(codigo)` y muestra el slug **solo lectura** y el nombre editable (Reactive Form tipado, reglas de la UT 03, mensaje inline).
- Si la escuela está inactiva (`activa = false`) no muestra el formulario: avisa "La escuela está inactiva. Reactívala para editarla" con un enlace de vuelta al listado. Si no existe (`ERR_ESC_02`), avisa que no se encontró.
- Éxito: vuelve al listado con el aviso "Escuela actualizada correctamente". `ERR_ESC_06` (se desactivó mientras se editaba) se muestra en un aviso del formulario.
- `tests/escuela-editar.spec.ts`: carga y rellena el nombre; el slug no se puede editar; nombre inválido no se envía; `PATCH` con el cuerpo esperado; inactiva y no encontrada muestran su aviso sin formulario; éxito navega con el aviso.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/escuelas/screens/escuela-editar.ts`
- `projects/frontend/escuela/src/app/features/escuelas/tests/escuela-editar.spec.ts`

## Fuera de alcance (no tocar)

- Editar el slug o al Administrador: no se puede.
- El montaje de la ruta (UT 07).

## Resultado esperado

El Super admin puede corregir el nombre de una escuela activa.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
