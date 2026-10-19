# `docs/errors/` — Catálogo de códigos de error

Esta carpeta documenta, para humanos (soporte, quien consume la API desde
la web o el móvil, o el propio equipo de backend meses después), qué
significa cada código de error (`ERR_{MODULO}_{NUMERO}`) que la API puede
devolver. La constante en el código le sirve al servicio que la lanza;
esto de acá le sirve a la persona que la recibe y no quiere leer código
fuente para entender qué pasó.

El **proceso** de cuándo y cómo se documenta un código nuevo (obligatorio
en el mismo cambio que lo introduce) vive en
[GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
Este `README.md` es solo el índice.

## Cómo está organizada

Una carpeta por **microservicio** (se crea recién cuando ese servicio
tiene su primer error real) y dentro un único `ERRORES_{SERVICIO}.md`. El
prefijo del código dice qué servicio lo produjo:

| Prefijo | Servicio | Carpeta |
| --- | --- | --- |
| `SYS` | Todos (transversales) | [`system/`](system/ERRORES_SISTEMA.md) |
| `IDE` | Identidad | [`identidad/`](identidad/ERRORES_IDENTIDAD.md) |
| `ESC` | Escuela | [`escuela/`](escuela/ERRORES_ESCUELA.md) |
| `PAG` | Pagos | `pagos/` (sin errores todavía) |
| `ACA` | Académico | `academico/` (sin errores todavía) |
| `CON` | Contenido | `contenido/` (sin errores todavía) |
| `NOT` | Notificaciones | `notificaciones/` (sin errores todavía) |
| `IA` | Asistente IA | `asistente-ia/` (sin errores todavía) |

## Índice de errores

### Sistema (`SYS`)

- [`ERR_SYS_00`](system/ERRORES_SISTEMA.md#err_sys_00) — Error interno no controlado (500)
- [`ERR_SYS_01`](system/ERRORES_SISTEMA.md#err_sys_01) — Validación de formato del request fallida (400)
- [`ERR_SYS_02`](system/ERRORES_SISTEMA.md#err_sys_02) — Límite de requests por IP superado (429, gateway)
- [`ERR_SYS_03`](system/ERRORES_SISTEMA.md#err_sys_03) — Ruta inexistente en el servicio (404)

### Identidad (`IDE`)

- [`ERR_IDE_01`](identidad/ERRORES_IDENTIDAD.md#err_ide_01) — El correo ya está registrado (409)
- [`ERR_IDE_02`](identidad/ERRORES_IDENTIDAD.md#err_ide_02) — No alcanza la edad mínima para registrarse (400)
- [`ERR_IDE_03`](identidad/ERRORES_IDENTIDAD.md#err_ide_03) — La escuela no existe o no está activa (404)

### Escuela (`ESC`)

- [`ERR_ESC_01`](escuela/ERRORES_ESCUELA.md#err_esc_01) — La escuela no existe o está inactiva (404)

<!--
  A medida que cada servicio lance su primer error de negocio, suma acá su
  sección "### Nombre (PREFIJO)" con la lista de sus códigos, igual que la
  de Sistema — ver GUIA_ERRORES_BACKEND.md.
-->
