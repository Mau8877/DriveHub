# Caso de uso

| Id   | Nombre         | Paquete               | Prioridad | Estado                      |
| ---- | -------------- | --------------------- | --------- | --------------------------- |
| CU13 | Solicitar clase | P3. Clases y horarios | Alta      | Absorbido por el CU14       |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Pedir clases (cantidad, tipo, categoría y preferencias de horario) para que la escuela las atienda.
**Plataforma:** Móvil

> **Este CU no tiene flujo propio.** En el diseño original ([PDF](../DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf)) el estudiante solicitaba clases y alguien las aprobaba antes de reservar. Con la compra de horas ([CU26](CU26%20-%20Realizar%20pago.md)) y la billetera de horas ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)) ya implementadas, el estudiante elige directamente la franja que quiere y la paga con sus horas: todo eso es [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md). El número CU13 no se reasigna a otro caso de uso (ver [CASOS_DE_USO.md](../../.claude/rules/CASOS_DE_USO.md)).

**Descripción:**
Lo que cubría este CU se reparte así: elegir cuántas clases y cuándo es parte de elegir franjas en el CU14; pagarlas es la compra de horas del CU26; y la aprobación por un administrador o instructor deja de existir porque el estudiante reserva solo en horarios que el propio instructor ofreció.

---

## Qué quedó sin uso

- La colección `solicitudes_clase` y su modelo (`SolicitudClase`, `EstadoSolicitud`, `TipoClase`, `Turno`, `PreferenciasSolicitud`) en Académico no se usan. Siguen como están, sin controller ni servicio, hasta que se decida retirarlos junto con una actualización de [DISENO_BASE_DE_DATOS.md](../DriveHub/DISENO_BASE_DE_DATOS.md). El campo opcional `solicitudId` de `reservas` queda siempre vacío.
- Si más adelante la escuela quiere asignar clases en lugar de que el estudiante las elija (por ejemplo, "10 clases de práctica por las tardes"), ese flujo se documenta como un caso de uso nuevo, no reabriendo este número.

## Desarrollador

Leonel
