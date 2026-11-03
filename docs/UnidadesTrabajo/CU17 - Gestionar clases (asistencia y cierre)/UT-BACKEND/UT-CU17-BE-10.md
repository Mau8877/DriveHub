# UT-CU17-BE-10 — Prueba de punta a punta y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-10` |
| **Nombre** | Prueba de punta a punta y documentación |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-08`, `UT-CU17-BE-09` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (todo cambio de esquema se documenta). Tomar como modelo UT-CU16-BE-09 y UT-CU15-BE-10. Prerrequisito: `academico`, `identidad`, `pagos`, `escuela` y el gateway levantados con **este** código (`docker compose up -d --build academico`, recrear si cambió una variable), y el token de un instructor de la escuela con clases ya empezadas.

## Solicitud de negocio

Antes de dar el CU17 por terminado hay que ver, con el servicio real y MongoDB real, que cerrar y corregir una clase funciona como dice el CU.

## Objetivo

El CU17 queda verificado de punta a punta contra el stack real y su documentación al día.

## Alcance

Preparar en `reservas` de la base de desarrollo (con una base temporal o datos de prueba que se borran al terminar) reservas `CONFIRMADA` del instructor de prueba: dos que ya empezaron, una que todavía no y una de otro instructor. Con el token del instructor, por el gateway:

1. **Cerrar con `PRESENTE`** y con tema, observaciones y km → `201`; la reserva queda `COMPLETADA` con su historial; existe la clase `CERRADA`; la billetera del estudiante **no** cambia; la franja sigue `RESERVADA`.
2. **Cerrar con `AUSENTE`** solo con observaciones → `201`; reserva `NO_ASISTIO`; la hora no se devuelve.
3. **Cerrar con `AUSENTE` y tema o km** → `400 ERR_ACA_38`; nada cambia.
4. **Cerrar una clase que aún no empezó** → `409 ERR_ACA_37`.
5. **Cerrar de nuevo la ya cerrada** → `409 ERR_ACA_37`, sin una segunda clase.
6. **Cerrar la reserva de otro instructor** → `404 ERR_ACA_35`.
7. **Datos inválidos** (km negativo, `kmFin` menor, tema de 121) → `400` y nada se guarda.
8. **Corregir** con `PATCH` solo los km → `200`; el tema no cambia; un `kmFin` menor que el `kmInicio` guardado → `400 ERR_ACA_38`; con un tema en blanco, se borra.
9. **`GET /clases/{codigo}`** de una clase propia → `200`; de otra → `404 ERR_ACA_35`.
10. **Cierre interrumpido:** crear a mano una `Clase` de una reserva todavía `CONFIRMADA` y cerrar → `201`, **una sola** clase y la reserva marcada.
11. **Conflicto:** dejar la reserva en `CANCELACION_PENDIENTE` (cancelación en curso) antes de cerrar → `409 ERR_ACA_37` y sin clase guardada.
12. **Agenda de CU16:** `GET /reservas/agenda` trae `cerrable: true` en las que ya empezaron y siguen `CONFIRMADA`, `claseCodigo` en las cerradas y `cancelable` y `cerrable` nunca los dos verdaderos.
13. **Roles:** con el token de un estudiante → `403 ERR_SYS_05`; sin token → `401 ERR_SYS_04`.
14. **Historial del estudiante** (CU14): la clase cerrada con `PRESENTE` aparece como `Realizada`, la `AUSENTE` como `No asistió`, y el texto del tema no aparece en su respuesta.

Documentación, en el mismo cambio:

- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`, colección `clases`: anotar que `estado` siempre se guarda `CERRADA` (`ABIERTA` sin uso), que `inicioReal`, `finReal` y `vehiculoCodigo` quedan vacíos, y que cada reserva tiene a lo sumo una clase (índice único de `reservaId`). No hay migración.
- `docs/CasosDeUso/CU17 - Gestionar clases (asistencia y cierre).md`: estado `En progreso` (o `Hecho` cuando la app móvil también esté).
- Este README: la tabla «Estado de la implementación» y las «Notas de implementación» con lo que quedó distinto al construirlo y los resultados de esta prueba (qué escenarios se hicieron, con qué datos).
- CU16 (UT-BE README y CU): mencionar que la agenda ganó `cerrable` y `claseCodigo`.

Archivos esperados (crear/tocar):

- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`
- `docs/CasosDeUso/CU17 - Gestionar clases (asistencia y cierre).md`
- `docs/UnidadesTrabajo/CU17 - Gestionar clases (asistencia y cierre)/UT-BACKEND/README.md`
- `docs/UnidadesTrabajo/CU16 - Consultar horario de clases/UT-BACKEND/README.md`

## Fuera de alcance (no tocar)

- Código de producción: si la prueba encuentra un fallo, se corrige en la UT que corresponda y se anota.
- La app móvil (`UT-MOBILE`) y su prueba manual.
- Datos de producción: todo lo que se crea para probar se borra al terminar.

## Resultado esperado

Cada escenario del CU17 se comprobó con el stack real, los datos de prueba se limpiaron y la documentación refleja lo construido.

## Validación

1. Los 14 escenarios dan el resultado indicado.
2. `./mvnw test` y `./mvnw checkstyle:check` en `academico` siguen pasando.
3. `DISENO_BASE_DE_DATOS.md` describe el uso real de la colección `clases`.
