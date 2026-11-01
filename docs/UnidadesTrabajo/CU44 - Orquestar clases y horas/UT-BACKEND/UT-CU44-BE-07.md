# UT-CU44-BE-07 — Académico: campos de revisión en la reserva

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-07` |
| **Nombre** | Académico: campos de revisión en la reserva |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

La decisión 9 del [README](./README.md#decisiones-de-diseño). [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md` en el mismo cambio), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok: `@Getter` en la clase, sin `@Setter` a nivel de clase). Leer `Reserva.java`.

## Solicitud de negocio

Cuando una reserva a medias no se puede resolver después de varios intentos, el sistema debe poder dejarla marcada para que una persona la revise, en vez de reintentarla para siempre.

## Objetivo

El documento `reservas` tiene los campos `intentosRetomar` y `requiereRevision`, con valores iniciales correctos y documentados.

## Alcance

- En `Reserva`: `int intentosRetomar` (inicial `0`) y `boolean requiereRevision` (inicial `false`). Solo `@Getter`; **sin setters**: se modifican con operaciones atómicas del repositorio (UT 08), no con `save()` completo.
- Las reservas ya existentes no tienen los campos: Mongo devuelve el valor por defecto de Java al leerlas, así que **no hace falta migrar** datos.
- Actualizar `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: los dos campos nuevos de `reservas`, con su significado.
- Test unitario: una `Reserva` nueva nace con `intentosRetomar = 0` y `requiereRevision = false`; un documento leído sin esos campos se deserializa con esos mismos valores (test de mapeo).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/model/Reserva.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/model/` (test)
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Las operaciones que incrementan o marcan los campos (UT 08).
- `ReservaResponse` y cualquier DTO: **los campos no se exponen al cliente**.
- Índices nuevos: no se consulta por `requiereRevision` desde ninguna pantalla.
- Cambios en `EstadoReserva`.

## Resultado esperado

La reserva puede llevar la cuenta de intentos y la marca de revisión, sin romper las reservas que ya existen.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Leer una reserva antigua de `db.reservas` (sin los campos) por el repositorio: `intentosRetomar = 0` y `requiereRevision = false`, sin error.
