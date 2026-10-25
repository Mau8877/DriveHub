# Caso de uso

| Id    | Nombre                    | Paquete   | Prioridad | Estado    |
| ----- | ------------------------- | --------- | --------- | --------- |
| CU25a | Gestionar planes de horas | P5. Pagos | Alta      | Pendiente |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Definir los planes que ofrece su escuela para tomar clases: cada plan tiene un nombre, una cantidad de horas y un costo en bolivianos (con su equivalente en dólares como referencia).
**Plataforma:** Web

> **Relación con [CU25: Gestionar plan de pagos y cuotas](../DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf).** Este CU solo **define y mantiene el catálogo de planes** de la escuela. Que un estudiante contrate un plan, cómo lo paga y cómo se divide en cuotas es del CU25 y de los CU de pago, y **no** forma parte de este CU. Un plan de horas no lleva cuotas ni categoría de licencia.

**Descripción:**
El Administrador de escuela tiene en su menú el grupo "Gestionar pagos" y, dentro, "Planes de horas". Ahí ve el listado de los planes de su escuela, del más reciente al más antiguo, con su nombre, sus horas, su costo en bolivianos, su equivalente en dólares y su estado, con búsqueda por nombre, filtro de estado y paginación. Desde el listado crea un plan nuevo (nombre, horas y costo en bolivianos), corrige sus datos y lo desactiva o lo reactiva. El sistema calcula el costo en dólares a partir del costo en bolivianos con el tipo de cambio configurado; no se escribe ni se guarda. Un plan desactivado deja de ofrecerse a los estudiantes, pero no se borra: conserva su historia y se puede reactivar. Cada escuela ve y toca únicamente sus propios planes.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.

## Flujo principal (listar)

1. El Administrador elige "Planes de horas" dentro de "Gestionar pagos".
2. El sistema muestra los planes de su escuela (nombre, horas, costo en Bs, costo en USD y estado), del más reciente al más antiguo, paginados.
3. El Administrador puede buscar por nombre y filtrar por estado (activos o desactivados).

## Flujo F1: crear un plan

1. Desde el listado, el Administrador elige "Nuevo plan".
2. El sistema muestra el formulario: nombre, horas y costo en bolivianos.
3. El Administrador completa el formulario y lo envía.
4. El sistema valida los datos y comprueba que no haya otro plan de su escuela con ese nombre.
5. El sistema crea el plan en estado activo y vuelve al listado, donde aparece con su costo en dólares calculado.

## Flujo F2: editar un plan

1. En el listado, el Administrador elige "Editar" en un plan.
2. El sistema muestra los datos actuales: nombre, horas y costo en bolivianos.
3. El Administrador cambia lo que necesite y envía.
4. El sistema valida y guarda; los campos que no se envían quedan como estaban.

## Flujo F3: desactivar un plan

1. En el listado, el Administrador elige "Desactivar" en un plan activo y confirma.
2. El sistema lo pasa a desactivado: el plan sigue en el listado, pero deja de ofrecerse para contratar.

## Flujo F4: reactivar un plan

1. En el listado, el Administrador elige "Reactivar" en un plan desactivado.
2. El sistema lo pasa a activo.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4, F2 paso 4):** Falta un dato obligatorio o su formato es inválido (nombre vacío o de más de 80 caracteres, horas que no sean un entero de 1 a 500, costo que no sea mayor a 0 o con más de dos decimales o que pase del máximo) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 4, F2 paso 4):** Ya existe otro plan de su escuela con ese nombre, sin distinguir mayúsculas ni espacios de los extremos, esté activo o desactivado → 409 `ERR_PAG_01`; no se cambia nada.
- **A3 (F2, F3, F4):** El plan no existe o es de otra escuela → 404 `ERR_PAG_02` (no se revela si existe en otra escuela).
- **A4 (F3, F4):** El plan ya está en el estado pedido (desactivar uno desactivado, reactivar uno activo) → 409 `ERR_PAG_03`.
- **A5 (cualquier flujo):** La persona no es Administrador de escuela → 403; sin sesión o con sesión vencida → 401.
- **A6 (cualquier flujo):** El token no trae la escuela de la cuenta → 403 `ERR_PAG_04`.
- **A7 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona planes. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Aislamiento por escuela:** cada plan guarda el código de su escuela y toda consulta o cambio se filtra por la escuela del token (claim `escuela`). Una escuela nunca ve ni toca planes de otra.
- **Nombre:** de 1 a 80 caracteres, se guarda sin espacios en los extremos. Es único dentro de la escuela, sin distinguir mayúsculas, **incluyendo los planes desactivados**: dos escuelas distintas sí pueden usar el mismo nombre.
- **Horas:** número entero entre 1 y 500. No hay horas fraccionarias.
- **Costo en bolivianos:** mayor a 0, con hasta dos decimales y hasta 99 999 999,99. Es el único precio que se guarda.
- **Costo en dólares:** es solo una referencia. Se calcula al responder, como `costo en Bs ÷ tipo de cambio`, redondeado a dos decimales (hacia arriba desde el medio centavo). **No se guarda** en la base ni se escribe en el formulario.
- **Tipo de cambio:** es una configuración del servicio de Pagos (por defecto 6,96 Bs por dólar, el cambio oficial). No hay un servicio externo ni una pantalla para cambiarlo: cambiarlo es cambiar esa configuración, y el costo en dólares de todos los planes se recalcula solo.
- **Cambiar un plan no afecta lo ya contratado:** cada contratación de un estudiante guarda su propio monto, así que editar el costo o las horas de un plan, o desactivarlo, no cambia lo que ya contrataron los estudiantes.
- **Desactivar no es borrado:** el plan conserva su historia y se ve en el listado con estado desactivado. Reactivarlo lo deja activo con sus datos.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Crear: existe el plan activo ligado a la escuela del Administrador.
- Editar: los campos enviados quedan cambiados; el estado, igual.
- Desactivar / reactivar: el plan queda en el estado destino.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los planes de su escuela, del más reciente al más antiguo, paginados, con búsqueda por nombre y filtro por estado, mostrando para cada uno su costo en bolivianos y su equivalente en dólares.
- **CA02:** Dados un nombre, unas horas y un costo válidos, el sistema debe crear el plan activo ligado a la escuela del Administrador.
- **CA03:** Dado un nombre ya usado por otro plan de la misma escuela (con otras mayúsculas o con espacios de los extremos, o aunque ese plan esté desactivado), el sistema debe responder 409 `ERR_PAG_01` sin crear ni cambiar nada; el mismo nombre en otra escuela sí se acepta.
- **CA04:** Dados datos inválidos (horas que no sean un entero de 1 a 500, costo menor o igual a 0 o con más de dos decimales, nombre vacío o muy largo), el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA05:** Dado un costo en bolivianos, el sistema debe mostrar su equivalente en dólares calculado con el tipo de cambio configurado y redondeado a dos decimales, sin guardarlo; al cambiar la configuración, el equivalente de todos los planes cambia sin tocar los datos.
- **CA06:** Dados datos válidos de edición, el sistema debe cambiar solo los campos enviados y dejar igual el estado; editar un plan no cambia lo que ya contrataron los estudiantes.
- **CA07:** Dado un plan activo, desactivarlo lo deja desactivado y sigue en el listado; reactivarlo lo deja activo.
- **CA08:** Dado un plan que ya está en el estado pedido, el sistema debe responder 409 `ERR_PAG_03` sin cambiar nada.
- **CA09:** Dado un plan de otra escuela o inexistente, el sistema debe responder 404 `ERR_PAG_02` a cualquier operación.
- **CA10:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA11:** Dado el menú de la web, la opción "Planes de horas" debe verse solo para el Administrador de escuela, dentro de "Gestionar pagos".

## Fuera de alcance

- **Cuotas, contratación y pago de un plan** por el estudiante: son del [CU25](../DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf) y de los CU de pago (CU26 en adelante). Un plan de horas no tiene cuotas.
- **Categoría de licencia** del plan y **descuentos, promociones o vigencia** de un plan.
- **Tipo de cambio en vivo** (consultar una API externa) y una pantalla para cambiarlo: hoy es una configuración del servicio.
- **Borrar un plan:** se desactiva.

## Desarrollador

Mauro
