# Unidades de trabajo — CU01-FE

**Caso de uso:** Pantalla web de registro del estudiante en el enlace de su escuela, sin sesión.
**Micro-frontend(s):** `identidad` (Angular, UT 01 a 04) y `shell` (Angular, UT 05) — ver [MICROSERVICIOS.md](../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU01 - Registrar estudiante`](../../CasosDeUso/CU01%20-%20Registrar%20estudiante.md)
**Recurso HTTP / contrato:** pantalla `/registro/:escuelaSlug`; consume `GET /api/v1/escuelas/{slug}` y `POST /api/v1/usuarios` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../.claude/rules/): [ARQUITECTURA_FRONTEND.md](../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [TAILWIND_STYLES_FRONTEND.md](../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Para formularios y reglas de validación compartidas con mobile: [FORMULARIOS_MOBILE.md](../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md#consistencia-de-reglas-de-negocio-con-el-frontend-web). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La escuela viene de la URL** (`/registro/:escuelaSlug`). La pantalla consulta `GET /api/v1/escuelas/{slug}` para mostrar el nombre de la escuela; si responde 404 muestra "escuela no disponible" y no muestra el formulario.
2. **La ruta es pública y vive fuera de `ShellLayout`** (sin menú ni cabecera): el shell monta en `/registro` un módulo público que expone el remoto `identidad` (`'./routes-publicas'`), distinto de `'./routes'`, que seguirá siendo el de las pantallas con sesión (CU05, CU06).
3. **Sin auto-login** (CA02): al terminar bien, el remoto navega a `/login?registrado=1` con `navegar(...)` del contexto del shell. El login real es CU02; esta UT solo agrega al login provisional del shell un aviso de "Cuenta creada".
4. **Reglas de validación iguales a backend y mobile** (en un solo archivo, importadas por el formulario): nombres y apellidos obligatorios (máx. 80); fecha de nacimiento obligatoria, pasada y con al menos 18 años cumplidos (`EDAD_MINIMA_REGISTRO = 18`); correo obligatorio, con formato válido (máx. 120); contraseña de 9 a 64 caracteres; teléfono opcional (máx. 20).
5. **Errores del backend en español, inline:** `ERR_IDE_01` bajo el campo correo, `ERR_IDE_02` bajo la fecha de nacimiento, `ERR_IDE_03` como estado "escuela no disponible"; un `ERR_SYS_01` con detalle por campo se muestra bajo cada campo.

## Contrato de datos / pantalla

| Elemento | Detalle |
|----------|---------|
| Ruta | `/registro/:escuelaSlug` (pública, fuera del layout del shell) |
| Campos | nombres, apellidos, fecha de nacimiento, correo, contraseña, teléfono (opcional) |
| Petición | `POST /api/v1/usuarios` con los campos + `escuelaSlug` (de la URL) |
| Éxito | navega a `/login?registrado=1` |
| Estados | cargando escuela, escuela no disponible, formulario, enviando, error general |

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU01-FE-01](./UT-CU01-FE-01.md) | Modelos y reglas de validación del registro | `identidad` | — |
| [UT-CU01-FE-02](./UT-CU01-FE-02.md) | `RegistroService` (llamadas a la API) | `identidad` | 01 |
| [UT-CU01-FE-03](./UT-CU01-FE-03.md) | Página de registro (formulario) | `identidad` | 01, 02 |
| [UT-CU01-FE-04](./UT-CU01-FE-04.md) | Rutas públicas y exposición por federación | `identidad` | 03 |
| [UT-CU01-FE-05](./UT-CU01-FE-05.md) | Ruta pública `/registro` y aviso en el login | `shell` | 04 |

**Cadena:** `01 → 02 → 03 → 04 → 05`. Necesita el backend (UT-CU01-BE-04, 06 y 13) solo para la validación manual de punta a punta; las UT 01 a 04 se prueban con `HttpTestingController`.

## Criterios de aceptación cubiertos

| CA (CU01) | Descripción | UTs |
|-----------|-------------|-----|
| CA02 | Tras registrarse, va al login sin sesión | 03, 05 |
| CA03 | Campos obligatorios y correo inválido, error por campo | 01, 03 |
| CA04 | Contraseña de menos de 9 caracteres | 01, 03 |
| CA05, CA06 | Menor de 18 años rechazado; exactamente 18 aceptado | 01, 03 |
| CA07 | Correo en uso (`ERR_IDE_01`) | 03 |
| CA08 | Escuela inexistente o inactiva | 02, 03 |
| CA01, CA09 | La petición nunca envía rol | 02 |

CA10 y CA11 son de backend/gateway.

## Follow-ups (no se implementan en estas UT)

- Pantalla de login real y guard de sesión del shell (CU02).
- Pantallas para que el Administrador cree Administradores de escuela e Instructores (CU01_1, CU01_2).
- Tests e2e entre shell y remoto.

## Qué NO incluyen estas UT

- Login, sesión, token o menú por rol del shell.
- Cualquier pantalla del remoto `identidad` distinta del registro (CU05, CU06).
- Cambios de versiones o de `shared` en `federation.config.mjs` de las apps Angular: solo se agrega un `exposes` al remoto `identidad`.
- Un componente de selección de escuela: la escuela siempre viene del enlace.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend: `identidad -- ...` (UT 01 a 04) y `shell -- ...` (UT 05).
