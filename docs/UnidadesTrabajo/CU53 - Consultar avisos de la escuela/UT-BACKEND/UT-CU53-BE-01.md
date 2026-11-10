# UT-CU53-BE-01 — Notificaciones: avisos a la escuela y bandeja del Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU53-BE-01` |
| **Nombre** | Avisos dirigidos a una escuela y lectura de su bandeja por el Administrador |
| **Caso de uso** | CU53-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — (sobre lo ya hecho en CU36, CU38 y CU45) |

## Antes de implementar

[NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) (capas, GraphQL, errores) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

El Administrador tiene que poder leer en el panel los avisos de su escuela, y los servicios necesitan una forma de mandárselos sin saber quién la administra.

## Objetivo

El pedido interno acepta una escuela como destino y la bandeja de CU38 atiende también al Administrador de escuela con la de su escuela.

## Alcance

- `dto/solicitud-notificacion.request.ts`: `usuarioCodigo` pasa a obligatorio solo sin `escuelaCodigo` (`@ValidateIf`); nuevo `escuelaCodigo?` (`@IsUUID`) con una restricción propia que impide mandarlo junto con `usuarioCodigo`.
- `services/envio-notificacion.service.ts`: sin `usuarioCodigo`, la bandeja es el `escuelaCodigo` y la decisión es fija `SOLO_BANDEJA` (sin preferencias, correo ni push).
- `services/bandeja.ts` (nuevo): `ROLES_CON_BANDEJA` y `bandejaDe(usuario)` (escuela del token para `ADMIN_ESCUELA`, `sub` para los demás; sin escuela, 403 `ERR_SYS_05`).
- `controllers/notificaciones.controller.ts` y `graphql/notificaciones.resolver.ts`: `@Roles(...ROLES_CON_BANDEJA)` y `bandejaDe(usuario)` en lugar de `usuario.codigo`.
- Tests: `tests/bandeja-escuela.spec.ts` (nuevo) y un caso en `tests/envio-notificacion.service.spec.ts`.

Archivos esperados (crear/tocar): los cinco de arriba más los dos specs.

## Fuera de alcance (no tocar)

- `historial-notificaciones.service.ts` y el repositorio: no cambian, reciben la bandeja ya resuelta.
- Preferencias y dispositivos: siguen siendo solo de personas.
- Los productores de avisos (UT 02 y 03).

## Resultado esperado

Un `POST /internal/v1/notificaciones` con `escuelaCodigo` deja un aviso `IN_APP` en la bandeja de la escuela; un `ADMIN_ESCUELA` lo lista, lo cuenta y lo marca por REST o GraphQL.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`: todo en verde, incluidos los casos nuevos.
2. `pnpm --dir projects/backend/notificaciones lint` y `build` sin errores.
3. Manual: con la credencial de servicio, pedir un aviso con `escuelaCodigo`; con el token de un Administrador de esa escuela, `query { conteoNoLeidas }` devuelve 1.
