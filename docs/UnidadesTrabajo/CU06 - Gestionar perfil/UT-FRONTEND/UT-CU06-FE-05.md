# UT-CU06-FE-05 — Pantalla de perfil y edición de datos personales

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-05` |
| **Nombre** | Pantalla de perfil y edición de datos personales |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-FE-04 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (formularios tipados, mensajes en español, estilos con tokens), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona ve sus datos y corrige los personales desde una sola pantalla.

## Objetivo

La pantalla `/perfil` muestra la cuenta y permite editar nombres, apellidos, teléfono y fecha de nacimiento (flujos "ver" y F1).

## Alcance

- `features/perfil/screens/perfil.ts` (componente `Perfil`, reemplaza el placeholder de la UT 04): al abrir llama a `PerfilService.obtener()` y muestra estados de carga y de error ("No pudimos cargar tu perfil. Reintentar").
  - Bloque de **solo lectura**: correo (con una insignia "Verificado" / "Sin verificar"), rol (nombre legible: Estudiante, Instructor, Administrador de escuela, Super admin) y escuela (`escuelaNombre`; se omite para el Super admin).
  - Formulario de **datos personales** con Reactive Forms tipados (`FormBuilder.nonNullable`): nombres, apellidos, teléfono (opcional) y fecha de nacimiento (`<input type="date">`, `max` = hoy). Reglas y límites **reutilizados de `features/registro`** por su `index.ts` (80, 80, 20, edad ≥ 18, fecha en el pasado); mensajes en español, inline bajo cada campo.
  - "Guardar cambios" deshabilitado si el formulario no cambió o es inválido o mientras guarda; solo envía los campos modificados (`PATCH`); un teléfono vaciado se manda como `null`.
  - Éxito: aviso "Datos guardados" y el formulario queda con los datos devueltos; emite `perfil-actualizado` con `{ nombres, apellidos, fotoUrl }` (`emitirEvento` de `@drivehub/mf-contract`, con el mismo nombre de evento que el shell; constante propia del remoto).
  - Error: `ERR_IDE_02` ("Debes tener al menos 18 años") bajo la fecha; otros códigos con el mensaje del mapa de la UT 04, sin borrar lo escrito.
  - Deja un contenedor de secciones debajo del formulario donde las UT 06, 07 y 08 agregan sus componentes (`<app-foto-perfil>`, `<app-cambiar-contrasena>`, `<app-cambiar-correo>`); en esta UT no existen todavía.
- `features/perfil/index.ts`: nada nuevo.
- Tests (`tests/perfil.spec.ts`): carga y muestra los datos; el bloque de solo lectura no tiene campos editables; sin escuela no se muestra la fila; guardar solo manda los campos cambiados y vacía el teléfono con `null`; `ERR_IDE_02` se muestra bajo la fecha; tras guardar se emite `perfil-actualizado`; falla la carga → mensaje con reintento; los límites de campo coinciden con los del registro.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/perfil/screens/perfil.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/perfil.spec.ts`
- `projects/frontend/identidad/src/app/features/registro/index.ts` (solo si falta exportar alguna regla)
- `projects/frontend/identidad/src/app/routes-perfil.ts`

## Fuera de alcance (no tocar)

- La foto, la contraseña y el correo (UT 06 a 08).
- Editar el correo, el rol o la escuela desde este formulario.
- Valores de color o tamaño arbitrarios.

## Resultado esperado

En `/perfil` la persona ve su cuenta y guarda cambios en sus datos personales, con el avatar del shell actualizado.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual (con el backend de CU06): cambiar nombres y teléfono, guardar y ver el aviso; poner una fecha que dé menos de 18 años y ver el error bajo el campo.
