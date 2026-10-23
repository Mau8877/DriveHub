# UT-CU08a-FE-04 — Perfil con CI, sexo, ubicación y categoría

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-FE-04` |
| **Nombre** | Perfil con CI, sexo, ubicación y categoría |
| **Caso de uso** | CU08a-FE |
| **Microservicio** | `identidad` (web) |
| **Depende de** | UT-CU08a-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md). Como base, la feature `perfil/` (CU06) y `registro.reglas.ts` (reglas de CI).

## Solicitud de negocio

Cada persona tiene que ver y corregir su CI, su sexo y su ubicación, y ver su categoría sin poder cambiarla.

## Objetivo

La pantalla de perfil muestra y edita los datos nuevos; la categoría aparece solo como lectura.

## Alcance

- `perfil.types.ts`: `Perfil` suma `ci`, `sexo`, `direccion`, `latitud`, `longitud` y `categoriaSigla` (`string | null`); la petición de edición suma `ci`, `sexo`, `latitud` y `longitud` (sin `direccion` ni `categoriaSigla`).
- `perfil.ts`: en la sección de datos personales, `ci` y `sexo` editables; una sección "Ubicación" con `<dh-mapa-ubicacion>` precargado con el punto guardado y, de solo lectura, la `direccion` guardada (si está en blanco, el texto "Dirección pendiente de calcular"); si el punto cambia, al guardar se envía el par y la pantalla muestra la dirección recalculada que devuelve el servidor. El Super admin no tiene ubicación: no se muestra la sección si no hay punto y el rol es `SUPER_ADMIN`. Para el rol `ESTUDIANTE`, una fila "Categoría" de solo lectura con `categoriaSigla` y una ayuda ("La asigna tu escuela"); para los demás roles, esa fila no existe.
- Las reglas de CI son las de `registro.reglas.ts` (se importan de la misma feature `registro` por su `index.ts`, como pide ARQUITECTURA_FRONTEND.md para el caso de `cuenta`/`registro`). `ERR_IDE_19` aparece bajo el CI ("Este CI ya está registrado").
- `perfil.service.ts`: el `PATCH` incluye solo los campos que cambiaron.
- Tests (`tests/`): se muestran los datos nuevos; la categoría se ve solo para estudiantes y nunca es un campo editable; el `PATCH` sin cambios de ubicación no envía coordenadas y con cambios sí envía el par; `ERR_IDE_19` bajo el CI; el Super admin sin sección de ubicación. Los tests existentes del perfil se ajustan.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/perfil/types/perfil.types.ts`
- `projects/frontend/identidad/src/app/features/perfil/screens/perfil.ts`
- `projects/frontend/identidad/src/app/features/perfil/api/perfil.service.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/` (specs de servicio y pantalla)
- `projects/frontend/identidad/src/app/features/registro/index.ts` (exportar las reglas de CI, si no lo hace)

## Fuera de alcance (no tocar)

- Foto, contraseña y correo.
- Editar la categoría (la cambia la escuela, CU08).
- El registro (UT 03), el mapa (UT 01) y el backend.
- La app móvil.

## Resultado esperado

El perfil de la web muestra y edita los datos nuevos, con la categoría solo de lectura.

## Validación

1. `pnpm --dir projects/frontend/identidad test` y `lint` sin errores.
2. Con todo levantado: entrar con `adminexample@gmail.com`, abrir el perfil, mover el punto del mapa y guardar; la dirección cambia. Un estudiante ve su categoría sin poder editarla.
