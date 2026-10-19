# Unidades de trabajo — CU02-FE

**Caso de uso:** Sesión única de la web: el shell inicia, guarda, renueva y recupera la sesión; los remotos la reciben y piden la renovación al shell.
**Micro-frontend(s):** `shell` (UT 02 a 06), `@drivehub/mf-contract` (UT 01), remotos Angular `identidad`, `escuela`, `pagos` (UT 07 a 09) y remotos React `examenes`, `contenido`, `asistente-ia` (UT 10 a 12) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU02 - Iniciar sesión`](../../../CasosDeUso/CU02%20-%20Iniciar%20sesi%C3%B3n.md)
**Recurso HTTP / contrato:** pantalla `/login` del shell; consume `POST /api/v1/auth/login` y `POST /api/v1/auth/refresh` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (sesión compartida), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [TANSTACK_QUERY_FRONTEND.md](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) y [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md) (remotos React), y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El shell es el único dueño de la sesión.** El access token vive solo en memoria del shell (nunca `localStorage`); el refresh token vive en una cookie `HttpOnly` que el navegador envía solo (mismo origen). Los remotos nunca inician sesión, solo piden el token (`obtenerToken`) y la renovación (`renovarSesion`).
2. **Recuperar la sesión al recargar o abrir otra pestaña:** antes de que arranque el router, el shell llama a `restaurar()` (renovación con la cookie). Si sirve, sigue con sesión; si no, `sesionGuard` lleva a `/login?redirect=<url pedida>`.
3. **Una renovación a la vez:** en la pestaña se comparte la promesa en curso y entre pestañas se serializa con Web Locks (`navigator.locks`), porque la rotación invalida el refresh token usado: dos renovaciones simultáneas con el mismo token harían fallar a una.
4. **Renovación proactiva y reactiva:** el shell renueva al 80 % de la vida del token (12 de 15 minutos) y, si una petición recibe 401, renueva una vez y la repite (shell y remotos). Si no se puede renovar, `renovar()` limpia la sesión y lleva a `/login` con el motivo.
5. **Redirección segura tras el login:** solo se acepta un `redirect` interno (empieza con `/` y no con `//`) para evitar redirecciones abiertas.
6. **Quién usa la web:** solo Super admin y Administrador de escuela; el backend rechaza al resto con `ERR_IDE_08` y la pantalla lo explica ("ingresa desde la app móvil").
7. **Contrato nuevo:** `ContextoShell.renovarSesion()` se agrega a `@drivehub/mf-contract` y llega a los remotos React por `MountProps`.

## Contrato de datos / pantalla

| Elemento | Detalle |
|----------|---------|
| Ruta | `/login` (con `loginGuard`: con sesión redirige a `/`) |
| Campos | correo y contraseña |
| Petición | `POST /api/v1/auth/login` con `dispositivo: 'WEB'` |
| Éxito | guarda la sesión y navega a `redirect` o `/` |
| Estados | enviando, error por código, motivo de sesión terminada |
| Guard | `sesionGuard` en todas las rutas bajo `ShellLayout`; `/registro` queda público |

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU02-FE-01](./UT-CU02-FE-01.md) | Contrato `renovarSesion` en `mf-contract` | `mf-contract` | — |
| [UT-CU02-FE-02](./UT-CU02-FE-02.md) | `AuthApiService` y modelos de la sesión | `shell` | 01 |
| [UT-CU02-FE-03](./UT-CU02-FE-03.md) | `SesionService` | `shell` | 02 |
| [UT-CU02-FE-04](./UT-CU02-FE-04.md) | Interceptor con reintento y `renovarSesion` en el contexto | `shell` | 01, 03 |
| [UT-CU02-FE-05](./UT-CU02-FE-05.md) | Arranque con recuperación de sesión, guards y rutas | `shell` | 03 |
| [UT-CU02-FE-06](./UT-CU02-FE-06.md) | Pantalla de inicio de sesión | `shell` | 03, 05 |
| [UT-CU02-FE-07](./UT-CU02-FE-07.md) | Reintento por 401 en `identidad` | `identidad` | 01, 04 |
| [UT-CU02-FE-08](./UT-CU02-FE-08.md) | Reintento por 401 en `escuela` | `escuela` | 01, 04 |
| [UT-CU02-FE-09](./UT-CU02-FE-09.md) | Reintento por 401 en `pagos` | `pagos` | 01, 04 |
| [UT-CU02-FE-10](./UT-CU02-FE-10.md) | Reintento por 401 en `examenes` | `examenes` | 01, 04 |
| [UT-CU02-FE-11](./UT-CU02-FE-11.md) | Reintento por 401 en `contenido` | `contenido` | 01, 04 |
| [UT-CU02-FE-12](./UT-CU02-FE-12.md) | Reintento por 401 en `asistente-ia` | `asistente-ia` | 01, 04 |

**Cadena:** `01 → 02 → 03 → 04, 05 → 06`; `01 + 04 → 07 … 12` (independientes entre sí). Aplicar la 01 y la 04 seguidas: entre ambas el build del shell queda roto porque `renovarSesion` es un miembro requerido del contrato. Necesita el backend de CU02 solo para la validación manual de punta a punta.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01, CA02 | Login web y navegar entre micro-frontends sin volver a ingresar | 03, 05, 06 |
| CA03 | Sin sesión lleva al login y vuelve a la URL pedida | 05, 06 |
| CA05 | Campos obligatorios sin enviar | 06 |
| CA04, CA06, CA08, CA09, CA10 | Mensajes de cada rechazo del backend | 06 |
| CA12 | Renovación en segundo plano antes de vencer | 03 |
| CA13 | Rechazo por sesión vencida: renovar una vez y repetir | 04, 07 a 12 |
| CA14 | Recargar o abrir otra pestaña recupera la sesión | 03, 05 |
| CA17 | Renovación rechazada lleva al login con el motivo | 03, 06 |
| CA18 | Con sesión vigente, `/login` lleva al inicio | 05 |

## Follow-ups (no se implementan en estas UT)

- Cerrar sesión (CU03), recuperar contraseña (CU04) y menú por rol del layout.
- Autorización por rol en las rutas del shell.
- Prueba end-to-end con navegador entre el shell y los remotos.

## Qué NO incluyen estas UT

- Registro de estudiantes (CU01), que sigue público y fuera del layout.
- Guardar la sesión fuera de la memoria del shell o leer la cookie de refresh desde JavaScript.
- Cambios de versiones o `shared` en `federation.config.mjs`.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend o paquete: `mf-contract`, `shell`, `identidad`, `escuela`, `pagos`, `examenes`, `contenido`, `asistente-ia`.
