# UT-CU06-FE-04 — Feature `perfil`: tipos, servicio de API y `./routes-perfil`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-04` |
| **Nombre** | Feature `perfil`: tipos, servicio de API y `./routes-perfil` |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (carpetas iguales en React y Angular, y la federación de las apps Angular)

## Solicitud de negocio

Las pantallas del perfil necesitan hablar con la API y el shell necesita una entrada para montarlas.

## Objetivo

El remoto `identidad` tiene la feature `perfil` con sus tipos y su servicio, y expone `./routes-perfil`.

## Alcance

- `features/perfil/types/perfil.types.ts`: `Perfil` (`codigo`, `nombres`, `apellidos`, `fechaNacimiento`, `telefono`, `email`, `correoVerificado`, `roles`, `escuelaCodigo`, `escuelaNombre`, `fotoUrl`), `EditarPerfilRequest` (campos opcionales), `CambiarContrasenaRequest` (con `dispositivo: 'WEB'`) y `CambiarCorreoRequest`.
- `features/perfil/api/perfil.service.ts` (`providedIn: 'root'`, `HttpClient` + `apiBaseUrl()`), ya desempaquetado por el `apiInterceptor`: `obtener()`, `editar(cambios)`, `subirFoto(archivo: File)` (`FormData` con la parte `archivo`, `PUT /usuarios/me/foto`), `quitarFoto()`, `cambiarContrasena(req)`, `solicitarCambioCorreo(req)`. Los errores llegan como `ApiError` con el código de negocio.
- `core/api/api.interceptor.ts`: si el cuerpo es `FormData`, no fija `Content-Type`. Resto sin cambios.
- `features/perfil/schemas/perfil.errores.ts`: mapa de código de negocio a mensaje en español (`ERR_IDE_02`, `ERR_IDE_01`, `ERR_IDE_11`, `ERR_IDE_14`, `ERR_IDE_15`, `ERR_IDE_16`, `ERR_IDE_17`, `ERR_IDE_18`) y una función que devuelve el mensaje de un `ApiError` (genérico "Ocurrió un error. Intenta más tarde." si el código no está).
- `features/perfil/index.ts` (barrel) con lo que otras features puedan necesitar; hoy nada más que `Perfil`.
- `app/routes-perfil.ts`: `Routes` con `provideHttpClient(withInterceptors([apiInterceptor]))` en la ruta raíz y un `path: ''` que, por ahora, carga un componente mínimo `Perfil` (placeholder que la UT 05 reemplaza).
- `federation.config.mjs`: `exposes` suma `'./routes-perfil': './src/app/routes-perfil.ts'`. Es una adición de `exposes` del remoto `identidad`, no cambia `shared` ni `skip`, por lo que no hay que replicarla en las otras apps Angular.
- Tests (`tests/perfil.service.spec.ts`): cada método llama a la URL y verbo correctos; `subirFoto` manda `FormData` con `archivo`; los errores llegan como `ApiError`; el mapa de mensajes cubre cada código.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/perfil/types/perfil.types.ts`
- `projects/frontend/identidad/src/app/features/perfil/api/perfil.service.ts`
- `projects/frontend/identidad/src/app/features/perfil/schemas/perfil.errores.ts`
- `projects/frontend/identidad/src/app/features/perfil/index.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/perfil.service.spec.ts`
- `projects/frontend/identidad/src/app/core/api/api.interceptor.ts`
- `projects/frontend/identidad/src/app/core/api/api.interceptor.spec.ts`
- `projects/frontend/identidad/src/app/routes-perfil.ts`
- `projects/frontend/identidad/federation.config.mjs`

## Fuera de alcance (no tocar)

- Las pantallas y componentes de cada sección (UT 05 a 08) y la página pública (UT 09).
- La ruta `/perfil` en el shell (UT 03).
- Cambios en `shared` o `skip` de la federación.

## Resultado esperado

El remoto expone `./routes-perfil` y la feature `perfil` tiene un servicio probado listo para las pantallas.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual: el `remoteEntry` del remoto lista `./routes-perfil`.
