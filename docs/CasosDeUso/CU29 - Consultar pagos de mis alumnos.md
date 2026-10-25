# Caso de uso

| Id   | Nombre                          | Paquete   | Prioridad | Estado    |
| ---- | ------------------------------- | --------- | --------- | --------- |
| CU29 | Consultar pagos de mis alumnos  | P5. Pagos | Media     | Pendiente |

**Actor principal:** Instructor
**Actores secundarios:** —
**Objetivo:** Ver a sus alumnos con sus horas compradas, cuánto tiempo han estado con él y los comprobantes de sus pagos.
**Plataforma:** Móvil

> "Mis alumnos" son los estudiantes que la escuela le asignó (asignación de alumnos a instructores, CU10, todavía por documentar). Las horas gastadas con él las registra la billetera de horas cuando Académico descuenta las horas de cada clase (casos de uso de Académico, por documentar); sin ese dato no se pueden mostrar.

**Descripción:**
El instructor abre "Mis alumnos" y ve la lista de los estudiantes que tiene asignados, con su nombre y tres datos: las horas que compraron, las que han gastado con él y las que les quedan. Puede buscar por nombre. Al abrir un alumno ve el detalle del tiempo que ha estado con él, clase a clase, y debajo un resumen básico de sus pagos (plan, fecha y estado) con acceso al comprobante de cada uno. El instructor ve solo a sus alumnos asignados y solo el tiempo gastado con él, no con otros instructores.

---

## Precondiciones

- El instructor tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela ([CU09: Gestionar instructores](CU09%20-%20Gestionar%20instructores.md)).
- El instructor tiene alumnos asignados.

## Flujo principal (listar)

1. El instructor elige "Mis alumnos".
2. El sistema muestra los estudiantes que tiene asignados: nombre, horas compradas, horas gastadas con él y horas restantes, ordenados por nombre y paginados.
3. El instructor puede buscar por nombre.

## Flujo F1: ver el detalle de un alumno

1. El instructor abre un alumno.
2. El sistema muestra el resumen de horas y el tiempo gastado con el instructor, clase a clase (fecha y horas), de la más reciente a la más antigua.
3. Debajo, el sistema muestra los pagos del alumno (plan, horas, fecha y estado), de más reciente a más antiguo.

## Flujo F2: ver un comprobante

1. En el detalle de un alumno, el instructor elige el comprobante de un pago confirmado.
2. El sistema entrega el comprobante en PDF ([CU30](CU30%20-%20Generar%20comprobante.md)).

## Flujos alternativos y excepciones

- **A1 (paso 2):** El instructor no tiene alumnos asignados → estado vacío con un aviso.
- **A2 (F1, F2):** El alumno no existe o no está asignado a este instructor → 404 (no se revela si existe).
- **A3 (F1 paso 2):** No hay datos de horas gastadas con él (el alumno todavía no tuvo clases) → se muestra cero y la lista de clases vacía.
- **A4 (F2):** El comprobante todavía no se generó → se informa que está en preparación.
- **A5 (cualquier paso):** La persona no es instructor → 403; sin sesión o con sesión vencida → 401.
- **A6 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo sus alumnos asignados:** el sistema consulta a Escuela cuáles son y nunca muestra un estudiante no asignado al instructor del token ni de otra escuela.
- **Solo el tiempo gastado con él:** las horas gastadas cuentan los consumos registrados con su código de instructor; el tiempo con otros instructores no se suma ni se muestra. Las horas compradas y las restantes son del alumno en total.
- **Qué ve de los pagos:** plan, horas, fecha, estado y el comprobante. Esta pantalla no tiene acciones: el instructor no confirma, revierte ni reembolsa nada.
- **Datos del alumno:** su nombre sale de Identidad; DriveHub no copia sus datos personales a Pagos.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). Mensajes en español, sin detalles técnicos.

## Postcondiciones

- No cambia ningún dato: es una consulta.

## Criterios de aceptación

- **CA01:** Dado un instructor con alumnos asignados, el sistema debe listarlos paginados con nombre, horas compradas, horas gastadas con él y horas restantes.
- **CA02:** Dada una búsqueda por nombre, el sistema debe mostrar solo los alumnos asignados que coincidan.
- **CA03:** Dado un alumno asignado, el sistema debe mostrar el tiempo gastado con el instructor, clase a clase, y los pagos del alumno con plan, horas, fecha y estado.
- **CA04:** Dado un alumno que tuvo clases con otro instructor, el sistema no debe sumar ese tiempo a las horas gastadas con este instructor.
- **CA05:** Dado un estudiante no asignado al instructor, inexistente o de otra escuela, el sistema debe responder 404.
- **CA06:** Dado un pago confirmado del alumno, el sistema debe entregar su comprobante en PDF; si todavía no existe, debe informar que está en preparación.
- **CA07:** Dado un instructor sin alumnos asignados, el sistema debe mostrar el estado vacío.
- **CA08:** Dada una persona que no es instructor, el sistema debe responder 403, y 401 si no hay sesión válida.

## Fuera de alcance

- **Asignar alumnos a instructores:** es de Escuela (CU10).
- **Registrar el tiempo de cada clase:** lo hace Académico al descontar las horas.
- **Cobrar, confirmar o reembolsar:** son de [CU26](CU26%20-%20Realizar%20pago.md), [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md) y [CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md).
- **Ver los pagos de toda la escuela** (Administrador) y **pantalla web** para el instructor.

## Desarrollador

Mauro
