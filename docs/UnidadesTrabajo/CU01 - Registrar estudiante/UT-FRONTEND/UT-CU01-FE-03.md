# UT-CU01-FE-03 — Página de registro (formulario)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-03` |
| **Nombre** | Página de registro (formulario) |
| **Caso de uso** | CU01-FE |
| **Microservicio** | `identidad` (micro-frontend Angular) |
| **Depende de** | UT-CU01-FE-01, UT-CU01-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (standalone, signals, Reactive Forms tipados con `FormBuilder.nonNullable`, errores en español bajo el campo, componentes de ruta sin sufijo `Component`) y [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) (solo clases de tokens, sin valores arbitrarios).

## Solicitud de negocio

Es la pantalla que ve el estudiante: debe indicarle claramente qué escuela es, qué datos faltan o están mal, y qué hacer al terminar.

## Objetivo

Existe la página de registro con su formulario, estados y manejo de errores.

## Alcance

- `src/app/features/registro/pages/registro.ts` (componente de ruta, selector `app-registro`) que:
  - Lee `escuelaSlug` de la ruta (`input()` con `withComponentInputBinding` o `ActivatedRoute`) y llama a `RegistroService.obtenerEscuela`. Estados con signals: `cargandoEscuela`, `escuela`, `escuelaNoDisponible`, `enviando`, `errorGeneral`, `registrado`.
  - Si la escuela no está disponible (404) muestra el mensaje "Esta escuela no está disponible" y no el formulario.
  - Formulario reactivo tipado con los campos nombres, apellidos, fecha de nacimiento (`<input type="date">`), correo, contraseña y teléfono (opcional), validado con `registro.reglas.ts` y mensajes de `registro.mensajes.ts`, inline debajo de cada campo. El botón "Registrarme" se deshabilita mientras `enviando`.
  - Muestra el nombre de la escuela en el título ("Registro en {nombre}").
  - Al enviar, llama a `registrar` con `escuelaSlug` de la URL; en éxito reemplaza el formulario por una confirmación: "Tu cuenta fue creada. Ingresa desde la app móvil de DriveHub." (signal `registrado`). No navega a otra ruta: los estudiantes no usan la web.
  - Errores del backend: `ERR_IDE_01` → error en el campo correo ("Este correo ya está registrado"); `ERR_IDE_02` → error en fecha de nacimiento ("Debes tener al menos 18 años"); `ERR_IDE_03` → estado "escuela no disponible"; `ERR_SYS_01` → mensaje general; cualquier otro → mensaje general en español, sin mostrar texto técnico.
- Estilos con clases de tokens (`bg-card`, `border-border`, `text-muted-foreground`, `text-destructive`...), centrado y legible en móvil.
- Tests (`registro.spec.ts`): formulario inválido no llama a `registrar`; con 17 años muestra el error de edad; con datos válidos llama a `registrar` con el cuerpo esperado y muestra la confirmación "ingresa desde la app móvil"; `ERR_IDE_01` marca el campo correo; 404 de la escuela oculta el formulario.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/registro/pages/registro.ts`
- `projects/frontend/identidad/src/app/features/registro/pages/registro.spec.ts`

## Fuera de alcance (no tocar)

- Rutas y federación (UT 04), y el shell (UT 05).
- Un selector de escuela o campo de código de escuela: la escuela viene de la URL.
- Iniciar sesión después de registrar, guardar tokens o tocar `SesionService`.
- Validación de formato del teléfono más allá del largo máximo.

## Resultado esperado

Con el service simulado, la página muestra el formulario de la escuela, valida cada campo con las reglas compartidas, registra y muestra la confirmación de la app móvil.

## Validación

1. `pnpm --dir projects/frontend/identidad test` pasa.
2. `pnpm --dir projects/frontend/identidad lint` y `build` pasan.
3. Revisión visual en el ancho de un teléfono (375 px) sin scroll horizontal.
