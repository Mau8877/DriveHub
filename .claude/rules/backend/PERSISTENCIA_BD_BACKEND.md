---
globs: projects/backend/**/*
---

# Persistencia / Base de Datos — Backend

Cada microservicio tiene **su propia base** y nadie más la toca (ver
[MICROSERVICIOS.md](../MICROSERVICIOS.md#una-base-por-servicio-sin-excepciones)):

| Base | Servicios | Por qué (de `docs/DriveHub/DriveHub_Bases_de_Datos_y_Microfrontends.pdf`) |
| --- | --- | --- |
| PostgreSQL 18 | Identidad, Escuela, Pagos | Relacional con integridad fuerte; Pagos necesita ACID para la saga |
| MongoDB 8 (replica set) | Académico, Contenido | Documentos con estructura variable (exámenes, preguntas anidadas, metadatos de archivos) |
| DynamoDB | Notificaciones, Asistente IA | Mucho historial consultado siempre por usuario y fecha; TTL para limpiar |

El diseño **real y vigente** de cada tabla/colección (campos, tipos, índices,
relaciones, por microservicio) está en
[`docs/DriveHub/DISENO_BASE_DE_DATOS.md`](../../../docs/DriveHub/DISENO_BASE_DE_DATOS.md);
el PDF `DriveHub_Diseno_Base_de_Datos.pdf` queda solo como guía histórica. Esta
rule define **cómo** se implementa ese diseño.

**Todo cambio de esquema actualiza ese Markdown en el mismo cambio**: una
migración nueva (Flyway o Laravel), un campo nuevo en un `@Document`, un
atributo o GSI nuevo en DynamoDB. Un esquema que cambió en el código pero no en
el Markdown es, a todo efecto práctico, un esquema sin documentar (mismo
criterio que `docs/errors/`).

## Identificadores: id interno Long + codigo público UUID v7

Regla de todo el proyecto, en las tres bases:

| Campo | Tipo | Rol |
| --- | --- | --- |
| id interno | `Long` / `BIGINT` | Clave primaria real. **Nunca** sale del servicio: ni DTOs, ni URLs, ni JWT, ni eventos. Las FK **dentro** del servicio apuntan a este id. |
| `codigo_{tabla}` | UUID **versión 7** | Identificador **público**: el único que viaja en DTOs, URLs (`/api/v1/{recurso}/{codigo}`), claims de JWT y eventos. Lo genera la **aplicación** al crear la fila, no la base. |

- **UUID v7, no v4**: es ordenable por tiempo (los primeros 48 bits son
  el timestamp en ms), así el índice único de la columna crece "al final"
  en vez de fragmentarse con valores aleatorios. Generador por stack:
  Java `common/util/UuidV7.generar()` (Java no trae v7), Python
  `uuid.uuid7()`, Node `uuid` (`v7()`), PHP `Str::uuid7()`.
- **Todas** las tablas/colecciones llevan su codigo, incluidas las de
  asociación (`usuario_rol` → `codigo_usuario_rol`) y los catálogos.
- **Persona única**: el `codigo_usuario` de Identidad es la persona en
  toda la plataforma. Escuela guarda ese valor en `estudiante.usuario_codigo`
  / `instructor.usuario_codigo`, y los demás servicios referencian al
  estudiante/instructor con ese mismo UUID (`estudiante_codigo`,
  `instructorCodigo`...).
- **Referencias a otro servicio**: `{entidad}_codigo` (Postgres/Laravel)
  o `{entidad}Codigo` (Spring Mongo, NestJS, FastAPI) con el codigo público
  del dueño, **sin FK física**. Ej.: `pago.reserva_codigo` = `codigoReserva`
  de Académico.
- Campos de negocio que en el diseño se llamaban `codigo` y no son el
  identificador se renombraron para no chocar: `permiso.clave`
  (`reserva.crear`) y `categoria_licencia.sigla` (`A`, `B`, `C`).

## PostgreSQL (Identidad, Escuela, Pagos)

### `BaseModel` (JPA)

Todo Model extiende el `BaseModel` de su servicio (en Pagos
`common/model/`; en Identidad y Escuela dentro de su adaptador de
persistencia, ver sus rules de arquitectura): `id` (`@Id`, `IDENTITY`),
`codigo` (UUID v7 en `@PrePersist`), `createdAt`/`updatedAt` (Hibernate) y
`deletedAt` (soft delete, `@SQLRestriction("deleted_at IS NULL")`).

La columna del codigo se fija por entidad con `@AttributeOverride`:

```java
@Entity
@Table(name = "usuario")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
public class UsuarioModel extends BaseModel { ... }
```

`JpaRepository<XxxModel, Long>`; para buscar por el identificador público
se agrega `findByCodigo(UUID codigo)`.

### Nombres

- **Tablas en singular y `snake_case`**, como en el diseño (`usuario`,
  `plan_pago`, `asignacion_alumno`). Siempre con `@Table(name = "...")`
  explícito.
- Columnas `snake_case`; la conversión desde `camelCase` es automática
  (no se anota `@Column(name)` solo para el case).
- FK internas: `{tabla_referenciada}_id` (`usuario_id`, `plan_pago_id`),
  siempre con índice si se filtra/joinea por ella (un `UNIQUE` compuesto
  que empieza por esa columna ya cuenta).
- `created_at` / `updated_at` / `deleted_at` en todas las tablas (el
  diseño los llamaba `creado_en`/`actualizado_en`; se unificó con BaseModel).
- Estados: `VARCHAR` + `CHECK (estado IN (...))` en la migración y
  `@Enumerated(EnumType.STRING)` en el Model (nunca `ORDINAL`).
- `CHAR(n)` se mapea con `@JdbcTypeCode(SqlTypes.CHAR)`; `JSONB` con
  `@JdbcTypeCode(SqlTypes.JSON)`.

### Migraciones Flyway

Formato obligatorio: **`V{yyyyMMddHHmmss}__descripcion.sql`**, ej.
`V20261018120000__crear_esquema_pagos.sql`. **No** se usa versión
secuencial (`V1__`, `V2__`): dos ramas paralelas generarían el mismo número
y Flyway revienta al mergear; el timestamp evita la colisión por diseño.
No se "corrige" a formato secuencial.

- `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.
- `codigo_{tabla} UUID NOT NULL UNIQUE` (sin default: lo genera la app).
  Para seeds dentro de una migración se usa `uuidv7()` (nativa de
  PostgreSQL 18).
- Hibernate corre con `ddl-auto=validate`: si el Model y la migración no
  coinciden, el servicio no arranca. Es intencional.
- Una migración ya aplicada no se edita: el cambio va en una migración nueva.
- **`spring.flyway.out-of-order=true`** en los tres servicios Postgres. Como la versión es la fecha, una
  rama puede traer una migración con fecha anterior a otra que una base ya aplicó (pasa al mergear
  ramas paralelas). Sin esta opción Flyway falla con «Detected resolved migration not applied to
  database» y el servicio no arranca; con ella aplica la que falta. La contrapartida: una migración
  nueva no puede depender del orden relativo con otra de fecha intermedia ya aplicada en otro entorno.

## MongoDB (Académico, Contenido)

- **`_id` entero (Long)**, no `ObjectId`, para cumplir la regla de id
  interno Long. MongoDB no tiene autoincremental: cada servicio usa una
  colección `contadores` (`{ _id: "reservas", secuencia: 42 }`) con un
  `findAndModify`/`findOneAndUpdate` atómico `$inc`. Se asigna al crear el
  documento:
  - Académico: `AsignarIdentificadoresCallback` + `SecuenciaService`
    (`common/mongo/`), sobre `BaseDocument`.
  - Contenido: `BaseModel::booted()` + `Secuencia` (`app/Common/`).
- **Codigo público**: Académico (Spring) `codigo{Coleccion}` en camelCase
  (`codigoReserva`, `codigoFranja`); Contenido (Laravel) `codigo_{coleccion}`
  en snake_case (`codigo_manual`). Cada stack sigue su convención de nombres
  de campo; ambos con índice único.
- **Colecciones** con el nombre del diseño (`franjas`, `reservas`,
  `banco_preguntas`, `manuales`...).
- **Índices** declarados en código, nunca creados a mano: Spring con
  `@Indexed`/`@CompoundIndex` en el Model (`spring.data.mongodb.auto-index-creation=true`),
  Laravel en `database/migrations/`.
- **Soft delete** (`deletedAt` / `deleted_at`): Laravel lo filtra solo
  (`SoftDeletes`); en Spring Data **no hay filtro automático**: cada query
  del repository filtra `deletedAt: null` explícitamente.
- **Transacciones multi-documento** (ej. reserva + franja + outbox)
  requieren el **replica set** (`rs0` en docker-compose). Desde el host se
  conecta con `?directConnection=true`.
- Datos embebidos (opciones de una pregunta, historial de estados, criterios
  de una prueba): `record` en Spring, arrays nativos en Laravel (sin cast
  `array` de Eloquent, que los serializaría como string).
- Copias deliberadas (ej. las preguntas dentro de `intentos_simulacro`, o
  fecha/hora de la franja dentro de `reservas`) son parte del diseño: se
  documentan con un comentario en el Model.

## DynamoDB (Notificaciones, Asistente IA)

- **No hay id Long**: DynamoDB no tiene autoincremental y su clave
  primaria (partición + orden) **es** el identificador. Es la única
  excepción a la regla de id interno.
- **Claves con codigos públicos**: partición = `usuarioCodigo` (la persona
  de Identidad) y orden = `'<fecha ISO>#<codigo{Tabla}>'` para todo lo que
  se consulta por usuario y fecha. Cada ítem guarda además su propio
  `codigo{Tabla}` (UUID v7, que reemplaza al ULID del diseño original: es
  igual de ordenable por tiempo).
- **Atributos en camelCase** en los dos servicios (Python lo resuelve con
  `ItemDynamo`, alias camelCase sobre campos snake_case).
- **Tablas declaradas en código**, una definición por servicio
  (`common/dynamodb/definicion-tablas.ts` / `app/common/dynamodb/tablas.py`)
  y creadas al arrancar si faltan (`DYNAMODB_CREAR_TABLAS=true`, solo
  desarrollo con dynamodb-local). En AWS real se provisionan por
  infraestructura. Cambiar el esquema de una tabla existente (un GSI
  nuevo) no se aplica solo: se hace a mano o recreando la tabla.
- **TTL** con un atributo numérico `ttl` (epoch en segundos) donde el
  diseño pide expiración (Notificaciones 180 días, EventosProcesados 7 días).
- **Índices dispersos** (ej. `NoLeidas`): el atributo de la clave del GSI
  solo existe en los ítems que tienen que aparecer en el índice.
- Ítems de más de 400 KB no entran: lo que crece sin límite (mensajes de
  una conversación) va en su propia tabla (`MensajesIA`).
