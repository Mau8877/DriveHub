# UT-CU03-BE-04 — Prueba de integración del cierre de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-04` |
| **Nombre** | Prueba de integración del cierre de sesión |
| **Caso de uso** | CU03-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU03-BE-01, UT-CU03-BE-02 |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Seguir el patrón de `AuthIntegrationTest` (CU02), incluido el `assumeTrue` de Docker.

## Solicitud de negocio

Confirmar con una base real que cerrar sesión deja inservible la sesión renovable y no toca la del otro dispositivo.

## Objetivo

Hay una prueba de integración que recorre login → logout → refresh.

## Alcance

Casos nuevos en `AuthIntegrationTest`:

- Login `WEB` → logout con la cookie → `200` con la cookie borrada → refresh con esa cookie → `401 ERR_IDE_09`.
- Login `MOVIL` → logout con el token en el cuerpo → refresh con ese token → `401 ERR_IDE_09`.
- Login en `WEB` y en `MOVIL` con la misma cuenta → logout de `WEB` → el refresh de `MOVIL` sigue funcionando (CA05).
- Logout sin cookie, o con un token inventado, o repetido dos veces → `200` siempre y no revoca otras sesiones.
- Logout sin `Authorization` funciona (CA07).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/test/java/com/drivehub/identidad/AuthIntegrationTest.java`

## Fuera de alcance (no tocar)

- Código de producción (si la prueba falla, se corrige en la UT 01 o 02).
- Pruebas del gateway (UT 03).

## Resultado esperado

El cierre de sesión queda verificado de punta a punta contra PostgreSQL.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker disponible los casos corren; sin Docker quedan `SKIPPED`).
2. `./mvnw checkstyle:check` sin errores.
