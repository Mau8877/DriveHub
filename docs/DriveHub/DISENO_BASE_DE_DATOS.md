# Diseño de base de datos — estado real

> **Fuente de verdad.** Este documento describe las tablas, colecciones e
> ítems **tal como existen hoy en el código** (migraciones Flyway, documentos
> de Spring Data MongoDB, migraciones de Laravel y definiciones de DynamoDB).
> El PDF [`DriveHub_Diseno_Base_de_Datos.pdf`](DriveHub_Diseno_Base_de_Datos.pdf)
> queda como **guía histórica**: si algo difiere, gana este archivo.
>
> **Se actualiza en el mismo cambio que toca el esquema** (una migración
> nueva, un campo nuevo en un documento, un GSI nuevo), igual que
> [`docs/errors/`](../errors/README.md) con los códigos de error.
>
> Reglas de implementación (ids, nombres, migraciones, soft delete):
> [PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

Última revisión: 2026-10-21 (se agregó CU12 en Académico).

## Contenido

1. [Visión general](#1-visión-general)
2. [Identidad — PostgreSQL](#2-identidad--postgresql)
3. [Escuela — PostgreSQL](#3-escuela--postgresql)
4. [Pagos — PostgreSQL](#4-pagos--postgresql)
5. [Académico — MongoDB](#5-académico--mongodb)
6. [Contenido — MongoDB](#6-contenido--mongodb)
7. [Notificaciones — DynamoDB](#7-notificaciones--dynamodb)
8. [Asistente IA — DynamoDB](#8-asistente-ia--dynamodb)
9. [Referencias entre servicios](#9-referencias-entre-servicios)
10. [Diferencias con el PDF y pendientes](#10-diferencias-con-el-pdf-y-pendientes)

---

## 1. Visión general

| Servicio | Base | Nombre | Tablas / colecciones | Cómo se crea el esquema |
| --- | --- | --- | --- | --- |
| `identidad` | PostgreSQL 18 | `identidad` | 7 | Flyway (`V…__*.sql`) |
| `escuela` | PostgreSQL 18 | `escuela` | 9 | Flyway |
| `pagos` | PostgreSQL 18 | `pagos` | 8 | Flyway |
| `academico` | MongoDB 8 (replica set `rs0`) | `academico` | 13 | Spring Data (`@Document`, `@Indexed`) |
| `contenido` | MongoDB 8 (replica set `rs0`) | `contenido` | 2 + `contadores` | Migración de Laravel |
| `notificaciones` | DynamoDB | — | 4 | `definicion-tablas.ts` (se crean al arrancar en dev) |
| `asistente-ia` | DynamoDB | — | 4 | `tablas.py` (se crean al arrancar en dev) |

`gateway` no tiene base de datos. **Una base por servicio**: ninguno lee la base de otro.

### Convenciones comunes

- **PostgreSQL:** cada tabla lleva además `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` (interno, no sale del servicio), `codigo_{tabla} UUID NOT NULL UNIQUE` (público, UUID v7 generado por la aplicación) y `created_at`, `updated_at`, `deleted_at` (`TIMESTAMPTZ`, soft delete). **No se repiten en las tablas de abajo**: solo se listan las columnas propias.
- **MongoDB:** cada documento lleva `_id` (Long secuencial, interno, de la colección `contadores`), `codigo{Coleccion}` / `codigo_{coleccion}` (UUID v7 público, índice único), `createdAt`/`updatedAt`/`deletedAt` (o `created_at`/… en Laravel). Tampoco se repiten. Los subdocumentos y arrays se guardan nativos.
- **DynamoDB:** no hay id Long; la clave primaria es el identificador. Los atributos van en `camelCase`.
- **Referencias a otro servicio:** `{entidad}_codigo` / `{entidad}Codigo` con el código público del dueño, **sin FK física** (ver [sección 9](#9-referencias-entre-servicios)).
- **Estados:** `VARCHAR` + `CHECK` en PostgreSQL; en Mongo/Dynamo, el valor del enum de código.

### Mapa de dependencias entre servicios

```mermaid
flowchart LR
    identidad[(identidad<br/>PostgreSQL)]
    escuela[(escuela<br/>PostgreSQL)]
    pagos[(pagos<br/>PostgreSQL)]
    academico[(academico<br/>MongoDB)]
    contenido[(contenido<br/>MongoDB)]
    notificaciones[(notificaciones<br/>DynamoDB)]
    asistente[(asistente-ia<br/>DynamoDB)]

    identidad -. "usuario.escuela_codigo" .-> escuela
    escuela -. "usuario_codigo" .-> identidad
    pagos -. "estudiante_codigo" .-> identidad
    pagos -. "categoria_sigla" .-> escuela
    pagos -. "reserva_codigo" .-> academico
    academico -. "pagoCodigo" .-> pagos
    academico -. "estudianteCodigo, instructorCodigo" .-> identidad
    academico -. "vehiculoCodigo, categoria" .-> escuela
    contenido -. "autor_codigo, categoria" .-> identidad
    notificaciones -. "usuarioCodigo" .-> identidad
    asistente -. "usuarioCodigo" .-> identidad
```

Las líneas punteadas son **referencias por código, sin FK**: se validan al escribir (REST al dueño) o se mantienen por eventos.

---

## 2. Identidad — PostgreSQL

Cuentas, roles, permisos y sesiones. Migraciones: `V20261018120000__crear_esquema_identidad.sql`, `V20261019110000__usuario_escuela_y_roles.sql` (CU01), `V20261019120000__usuario_bloqueado_hasta.sql` (CU02), `V20261020120000__correo_verificado_y_tipo_token.sql` (CU04b), `V20261021120000__cambio_de_correo.sql` (CU06), `V20261023100000__crear_perfil.sql` (CU08a), `V20261023120000__extension_unaccent.sql` (CU08).

```mermaid
erDiagram
    usuario ||--o{ usuario_rol : "tiene"
    rol ||--o{ usuario_rol : "asignado en"
    rol ||--o{ rol_permiso : "otorga"
    permiso ||--o{ rol_permiso : "pertenece a"
    usuario ||--o{ refresh_token : "sesiones"
    usuario ||--o{ token_recuperacion : "recuperaciones"
    usuario ||--|| perfil : "datos de la persona"

    usuario {
        bigint id PK
        uuid codigo_usuario UK
        varchar email UK
        uuid escuela_codigo "NULL = Super admin"
        varchar estado
    }
    perfil {
        bigint id PK
        uuid codigo_perfil UK
        bigint usuario_id FK, UK
        varchar ci UK
        varchar sexo
        varchar categoria_sigla "Escuela"
    }
    rol {
        bigint id PK
        varchar nombre UK
    }
    permiso {
        bigint id PK
        varchar clave UK
    }
    usuario_rol {
        bigint usuario_id FK
        bigint rol_id FK
    }
    rol_permiso {
        bigint rol_id FK
        bigint permiso_id FK
    }
    refresh_token {
        bigint usuario_id FK
        varchar token_hash UK
        varchar dispositivo
    }
    token_recuperacion {
        bigint usuario_id FK
        varchar token_hash UK
    }
```

### `usuario`

Cuenta de acceso. `codigo_usuario` es la **persona única** en toda la plataforma. CU01, CU02, CU06. Los datos de la persona (nombres, contacto, CI, sexo, ubicación) están en [`perfil`](#perfil) desde CU08a.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `email` | `VARCHAR(120) NOT NULL UNIQUE` | Login. Único en **toda** la plataforma, guardado en minúsculas |
| `password_hash` | `VARCHAR(255) NOT NULL` | BCrypt, nunca en claro |
| `escuela_codigo` | `UUID` | Escuela (tenant) de la cuenta, **sin FK** → `escuela.codigo_escuela`. `NULL` para el Super admin. Índice `idx_usuario_escuela_codigo`. *CU01* |
| `estado` | `VARCHAR(12) NOT NULL DEFAULT 'ACTIVO'` | `CHECK IN ('ACTIVO','INACTIVO','BLOQUEADO')` |
| `intentos_fallidos` | `INT NOT NULL DEFAULT 0` | Se reinicia en un login correcto. Bloqueo al 5.º fallo. *CU02* |
| `bloqueado_hasta` | `TIMESTAMPTZ` | Fin del bloqueo (15 min); se levanta solo. *CU02* |
| `correo_verificado` | `BOOLEAN NOT NULL DEFAULT FALSE` | El correo se verificó con el enlace enviado. Todas las cuentas, también las existentes, empiezan en `false`. No bloquea el login: solo condiciona la recuperación de contraseña. *CU04b* |
| `ultimo_login` | `TIMESTAMPTZ` | |

### `perfil`

Datos de la persona, 1 a 1 con `usuario`, para **todos** los roles (Super admin, Administrador, Instructor y Estudiante). *CU08a*

| Columna | Tipo | Notas |
| --- | --- | --- |
| `codigo_perfil` | `UUID NOT NULL UNIQUE` | Público solo dentro del servicio: la persona se identifica por `codigo_usuario` |
| `usuario_id` | `BIGINT NOT NULL UNIQUE REFERENCES usuario(id)` | El 1 a 1 |
| `nombres` | `VARCHAR(80) NOT NULL` | |
| `apellidos` | `VARCHAR(80) NOT NULL` | |
| `telefono` | `VARCHAR(20)` | |
| `foto_url` | `TEXT` | **Clave** del objeto en el bucket privado `drivehub-identidad` (`usuarios/{codigo_usuario}/{uuid v7}.{ext}`), no una URL: la URL de lectura se firma al responder. `NULL` si no tiene foto. *CU06* |
| `fecha_nacimiento` | `DATE NOT NULL` | Para validar la edad mínima (18) al registrarse. *CU01* |
| `ci` | `VARCHAR(15) NOT NULL UNIQUE` | Documento de identidad: único en toda la plataforma, sin espacios y en mayúsculas |
| `sexo` | `VARCHAR(10) NOT NULL` | `CHECK IN ('MASCULINO','FEMENINO')` |
| `direccion` | `VARCHAR(300)` | La calcula el servidor desde `latitud` y `longitud` (geocodificación inversa); en blanco si no se pudo. `NULL` para el Super admin |
| `latitud` | `NUMERIC(9,6)` | Punto elegido en el mapa, `CHECK BETWEEN -90 AND 90`. Van juntas con `longitud` (`CHECK ((latitud IS NULL) = (longitud IS NULL))`) |
| `longitud` | `NUMERIC(9,6)` | `CHECK BETWEEN -180 AND 180` |
| `categoria_sigla` | `VARCHAR(3)` | → `escuela.categoria_licencia.sigla`, sin FK. Solo el estudiante la tiene (arranca en `SIN`) y solo la escuela la cambia |

`ci` y `sexo` son `NOT NULL` para todos; `direccion`, las coordenadas y la categoría son nulables en la base y el caso de uso las exige según el rol (el Super admin no tiene ubicación). La migración rellena las cuentas anteriores con valores provisionales (`ci = 'X' + id`, `sexo = 'MASCULINO'`, `direccion = 'X'`).

**Extensión `unaccent`** (CU08, `V20261023120000`): la búsqueda de estudiantes por nombre compara `unaccent(lower(nombres || ' ' || apellidos))`, así "jose" encuentra a "José". Solo habilita la extensión: no crea tablas ni índices.

### `rol`

Catálogo de roles. Sembrado por migración (los nombres son **datos**, no un enum cerrado).

| Columna | Tipo | Notas |
| --- | --- | --- |
| `nombre` | `VARCHAR(30) NOT NULL UNIQUE` | `SUPER_ADMIN`, `ADMIN_ESCUELA`, `INSTRUCTOR`, `ESTUDIANTE` |
| `descripcion` | `VARCHAR(120)` | |

### `permiso`

Permisos finos (previstos, sin caso de uso asignado: el CU05 original, gestionar roles y permisos, se reasignó a Gestionar escuelas). Todavía sin filas.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `clave` | `VARCHAR(60) NOT NULL UNIQUE` | Ej. `reserva.crear` |
| `descripcion` | `VARCHAR(120)` | |

### `usuario_rol`

Roles de cada usuario (asociación con código propio, no `@ManyToMany`).

| Columna | Tipo | Notas |
| --- | --- | --- |
| `usuario_id` | `BIGINT NOT NULL REFERENCES usuario(id)` | |
| `rol_id` | `BIGINT NOT NULL REFERENCES rol(id)` | Índice `idx_usuario_rol_rol_id` |
| `asignado_en` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | |
| | `UNIQUE (usuario_id, rol_id)` | |

### `rol_permiso`

| Columna | Tipo | Notas |
| --- | --- | --- |
| `rol_id` | `BIGINT NOT NULL REFERENCES rol(id)` | |
| `permiso_id` | `BIGINT NOT NULL REFERENCES permiso(id)` | Índice `idx_rol_permiso_permiso_id` |
| | `UNIQUE (rol_id, permiso_id)` | |

### `refresh_token`

Sesiones renovables (CU02, CU03). **Solo el hash SHA-256**: el valor en claro lo tiene el cliente. Una sesión activa por usuario y dispositivo.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `usuario_id` | `BIGINT NOT NULL REFERENCES usuario(id)` | |
| `token_hash` | `VARCHAR(64) NOT NULL UNIQUE` | SHA-256 en hexadecimal |
| `dispositivo` | `VARCHAR(10) NOT NULL` | `CHECK IN ('WEB','MOVIL')` |
| `expira_en` | `TIMESTAMPTZ NOT NULL` | 7 días desde cada renovación |
| `revocado` | `BOOLEAN NOT NULL DEFAULT FALSE` | `true` al renovar (rotación), al iniciar sesión de nuevo en el dispositivo y en CU03 |

Índice `idx_refresh_token_usuario_revocado (usuario_id, revocado)`.

### `token_recuperacion`

Enlaces enviados por correo, de un solo uso: verificar el correo (CU04b), restablecer la contraseña (CU04) y confirmar el cambio de correo (CU06). Solo se guarda el hash.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `usuario_id` | `BIGINT NOT NULL REFERENCES usuario(id)` | Índice `idx_token_recuperacion_usuario_id` |
| `tipo` | `VARCHAR(20) NOT NULL DEFAULT 'RECUPERACION'` | `CHECK IN ('RECUPERACION','VERIFICACION','CAMBIO_CORREO')`. Un enlace de un tipo no sirve para los otros. *CU04b, CU06* |
| `correo_nuevo` | `VARCHAR(120)` | Dirección a la que pasará la cuenta al confirmar el enlace. Solo en `CAMBIO_CORREO` (`CHECK (tipo = 'CAMBIO_CORREO') = (correo_nuevo IS NOT NULL)`). El correo de la cuenta no cambia hasta confirmar. *CU06* |
| `token_hash` | `VARCHAR(64) NOT NULL UNIQUE` | |
| `expira_en` | `TIMESTAMPTZ NOT NULL` | |
| `usado` | `BOOLEAN NOT NULL DEFAULT FALSE` | |

**Datos sembrados:** los 4 roles (migración). En el perfil `dev`, además, `superadmin@gmail.com` (`SUPER_ADMIN`, sin escuela) y `adminexample@gmail.com` (`ADMIN_ESCUELA` de `escuelaexample`) e `instructorexample@gmail.com` (`INSTRUCTOR` de `escuelaexample`), todos con contraseña `123123123`.

---

## 3. Escuela — PostgreSQL

Escuelas (tenants), instructores, flota, categorías de licencia y asignaciones. El **estudiante no tiene tabla propia** (CU08a): es una cuenta con su perfil en [Identidad](#2-identidad--postgresql) y Escuela lo referencia por su `codigo_usuario`. Migraciones: `V20261018120000__crear_esquema_escuela.sql`, `V20261019100000__crear_tabla_escuela.sql` (CU01), `V20261021100000__escuela_estado_por_borrado_logico.sql` (CU05), `V20261023100000__escuela_sin_estudiante_y_categorias.sql` (CU08a), `V20261024100000__vehiculo_escuela_codigo.sql` y `V20261024110000__vehiculo_imagen_url.sql` (CU07). CU09: `V20261024120000__instructor_con_escuela.sql`.

```mermaid
erDiagram
    categoria_licencia ||--o{ vehiculo : "categoría"
    categoria_licencia ||--o{ instructor_categoria : "habilita"
    instructor ||--o{ instructor_categoria : "enseña"
    vehiculo ||--o{ mantenimiento_vehiculo : "mantenimientos"
    vehiculo ||--o{ asignacion_vehiculo : "asignado"
    instructor ||--o{ asignacion_vehiculo : "usa"
    instructor ||--o{ asignacion_alumno : "tiene alumnos"

    escuela {
        bigint id PK
        uuid codigo_escuela UK
        varchar slug UK
        timestamptz deleted_at "NULL = activa"
    }
    categoria_licencia {
        bigint id PK
        varchar sigla UK
    }
    instructor {
        bigint id PK
        uuid usuario_codigo UK "Identidad"
        uuid escuela_codigo "Tenant, sin FK"
        varchar nro_licencia UK
    }
    vehiculo {
        bigint id PK
        varchar placa UK
        bigint categoria_id FK
    }
```

> `escuela` aún **no** se relaciona con las demás tablas de este servicio, salvo `instructor` (que ya guarda su `escuela_codigo`, CU09): el de `vehiculo`... se agrega con el diseño del filtro por tenant (antes de CU07). Ver [sección 10](#10-diferencias-con-el-pdf-y-pendientes).

### `escuela`

El **tenant** de la plataforma SaaS: cada autoescuela cliente es una fila. CU01, CU02, CU05.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `codigo_escuela` | `UUID NOT NULL UNIQUE` | Es el `escuela_codigo` que guardan los demás servicios |
| `slug` | `VARCHAR(60) NOT NULL UNIQUE` | Identificador legible del enlace de registro (`/register/{slug}`) |
| `nombre` | `VARCHAR(120) NOT NULL` | |
| `deleted_at` | `TIMESTAMPTZ` | Borrado lógico y **única fuente del estado**: activa si es `NULL`, inactiva si tiene fecha. Una escuela inactiva no admite registros ni inicios de sesión de nadie de ella. Desactivar es poner la fecha y reactivar es quitarla (CU05) |

La columna `estado` (`ACTIVA`/`INACTIVA`) se **eliminó** en `V20261021100000`: era una segunda fuente que podía contradecir a `deleted_at`; la migración pasó a borrado lógico las filas que estaban `INACTIVA`. Como el filtro automático de soft delete oculta las filas eliminadas, el listado, la consulta por código y la unicidad del `slug` (que sigue reservado aunque la escuela esté eliminada) usan consultas nativas.

En `dev` se siembra `escuelaexample` con código fijo `0198f0a0-0000-7000-8000-000000000001`.

### `categoria_licencia`

Catálogo (A, B, C…). `sigla` es el valor que usan los demás servicios.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `sigla` | `VARCHAR(3) NOT NULL UNIQUE` | |
| `descripcion` | `VARCHAR(100) NOT NULL` | |
| `edad_minima` | `SMALLINT` | |
| `clases_minimas` | `SMALLINT` | Mínimo de clases prácticas para rendir el examen |

Sembrada por migración (CU08a), en este orden: `P` Particular, `M` Motocicleta, `A` Profesional A, `B` Profesional B, `C` Profesional C y `SIN` Sin categoría (con la que arranca todo estudiante). `edad_minima` y `clases_minimas` quedan sin definir hasta confirmar los valores. **Las siglas y descripciones están pendientes de confirmar con el equipo.**

### `instructor`

Ficha profesional del instructor (CU09).

| Columna | Tipo | Notas |
| --- | --- | --- |
| `usuario_codigo` | `UUID NOT NULL UNIQUE` | → `identidad.usuario.codigo_usuario`, sin FK. El CI y los datos personales están en el `perfil` de Identidad |
| `escuela_codigo` | `UUID NOT NULL` | Escuela (tenant) de la ficha, sin FK (CU09). Índice `idx_instructor_escuela_codigo_estado (escuela_codigo, estado)` |
| `nro_licencia` | `VARCHAR(30) NOT NULL UNIQUE` | |
| `licencia_vence` | `DATE` | |
| `estado` | `VARCHAR(12) NOT NULL DEFAULT 'ACTIVO'` | `CHECK IN ('ACTIVO','INACTIVO','LICENCIA')` |
| `fecha_contratacion` | `DATE` | |

### `instructor_categoria`

Categorías que enseña cada instructor.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `instructor_id` | `BIGINT NOT NULL REFERENCES instructor(id)` | |
| `categoria_id` | `BIGINT NOT NULL REFERENCES categoria_licencia(id)` | Índice `idx_instructor_categoria_categoria_id` |
| | `UNIQUE (instructor_id, categoria_id)` | |

### `vehiculo`

Flota (CU07). Es la primera tabla de Escuela con `escuela_codigo` (tenant): las demás lo suman con sus casos de uso.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `escuela_codigo` | `UUID NOT NULL` | → `escuela.codigo_escuela`, sin FK. Es el claim `escuela` del JWT; toda consulta del repositorio filtra por él. Índice `idx_vehiculo_escuela_codigo` |
| `placa` | `VARCHAR(10) NOT NULL UNIQUE` | Única en toda la plataforma, no por escuela |
| `marca` | `VARCHAR(40) NOT NULL` | |
| `modelo` | `VARCHAR(40) NOT NULL` | |
| `anio` | `SMALLINT` | |
| `color` | `VARCHAR(20)` | |
| `transmision` | `VARCHAR(10) NOT NULL` | `CHECK IN ('MANUAL','AUTOMATICO')` |
| `categoria_id` | `BIGINT REFERENCES categoria_licencia(id)` | Índice `idx_vehiculo_categoria_id` |
| `estado` | `VARCHAR(14) NOT NULL DEFAULT 'DISPONIBLE'` | `CHECK IN ('DISPONIBLE','MANTENIMIENTO','BAJA')` |
| `kilometraje` | `INT NOT NULL DEFAULT 0` | |
| `soat_vence` | `DATE` | |
| `inspeccion_vence` | `DATE` | |
| `imagen_url` | `VARCHAR(300)` | URL https de la foto en Cloudinary (`res.cloudinary.com`); el archivo no se guarda en la base |

### `mantenimiento_vehiculo`

| Columna | Tipo | Notas |
| --- | --- | --- |
| `vehiculo_id` | `BIGINT NOT NULL REFERENCES vehiculo(id)` | Índice `idx_mantenimiento_vehiculo_vehiculo_id` |
| `fecha` | `DATE NOT NULL` | |
| `tipo` | `VARCHAR(12) NOT NULL` | `CHECK IN ('PREVENTIVO','CORRECTIVO')` |
| `descripcion` | `TEXT` | |
| `costo` | `NUMERIC(10,2)` | |
| `km_al_momento` | `INT` | |

### `asignacion_vehiculo`

Qué instructor usa cada auto (CU10). Un vehículo tiene una sola asignación vigente.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `vehiculo_id` | `BIGINT NOT NULL REFERENCES vehiculo(id)` | |
| `instructor_id` | `BIGINT NOT NULL REFERENCES instructor(id)` | Índice `idx_asignacion_vehiculo_instructor_id` |
| `fecha_inicio` | `DATE NOT NULL` | |
| `fecha_fin` | `DATE` | `NULL` = vigente |
| | `UNIQUE (vehiculo_id) WHERE fecha_fin IS NULL AND deleted_at IS NULL` | `uq_asignacion_vehiculo_vigente` |

### `asignacion_alumno`

Instructor asignado a cada estudiante. Un estudiante tiene un solo instructor vigente.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `instructor_id` | `BIGINT NOT NULL REFERENCES instructor(id)` | Índice `idx_asignacion_alumno_instructor_id` |
| `estudiante_codigo` | `UUID NOT NULL` | → `identidad.usuario.codigo_usuario` del estudiante, sin FK (CU08a) |
| `fecha_inicio` | `DATE NOT NULL` | |
| `fecha_fin` | `DATE` | `NULL` = vigente |
| | `UNIQUE (estudiante_codigo) WHERE fecha_fin IS NULL AND deleted_at IS NULL` | `uq_asignacion_alumno_vigente` |

---

## 4. Pagos — PostgreSQL

Planes, cuotas, pagos, pasarela, comprobantes y reversiones. Migración: `V20261018120000__crear_esquema_pagos.sql`. El estado de la saga reserva-pago **no** vive acá ([SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md)).

```mermaid
erDiagram
    plan_pago ||--o{ inscripcion_plan : "contratado en"
    inscripcion_plan ||--o{ cuota : "cuotas"
    cuota ||--o{ pago : "se paga con"
    pago ||--o{ transaccion_pasarela : "intercambios"
    pago ||--|| comprobante : "comprobante"
    pago ||--o{ reversion_pago : "reversiones"

    plan_pago {
        bigint id PK
        varchar nombre
        numeric monto_total
    }
    inscripcion_plan {
        bigint id PK
        uuid estudiante_codigo "Identidad"
        bigint plan_pago_id FK
    }
    cuota {
        bigint id PK
        bigint inscripcion_plan_id FK
        smallint nro
    }
    pago {
        bigint id PK
        uuid estudiante_codigo "Identidad"
        bigint cuota_id FK
        uuid reserva_codigo "Académico"
        varchar idempotency_key UK
    }
    transaccion_pasarela {
        bigint pago_id FK
        varchar referencia_externa UK
    }
    comprobante {
        bigint pago_id FK, UK
        varchar nro_comprobante UK
    }
    reversion_pago {
        bigint pago_id FK
    }
    outbox_evento {
        bigint id PK
        varchar tipo
    }
```

### `plan_pago` (CU25)

| Columna | Tipo | Notas |
| --- | --- | --- |
| `nombre` | `VARCHAR(80) NOT NULL` | |
| `categoria_sigla` | `VARCHAR(3)` | → `escuela.categoria_licencia.sigla`, sin FK |
| `nro_clases` | `SMALLINT NOT NULL` | |
| `monto_total` | `NUMERIC(10,2) NOT NULL` | |
| `nro_cuotas` | `SMALLINT NOT NULL` | |
| `activo` | `BOOLEAN NOT NULL DEFAULT TRUE` | |

### `inscripcion_plan` (CU25, CU28)

Plan contratado por un estudiante.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `estudiante_codigo` | `UUID NOT NULL` | → Identidad. Índice `idx_inscripcion_plan_estudiante_codigo` |
| `plan_pago_id` | `BIGINT NOT NULL REFERENCES plan_pago(id)` | Índice `idx_inscripcion_plan_plan_pago_id` |
| `monto_total` | `NUMERIC(10,2) NOT NULL` | |
| `saldo` | `NUMERIC(10,2) NOT NULL` | |
| `estado` | `VARCHAR(12) NOT NULL DEFAULT 'ACTIVO'` | `CHECK IN ('ACTIVO','COMPLETADO','CANCELADO')` |
| `fecha_inicio` | `DATE NOT NULL` | |

### `cuota` (CU25, CU28)

| Columna | Tipo | Notas |
| --- | --- | --- |
| `inscripcion_plan_id` | `BIGINT NOT NULL REFERENCES inscripcion_plan(id)` | |
| `nro` | `SMALLINT NOT NULL` | `UNIQUE (inscripcion_plan_id, nro)` |
| `monto` | `NUMERIC(10,2) NOT NULL` | |
| `fecha_vence` | `DATE NOT NULL` | |
| `estado` | `VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'` | `CHECK IN ('PENDIENTE','PAGADA','VENCIDA','ANULADA')` |

Índice `idx_cuota_estado_fecha_vence (estado, fecha_vence)` para el job de vencidas.

### `pago` (CU26 a CU29)

`idempotency_key` evita cobros duplicados por reintentos.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `estudiante_codigo` | `UUID NOT NULL` | → Identidad. Índice `idx_pago_estudiante_created (estudiante_codigo, created_at DESC)` |
| `concepto` | `VARCHAR(10) NOT NULL` | `CHECK IN ('CUOTA','CLASE','EXAMEN')` |
| `cuota_id` | `BIGINT REFERENCES cuota(id)` | Índice `idx_pago_cuota_id` |
| `reserva_codigo` | `UUID` | → `academico.reservas.codigoReserva`, sin FK. Índice `idx_pago_reserva_codigo` |
| `monto` | `NUMERIC(10,2) NOT NULL` | |
| `moneda` | `CHAR(3) NOT NULL DEFAULT 'BOB'` | |
| `metodo` | `VARCHAR(15) NOT NULL` | `CHECK IN ('QR','TRANSFERENCIA','EFECTIVO')` |
| `estado` | `VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'` | `CHECK IN ('PENDIENTE','CONFIRMADO','FALLIDO','REVERTIDO')` |
| `idempotency_key` | `VARCHAR(64) NOT NULL UNIQUE` | |
| `saga_codigo` | `UUID` | Saga reserva-pago (CU44) |
| `confirmado_en` | `TIMESTAMPTZ` | |

### `transaccion_pasarela` (CU26, CU27)

| Columna | Tipo | Notas |
| --- | --- | --- |
| `pago_id` | `BIGINT NOT NULL REFERENCES pago(id)` | Índice `idx_transaccion_pasarela_pago_id` |
| `proveedor` | `VARCHAR(30) NOT NULL` | |
| `referencia_externa` | `VARCHAR(80) UNIQUE` | |
| `qr_payload` | `TEXT` | |
| `estado` | `VARCHAR(12) NOT NULL` | |
| `respuesta` | `JSONB` | |

### `comprobante` (CU30)

Uno por pago.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `pago_id` | `BIGINT NOT NULL UNIQUE REFERENCES pago(id)` | |
| `nro_comprobante` | `VARCHAR(20) NOT NULL UNIQUE` | |
| `monto` | `NUMERIC(10,2) NOT NULL` | |
| `emitido_en` | `TIMESTAMPTZ NOT NULL` | |
| `url_pdf` | `TEXT` | |

### `reversion_pago` (CU31)

Compensación de la saga.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `pago_id` | `BIGINT NOT NULL REFERENCES pago(id)` | Índice `idx_reversion_pago_pago_id` |
| `motivo` | `VARCHAR(200)` | |
| `monto` | `NUMERIC(10,2)` | |
| `estado` | `VARCHAR(10) NOT NULL DEFAULT 'SOLICITADA'` | `CHECK IN ('SOLICITADA','EJECUTADA','FALLIDA')` |
| `saga_codigo` | `UUID` | |
| `ejecutado_en` | `TIMESTAMPTZ` | |

### `outbox_evento` (CU27, CU44)

Eventos pendientes de publicar a RabbitMQ: se escriben en la misma transacción que el cambio y un proceso aparte los publica.

| Columna | Tipo | Notas |
| --- | --- | --- |
| `tipo` | `VARCHAR(60) NOT NULL` | |
| `payload` | `JSONB NOT NULL` | |
| `publicado_en` | `TIMESTAMPTZ` | `NULL` hasta que RabbitMQ confirma. Índice parcial `idx_outbox_evento_pendientes (created_at) WHERE publicado_en IS NULL` |

---

## 5. Académico — MongoDB

Horarios, reservas, clases, exámenes y simulacros. Los índices se declaran en el código (`@Indexed`, `@CompoundIndex`; `auto-index-creation=true`). Las referencias a otras colecciones del mismo servicio usan el `_id` Long (`franjaId`, `reservaId`…); a otros servicios, el código público.

```mermaid
flowchart TD
    plantillas_horario -->|plantillaId| franjas
    franjas -->|reservaId| reservas
    solicitudes_clase -->|solicitudId| reservas
    reservas -->|reprogramadaDeId| reservas
    reservas -->|reservaId| clases
    clases -->|claseId| pruebas_clase
    simulacros_config -->|simulacroId| intentos_simulacro
    banco_preguntas -.->|copia en el intento| intentos_simulacro
    intentos_simulacro -->|origenId| resultados
    pruebas_clase -->|origenId| resultados
    examenes_conduccion -->|origenId| resultados
    contadores
    outbox_eventos
```

### `contadores`

Secuencias para el `_id` Long de cada colección (`{ _id: "reservas", secuencia: 42 }`), con `$inc` atómico.

### `plantillas_horario` (CU12)

Horario semanal base del instructor.

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoPlantillaHorario` | UUID | Único |
| `escuelaCodigo` | UUID | → Escuela `escuela.codigo_escuela`. Se toma del claim `escuela` del JWT al crear (es el dato, no el filtro por tenant) |
| `instructorCodigo` | UUID | → Identidad. Índice |
| `vehiculoCodigo` | UUID | → Escuela `vehiculo.codigo_vehiculo`. **Vacío** hasta que CU10 asigne autos a instructores |
| `nombre` | string | Hasta 60 caracteres. No es único |
| `diasSemana` | array de int | 1 (lunes) a 7 (domingo), sin repetir y ordenado. La ventana de horas es la misma para todos los días |
| `horaInicio`, `horaFin` | string | `HH:mm` |
| `duracionClaseMin` | int | Ej. 60 |
| `vigenteDesde` | date | |
| `vigenteHasta` | date | Opcional |
| `estado` | enum | `ACTIVA`, `PAUSADA`, `INACTIVA`. Pausar y reanudar son reversibles; desactivar (`INACTIVA`) es definitivo |

Un horario no se edita: se crea, se pausa o reanuda y se desactiva. Dos horarios **activos** del mismo instructor no pueden cruzarse (algún día en común, horas y vigencias que se solapan); los pausados no cuentan.

Los documentos creados antes del refactor (con `diaSemana` y `activa`) se convierten al arrancar el servicio (`HorariosMigracion`): `diasSemana = [diaSemana]`, `nombre` armado con el día y la ventana, y `estado` según `activa`.

### `franjas` (CU11, CU12, CU14, CU16)

Horario concreto reservable. Reservar es un `findOneAndUpdate` de `LIBRE` a `RESERVADA` (atómico).

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoFranja` | UUID | Único |
| `escuelaCodigo` | UUID | → Escuela `escuela.codigo_escuela`. Del claim `escuela` del JWT al crear |
| `plantillaId` | long | Opcional → `plantillas_horario._id` |
| `instructorCodigo`, `vehiculoCodigo` | UUID | Identidad / Escuela. `vehiculoCodigo` queda vacío hasta CU10 |
| `fecha` | date | |
| `horaInicio`, `horaFin` | string | `HH:mm` |
| `estado` | enum | `LIBRE`, `RESERVADA`, `BLOQUEADA` |
| `reservaId` | long | Opcional → `reservas._id` |

Índices únicos: `uq_franja_instructor_fecha_hora (instructorCodigo, fecha, horaInicio)` y `uq_franja_vehiculo_fecha_hora (vehiculoCodigo, fecha, horaInicio)`. Este último es **parcial** (`partialFilterExpression: { vehiculoCodigo: { $exists: true } }`): sin ese filtro, las franjas sin vehículo de dos instructores distintos en la misma fecha y hora chocarían entre sí. Si una base de desarrollo ya tenía el índice sin filtro, hay que borrarlo una vez (`db.franjas.dropIndex('uq_franja_vehiculo_fecha_hora')`) para que se cree el parcial.

Las franjas se generan desde el horario para 28 días contando hoy, y una tarea diaria completa las que falten. Al pausar o desactivar un horario sus franjas futuras `LIBRE`/`BLOQUEADA` se **borran físicamente** (son datos derivados; un borrado lógico seguiría ocupando la clave única del instructor).

### `solicitudes_clase` (CU13)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoSolicitudClase` | UUID | Único |
| `estudianteCodigo` | UUID | → Identidad. Índice |
| `categoria` | string | Sigla de categoría |
| `tipo` | enum | `PRACTICA`, `TEORICA` |
| `cantidadClases` | int | |
| `preferencias` | subdocumento | `{ dias: [1..7], turno: MANANA\|TARDE\|NOCHE }`. Opcional |
| `estado` | enum | `PENDIENTE`, `APROBADA`, `RECHAZADA`, `CANCELADA` |
| `motivoRechazo` | string | Opcional |
| `resueltaPorCodigo` | UUID | Opcional (admin/instructor) |
| `resueltaEn` | instant | |

### `reservas` (CU14, CU15, CU16)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoReserva` | UUID | Único |
| `estudianteCodigo`, `instructorCodigo`, `vehiculoCodigo` | UUID | Identidad / Escuela |
| `franjaId` | long | → `franjas._id` |
| `solicitudId` | long | Opcional → `solicitudes_clase._id` |
| `fecha`, `horaInicio`, `horaFin` | date, string, string | **Copia** de la franja, para consultar sin join |
| `estado` | enum | `PENDIENTE_PAGO`, `CONFIRMADA`, `CANCELADA`, `COMPLETADA`, `NO_ASISTIO` |
| `pagoCodigo` | UUID | Opcional → Pagos `pago.codigo_pago` |
| `sagaCodigo` | UUID | Saga reserva-pago (CU44) |
| `reprogramadaDeId` | long | Opcional → `reservas._id` de la original |
| `historialEstados` | array | `[{ estado, en, motivo }]` |
| `expiraPagoEn` | instant | Si no se paga a tiempo, la saga cancela y libera la franja |

Índices compuestos: `idx_reserva_estudiante_fecha`, `idx_reserva_instructor_fecha`, `idx_reserva_estado_expira (estado, expiraPagoEn)`.

### `clases` (CU17)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoClase` | UUID | Único |
| `reservaId` | long | → `reservas._id`. Único |
| `estudianteCodigo`, `instructorCodigo`, `vehiculoCodigo` | UUID | Índices en estudiante e instructor |
| `fecha` | date | |
| `inicioReal`, `finReal` | instant | Opcionales |
| `asistencia` | enum | `PRESENTE`, `TARDE`, `AUSENTE` |
| `tema`, `observaciones` | string | |
| `kmInicio`, `kmFin` | int | |
| `estado` | enum | `ABIERTA`, `CERRADA` |

### `pruebas_clase` (CU18)

Evaluación práctica durante la clase.

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoPruebaClase` | UUID | Único |
| `claseId` | long | → `clases._id`. Índice |
| `estudianteCodigo`, `instructorCodigo` | UUID | |
| `tipo` | enum | `MANEJO`, `ESTACIONAMIENTO`, `CIUDAD` |
| `criterios` | array | `[{ codigo (clave legible), descripcion, puntaje, maximo }]` |
| `puntajeTotal` | double | |
| `resultado` | enum | `APROBADO`, `REPROBADO` |
| `observaciones` | string | Opcional |
| `fecha` | date | |

### `examenes_conduccion` (CU22)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoExamenConduccion` | UUID | Único |
| `estudianteCodigo` | UUID | Índice |
| `categoria` | string | Sigla |
| `fechaProgramada` | instant | |
| `evaluadorCodigo`, `vehiculoCodigo` | UUID | |
| `intento` | int | 1, 2, 3… |
| `estado` | enum | `PROGRAMADO`, `REALIZADO`, `CANCELADO`, `NO_ASISTIO` |
| `puntaje` | double | Opcional |
| `resultado` | enum | `APROBADO`, `REPROBADO`. Opcional |
| `observaciones` | string | Opcional |

### `banco_preguntas` (CU19)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoPregunta` | UUID | Único |
| `escuelaCodigo` | UUID | Escuela (tenant) dueña de la pregunta, el claim `escuela` del JWT, sin relación física. Todo listado y toda consulta filtran por él (CU19) |
| `categoria`, `tema` | string | Índice compuesto `idx_pregunta_escuela_categoria_tema_activa (escuelaCodigo, categoria, tema, activa)`: CU20 arma el simulacro con preguntas activas por categoría y tema de la escuela |
| `tipo` | enum | `OPCION_UNICA`, `OPCION_MULTIPLE`, `VERDADERO_FALSO` |
| `enunciado` | string | |
| `imagenUrl` | string | Opcional |
| `opciones` | array | `[{ id (letra), texto, correcta }]`. El `id` (A, B, C…) lo asigna el servicio por orden. De 2 a 6 opciones (exactamente 2 en `VERDADERO_FALSO`); 1 correcta en `OPCION_UNICA` y `VERDADERO_FALSO`, 1 o más en `OPCION_MULTIPLE` |
| `explicacion` | string | Se muestra al corregir |
| `dificultad` | int | 1 a 3 |
| `activa` | boolean | Una pregunta no se borra: se desactiva y deja de entrar en los simulacros nuevos (CU19) |
| `creadaPorCodigo` | UUID | Persona de Identidad |

### `simulacros_config` (CU20)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoSimulacro` | UUID | Único |
| `nombre`, `categoria` | string | |
| `duracionMin`, `nroPreguntas` | int | |
| `puntajeAprobacion` | double | |
| `reglas` | array | `[{ tema, cantidad }]`: cuántas preguntas de cada tema por intento |
| `activo` | boolean | |

### `intentos_simulacro` (CU21)

Las preguntas se **copian** dentro del intento: editar el banco no altera intentos pasados.

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoIntentoSimulacro` | UUID | Único |
| `simulacroId` | long | → `simulacros_config._id` |
| `estudianteCodigo` | UUID | Índice compuesto `idx_intento_estudiante_iniciado (estudianteCodigo, iniciadoEn DESC)` |
| `estado` | enum | `EN_CURSO`, `FINALIZADO`, `EXPIRADO` |
| `iniciadoEn`, `finalizadoEn` | instant | `finalizadoEn` opcional |
| `preguntas` | array | `[{ preguntaId, enunciado, opciones, elegidas, correcta }]` |
| `aciertos` | int | |
| `puntaje` | double | |
| `aprobado` | boolean | |

### `resultados` (CU23)

Vista consolidada de notas: se escribe al cerrar cada evaluación y publica `examen.calificado`.

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoResultado` | UUID | Único |
| `estudianteCodigo` | UUID | Índice compuesto `idx_resultado_estudiante_fecha (estudianteCodigo, fecha DESC)` |
| `origen` | enum | `SIMULACRO`, `PRUEBA_CLASE`, `EXAMEN_CONDUCCION` |
| `origenId` | long | `_id` del documento de origen |
| `titulo` | string | |
| `puntaje` | double | |
| `aprobado` | boolean | |
| `fecha` | instant | |

### `outbox_eventos` (CU44)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigoOutboxEvento` | UUID | Único |
| `tipo` | string | Ej. `clase.reservada`, `examen.calificado` |
| `payload` | documento | |
| `publicadoEn` | instant | `null` hasta que RabbitMQ confirma. Índice `idx_outbox_pendientes (publicadoEn, createdAt)` |

---

## 6. Contenido — MongoDB

Solo **metadatos**: los PDF y videos viven en el bucket `drivehub-contenido` de MinIO. Los campos van en `snake_case` (Eloquent). Migraciones: `2026_10_18_120000_crear_colecciones_contenido.php` y `2026_10_24_120000_manuales_por_escuela.php` (CU32). `_id` Long con la colección `contadores`.

### `manuales` (CU32, CU34)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigo_manual` | UUID | Índice único `uq_manuales_codigo` |
| `escuela_codigo` | UUID | Escuela (tenant) dueña del manual, el claim `escuela` del JWT. Todo el listado y toda búsqueda filtran por él |
| `titulo` | string | |
| `descripcion` | string | Opcional |
| `categoria` | string | Sigla de categoría (Escuela) o `GENERAL` |
| `tema` | string | Opcional |
| `version` | int | Sube cada vez que se reemplaza el archivo |
| `archivo` | subdocumento | `{ url, mime, tamano_bytes, sha256, nombre }`. `url` es la dirección pública del PDF en el bucket (`{MINIO_PUBLIC_URL}/drivehub-contenido/escuelas/{escuela}/manuales/{uuid}.pdf`) |
| `estado` | enum | `BORRADOR`, `PUBLICADO`, `ARCHIVADO` |
| `etiquetas` | array de string | |
| `publicado_por_codigo` | UUID | Persona de Identidad |

Índices: `idx_manuales_estado_categoria (estado, categoria)`, `idx_manuales_escuela_estado_categoria (escuela_codigo, estado, categoria)` y de texto `idx_manuales_texto (titulo, descripcion, etiquetas)`. La búsqueda del listado usa expresión regular sin distinguir mayúsculas sobre título, descripción y etiquetas, no el índice de texto.

### `tutoriales` (CU33, CU35)

| Campo | Tipo | Notas |
| --- | --- | --- |
| `codigo_tutorial` | UUID | Índice único `uq_tutoriales_codigo` |
| `titulo` | string | |
| `descripcion` | string | Opcional |
| `autor_codigo` | UUID | Instructor (Identidad) |
| `categoria` | string | Sigla de categoría |
| `tipo` | enum | `VIDEO`, `PDF`, `ENLACE` |
| `recurso` | subdocumento | `{ url, duracion_seg }` |
| `audiencia` | array de enum | `ESTUDIANTE` y/o `INSTRUCTOR` |
| `estado` | enum | `BORRADOR`, `PUBLICADO`, `ARCHIVADO` |
| `etiquetas` | array de string | |

Índices: `idx_tutoriales_autor_estado (autor_codigo, estado)` y de texto `idx_tutoriales_texto`.

---

## 7. Notificaciones — DynamoDB

Todo se consulta por usuario y fecha: partición `usuarioCodigo` (la persona de Identidad) y orden `sk = '<fecha ISO>#<codigoNotificacion>'`. Facturación `PAY_PER_REQUEST`.

| Tabla | Clave de partición | Clave de orden | TTL | Índices |
| --- | --- | --- | --- | --- |
| `Notificaciones` | `usuarioCodigo` (S) | `sk` (S) | `ttl` — 180 días | GSI `NoLeidas`: `usuarioCodigo` + `noLeidaTs` (proyección `ALL`, **disperso**) |
| `PreferenciasNotificacion` | `usuarioCodigo` (S) | — | — | — |
| `DispositivosPush` | `usuarioCodigo` (S) | `codigoDispositivoPush` (S) | — | — |
| `EventosProcesados` | `eventoCodigo` (S) | — | `ttl` — 7 días | — |

### `Notificaciones` (CU36, CU38)

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `usuarioCodigo`, `sk` | S | Clave |
| `codigoNotificacion` | S | UUID v7 |
| `tipo` | S (enum) | `RESERVA`, `PAGO`, `EXAMEN`, `RECORDATORIO` |
| `canal` | S (enum) | `EMAIL`, `PUSH`, `IN_APP` |
| `titulo`, `mensaje` | S | |
| `datos` | M | Contexto para abrir el detalle (ej. `{ reservaCodigo, fecha }`). Opcional |
| `estado` | S (enum) | `ENVIADA`, `FALLIDA` |
| `leida` | BOOL | |
| `noLeidaTs` | S | **Solo existe mientras no se lea**: por eso el GSI `NoLeidas` lista únicamente las no leídas |
| `creadaEn` | S | ISO |
| `ttl` | N | Epoch en segundos (180 días) |

### `PreferenciasNotificacion` (CU37)

Un ítem por usuario.

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `usuarioCodigo` | S | Clave |
| `codigoPreferenciasNotificacion` | S | UUID v7 |
| `canales` | M | `{ email, push, inApp }` (BOOL) |
| `tipos` | M | `{ RESERVA, PAGO, EXAMEN, RECORDATORIO }` (BOOL) |
| `silencioDesde`, `silencioHasta` | S | `HH:mm`. Opcionales |
| `email` | S | |
| `actualizadoEn` | S | |

### `DispositivosPush` (CU36)

Tokens de la app móvil.

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `usuarioCodigo`, `codigoDispositivoPush` | S | Clave |
| `tokenFcm` | S | |
| `plataforma` | S (enum) | `ANDROID`, `IOS` |
| `activo` | BOOL | |
| `ultimoUso` | S | |

### `EventosProcesados` (CU36)

Idempotencia de los consumidores de RabbitMQ: el broker puede entregar el mismo evento dos veces.

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `eventoCodigo` | S | Clave |
| `procesadoEn` | S | |
| `ttl` | N | Epoch en segundos (7 días) |

---

## 8. Asistente IA — DynamoDB

Las imágenes y audios no se guardan en las tablas: va la **clave del archivo** en el bucket `drivehub-asistente-ia`. Facturación `PAY_PER_REQUEST`.

| Tabla | Clave de partición | Clave de orden | Notas |
| --- | --- | --- | --- |
| `SesionesIA` | `usuarioCodigo` (S) | `sk` (S) = `<fecha ISO>#<codigoSesionIa>` | |
| `MensajesIA` | `sesionCodigo` (S) | `seq` (N) | Aparte de `SesionesIA` para no pasar el límite de 400 KB por ítem |
| `AyudasTablero` | `usuarioCodigo` (S) | `sk` (S) = `<fecha ISO>#<codigoAyudaTablero>` | |
| `AgentesIA` | `codigoAgenteIa` (S) | — | |

### `SesionesIA` (CU39)

Pruebas de conducción asistidas y chats.

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `usuarioCodigo`, `sk` | S | Clave |
| `codigoSesionIa` | S | UUID v7 |
| `tipo` | S (enum) | `PRUEBA_CONDUCCION`, `CHAT` |
| `categoria`, `escenario` | S | Opcionales |
| `estado` | S (enum) | `EN_CURSO`, `FINALIZADA` |
| `puntaje` | N | Opcional |
| `retroalimentacion` | S | Opcional |
| `modelo` | S | Modelo de IA usado |
| `duracionSeg` | N | Opcional |
| `creadaEn` | S | |

### `MensajesIA` (CU39)

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `sesionCodigo`, `seq` | S, N | Clave. `sesionCodigo` = `codigoSesionIa` |
| `codigoMensajeIa` | S | UUID v7 |
| `rol` | S (enum) | `USER`, `ASSISTANT` |
| `contenido` | S | |
| `mediaKey` | S | Clave de un archivo en el bucket. Opcional |
| `ts` | S | |

### `AyudasTablero` (CU40, CU42)

Ayuda por foto del tablero.

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `usuarioCodigo`, `sk` | S | Clave |
| `codigoAyudaTablero` | S | UUID v7 |
| `imagenKey` | S | Clave del archivo en el bucket |
| `indicadores` | L de M | `[{ codigo (clave legible), nombre, confianza, severidad }]`. Los valores de `severidad` se fijan con CU40 |
| `respuesta` | S | Explicación para el estudiante |
| `modelo` | S | |
| `latenciaMs` | N | |
| `util` | BOOL | Opinión del estudiante. Opcional |
| `creadaEn` | S | |

### `AgentesIA` (CU43)

Agentes y flujos de n8n que expone DriveHub (n8n guarda su propio estado interno).

| Atributo | Tipo | Notas |
| --- | --- | --- |
| `codigoAgenteIa` | S | Clave (UUID v7) |
| `nombre`, `descripcion` | S | |
| `workflowN8n` | S | Id del flujo en n8n |
| `webhookUrl` | S | |
| `parametros` | M | Opcional |
| `activo` | BOOL | |
| `actualizadoPorCodigo` | S | Administrador (Identidad) que lo modificó por última vez |

---

## 9. Referencias entre servicios

Todas por **código público**, sin FK física.

| Desde | Campo | Hacia | Dueño del dato |
| --- | --- | --- | --- |
| `identidad.usuario` | `escuela_codigo` | `escuela.escuela.codigo_escuela` | Escuela |
| `escuela.instructor` / `asignacion_alumno` | `usuario_codigo` / `estudiante_codigo` | `identidad.usuario.codigo_usuario` | Identidad |
| `pagos.plan_pago` | `categoria_sigla` | `escuela.categoria_licencia.sigla` | Escuela |
| `pagos.inscripcion_plan` / `pago` | `estudiante_codigo` | `identidad.usuario.codigo_usuario` | Identidad |
| `pagos.pago` | `reserva_codigo` | `academico.reservas.codigoReserva` | Académico |
| `academico.reservas` | `pagoCodigo` | `pagos.pago.codigo_pago` | Pagos |
| `academico.*` | `estudianteCodigo`, `instructorCodigo`, `evaluadorCodigo`, `creadaPorCodigo`, `resueltaPorCodigo` | `identidad.usuario.codigo_usuario` | Identidad |
| `academico.*` | `vehiculoCodigo` | `escuela.vehiculo.codigo_vehiculo` | Escuela |
| `academico.*` | `categoria` | `escuela.categoria_licencia.sigla` | Escuela |
| `contenido.manuales` / `tutoriales` | `publicado_por_codigo`, `autor_codigo` | `identidad.usuario.codigo_usuario` | Identidad |
| `contenido.*` | `categoria` | `escuela.categoria_licencia.sigla` | Escuela |
| `notificaciones.*`, `asistente-ia.*` | `usuarioCodigo` | `identidad.usuario.codigo_usuario` | Identidad |

---

## 10. Diferencias con el PDF y pendientes

### Cambios respecto al PDF (ya implementados)

| Cambio | Dónde | Por qué |
| --- | --- | --- |
| Roles: `ADMIN` pasó a `ADMIN_ESCUELA` y se agregó `SUPER_ADMIN` | `identidad.rol` | Los 4 roles reales del SaaS |
| Tabla nueva `escuela` | `escuela` | La escuela es el tenant de la plataforma |
| `fecha_nacimiento`, `escuela_codigo` | `identidad.usuario` | Edad mínima del registro (CU01) y escuela de la cuenta |
| `bloqueado_hasta` | `identidad.usuario` | Bloqueo temporal de 15 minutos (CU02) |
| `correo_verificado`; `token_recuperacion.tipo` | `identidad.usuario`, `identidad.token_recuperacion` | Verificar el correo (CU04b) y distinguir esos enlaces de los de recuperación |
| `token_recuperacion.correo_nuevo` y tipo `CAMBIO_CORREO` | `identidad.token_recuperacion` | Guardar la dirección nueva mientras se espera la confirmación del cambio de correo (CU06) |
| `permiso.clave` y `categoria_licencia.sigla` | `identidad`, `escuela` | Evitar chocar con el `codigo` UUID público |
| `created_at`/`updated_at`/`deleted_at` en vez de `creado_en`/`actualizado_en` | todas las tablas | Se unificó con `BaseModel` |
| Código público UUID v7 (no ULID) en DynamoDB | `notificaciones`, `asistente-ia` | Igual de ordenable por tiempo |

### Pendiente de diseñar

- **Filtro por tenant.** `vehiculo` (CU07), `manuales` (CU32) y `banco_preguntas` (CU19) ya tienen el código de la escuela, con el filtro explícito en su repositorio a partir del claim `escuela`; los estudiantes quedan aislados porque la cuenta lleva `escuela_codigo` (CU08a). Falta en las demás tablas de Escuela (`instructor`…), Pagos, el resto de Académico (`simulacros_config` en CU20, `intentos_simulacro`…), `tutoriales` de Contenido, Notificaciones y Asistente IA, y el mecanismo de filtrado automático por el claim `escuela` del JWT. El claim ya viaja en el token (CU02).
- **Estado de la saga reserva-pago:** dueño por decidir ([SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md)).
- **Valores de `severidad`** de los indicadores del tablero (se fijan con CU40).
- **Job de limpieza** de `refresh_token` vencidos.
