# UT-CU45-BE-04 — Resolvers de avisos, preferencias y dispositivos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-BE-04` |
| **Nombre** | Resolvers de avisos, preferencias y dispositivos |
| **Caso de uso** | CU45-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU45-BE-02, UT-CU45-BE-03 |

## Antes de implementar

[NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) (sección «GraphQL»), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (se loguea en el service, no en el resolver).

## Solicitud de negocio

La persona tiene que poder pedir sus avisos, su contador y sus preferencias en una sola petición, y hacer los cambios de siempre, con las mismas reglas que en REST.

## Objetivo

Los tres features exponen sus consultas y mutaciones por GraphQL llamando a los mismos services que sus controllers.

## Alcance

Por feature, en `{feature}/graphql/` (y registrado como provider en su módulo):

- **notificaciones:** `notificacion.type.ts` (`Notificacion`, `PaginaNotificaciones`, enum `TipoNotificacion`), `consultar-notificaciones.args.ts` (`page`, `pageSize`, `leida`, `tipo`, con las reglas de REST) y `notificaciones.resolver.ts` con `notificaciones`, `conteoNoLeidas`, `marcarLeida` y `marcarTodasLeidas`. Roles `ESTUDIANTE` e `INSTRUCTOR`.
- **preferencias:** `preferencias.type.ts`, `preferencias.input.ts` (mismas reglas y mensajes que `PreferenciasRequest`) y `preferencias.resolver.ts` con `preferencias` y `guardarPreferencias`. Roles `ESTUDIANTE` e `INSTRUCTOR`.
- **dispositivos:** `dispositivo.type.ts` (enum `Plataforma`, sin el token de Firebase), `registrar-dispositivo.input.ts` y `dispositivos.resolver.ts` con `registrarDispositivo` y `quitarDispositivo`. Solo sesión válida, como el controller.
- Quién es la persona sale de `@UsuarioActual()`; ningún argumento recibe un código de persona.
- Tests: `graphql-api.spec.ts` (cada operación con el service mockeado, autenticación, roles, aislamiento entre personas, validaciones y errores catalogados) y `reglas-iguales-a-rest.spec.ts` (los mismos datos dan los mismos mensajes en REST y GraphQL).

**Archivos esperados (crear/tocar):** los tipos, inputs, args y resolvers de arriba; `notificaciones.module.ts`, `preferencias.module.ts`, `dispositivos.module.ts`; `graphql-api.spec.ts`, `reglas-iguales-a-rest.spec.ts`.

## Fuera de alcance (no tocar)

- Los services, repositories y controllers (no se modifican).
- Un listado de dispositivos o cualquier campo que REST no exponga.
- Consultas que juntan datos de otros servicios (follow-up del README).
- Migrar clientes (móvil y web) a GraphQL.

## Resultado esperado

Las ocho operaciones del contrato funcionan por `POST /api/v1/graphql` con las mismas reglas, permisos y códigos que sus rutas REST.

## Validación

1. `pnpm --dir projects/backend/notificaciones exec vitest run src/common/graphql` pasa.
2. Una consulta con la bandeja, el contador y las preferencias se responde en una sola respuesta.
3. Una consulta que pide `usuarioCodigo` como argumento, o el campo `tokenFcm` de un dispositivo, se rechaza (el esquema no los tiene).
4. `pnpm --dir projects/backend/notificaciones test` completo, `lint` y `build` sin errores.
