---
globs: projects/backend/{gateway,identidad,escuela,pagos,academico}/**/*
---

# Convenciones de Código Java — Backend

## Alcance

Aplica a los 5 proyectos Spring Boot: `gateway`, `identidad`, `escuela`,
`pagos` y `academico`. Paquete raíz: **`com.drivehub.{servicio}`**.

## Estructura de paquetes

Depende de la arquitectura de cada servicio (ver
[MICROSERVICIOS.md](../MICROSERVICIOS.md#mapa-de-servicios-backend)):

- **Capas** (`pagos`, `academico`): `common/`, `config/`, `features/{modulo}/{controller,service,repository,dto,model,enums,mapper}`
  — detalle en [ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md#estructura-de-paquetes).
- **Hexagonal** (`identidad`): [ARQUITECTURA_HEXAGONAL_BACKEND.md](ARQUITECTURA_HEXAGONAL_BACKEND.md).
- **Clean** (`escuela`): [ARQUITECTURA_CLEAN_BACKEND.md](ARQUITECTURA_CLEAN_BACKEND.md).

En los tres, `common/` contiene lo mismo: `exception/` (jerarquía +
`GlobalExceptionHandler`), `response/` (`ApiResponse`, `PageMeta`) y
`util/` (`UuidV7`). Es una copia por servicio a propósito (cada servicio se
compila y despliega solo, ver ARQUITECTURA_BACKEND.md#regla-de-dependencia).

Una clase que solo la usa un módulo vive **dentro de ese módulo**, no en
`common/` — `common/` es exclusivamente para lo que de verdad comparten dos o
más módulos del servicio. No se mueve algo a `common/` "por las dudas".

`enums/` (en capas; en hexagonal/clean los enums viven junto al modelo de
dominio) es solo para **enumeraciones de negocio** — un campo que solo
puede tomar un set fijo de valores, persistido `@Enumerated(EnumType.STRING)`
(nunca `ORDINAL`). No confundir con `{Recurso}ErrorCodes` (el catálogo de
códigos de error del módulo, ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md))
— vive suelto en la raíz del paquete del módulo. Si el módulo no tiene
ninguna enumeración de negocio, la carpeta directamente no existe. Un enum
que usan dos módulos del mismo servicio va en `common/enums/` (ej.
`ResultadoEvaluacion` en Académico).

Todo Model JPA extiende el `BaseModel` de su servicio y todo documento de
Mongo el `BaseDocument`: `id` Long interno + codigo UUID v7 público +
`createdAt`/`updatedAt` + `deletedAt` (soft delete). Detalle completo en
[PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md#identificadores-id-interno-long--codigo-público-uuid-v7).

## Nombres de clases

| Tipo | Convención | Ejemplo |
| --- | --- | --- |
| Controller | `{Recurso}Controller` | `PagoController` |
| Service | `{Recurso}Service` (clase concreta, **sin** interfaz `IXxx`/`XxxImpl` salvo que exista más de una implementación real) | `UsuarioService` |
| Repository | `{Recurso}Repository extends JpaRepository<...>` (o `MongoRepository`) | `PagoRepository` |
| Model (capas) | Nombre de dominio en singular, sin sufijo | `Pago`, `Reserva` |
| Model JPA (hexagonal / clean) | `{Dominio}Model`: el nombre sin sufijo es del modelo de dominio | `UsuarioModel`, `VehiculoModel` |
| DTO de entrada | `{Recurso}Request` (o `Crear{Recurso}Request` / `Actualizar{Recurso}Request` si hace falta distinguir create de update) | `CrearUsuarioRequest` |
| DTO de salida | `{Recurso}Response` | `UsuarioResponse` |
| Mapper | `{Recurso}Mapper` | `UsuarioMapper` |
| Excepción de negocio | Ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md) | `NotFoundException` |
| Códigos de error de un módulo | `{Recurso}ErrorCodes` (interfaz de constantes o `enum`) | `UsuarioErrorCodes` |

> **"Model", no "Entity"** — así se le llama en este proyecto a la clase
> mapeada a una tabla, y así se llama su carpeta (`model/`, no `entity/`).
> Esto es solo el nombre que usamos nosotros para referirnos a ella; la
> clase en sí **sigue llevando la anotación `@Entity` de JPA/Jakarta
> Persistence** (`import jakarta.persistence.Entity;`) porque es un
> requisito del framework, no hay forma de evitarlo ni de reemplazarlo por
> una anotación `@Model` que no existe. No te confundas si ves `@Entity`
> en el código de un Model — es lo esperado.

No se usa la clase `Service` como interfaz con un único `ServiceImpl` — eso
es sobre-ingeniería para este proyecto salvo que un módulo concreto
justifique tener más de una implementación intercambiable (ej. para tests
con un doble real, algo que ya cubre Mockito sin necesidad de la interfaz).

## Anotaciones/decorators custom

Las anotaciones custom del proyecto (ej. `@CurrentUserId`, ver
[SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md)) se definen **una
sola vez**, en `security/` si están ligadas a autenticación, o en
`common/` si son de propósito general y no específicas de seguridad. No se
redefine la misma anotación en dos módulos — si dos features necesitan
"lo mismo", es la misma anotación importada, no una copia.

## Estilo general

- 4 espacios de indentación (ya fijado en `.editorconfig` para `*.java`).
- Constructor injection siempre, vía `@RequiredArgsConstructor` de Lombok
  sobre los campos `private final` del `service`/`controller` — no se
  escribe el constructor a mano cuando Lombok puede generarlo (ver
  [Lombok](#lombok) abajo).
- Los `record` de Java se prefieren para DTOs y para `ApiResponse`/`PageMeta`
  (inmutables, sin boilerplate) por sobre clases con Lombok/getters manuales
  — un DTO **no** se convierte en una clase con `@Getter`/`@Setter` de
  Lombok solo porque Lombok esté disponible, sigue siendo un `record`.

## Lombok

**Ya agregado** en `pom.xml` (`org.projectlombok:lombok`, `optional`, con
su path correspondiente en `annotationProcessorPaths` del
`maven-compiler-plugin`, al lado del de
`spring-boot-configuration-processor` — un `annotationProcessorPaths`
custom con más de un elemento necesita listar **todos** los procesadores
que tienen que correr, agregar Lombok sin tocar esa lista lo dejaría sin
efecto). **No se escriben getters/setters/constructores a mano** donde
Lombok los pueda generar — es exactamente el boilerplate que esta
dependencia existe para eliminar.

| Dónde | Qué anotación | Por qué |
| --- | --- | --- |
| `model/` (Models JPA, incluido `BaseModel`) | `@Getter` a nivel de clase; `@Setter` **por campo**, solo en los campos que de verdad necesitan mutarse desde fuera (ej. `deletedAt` para soft delete) | Un Model normalmente no debería exponer setters de todos sus campos sin criterio — igual que `BaseModel` no expone `setCodigo()`/`setCreatedAt()` (son de solo lectura una vez persistidos), cada Model nuevo decide campo por campo qué setter tiene sentido, no `@Setter` a nivel de clase por defecto |
| `service/`, `controller/` (constructor injection) | `@RequiredArgsConstructor` sobre la clase, con los campos `private final` | Reemplaza el constructor explícito que pedía la versión anterior de esta regla |

### Qué NO se usa

- **`@Data`** en un Model: agrupa `@Getter`+`@Setter`+`@ToString`+`@EqualsAndHashCode`+`@RequiredArgsConstructor`
  de una sola vez, pero `@ToString`/`@EqualsAndHashCode` automáticos sobre
  una entidad JPA son un problema conocido — `@ToString` expone **todos**
  los campos, incluidos los que
  [LOGGING_BACKEND.md](LOGGING_BACKEND.md#nunca-loguear) prohíbe loguear
  (ej. `passwordHash`), y `@EqualsAndHashCode` sobre una relación
  `@ManyToOne`/`@OneToMany` puede disparar carga perezosa (lazy loading) o
  recursión infinita en relaciones bidireccionales. Por eso cada Model
  compone explícitamente `@Getter`/`@Setter` (nunca `@Data`), y no agrega
  `@ToString`/`@EqualsAndHashCode` de Lombok salvo una necesidad puntual
  evaluada caso por caso (y en ese caso, con `@ToString.Exclude`/
  `@EqualsAndHashCode.Exclude` explícito sobre cualquier campo sensible o
  relación lazy).
- **Lombok en DTOs**: los DTOs siguen siendo `record` (ver arriba) — Lombok
  no reemplaza esa decisión.
- **`@Slf4j`**: el logging estructurado del proyecto usa
  `LoggerFactory.getLogger(...)` explícito — no se evaluó todavía
  reemplazarlo por la anotación de Lombok; se mantiene como está hasta que
  se decida explícitamente lo contrario.
