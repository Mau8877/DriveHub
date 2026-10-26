# Unidades de trabajo — CU14-BE

**Caso de uso:** Reservar horario: el estudiante ve las franjas libres de los instructores de su escuela, reserva varias a la vez gastando una hora de su billetera por franja, y consulta sus clases próximas y su historial.
**Microservicio(s):** `academico` (UT 02 a 10) e `identidad` (UT 01) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). El gateway ya enruta `franjas`, `reservas` (Académico) y `usuarios` (Identidad); no necesita cambios.
**Fuente CU:** [`CU14 - Reservar horario`](../../../CasosDeUso/CU14%20-%20Reservar%20horario.md) (absorbe al [CU13](../../../CasosDeUso/CU13%20-%20Solicitar%20clase.md))
**Recurso HTTP / contrato:** `/api/v1/franjas/disponibles`, `/api/v1/franjas/instructores` y `/api/v1/reservas` (Académico, solo rol `ESTUDIANTE`); `/api/v1/usuarios/instructores/nombres` (Identidad)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `academico`), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). La UT 01 (Identidad) además [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md). Y el contrato de la billetera: [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Se reserva con horas, no con un pago por reserva.** La billetera de Pagos (CU27) ya guarda el saldo, así que reservar es *descontar una hora*. Esto reemplaza el diseño de la saga reserva-pago ([SAGA_RESERVA_PAGO.md](../../../DriveHub/SAGA_RESERVA_PAGO.md)) para este caso: no hay cobro ni QR por reserva. `pagoCodigo`, `sagaCodigo` y `expiraPagoEn` de `reservas` quedan sin uso en este CU; `PENDIENTE_PAGO` se reutiliza como estado transitorio «franja apartada, hora todavía no descontada». **Es una saga simple, coordinada por Académico:** los pasos son apartar la franja y descontar la hora, la compensación es liberar la franja y devolver la hora, y la reserva es el registro de su estado. No hay orquestador aparte, tabla `SagaEstado` ni eventos; no se construye el CU44 para este flujo (queda a decisión de Mauro).
2. **`claseCodigo` del contrato de la billetera = `codigoReserva`.** La clase (CU17) se crea después a partir de la reserva; hasta entonces el código de la reserva identifica el consumo. CU15 devolverá la hora con ese mismo código.
3. **Orden de cada franja: apartar → descontar → confirmar.** (a) `LIBRE → RESERVADA` atómico en la franja; (b) se crea la reserva `PENDIENTE_PAGO`; (c) se descuenta 1 hora en Pagos con `Idempotency-Key: reserva-{codigoReserva}`; (d) la reserva pasa a `CONFIRMADA`. Si (c) falla, se libera la franja y la reserva queda `CANCELADA`. Es el orden que no deja a un estudiante cobrado sin franja.
4. **Sin transacciones entre documentos ni entre servicios.** Igual que en CU12: se diseña para que cada paso sea repetible. El hueco que queda (la app o el servicio se caen entre (c) y (d)) lo cierra la UT 09: al consultar sus clases, el propio estudiante retoma sus reservas `PENDIENTE_PAGO` de más de 2 minutos **repitiendo el descuento con la misma clave**: Pagos devuelve el resultado anterior si ya había descontado (no cobra dos veces) o descuenta ahora. No hay un job porque Pagos exige el token del estudiante para descontar y devolver, y un proceso periódico no tiene sesión.
5. **Confirmar varias franjas es por franja, no todo o nada.** Se procesan una por una; la respuesta lista las `reservadas` y las `rechazadas` (con fecha, hora, instructor y `error`). HTTP `200` siempre que la petición sea válida, aunque todas se rechacen.
6. **Costo: `app.reservas.horas-por-clase=1`**, configuración del servicio. Máximo `app.reservas.maximo-por-confirmacion=10`. Un `Clock` inyectado y la zona `app.horarios.zona-horaria` (ya existe) deciden «ahora».
7. **Quién se ofrece:** todas las franjas `LIBRE` y futuras con el `escuelaCodigo` del estudiante (claim `escuela`). Sin filtro por asignación (CU10).
8. **Nombres de instructor con una consulta mínima a Identidad (UT 01):** `POST /api/v1/usuarios/instructores/nombres` devuelve solo `codigo` y `nombreCompleto` de los instructores de la escuela de quien pregunta, y la pueden llamar el Estudiante y el Instructor. No se reutiliza `/instructores/consulta` (solo Administrador) porque devuelve CI, correo y teléfono, que un estudiante no debe ver. Académico no guarda nombres: los pide por lotes de hasta 100 y los une a la respuesta.
9. **Autorización en el servicio** con `@PreAuthorize("hasRole('ESTUDIANTE')")`; el estudiante y su escuela salen del token (`@CurrentUserId`, `@CurrentEscuelaId`). Una reserva de otro estudiante nunca aparece en sus listados.
10. **Filtros de franjas:** `filter[fecha]` (un día) o `filter[fechaDesde]`/`filter[fechaHasta]` (hasta 28 días, el horizonte de generación), `filter[instructor]` (código), `filter[horaDesde]` y `filter[horaHasta]` (`HH:mm`, sobre la hora de inicio, `horaDesde` inclusiva y `horaHasta` exclusiva). La app traduce mañana/tarde/noche a `00:00–12:00`, `12:00–18:00` y `18:00–24:00` (la noche sin `horaHasta`). Sin fechas, desde hoy. Orden fijo: fecha, hora de inicio.
11. **Mis clases e Historial son una sola consulta con `filter[vista]`:** `proximas` (confirmadas que no terminaron, ascendente) o `historial` (resto, descendente; una `CANCELADA` entra solo si alguna vez estuvo `CONFIRMADA`, porque la que nunca logró confirmarse no fue una clase del estudiante). Se calcula con la fecha y `horaFin` de la reserva (copia de la franja). Las `PENDIENTE_PAGO` no se listan.
12. **El saldo final sale de Pagos:** al terminar, Académico pide `GET /billeteras-horas/mia` con el token del estudiante y lo devuelve como `horasDisponibles`. Si esa lectura falla, la respuesta lleva `horasDisponibles: null` y la app lo vuelve a pedir; las reservas ya hechas no se deshacen.
13. **Errores nuevos (siguiente libre `ERR_ACA_17`):** `ERR_ACA_17` (503), `ERR_ACA_18`, `ERR_ACA_19`, `ERR_ACA_20` (409). Los tres 409 viajan como `error` dentro de cada franja rechazada y no como respuesta HTTP de toda la petición. Cada uno se documenta en `docs/errors/academico/` en la UT que lo introduce.

## Contrato de datos / recurso HTTP

Todas las rutas de Académico exigen sesión con rol `ESTUDIANTE` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol, `403 ERR_ACA_16` si el token no trae escuela). Respuestas con el sobre estándar.

### Franjas disponibles (`franjas`)

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `GET /api/v1/franjas/disponibles` | `page`, `pageSize` (máx. 100), `filter[fecha]` o `filter[fechaDesde]`+`filter[fechaHasta]`, `filter[instructor]`, `filter[horaDesde]`, `filter[horaHasta]` | `200`, `data`: lista de `FranjaDisponibleResponse` + `meta` |
| `GET /api/v1/franjas/instructores` | `filter[fecha]` o `filter[fechaDesde]`+`filter[fechaHasta]` (opcionales) | `200`, `data`: lista de `InstructorDisponibleResponse` (sin paginar: son pocos), con meta de una página |

`FranjaDisponibleResponse`: `codigo`, `fecha`, `horaInicio`, `horaFin`, `instructorCodigo`, `instructorNombre`. `InstructorDisponibleResponse`: `codigo`, `nombre`.

### Reservas (`reservas`)

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `POST /api/v1/reservas` | `ReservarFranjasRequest` | `200`, `data`: `ReservasResultadoResponse`, «Reservas procesadas correctamente» |
| `GET /api/v1/reservas` | `page`, `pageSize`, `filter[vista]` (`proximas` por defecto, o `historial`) | `200`, `data`: lista de `ReservaResponse` + `meta` |

`ReservarFranjasRequest`: `franjaCodigos` (lista de UUID, de 1 a 10, sin repetir; `ERR_SYS_01` si no).

`ReservasResultadoResponse`:

| Campo | Tipo | Contenido |
|-------|------|-----------|
| `reservadas` | lista de `ReservaResponse` | Las que quedaron `CONFIRMADA` |
| `rechazadas` | lista | Por cada una: `franjaCodigo`, `fecha`, `horaInicio`, `horaFin`, `instructorNombre`, `error` (`ERR_ACA_10`, `ERR_ACA_17`, `ERR_ACA_18`, `ERR_ACA_19`, `ERR_ACA_20`), `mensaje` |
| `horasDisponibles` | int o null | Saldo real tras confirmar |

`ReservaResponse`: `codigo`, `fecha`, `horaInicio`, `horaFin`, `instructorCodigo`, `instructorNombre`, `estado` (`CONFIRMADA`, `COMPLETADA`, `NO_ASISTIO` o `CANCELADA`), `terminada` (boolean: la hora de fin ya pasó; una `CONFIRMADA` con `terminada: true` es «Pendiente de cierre»). No expone ids internos.

### Identidad

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `POST /api/v1/usuarios/instructores/nombres` | `{ "codigos": [UUID, ... hasta 100] }` | `200`, `data`: lista de `{ codigo, nombreCompleto }` de los instructores de la escuela de quien pregunta (los demás se omiten) |

### Lo que Académico usa de Pagos (ya implementado, CU27)

`GET /api/v1/billeteras-horas/mia`; `POST /api/v1/billeteras-horas/consumos` (header `Idempotency-Key`, cuerpo `{ estudianteCodigo, horas, instructorCodigo, claseCodigo }`; `409 ERR_PAG_09` sin horas); `POST /api/v1/billeteras-horas/devoluciones` (header `Idempotency-Key`, cuerpo `{ estudianteCodigo, claseCodigo }`; `404 ERR_PAG_16` si no había nada que devolver). Siempre con el `Bearer` del estudiante.

### Errores nuevos (`ERR_ACA_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_ACA_17` | 503 | `ServicioNoDisponibleException` | Pagos o Identidad no responden (como error de toda la petición en los listados; como `error` de una franja al reservar) |
| `ERR_ACA_18` | 409 | `ConflictException` | La franja ya no está libre, no existe o no es de la escuela del estudiante (como `error` de una franja rechazada) |
| `ERR_ACA_19` | 409 | `ConflictException` | Pagos informa que no hay horas suficientes (`ERR_PAG_09`) |
| `ERR_ACA_20` | 409 | `ConflictException` | El estudiante ya tiene una clase reservada a esa fecha y hora |

Se reutilizan `ERR_ACA_10` (la franja ya empezó o pasó) y `ERR_ACA_16` (token sin escuela).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU14-BE-01](./UT-CU14-BE-01.md) | Identidad: nombres de instructores para el estudiante | — |
| [UT-CU14-BE-02](./UT-CU14-BE-02.md) | Modelo y repositorio de reservas | — |
| [UT-CU14-BE-03](./UT-CU14-BE-03.md) | Repositorio de franjas: disponibles, apartar y liberar | — |
| [UT-CU14-BE-04](./UT-CU14-BE-04.md) | Clientes de Pagos e Identidad en Académico | — |
| [UT-CU14-BE-05](./UT-CU14-BE-05.md) | Consultar franjas e instructores disponibles | 01, 03, 04 |
| [UT-CU14-BE-06](./UT-CU14-BE-06.md) | `ReservaService`: reservar franjas | 02, 03, 04 |
| [UT-CU14-BE-07](./UT-CU14-BE-07.md) | Endpoint `POST /reservas` | 06 |
| [UT-CU14-BE-08](./UT-CU14-BE-08.md) | Mis clases e historial | 01, 02, 04 |
| [UT-CU14-BE-09](./UT-CU14-BE-09.md) | Retomar reservas interrumpidas | 06, 08 |
| [UT-CU14-BE-10](./UT-CU14-BE-10.md) | Prueba de punta a punta y documentación | 05, 07, 09 |

**Cadena:** `01, 02, 03, 04` son independientes → `05` (necesita 01, 03, 04) y `06` (02, 03, 04) → `07` (06) y `08` (01, 02, 04) → `09` (06, 08) → `10`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 2 y 3 | Ver y filtrar franjas libres por día, momento y instructor | 01, 03, 04, 05 |
| Flujo principal, pasos 5 a 8 | Confirmar: apartar la franja, descontar la hora, informar el resultado | 02, 03, 04, 06, 07 |
| F1 | Mis clases | 01, 02, 04, 08 |
| F2 | Historial | 01, 02, 04, 08 |
| S1 | Retomar una reserva interrumpida | 06, 09 |
| A1 | Sin franjas: lista vacía | 05 |
| A3, A4, A5 | Franja ocupada, ya empezada o solapada | 03, 06 |
| A6, A7 | Sin horas / Pagos no responde | 04, 06 |
| A8, A9, A10 | Validación, token sin escuela, rol | 07 |
| A13 | Identidad no responde | 04, 05, 08 |
| CA01 a CA18 | Criterios de aceptación | 05 a 10 |

## Follow-ups (no se implementan en estas UT)

- **Cancelar o reprogramar** y devolver la hora (CU15). Usará `POST /billeteras-horas/devoluciones` con `claseCodigo = codigoReserva`.
- **Limitar instructores por alumno** y asignar vehículo (CU10, CU11).
- **Retirar la saga reserva-pago** de [SAGA_RESERVA_PAGO.md](../../../DriveHub/SAGA_RESERVA_PAGO.md) y el modelo `SolicitudClase` cuando se confirme que no se usan; avisar a Mauro (dueño de CU44).
- **Notificar al instructor** de una reserva nueva (CU37, CU38).

## Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **`ReservaService.reservar` devuelve el DTO** (`ReservasResultadoResponse`), no un resultado interno: el service une los nombres de los instructores y arma la respuesta, y el controller solo la envuelve. Lo mismo `listar` (devuelve `Page<ReservaResponse>`). No hay un `ReservaMapper` aparte.
- **Los nombres de instructor faltan con un valor de respaldo, no con un error, al reservar:** si Identidad no responde cuando la reserva ya está hecha, el instructor se muestra como «Instructor» y nada se deshace. En los listados (franjas, mis clases, historial) sí es `503 ERR_ACA_17`.
- **El historial no muestra las canceladas que nunca se confirmaron:** una reserva que falló al reservar (sin horas, Pagos caído) queda `CANCELADA` como registro del intento, pero no fue una clase del estudiante. El repositorio solo incluye una `CANCELADA` si su `historialEstados` contiene `CONFIRMADA` (decisión que salió de la prueba de punta a punta).
- **La reserva repetida es inocua:** pedir otra vez una franja que ya es del estudiante y está `CONFIRMADA` cuenta como reservada sin descontar de nuevo (`ReservaRepository.findByFranjaIdAndEstudianteCodigoAndEstadoAndDeletedAtIsNull`).
- **`app.reservas.minutos-para-retomar=2`** es la tercera propiedad de `ReservasProperties` (junto a `horas-por-clase` y `maximo-por-confirmacion`).
- **Docker/Testcontainers en Windows:** los tests contra MongoDB real quedan `SKIPPED` con el socket restringido de Docker Desktop (ver TESTING_BACKEND.md). Se verificaron corriéndolos contra el Mongo del compose con una base temporal (`academico_ut_tmp`, borrada al terminar).
- **`PAGOS_URL` en `docker-compose.yml`:** hay que recrear el contenedor (`docker compose up -d academico`); un `restart` no toma la variable.

## Qué NO incluyen estas UT

- Cancelación, reprogramación o devolución de horas (CU15), cierre de la clase y asistencia (CU17), calendario del instructor (CU16).
- Comprar horas o cambiar la billetera: es de Pagos (CU26 a CU31). Estas UT solo la consumen.
- Ninguna pantalla: la app móvil está en [`UT-MOBILE`](../UT-MOBILE/README.md).
- Eventos de RabbitMQ: no se publica ninguno en este CU.
