# UT-CU47-BE-02 — Credencial de servicio para rutas internas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU47-BE-02` |
| **Nombre** | Credencial de servicio para rutas internas |
| **Caso de uso** | CU47-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), la tabla de comunicación de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) (CU44) y, como modelo a copiar, `CredencialServicioFilter` de `projects/backend/pagos/.../config/`.

## Solicitud de negocio

Solo n8n puede pedir códigos, vínculos y sesiones de WhatsApp; nadie más, ni siquiera con un token de usuario.

## Objetivo

Toda ruta bajo `/internal/` de Identidad exige el header `X-Service-Key` correcto.

## Alcance

- Filtro `CredencialServicioFilter` en `infrastructure/config/`: para rutas `/internal/**` compara `X-Service-Key` con `app.internal.credencial-n8n` (variable `IDENTIDAD_CREDENCIAL_N8N`) en **tiempo constante** (`MessageDigest.isEqual`); si falta o no coincide responde 401 con el sobre estándar y `ERR_SYS_04`, sin revelar la causa. Nunca escribe la credencial en el log.
- `SecurityConfig`: `/internal/**` pasa por este filtro y queda fuera de la autenticación JWT; sigue cerrado todo lo demás.
- Si la variable no está configurada, el servicio **rechaza** todas las rutas internas (no las deja abiertas).
- Variable en `.env.example` y `docker-compose.yml`.
- Tests: sin header, con header incorrecto, con header correcto, con variable vacía, y que las rutas `/api/v1/**` no se vean afectadas.

Archivos esperados (crear/tocar): `CredencialServicioFilter.java`, `SecurityConfig.java`, `application.properties`, `.env.example`, `docker-compose.yml` y los tests.

## Fuera de alcance (no tocar)

- Los endpoints de WhatsApp (UT 03 a 05) y el gateway: estas rutas **no** se registran allí.

## Resultado esperado

Una petición a cualquier `/internal/**` sin la credencial correcta recibe 401; con ella llega al controller.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. `curl -i http://localhost:8081/internal/v1/whatsapp/sesiones` sin header → 401 `ERR_SYS_04`.
3. El gateway no tiene ninguna ruta `/internal`: `curl -i http://localhost:8080/internal/v1/whatsapp/sesiones` → 404.
