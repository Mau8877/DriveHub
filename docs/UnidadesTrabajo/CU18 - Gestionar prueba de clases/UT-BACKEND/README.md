# Unidades de trabajo — CU18-BE

**Caso de uso:** Gestionar prueba de clases: el instructor evalúa a un estudiante en una clase ya cerrada con criterios puntuados, la prueba queda guardada con su total y su resultado, y la nota entra en el historial de resultados del estudiante.
**Microservicio(s):** `academico` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md). Pagos, Identidad, Escuela y el gateway **no cambian**: el recurso `pruebas-clase` ya está en el gateway y en `MICROSERVICIOS.md`, evaluar no toca la billetera y la agenda ya trae el nombre del estudiante.
**Fuente CU:** [`CU18 - Gestionar prueba de clases`](../../../CasosDeUso/CU18%20-%20Gestionar%20prueba%20de%20clases.md)
**Recurso HTTP / contrato:** `POST /api/v1/pruebas-clase`, `GET /api/v1/pruebas-clase?filter[claseCodigo]=...` y `GET /api/v1/pruebas-clase/criterios` (Académico, solo rol `INSTRUCTOR`), y dos campos nuevos en la agenda de [CU16](../../CU16%20-%20Consultar%20horario%20de%20clases/UT-BACKEND/README.md)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas, `academico`; un módulo habla con otro por el service del dueño), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query; todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (todo `GET` de colección se pagina), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca loguear el texto de las observaciones), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Y lo construido por [CU17-BE](../../CU17%20-%20Gestionar%20clases%20%28asistencia%20y%20cierre%29/UT-BACKEND/README.md) (`Clase`, `ClaseService`, `CierreDeReservaService`, `AgendaService.codigosPorReserva`) y por [CU22](../../../CasosDeUso/CU22%20-%20Gestionar%20examen%20de%20conducci%C3%B3n.md) (`ExamenConduccionService.registrarResultado`, que ya escribe una nota en `resultados`). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Qué ya existe (y no se rehace)

- `PruebaClase` (modelo con `claseId`, `estudianteCodigo`, `instructorCodigo`, `tipo`, `criterios`, `puntajeTotal`, `resultado`, `observaciones`, `fecha`), `TipoPrueba` (`MANEJO`, `ESTACIONAMIENTO`, `CIUDAD`), `CriterioEvaluacion` (`codigo`, `descripcion`, `puntaje`, `maximo`) y `ResultadoEvaluacion` (`APROBADO`, `REPROBADO`). Solo falta cómo crear un `PruebaClase` y su repositorio.
- `Resultado` con `OrigenResultado.PRUEBA_CLASE` ya definido, y `ResultadoRepository`. CU23 ya lista lo que haya en `resultados`.
- El recurso `pruebas-clase` en el gateway y en `MICROSERVICIOS.md`.

## Decisiones de diseño

Las tomó este desglose porque el CU no las resolvía; si alguna no convence, se cambia **antes** de implementar.

1. **Recurso `pruebas-clase`, dentro del módulo `clases`.** El modelo `PruebaClase` ya vive en `features/clases/`, así que el `PruebaClaseService`, el `PruebaClaseController`, los DTOs y el repositorio van ahí; no se crea un módulo nuevo. Usa `ClaseRepository` directamente (mismo módulo) y habla con `reservas` solo por `CierreDeReservaService`.
2. **Evaluar es `POST /api/v1/pruebas-clase`** con el código de la clase en el cuerpo, y consultar es `GET /api/v1/pruebas-clase?filter[claseCodigo]=...`: la prueba tiene entidad propia, así que va en su raíz con un filtro y no anidada ([ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). El filtro `claseCodigo` es **obligatorio** (sin él `400 ERR_SYS_01`): no hay listado general de pruebas en este CU. Como todo listado, se pagina (`page`, `pageSize`) y lleva `meta`, aunque una clase tenga a lo sumo tres.
3. **El catálogo de criterios lo sirve el servidor**, `GET /api/v1/pruebas-clase/criterios`, para que la app no copie las claves, las descripciones ni el máximo. Es un objeto (`data.tipos`), no un listado paginado: son tres tipos fijos. Los criterios viven en código (`CatalogoDePruebas`), no en la base.
4. **Qué se puede evaluar.** La clase existe, es del instructor y de su escuela (`404 ERR_ACA_35` si no), tiene asistencia `PRESENTE` o `TARDE` y su **reserva está `COMPLETADA`**. Una `Clase` existe sin que la reserva esté marcada solo en un cierre interrumpido (CU17, S1); por eso se pregunta a la reserva por `CierreDeReservaService.reservaCompletada(reservaId)` (método nuevo, UT 05), nunca por `ReservaRepository`. Si no se cumple: `409 ERR_ACA_39`.
5. **Una prueba por tipo y clase, garantizada por un índice único compuesto** `(claseId, tipo)` (UT 03): dos peticiones a la vez crean una sola y la otra recibe `409 ERR_ACA_40`. Es un cambio de esquema: se documenta en `DISENO_BASE_DE_DATOS.md`. El índice del diseño sobre `claseId` solo es de búsqueda; el compuesto lo cubre y lo reemplaza.
6. **Orden al guardar y cómo se repite (A8 del CU).** Primero se guarda la `PruebaClase` y después la `Resultado` (la nota), con `origenId` igual al `_id` de la prueba. Si el servicio se cae entre las dos, queda la prueba sin nota. Reintentar el mismo `POST`: si la prueba de ese tipo ya existe **y su nota también**, `409 ERR_ACA_40`; si existe **sin nota**, el sistema escribe la nota y responde `201` con la prueba **ya guardada** (los puntajes del reintento no la cambian: la prueba es inmutable). No hay job: lo repite el instructor, como en CU17.
7. **Reglas de los datos** (`ReglasDePrueba`, una sola clase):
   - Los criterios enviados son **exactamente** los del tipo: ni uno menos (`falta un criterio`), ni uno más, ni uno repetido, ni uno de otro tipo → `ValidationException` `ERR_ACA_41` (`400`).
   - Cada `puntaje` es un número de 0 al máximo (10) en pasos de 0,5; fuera de eso → `ERR_ACA_41`.
   - `observaciones` hasta 500 (Bean Validation, `ERR_SYS_01`); un texto en blanco se guarda como `null`.
   - **Total** = (suma de puntajes ÷ suma de máximos) × 100, con dos decimales (`HALF_UP`). **Resultado**: `APROBADO` con 70 o más, `REPROBADO` con menos. Las dos constantes (70 y 10) viven en `ReglasDePrueba`.
   - Los criterios guardados llevan **copia** de `descripcion` y `maximo` del catálogo del momento (el catálogo puede cambiar sin alterar pruebas viejas).
8. **La nota (`Resultado`)**: `escuelaCodigo` del token, `estudianteCodigo` y `origenId` de la prueba, `origen = PRUEBA_CLASE`, `titulo = "Prueba de clase: " + etiqueta del tipo` (`Manejo`, `Estacionamiento`, `Ciudad`), `puntaje` = el total, `aprobado`, `fecha` = el instante del guardado (`Clock`), como lo hace `ExamenConduccionService.registrarResultado`. No se publica ningún evento.
9. **Fecha de la prueba**: la `fecha` de la clase dictada (no el día en que se evalúa).
10. **Agenda de CU16, dos campos nuevos** (cambio compatible hacia atrás, UT 07): `conPrueba` (la clase tiene al menos una prueba) y `evaluable` (la reserva está `COMPLETADA` y le falta algún tipo; lo calcula el servidor). Se calculan solo para reservas `COMPLETADA` de la página, con una consulta `findByClaseIdIn...`; el resto trae `false`.
11. **Errores nuevos (siguiente libre `ERR_ACA_39`):** `ERR_ACA_39` (409, la clase no se puede evaluar), `ERR_ACA_40` (409, ya hay una prueba de ese tipo) y `ERR_ACA_41` (400, los datos de la prueba no son válidos). `ERR_ACA_35` se reutiliza para «no existe o es de otro instructor» (se amplía su descripción), y también `ERR_ACA_16` y `ERR_SYS_01`. Cada código se documenta en `docs/errors/academico/` en la UT que lo introduce.
12. **La prueba no se corrige ni se borra**: no hay `PATCH` ni `DELETE`. El estudiante tampoco ve el detalle: este CU no le agrega ninguna ruta.

## Contrato de datos / recurso HTTP

Todas las rutas exigen sesión con rol `INSTRUCTOR` (`401 ERR_SYS_04` sin sesión, `403 ERR_SYS_05` con otro rol, `403 ERR_ACA_16` si el token no trae escuela). Respuestas con el sobre estándar. El instructor y su escuela salen del token.

| Método y ruta | Entrada | Respuesta |
|---------------|---------|-----------|
| `POST /api/v1/pruebas-clase` | `EvaluarClaseRequest` | `201`, `data`: `PruebaClaseResponse`, «Prueba de clase registrada correctamente» |
| `GET /api/v1/pruebas-clase?filter%5BclaseCodigo%5D={codigo}` | `page`, `pageSize` | `200`, `data`: `PruebaClaseResponse[]` + `meta`, «Listado obtenido correctamente» |
| `GET /api/v1/pruebas-clase/criterios` | — | `200`, `data`: `CatalogoPruebasResponse`, «Criterios obtenidos correctamente» |

`EvaluarClaseRequest`:

| Campo | Tipo | Reglas |
|-------|------|--------|
| `claseCodigo` | UUID | Obligatorio. El `claseCodigo` de la agenda |
| `tipo` | `MANEJO`, `ESTACIONAMIENTO` o `CIUDAD` | Obligatorio |
| `criterios` | lista de `{ codigo, puntaje }` | Obligatoria; exactamente los criterios del tipo (ver decisión 7) |
| `observaciones` | texto | Opcional, hasta 500 |

`PruebaClaseResponse`:

| Campo | Tipo | Contenido |
|-------|------|-----------|
| `codigo` | UUID | El `codigoPruebaClase` |
| `claseCodigo` | UUID | La clase evaluada |
| `tipo` | enum | `MANEJO`, `ESTACIONAMIENTO` o `CIUDAD` |
| `criterios` | lista | `{ codigo, descripcion, puntaje, maximo }`, en el orden del catálogo |
| `puntajeTotal` | número | Sobre 100, dos decimales |
| `resultado` | `APROBADO` o `REPROBADO` | |
| `observaciones` | texto o `null` | |
| `fecha` | fecha | `yyyy-MM-dd`, la de la clase |

`CatalogoPruebasResponse`: `tipos`: lista de `{ tipo, etiqueta, criterios: [{ codigo, descripcion, maximo }] }`, en el orden `MANEJO`, `ESTACIONAMIENTO`, `CIUDAD`.

Cambios a `ClaseAgendaResponse` (CU16): `evaluable` y `conPrueba` (boolean).

### Errores (`ERR_ACA_*`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_ACA_35` | 404 | `NotFoundException` | La clase no existe, está borrada o es de otro instructor |
| `ERR_ACA_39` | 409 | `ConflictException` | La clase no se puede evaluar: es `AUSENTE` o su cierre no terminó (reserva no `COMPLETADA`) |
| `ERR_ACA_40` | 409 | `ConflictException` | La clase ya tiene una prueba de ese tipo (con su nota ya escrita) |
| `ERR_ACA_41` | 400 | `ValidationException` | Los criterios no son los del tipo (falta, sobra, repetido o de otro tipo) o un puntaje no es válido |

Se reutilizan: `ERR_ACA_16` (el token no trae la escuela) y `ERR_SYS_01` (cuerpo, tamaños, tipo, filtro `claseCodigo` o paginación inválidos).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU18-BE-01](./UT-CU18-BE-01.md) | Errores `ERR_ACA_39`, `ERR_ACA_40` y `ERR_ACA_41` | — |
| [UT-CU18-BE-02](./UT-CU18-BE-02.md) | El catálogo de criterios por tipo | — |
| [UT-CU18-BE-03](./UT-CU18-BE-03.md) | Persistencia: creación de `PruebaClase`, índice único y repositorios | — |
| [UT-CU18-BE-04](./UT-CU18-BE-04.md) | DTOs y reglas de la prueba | 01, 02 |
| [UT-CU18-BE-05](./UT-CU18-BE-05.md) | `PruebaClaseService`: evaluar una clase | 01, 03, 04 |
| [UT-CU18-BE-06](./UT-CU18-BE-06.md) | `PruebaClaseService`: consultar pruebas y criterios | 02, 03, 04, 05 |
| [UT-CU18-BE-07](./UT-CU18-BE-07.md) | La agenda de CU16 con `evaluable` y `conPrueba` | 03, 05 |
| [UT-CU18-BE-08](./UT-CU18-BE-08.md) | `PruebaClaseController` | 05, 06 |
| [UT-CU18-BE-09](./UT-CU18-BE-09.md) | Prueba de punta a punta y documentación | 07, 08 |

**Cadena:** `01`, `02` y `03` son independientes → `04` (01, 02) → `05` (01, 03, 04) → `06` (05) y `07` (05) → `08` (05, 06) → `09` (07, 08).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 3 a 6 | Mostrar los criterios, calcular el total y el resultado, guardar la prueba y su nota | 02, 03, 04, 05, 06, 08 |
| F1 | Consultar las pruebas de una clase | 03, 06, 08 |
| A1 | Clase no evaluable | 01, 05 |
| A2 | Ya hay una prueba de ese tipo | 01, 03, 05 |
| A3 | Clase inexistente o ajena | 05, 06 |
| A4 | Datos inválidos | 01, 04, 05 |
| A5, A6 | Rol, sesión, escuela y límite de peticiones | 08 |
| A8 | Prueba guardada sin nota: se completa al reintentar | 03, 05 |
| CA12 | La agenda ofrece «Evaluar» y «Ver prueba» | 07 |
| CA01 a CA11, CA13 | Criterios de aceptación | 01 a 09 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 08 | **Hechas** (2026-11-01), con pruebas: ver los resultados abajo y `./mvnw checkstyle:check` limpio. Los tests de repositorio se corrieron contra el Mongo del compose (`PruebaClaseRepositoryTest` 6) |
| 09 (punta a punta) | **Hecha** (2026-11-01) contra el stack real con `instructorexample@gmail.com` y `estudianteprueba@gmail.com`: 36 de 38 comprobaciones iguales a lo esperado al primer intento; las otras dos se explican abajo. Tras el arreglo, el escenario 12 se repitió con 4 peticiones a la vez: un `201`, tres `409`, una sola prueba y una sola nota. Los datos de prueba se borraron al terminar. **No verificado:** la parte «instructor ve las notas en las notas de sus alumnos» del escenario 15 (ver abajo) |

## Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **`ReglasDePrueba` es una clase de métodos estáticos**, no un `@Component`, igual que `ReglasDeClase` de CU17: no tiene dependencias. `CatalogoDePruebas` también es estático.
- **Las pruebas de una clase se listan en el orden en que se hicieron** (por `_id`), no en el del catálogo: el tipo se guarda como texto y ordenar por él daría orden alfabético. La respuesta de `GET /pruebas-clase/criterios` sí va en el orden del catálogo.
- **Paginación indulgente, como los demás listados de Académico:** una `page` menor que 1 se toma como 1 y un `pageSize` menor que 1 como 20 (máximo 100); no responden `400`. Lo que sí responde `400 ERR_SYS_01` es faltar `filter[claseCodigo]` o mandarlo sin forma de UUID.
- **`PruebaClase` ganó un constructor** con los datos de la prueba y pasó su índice simple de `claseId` al compuesto `uq_prueba_clase_tipo`; ningún campo cambió.
- **`PruebaClaseService.tiposEvaluadosPorReserva`** recibe los `_id` de las reservas `COMPLETADA` de la página y devuelve, por reserva, los tipos ya hechos (la reserva sin clase no aparece), con dos consultas. `AgendaService` ganó esa dependencia y la usa sin consultar si la página no tiene `COMPLETADA`.
- **`CierreDeReservaService.reservaCompletada`** usa `ReservaRepository.findById` y descarta una reserva borrada.
- **El `GET` de pruebas no trae el nombre del estudiante** ni llama a Identidad: la app ya lo tiene de la agenda.
- **Los tests de repositorio con MongoDB real se omiten solos en esta máquina**; se corrieron parchando solo durante la corrida `MongoIntegrationTestBase` (base temporal `academico_ut_tmp`, ya borrada, archivo restaurado), como en CU15 a CU17.

### Hallazgos de la prueba de punta a punta

- **Escenario 12 (dos peticiones a la vez): fallo real, ya corregido.** Dos `POST` simultáneos del mismo tipo devolvían los dos `201` con la misma prueba (la segunda encontraba la prueba recién guardada sin nota y la «completaba») y **escribían dos notas** para la misma prueba. Se arregló con un índice único `uq_resultado_origen (origen, origenId)` en `resultados` (una evaluación, una nota) y tratando el `DuplicateKeyException` al escribir la nota como «ya estaba escrita». Hay tests en `PruebaClaseServiceTest` y `PruebaClaseRepositoryTest`. Con el arreglo, dos o más peticiones a la vez dan un `201` y el resto `409 ERR_ACA_40`, o un `201` y otro `201` con la misma prueba si la segunda llega justo entre las dos escrituras; en ningún caso hay dos pruebas ni dos notas.
- **Escenario 15, parte del instructor: sin verificar.** `GET /resultados/de-mis-alumnos` devolvió vacío porque la cuenta `instructorexample@gmail.com` no existe como instructor en Escuela, que es quien dice qué alumnos tiene asignados (CU23, ya documentado). No se tocó la base de Escuela para forzarlo. La parte del estudiante («Mis notas») sí se verificó.
- **Datos viejos:** una base que ya tuviera dos notas con el mismo `(origen, origenId)` no podría crear el índice único; en desarrollo no había ninguna salvo la que provocó esta prueba.

## Follow-ups (no se implementan en estas UT)

- **Editar o borrar** una prueba, y criterios configurables por escuela.
- **Mostrar al estudiante** el detalle de sus pruebas.
- **Avisar** al estudiante de su nota: CU37 y CU38.
- **Una vista web** para el Administrador de las pruebas de clase.

## Qué NO incluyen estas UT

- La app móvil: está en [`UT-MOBILE`](../UT-MOBILE/README.md).
- Cambios en Pagos, Identidad, Escuela o el gateway.
- Cerrar o corregir la clase (CU17) y el examen de conducción (CU22).
- Eventos de RabbitMQ y notificaciones: no se publica nada.
- Cambios en las pantallas de «Mis notas» (CU23): ya listan lo que haya en `resultados`.
