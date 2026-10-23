# UT-CU12-BE-02 — Models de horarios: escuela, índice de vehículo parcial y constructores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-02` |
| **Nombre** | Models de horarios: escuela, índice de vehículo parcial y constructores |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb-académico-contenido), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok) (`@Getter` de clase, `@Setter` solo por campo), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (patrón `assumeTrue` de Docker; modelo: `TestcontainersSmokeTest`).

## Solicitud de negocio

Las franjas de distintos instructores tienen que poder coexistir en la misma fecha y hora aunque todavía no tengan vehículo, y cada horario debe saber a qué escuela pertenece.

## Objetivo

`PlantillaHorario` y `Franja` guardan la escuela, se pueden construir desde el código, y el índice único por vehículo solo aplica cuando hay vehículo.

## Alcance

- **`PlantillaHorario`:** campo `escuelaCodigo` (`UUID`, obligatorio, sin setter). Constructor público con los campos que se fijan al crear: `escuelaCodigo`, `instructorCodigo`, `diaSemana`, `horaInicio`, `horaFin`, `duracionClaseMin`, `vigenteDesde`, `vigenteHasta`; `activa = true`. Se mantiene el `@NoArgsConstructor` (lo exige Spring Data). `vehiculoCodigo` queda sin tocar y sin asignar.
- **`Franja`:** campo `escuelaCodigo` (`UUID`, obligatorio, sin setter). Constructor público con `escuelaCodigo`, `plantillaId`, `instructorCodigo`, `fecha`, `horaInicio`, `horaFin`; `estado = LIBRE`.
- **Índice `uq_franja_vehiculo_fecha_hora`:** pasa a **parcial** (`partialFilter = "{'vehiculoCodigo': {$exists: true}}"` en `@CompoundIndex`). El de instructor se mantiene tal cual. Spring Data no escribe los campos `null`, así que una franja sin vehículo no entra al índice.
- **Si ya existe el índice viejo en tu base de desarrollo**, el arranque falla con un conflicto de opciones de índice. Se resuelve una vez con `docker exec drivehub-mongo mongosh academico --eval "db.franjas.dropIndex('uq_franja_vehiculo_fecha_hora')"` (o borrando la colección, que está vacía). Dejarlo escrito en el comentario del Model.
- **Documentación:** actualizar las secciones `plantillas_horario` y `franjas` de [DISENO_BASE_DE_DATOS.md](../../../DriveHub/DISENO_BASE_DE_DATOS.md) (campo `escuelaCodigo`, índice parcial, nota de que `vehiculoCodigo` queda vacío hasta CU10) y su fecha de "Última revisión".
- **Test de integración con MongoDB real** (Testcontainers, `MongoDBContainer` mongo:8, patrón `assumeTrue`; contexto mínimo con `MongoTemplate`, sin levantar toda la app): dos franjas de **instructores distintos**, misma fecha y hora, **sin vehículo** → ambas se guardan; dos franjas del **mismo instructor**, misma fecha y hora → `DuplicateKeyException`; dos franjas con el **mismo vehículo**, misma fecha y hora → `DuplicateKeyException`.
- Test unitario: los constructores fijan `activa = true` y `estado = LIBRE` y no asignan `vehiculoCodigo`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/model/PlantillaHorario.java`, `Franja.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/model/FranjaIndicesTest.java`, `PlantillaHorarioTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Repositorios y servicios (UT 05 en adelante).
- Asociar el vehículo a un horario o franja (CU10).
- Los demás documentos de Académico (reservas, clases, etc.) y `BaseDocument`.
- Un filtro automático por escuela.

## Resultado esperado

Los Models se crean con su escuela, y dos instructores pueden tener franjas a la misma hora sin vehículo mientras el mismo instructor o el mismo vehículo siguen sin poder duplicarse.

## Validación

1. `./mvnw test` en `academico` pasa (el test de índices queda `SKIPPED` si Docker no está disponible, ver TESTING_BACKEND.md).
2. `./mvnw checkstyle:check` sin errores.
3. Con `docker compose up` y MongoDB corriendo, `academico` arranca sin error de índices y `db.franjas.getIndexes()` en `mongosh` muestra `uq_franja_vehiculo_fecha_hora` con `partialFilterExpression`.
