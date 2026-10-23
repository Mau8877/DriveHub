# UT-CU12-BE-12 — Prueba de integración del CU12

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-12` |
| **Nombre** | Prueba de integración del CU12 |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-10`, `UT-CU12-BE-11` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (**patrón obligatorio**: `DockerClientFactory.isDockerAvailable()` + `Assumptions.assumeTrue` en un `@BeforeAll` y container a mano, sin `@Testcontainers`/`@Container`; modelo: `TestcontainersSmokeTest`), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

Antes de dar el CU por terminado hay que comprobar, con una base real y de punta a punta, que el instructor puede gestionar su horario como describe el caso de uso.

## Objetivo

Una prueba de integración recorre el CU12 completo contra un MongoDB real, a través de la seguridad y los controllers.

## Alcance

- `HorariosIntegracionTest`: `@SpringBootTest` + `@AutoConfigureMockMvc` con un `MongoDBContainer` (`mongo:8`) iniciado a mano en el `@BeforeAll` (tras el `assumeTrue`), apuntando la aplicación a él con `System.setProperty("spring.mongodb.uri", ...)` antes de que cargue el contexto. `app.horarios.job-habilitado=false` (la generación se invoca a mano) y un `Clock` de prueba **mutable** (`@TestConfiguration`, `@Primary`) fijado en un lunes a las 09:30 de la zona de la escuela (por ejemplo, `2026-10-26T09:30`).
- Usuarios con el post-procesador `jwt()`: Instructor A y B (`sub` distintos, claim `escuela`, autoridad `ROLE_INSTRUCTOR`) y un Estudiante (`ROLE_ESTUDIANTE`).
- Escenarios (cada uno verifica el sobre y, cuando aplica, el estado real en la base):
  1. **Crear** (CA02, CA03, CA04): lunes 08:00–12:00 en clases de 60 min → `201` con `franjasGeneradas = 14` (cuatro lunes en la ventana de 28 días, menos las franjas de las 08:00 y las 09:00 de hoy, que ya empezaron). Las franjas del `2026-10-26` son las de las 10:00 y las 11:00; las del `2026-11-02`, cuatro.
  2. **Listar horarios** (CA01) con y sin `filter[activa]`, y **listar franjas** por `filter[fecha]` (CA08) y sin `filter[fecha]` (desde hoy); fecha sin franjas → `data: []`.
  3. **Datos inválidos** (CA05, CA06): día 9 → `400 ERR_SYS_01`; `horaFin` ≤ `horaInicio` → `ERR_ACA_01`; ventana que no es múltiplo → `ERR_ACA_02`; vigencia inválida → `ERR_ACA_03`; en ninguno se crea nada.
  4. **Cruce** (CA07): otro horario del lunes de 11:00 a 13:00 → `409 ERR_ACA_04`; uno de 12:00 a 14:00 (comparte el borde) → `201`.
  5. **Bloquear y desbloquear** (CA09 a CA11): una franja libre futura pasa a `BLOQUEADA` y vuelve a `LIBRE`; bloquear la ya bloqueada → `409 ERR_ACA_08`; desbloquear una libre → `409 ERR_ACA_09`; con el reloj adelantado a las 10:30, la franja de las 10:00 de hoy ya empezó → `409 ERR_ACA_10` al bloquearla (y al desbloquear una bloqueada); una franja marcada `RESERVADA` directamente en la base → `409 ERR_ACA_08` y sin cambios.
  6. **Ajeno** (CA12, CA17): el Instructor B no ve las franjas ni los horarios de A en los listados y obtiene `404 ERR_ACA_07` / `ERR_ACA_05` al intentar bloquear o desactivar uno de A.
  7. **Desactivar** (CA13, CA14): con una franja reservada marcada en la base, desactivar el horario deja `activa = false`, elimina las franjas futuras libres y bloqueadas, conserva la reservada y las pasadas; repetirlo → `409 ERR_ACA_06`; después se puede crear otro horario en la misma hora sin conflicto.
  8. **Generación diaria** (CA16): llamar a `extenderHorariosActivos()` con el mismo reloj no crea nada (idempotente); avanzando el reloj 7 días crea las franjas del lunes que entró en la ventana; un horario desactivado no genera nada.
  9. **Seguridad** (CA15): sin token → `401 ERR_SYS_04`; Estudiante → `403 ERR_SYS_05` en cada endpoint.
- Se limpia la base y se restablece el reloj a su hora inicial entre escenarios (`@BeforeEach`).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/HorariosIntegracionTest.java`

## Fuera de alcance (no tocar)

- Código de producción: si la prueba revela un defecto, se corrige en la UT que lo introdujo.
- El gateway y el JWKS reales: el JWT se simula con `jwt()`.
- Pruebas contra Identidad u otros servicios.

## Resultado esperado

Existe una prueba que demuestra que el CU12 cumple sus criterios de aceptación de backend de punta a punta.

## Validación

1. `./mvnw test` en `academico` pasa con `HorariosIntegracionTest` en verde (queda `SKIPPED` si Docker no está disponible; correrla al menos una vez en una máquina o en el CI, donde Docker sí está).
2. `./mvnw checkstyle:check` sin errores.
