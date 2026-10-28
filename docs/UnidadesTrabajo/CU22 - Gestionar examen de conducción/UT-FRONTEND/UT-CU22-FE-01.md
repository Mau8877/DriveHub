# UT-CU22-FE-01 — Acción con formulario en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-01` |
| **Nombre** | Acción con formulario en `<dh-gestion>` |
| **Caso de uso** | CU22-FE |
| **Microservicio** | `gestion` (paquete compartido `packages/gestion`) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md): lo que falta se amplía en el paquete para todos.

## Solicitud de negocio

El Administrador registra el puntaje de un examen sin salir del listado.

## Objetivo

Una acción por fila puede abrir un formulario en el drawer y enviar su cuerpo a la ruta de la acción.

## Alcance

- `dh-gestion.js`: la vista `accion` (`_abrirAccion`, `_definicion`), el envío con el método y la ruta de la acción y su `mensajeExito`; título y botón del drawer.
- `index.d.ts`: `Accion.formulario`.
- Tests en `dh-gestion.test.js`.

Archivos esperados (crear/tocar): `dh-gestion.js`, `index.d.ts`, `dh-gestion.test.js`.

## Fuera de alcance (no tocar)

- Crear y editar, y las acciones sin formulario (siguen igual, con o sin `confirmar`).

## Resultado esperado

Una acción con `formulario` abre el drawer; al enviar llama a `{recurso}/{ruta}` con el cuerpo y vuelve a la lista con el aviso.

## Validación

1. `pnpm --dir packages/gestion test` (165 tests).
2. En un esquema con una acción con formulario: abre el drawer, valida los campos y envía el cuerpo.
