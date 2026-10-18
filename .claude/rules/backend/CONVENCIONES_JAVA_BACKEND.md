---
globs: projects/backend/**/*
---

# Convenciones de Código Java — Backend

## Estructura de paquetes

Todo bajo `com.example.backend`, siguiendo el scaffold feature-based ya
creado:

```
com.example.backend
├── common/
│   ├── constant/       # Constantes compartidas entre módulos
│   ├── exception/       # BusinessException y jerarquía (ver EXCEPCIONES_BACKEND.md)
│   ├── model/           # BaseModel (codigo UUID, timestamps, soft delete -- ver abajo)
│   ├── response/        # ApiResponse, PageMeta (ver RESPONSES_BACKEND.md)
│   └── util/            # Utilidades genéricas sin estado
├── config/               # Configuración de Spring (SecurityConfig, etc.)
├── security/             # JWT, filtros, @CurrentUserId, UserDetailsServiceImpl
└── features/
    └── {modulo}/         # ej. usuarios/, auth/, pedidos/
        ├── controller/
        ├── enums/        # Enumeraciones de negocio del módulo (si aplica, ver nota abajo)
        ├── service/
        ├── repository/
        ├── dto/          # XxxRequest, XxxResponse
        ├── model/        # La clase mapeada a una tabla (sigue llevando @Entity de JPA, ver nota abajo)
        └── mapper/       # (si el mapeo Model↔DTO no es trivial)
```

Una clase que solo la usa un módulo vive **dentro de ese módulo**, no en
`common/` — `common/` es exclusivamente para lo que de verdad comparten dos o
más features. No se mueve algo a `common/` "por las dudas".

`enums/` es solo para **enumeraciones de negocio propias del módulo** (ej.
`TipoNotificacion { SUCCESS, INFO, WARNING, ERROR }` en `notificaciones/`) —
un campo del Model que solo puede tomar un set fijo de valores, persistido
`@Enumerated(EnumType.STRING)` (nunca `ORDINAL`, mismo criterio que `Role`
en [SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)).
No confundir con `{Recurso}ErrorCodes` (el catálogo de códigos de error del
módulo, ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)) — eso sigue
sin una subcarpeta dedicada, vive suelto en la raíz del paquete del módulo.
Si el módulo no tiene ninguna enumeración de negocio, la carpeta
directamente no existe (no se crea vacía "por si acaso").

Todo Model de un módulo extiende
[`common/model/BaseModel.java`](../../../projects/backend/src/main/java/com/example/backend/common/model/BaseModel.java):
`id` (`Long`, el `@Id`/PK real, nunca expuesto fuera de persistencia) +
`codigo` (`UUID`, el identificador público que sí viaja en DTOs/URLs/JWT —
ver [PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md#identificador-de-fila-id-interno--codigo-público-patrón-basemodel)
para el detalle completo del porqué de esta separación), `createdAt`/`updatedAt`
(de solo lectura, los administra Hibernate vía
`@CreationTimestamp`/`@UpdateTimestamp`), y `deletedAt` (soft delete —
`@SQLRestriction("deleted_at IS NULL")` en `BaseModel` filtra las filas
borradas de **toda** query que Hibernate genere para la entidad, sin tener
que repetir esa condición a mano en cada `Repository`).

## Nombres de clases

| Tipo | Convención | Ejemplo |
| --- | --- | --- |
| Controller | `{Recurso}Controller` | `UsuarioController` |
| Service | `{Recurso}Service` (clase concreta, **sin** interfaz `IXxx`/`XxxImpl` salvo que exista más de una implementación real) | `UsuarioService` |
| Repository | `{Recurso}Repository extends JpaRepository<...>` | `UsuarioRepository` |
| Model | Nombre de dominio en singular, sin sufijo | `Usuario` |
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
