# UT-CU06-FE-06 — Sección de la foto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-06` |
| **Nombre** | Sección de la foto |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-FE-05 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona sube, cambia o quita su foto y la ve reflejada en la barra superior sin recargar.

## Objetivo

El componente `FotoPerfil` (flujo F2) dentro de la pantalla de perfil.

## Alcance

- `features/perfil/components/foto-perfil.ts` (`app-foto-perfil`, recibe `perfil = input.required<Perfil>()` y emite `perfilCambiado`): muestra la foto actual (o las iniciales) y los botones "Subir foto" / "Cambiar foto" (un `<input type="file" accept="image/jpeg,image/png,image/webp">` oculto activado por el botón) y "Quitar foto" (solo si hay foto).
- **Revisión previa en el cliente** (solo feedback inmediato; el backend decide) en `features/perfil/schemas/foto.reglas.ts`: tipo JPG, PNG o WebP (por el tipo del archivo), tamaño ≤ 5 MB y dimensiones ≤ 4000×4000 px (se leen con `createImageBitmap` o un `Image` temporal). Mensajes en español: "Solo se admiten imágenes JPG, PNG o WebP", "La imagen no puede superar 5 MB", "La imagen no puede superar 4000×4000 píxeles". Un archivo rechazado no se envía.
- Con un archivo válido: vista previa con `URL.createObjectURL` (se libera con `revokeObjectURL`), botón "Guardar foto" y "Cancelar"; al guardar llama a `PerfilService.subirFoto` y deshabilita los botones mientras sube.
- Éxito: aviso "Foto actualizada", el perfil devuelto reemplaza al actual, y emite `perfil-actualizado` (con la `fotoUrl` nueva) para el avatar del shell. Quitar: pide confirmación en línea ("¿Quitar tu foto?"), llama a `quitarFoto`, aviso "Foto eliminada" y emite el evento con `fotoUrl: null`.
- Errores del backend con el mapa de la UT 04 (`ERR_IDE_16`, `ERR_IDE_17`); si la imagen mostrada falla al cargar, cae a las iniciales.
- Se agrega a la pantalla `Perfil` (UT 05).
- Tests (`tests/foto-perfil.spec.ts` y `tests/foto.reglas.spec.ts`): reglas de tipo, tamaño y dimensiones; un archivo inválido no llama a la API y muestra su mensaje; uno válido sube `FormData` con `archivo`; tras subir se emite `perfil-actualizado`; quitar pide confirmación y llama a `quitarFoto`; `ERR_IDE_16` se muestra sin perder la foto actual.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/perfil/components/foto-perfil.ts`
- `projects/frontend/identidad/src/app/features/perfil/schemas/foto.reglas.ts`
- `projects/frontend/identidad/src/app/features/perfil/screens/perfil.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/foto-perfil.spec.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/foto.reglas.spec.ts`

## Fuera de alcance (no tocar)

- Recortar, rotar o comprimir la imagen antes de subirla.
- El avatar del shell (UT CU06-FE-02): solo recibe el evento.
- Arrastrar y soltar (el selector de archivo alcanza).

## Resultado esperado

La persona sube una foto válida y la ve en el perfil y en la barra superior; las inválidas se rechazan con un mensaje claro.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual (con el backend de CU06): subir un PNG, ver el avatar cambiar; subir un PDF renombrado a `.png` y ver el rechazo del backend; subir un archivo de más de 5 MB y ver el mensaje del cliente; quitar la foto.
