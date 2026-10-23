# UT-CU12-BE-01 — `@CurrentUserId` y escuela del token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-01` |
| **Nombre** | `@CurrentUserId` y escuela del token |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio) (usuario autenticado en un controller), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#anotacionesdecorators-custom), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Tomar como modelo `CurrentUserId` y `CurrentUserIdArgumentResolver` de `identidad`.

## Solicitud de negocio

Un instructor solo puede gestionar sus propios horarios: quién es y a qué escuela pertenece tiene que salir de su sesión, nunca de un dato que envíe la app.

## Objetivo

Los controllers de Académico pueden recibir como parámetros el código de la persona autenticada y el de su escuela, ya extraídos del JWT.

## Alcance

- `config/CurrentUserId` (anotación `@Target(PARAMETER)`): inyecta un `UUID` con el `sub` del JWT.
- `config/CurrentEscuelaId` (anotación): inyecta un `UUID` con el claim `escuela` del JWT; `null` si el claim no existe (el Super admin no tiene escuela).
- `config/CurrentUserArgumentResolvers` (un solo `HandlerMethodArgumentResolver` que atiende las dos anotaciones, o dos clases si queda más claro) y `config/WebMvcConfig` que los registra. Si no hay `Authentication` con `Jwt`, lanza `UnauthorizedException` con `ERR_SYS_04` (no debería pasar: la cadena de seguridad ya rechaza antes).
- Tests unitarios del resolver: devuelve el `sub` como `UUID`; devuelve el claim `escuela`; devuelve `null` sin el claim; sin autenticación lanza `UnauthorizedException`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/config/CurrentUserId.java`, `CurrentEscuelaId.java`, `CurrentUserArgumentResolvers.java`, `WebMvcConfig.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/config/CurrentUserArgumentResolversTest.java`

## Fuera de alcance (no tocar)

- `SecurityConfig` y `JwtRolesConverter`: ya validan el token y arman los roles.
- Los controllers que usarán las anotaciones (UT 11).
- Un filtro automático por escuela: solo se **lee** el claim.
- Cualquier cambio en `identidad`: se define su propia copia de la anotación (cada servicio la tiene, ver CONVENCIONES_JAVA_BACKEND.md).

## Resultado esperado

Un controller de Académico puede declarar `@CurrentUserId UUID instructorCodigo` y `@CurrentEscuelaId UUID escuelaCodigo` y recibirlos listos.

## Validación

1. `./mvnw test` en `academico` pasa con `CurrentUserArgumentResolversTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
