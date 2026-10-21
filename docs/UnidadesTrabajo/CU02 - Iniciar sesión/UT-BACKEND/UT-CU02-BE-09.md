# UT-CU02-BE-09 — Escuela por código en `EscuelaRestAdapter`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-09` |
| **Nombre** | Escuela por código en `EscuelaRestAdapter` |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-03, UT-CU02-BE-04 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)

## Solicitud de negocio

Para saber si la escuela de la cuenta está suspendida, Identidad le pregunta a Escuela, que es el dueño del dato.

## Objetivo

`EscuelaConsultaPort.buscarPorCodigo` está implementado por REST interno.

## Alcance

- `EscuelaRestAdapter.buscarPorCodigo(UUID)`: `GET {app.escuela.url}/api/v1/escuelas/por-codigo/{codigo}`, desempaqueta el sobre y devuelve `Optional<EscuelaRef>` con `activa`; 404 → vacío; otros fallos se propagan.
- `EscuelaRef` lleva `activa` (en `buscarActivaPorSlug` siempre `true`, porque Escuela solo devuelve activas).
- Tests contra el Escuela falso de `EscuelaRestAdapterTest`: escuela activa, escuela suspendida (`activa=false`), código inexistente (404 → vacío) y error 500 → excepción.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/escuela/EscuelaRestAdapter.java`
- `.../domain/port/out/EscuelaRef.java`
- `src/test/java/.../EscuelaRestAdapterTest.java`

## Fuera de alcance (no tocar)

- Cache, reintentos o circuit breaker.
- Cambiar `buscarActivaPorSlug` más allá de poblar `activa`.
- El servicio de login (UT 05).

## Resultado esperado

Identidad puede saber si la escuela de una cuenta está activa.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. `./mvnw checkstyle:check` sin errores.
