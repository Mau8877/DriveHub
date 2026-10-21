# UT-CU02-FE-06 — Pantalla de inicio de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-06` |
| **Nombre** | Pantalla de inicio de sesión |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU02-FE-03, UT-CU02-FE-05 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

Es la puerta de entrada de la web: debe decir con claridad qué falló y a dónde ir, y llevar a la persona a donde iba.

## Objetivo

`features/login/screens/login.ts` es el formulario real de inicio de sesión.

## Alcance

- Formulario reactivo tipado (correo y contraseña, ambos obligatorios; correo con formato) con mensajes en español inline y las mismas reglas que el backend. Botón deshabilitado mientras envía.
- Al enviar llama a `SesionService.iniciarSesion`. Éxito → navega a la URL del parámetro `redirect` si es una ruta interna segura (empieza con `/` y no con `//`), si no a `/`.
- Errores por código: `ERR_IDE_04` → "Correo o contraseña incorrectos"; `ERR_IDE_05` → el mensaje del servidor (dice cuándo reintentar); `ERR_IDE_06` → "Tu cuenta está inactiva. Contacta a tu administrador"; `ERR_IDE_07` → "La escuela de tu cuenta está suspendida"; `ERR_IDE_08` → el mensaje del servidor (indica la plataforma correcta); otros → mensaje general en español sin texto técnico.
- Muestra el aviso del `motivo` de la URL cuando una renovación fallida lleva al login (`ERR_IDE_05` a `ERR_IDE_09`; `ERR_IDE_09` = "Tu sesión terminó. Inicia sesión de nuevo"). Sin `motivo` (primera visita o redirección del guard) no muestra aviso.
- Texto fijo: "Si eres estudiante o instructor, ingresa desde la app móvil". Estilos solo con clases de tokens, legible en móvil.
- Quitar el texto "Pendiente de implementar (CU02)".
- Tests: formulario inválido no llama a la API; éxito navega al `redirect` seguro; un `redirect` externo o con `//` se ignora; cada código de error muestra su mensaje; el botón se deshabilita enviando.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/features/login/screens/login.ts`, `login.spec.ts`

## Fuera de alcance (no tocar)

- Enlaces a recuperar contraseña (CU04) o a crear cuenta: no se agregan.
- Menú por rol y avisos.
- Cerrar sesión (CU03).
- Guardar el correo recordado en el navegador.

## Resultado esperado

Un administrador inicia sesión en la web, llega al panel o a la URL que había pedido y puede navegar entre micro-frontends sin volver a ingresar.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Con el stack: iniciar sesión con `adminexample@gmail.com` entra al panel; con un estudiante muestra el mensaje de la app móvil; recargar la página conserva la sesión.
