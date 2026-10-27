# UT-CU44-BE-01 — Pagos: filtro de credencial de servicio y `ERR_PAG_19`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-01` |
| **Nombre** | Pagos: filtro de credencial de servicio y `ERR_PAG_19` |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Las decisiones 3, 4 y 12 del [README](./README.md#decisiones-de-diseño). [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (rate limit del gateway como ejemplo de filtro que escribe el sobre a mano), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Mirar cómo `RestAuthenticationHandlers` ya escribe el sobre de un `401`.

## Solicitud de negocio

Un proceso de Académico sin sesión de ningún estudiante debe poder pedir a Pagos descontar o devolver horas, y nadie más (ni el navegador ni la app) debe poder hacerlo.

## Objetivo

Toda petición a `/internal/**` de Pagos exige el header `X-Service-Key` con la credencial configurada; sin ella responde `401 ERR_PAG_19` y no llega a ningún controller.

## Alcance

- Propiedad `app.internal.credencial-academico` en `application.properties`, con valor `${PAGOS_CREDENCIAL_ACADEMICO:}` (vacío por defecto).
- `CredencialServicioFilter` (`OncePerRequestFilter`) en `com.drivehub.pagos.config`:
  - Solo actúa sobre rutas que empiezan por `/internal/` (`shouldNotFilter` para el resto).
  - Lee `X-Service-Key` y lo compara con la credencial configurada usando `MessageDigest.isEqual` sobre los bytes (tiempo constante, no `equals`).
  - Si la credencial configurada está vacía, **rechaza todo** (falla cerrado).
  - Si falta, no coincide o no hay credencial configurada: responde `401` con el sobre estándar (`status: failed`, `error: ERR_PAG_19`, mensaje «Credencial de servicio inválida») escrito a mano y **no** continúa la cadena.
  - Registra un `WARN` (`module: internal`) con la ruta, **nunca** el valor recibido ni el configurado.
- `SecurityConfig`: `permitAll` para `/internal/**` (la credencial reemplaza al JWT en estas rutas) y el filtro agregado con `addFilterBefore(...)` delante del filtro de Bearer. El filtro se construye en `SecurityConfig`, **no** se anota `@Component` (así no se registra dos veces en la cadena del servlet).
- Constante `CREDENCIAL_INVALIDA = "ERR_PAG_19"` en `PagosErrorCodes`.
- Documentar `ERR_PAG_19` en `docs/errors/pagos/ERRORES_PAGOS.md` (formato obligatorio de la guía) y su link en `docs/errors/README.md`.
- Tests unitarios del filtro: credencial correcta → continúa; header ausente → 401 `ERR_PAG_19`; credencial incorrecta → 401; credencial configurada vacía → 401 aunque el header venga vacío; ruta `/api/v1/...` → el filtro no actúa; el valor del header nunca aparece en el log.
- Test `@WebMvcTest`/`MockMvc` sobre `SecurityConfig` con una ruta de prueba bajo `/internal/`: con credencial responde `200`, sin ella `401`, y una ruta `/api/v1/...` sigue pidiendo JWT.

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/config/CredencialServicioFilter.java` (crear)
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/config/SecurityConfig.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java`
- `projects/backend/pagos/src/main/resources/application.properties`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/config/` (tests)
- `docs/errors/pagos/ERRORES_PAGOS.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Los endpoints internos y sus controllers (UT 03).
- Cambios en `BilleteraService` (UT 02).
- La variable en `docker-compose.yml` y `.env.example` (UT 05).
- Las rutas `/api/v1/**` y su JWT: no cambian.
- Registrar nada en el gateway.

## Resultado esperado

Pagos tiene una puerta cerrada en `/internal/**`: solo pasa quien manda la credencial exacta. Sin credencial configurada, nadie pasa.

## Validación

1. `./mvnw test` en `pagos` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el test de seguridad: `/internal/prueba` sin header → `401` con `error: ERR_PAG_19`; con el header correcto → `200`.
4. Revisar el log del test: no aparece ningún valor de credencial.
