# UT-CU53-BE-03 — Académico: avisar a la escuela de clases canceladas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU53-BE-03` |
| **Nombre** | Aviso de clase confirmada cancelada en la bandeja de la escuela |
| **Caso de uso** | CU53-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU53-BE-01 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una clase confirmada que se cae afecta la agenda del instructor y del auto: el Administrador tiene que saberlo.

## Objetivo

Cuando se cancela una clase que ya estaba confirmada (la cancele el estudiante o el instructor), Académico deja un aviso «Clase cancelada» en la bandeja de la escuela.

## Alcance

- `common/notificaciones/NotificacionesClient.java`: `avisarEscuela(...)`, igual que en Pagos.
- `features/reservas/service/AvisosReserva.java`: en `claseCancelada`, en las dos ramas de una clase confirmada, `avisarEscuela` con `El estudiante|El instructor canceló la clase del {fecha} a las {hora}.`. La escuela sale de la franja de la reserva (`FranjaRepository.findById`); sin franja, no avisa. Evento `clase.cancelada.escuela`.
- Tests: `AvisosReservaTest` y `NotificacionesClientTest`.

Archivos esperados (crear/tocar): los dos de producción y sus dos tests.

## Fuera de alcance (no tocar)

- Una reserva que nunca se confirmó (`PENDIENTE_PAGO`): no avisa a la escuela.
- Confirmación, reprogramación, recordatorios y exámenes.
- Guardar la escuela en la reserva.

## Resultado esperado

Cada clase confirmada que se cancela aparece una sola vez en la bandeja de su escuela, sin frenar la cancelación.

## Validación

1. `./mvnw test -Dtest=AvisosReservaTest,NotificacionesClientTest` en `projects/backend/academico`: verde.
2. `./mvnw checkstyle:check` sin errores.
3. Manual: el estudiante cancela una clase confirmada; el Administrador ve «Clase cancelada» en la campana.
