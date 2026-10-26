# UT-CU14-BE-10 — Prueba de punta a punta y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-10` |
| **Nombre** | Prueba de punta a punta y documentación |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU14-BE-05`, `UT-CU14-BE-07`, `UT-CU14-BE-09` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (Testcontainers con `assumeTrue`), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md#comunicación) (las llamadas REST entre servicios se anotan en su tabla), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

Antes de dar el CU por terminado hay que probar el flujo completo con las piezas juntas, y dejar la documentación del proyecto al día con lo que cambió.

## Objetivo

El flujo «ver franjas → reservar varias → ver Mis clases» está probado con una base real, y los documentos de diseño reflejan el CU14.

## Alcance

- Prueba de integración en Académico (`@SpringBootTest` con MongoDB real por Testcontainers, patrón `assumeTrue`; Pagos e Identidad simulados con `HttpServer` del JDK):
  - Un estudiante con 3 horas elige 4 franjas libres de dos instructores: se reservan 3, la cuarta sale `ERR_ACA_19`, el saldo simulado queda en 0, `GET /reservas` lista las 3 en orden de fecha, y `GET /reservas?filter[vista]=historial` está vacío.
  - Dos estudiantes piden la misma franja a la vez (dos hilos): una sola reserva `CONFIRMADA`, la otra rechazada con `ERR_ACA_18`, y Pagos recibió un solo descuento.
  - Pagos caído al descontar: la franja queda `LIBRE` y no hay reserva visible.
  - Reserva `PENDIENTE_PAGO` antigua: se resuelve al listar.
  - Un estudiante no ve las reservas de otro, y una franja de otra escuela se rechaza con `ERR_ACA_18`.
- [`DISENO_BASE_DE_DATOS.md`](../../../DriveHub/DISENO_BASE_DE_DATOS.md): en `reservas`, agregar el campo `horasCobradas`; anotar que `pagoCodigo`, `sagaCodigo`, `expiraPagoEn` y `solicitudId` no se usan en el CU14, y que `solicitudes_clase` queda sin uso (CU13 absorbido por CU14).
- [`MICROSERVICIOS.md`](../../../../.claude/rules/MICROSERVICIOS.md): en la tabla de llamadas REST entre servicios, las filas **Académico → Pagos** (consultar saldo, descontar y devolver horas, reenviando el Bearer del estudiante, CU14) y **Académico → Identidad** (nombres de instructores, CU14); y actualizar el texto de la saga (CU44) aclarando que reservar con horas no usa la saga.
- [`SAGA_RESERVA_PAGO.md`](../../../DriveHub/SAGA_RESERVA_PAGO.md): una nota al inicio que diga que, con la billetera de horas, reservar es descontar una hora y no cobrar por reserva (CU14), y qué parte del diseño queda sin efecto.
- [`CONTRATO_BILLETERA_HORAS.md`](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md): sección «Pendiente de acordar con Leonel» resuelta: se descuenta **al reservar**, por REST síncrono, con `claseCodigo = codigoReserva`; si Pagos no responde, la reserva falla y la franja queda libre.
- `docs/CasosDeUso/CU14 - Reservar horario.md`: cambiar el **Estado** a `Hecho` cuando todas las UT (backend y móvil) estén terminadas.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/ReservarHorarioIntegracionTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`, `docs/DriveHub/SAGA_RESERVA_PAGO.md`, `docs/DriveHub/CONTRATO_BILLETERA_HORAS.md`
- `.claude/rules/MICROSERVICIOS.md`

## Fuera de alcance (no tocar)

- Código de producción de las UT anteriores (si la prueba encuentra un fallo, se corrige en la UT que corresponde).
- Retirar el modelo `SolicitudClase` ni la saga: solo se documentan como sin uso.
- Documentación de CU15, CU16 o CU17.

## Resultado esperado

El flujo completo del CU14 funciona contra una base real y los documentos del proyecto describen lo que se construyó.

## Validación

1. `./mvnw test` en `academico` pasa (las pruebas con MongoDB real quedan `SKIPPED` si Docker no está disponible; correrlas con Docker accesible al menos una vez).
2. `./mvnw checkstyle:check` sin errores.
3. Los enlaces nuevos de los documentos modificados abren (revisar a mano los de `MICROSERVICIOS.md` y `SAGA_RESERVA_PAGO.md`).
