---
globs: projects/backend/**/*
---

# Persistencia / Base de Datos — Backend

## Identificador de fila: `id` interno + `codigo` público (patrón `BaseModel`)

Todo Model extiende
[`common/model/BaseModel.java`](../../../projects/backend/src/main/java/com/example/backend/common/model/BaseModel.java)
(ver [ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md)), que define
**dos identificadores distintos**, ambos obligatorios y con roles que no
se mezclan:

| Campo | Tipo | Rol |
| --- | --- | --- |
| `id` | `Long`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | Clave primaria real de la tabla. **Nunca** se expone en un DTO, una URL, un JWT, ni ningún contrato con el cliente — es un detalle interno de persistencia. |
| `codigo` | `UUID` | El identificador **público** de la fila — el que sí viaja en DTOs, URLs (`/api/v1/{recurso}/{codigo}`), y claims de JWT. Se genera en la aplicación (`@PrePersist`, `UUID.randomUUID()` si es `null`), no lo genera la base de datos. |

- El nombre de columna de `id` es siempre `id` (genérico, igual en todas
  las tablas). El de `codigo`, en cambio, se define **por entidad**
  (`codigo_usuario`, `codigo_pedido`, etc. — se decide al crear el Model,
  antes de escribir su migración Flyway) porque `BaseModel` es una única
  clase compartida por todos los Models. Cada entidad concreta fija el
  nombre de columna real con `@AttributeOverride` sobre el campo heredado:

  ```java
  @Entity
  @Table(name = "users")
  @AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
  public class Usuario extends BaseModel {
      // ...
  }
  ```

- `createdAt`/`updatedAt`/`deletedAt` (los otros tres campos de
  `BaseModel`) **sí** usan el mismo nombre de columna en todas las tablas
  (`created_at`/`updated_at`/`deleted_at`) — solo `codigo` cambia de
  nombre por entidad.
- `JpaRepository<Entidad, Long>` — el tipo de ID del repositorio es
  siempre `Long` (el de `id`), nunca `UUID`. Para buscar por el
  identificador público se agrega un finder propio,
  `findByCodigo(UUID codigo)`, en el repositorio de cada Model — no se usa
  `findById(UUID)` para eso (no compilaría, y conceptualmente sería
  buscar por la clave equivocada).

## Nombres de migraciones Flyway

Formato obligatorio: **`V{yyyyMMddHHmmss}__descripcion.sql`**, ej.:

```
V20250115143022__add_user_table.sql
V20250116091500__add_pedido_estado_column.sql
```

**No se usa versión secuencial simple** (`V1__`, `V2__`, `V3__...`).

### Por qué

Con versión secuencial, dos ramas paralelas que agregan cada una su propia
migración terminan generando el mismo número (`V5__`) de forma independiente.
Al mergear, Flyway ve dos migraciones con el mismo número de versión y
revienta (o, peor, una pisa el checksum esperado de la otra si alguien las
renombra a mano para "resolver" el conflicto). El timestamp evita la
colisión por diseño: es prácticamente imposible que dos migraciones creadas
en momentos distintos por personas distintas generen el mismo timestamp.

**No "corregir" esto a formato secuencial** aunque parezca más prolijo o más
fácil de leer en una lista de archivos — es una decisión deliberada para
evitar conflictos de merge, no un descuido.

> Excepción histórica: `V1__init_schema.sql` (la migración inicial del
> scaffold) ya fue aplicada y queda con su nombre actual — no se renombra una
> migración ya aplicada (invalidaría su checksum en `flyway_schema_history`).
> A partir de la **próxima** migración nueva, se usa el formato timestamp.

## Convención de nombres: Models, tablas, columnas

- **Java (Models/campos)**: `camelCase`. Clases en `PascalCase` singular
  (`Usuario`, `PedidoItem`).
- **Base de datos (tablas/columnas)**: `snake_case`. Tablas en plural
  (`usuarios`, `pedido_items`).
- La conversión `camelCase` (Java) → `snake_case` (columna) es **automática**
  vía la estrategia de nombrado default de Spring Boot/Hibernate
  (`SpringPhysicalNamingStrategy` sobre `CamelCaseToUnderscoresNamingStrategy`)
  — no hace falta anotar `@Column(name = "...")` en cada campo solo para
  convertir el case. Se usa `@Column(name = "...")` explícito únicamente
  cuando el nombre de columna necesita diferir del derivado automáticamente
  por alguna razón puntual (ej. una palabra reservada de SQL).
- Nombre de tabla explícito (`@Table(name = "...")`) cuando el plural
  automático no da el resultado correcto en español (ej. un Model
  `MenuSemanal` no debería terminar en una tabla mal pluralizada) — se revisa
  caso por caso, no se asume que el default siempre acierta en español.

## Claves foráneas e índices

- Nombre de columna FK: `{model_referenciado_singular}_id` (ej.
  `usuario_id` en la tabla `pedidos`).
- Toda FK lleva su índice correspondiente si se va a filtrar/joinear por ella
  con frecuencia (no depender del default de Postgres, que no indexa FKs
  automáticamente).
