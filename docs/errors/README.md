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
| `ACA` | Académico | [`academico/`](academico/ERRORES_ACADEMICO.md) |
| `CON` | Contenido | `contenido/` (sin errores todavía) |
| `NOT` | Notificaciones | `notificaciones/` (sin errores todavía) |
| `IA` | Asistente IA | `asistente-ia/` (sin errores todavía) |

## Índice de errores

### Sistema (`SYS`)

- [`ERR_SYS_00`](system/ERRORES_SISTEMA.md#err_sys_00) — Error interno no controlado (500)
- [`ERR_SYS_01`](system/ERRORES_SISTEMA.md#err_sys_01) — Validación de formato del request fallida (400)
- [`ERR_SYS_02`](system/ERRORES_SISTEMA.md#err_sys_02) — Límite de requests por IP superado (429, gateway)
- [`ERR_SYS_03`](system/ERRORES_SISTEMA.md#err_sys_03) — Ruta inexistente en el servicio (404)
- [`ERR_SYS_04`](system/ERRORES_SISTEMA.md#err_sys_04) — No autenticado: token ausente, inválido o vencido (401)
- [`ERR_SYS_05`](system/ERRORES_SISTEMA.md#err_sys_05) — Sin permiso para la acción (403)

### Identidad (`IDE`)

- [`ERR_IDE_01`](identidad/ERRORES_IDENTIDAD.md#err_ide_01) — El correo ya está registrado (409)
- [`ERR_IDE_02`](identidad/ERRORES_IDENTIDAD.md#err_ide_02) — No alcanza la edad mínima para registrarse (400)
- [`ERR_IDE_03`](identidad/ERRORES_IDENTIDAD.md#err_ide_03) — La escuela no existe o no está activa (404)
- [`ERR_IDE_04`](identidad/ERRORES_IDENTIDAD.md#err_ide_04) — Credenciales de inicio de sesión incorrectas (401)
- [`ERR_IDE_05`](identidad/ERRORES_IDENTIDAD.md#err_ide_05) — Cuenta bloqueada temporalmente (423)
- [`ERR_IDE_06`](identidad/ERRORES_IDENTIDAD.md#err_ide_06) — Cuenta inactiva (403)
- [`ERR_IDE_07`](identidad/ERRORES_IDENTIDAD.md#err_ide_07) — Escuela de la cuenta suspendida (403)
- [`ERR_IDE_08`](identidad/ERRORES_IDENTIDAD.md#err_ide_08) — El rol no puede usar esta plataforma (403)
- [`ERR_IDE_09`](identidad/ERRORES_IDENTIDAD.md#err_ide_09) — Sesión renovable inválida, vencida o ya usada (401)
- [`ERR_IDE_10`](identidad/ERRORES_IDENTIDAD.md#err_ide_10) — El correo de la cuenta ya está verificado (409)
- [`ERR_IDE_11`](identidad/ERRORES_IDENTIDAD.md#err_ide_11) — Enlace de verificación, recuperación o cambio de correo inválido, vencido o ya usado (400)
- [`ERR_IDE_12`](identidad/ERRORES_IDENTIDAD.md#err_ide_12) — La escuela ya tiene un Administrador (409)
- [`ERR_IDE_13`](identidad/ERRORES_IDENTIDAD.md#err_ide_13) — El Administrador ya inició sesión y no se puede revertir (409)
- [`ERR_IDE_14`](identidad/ERRORES_IDENTIDAD.md#err_ide_14) — La contraseña actual no es correcta (400)
- [`ERR_IDE_15`](identidad/ERRORES_IDENTIDAD.md#err_ide_15) — La contraseña nueva es igual a la actual (400)
- [`ERR_IDE_16`](identidad/ERRORES_IDENTIDAD.md#err_ide_16) — La foto de perfil no es una imagen válida (400)
- [`ERR_IDE_17`](identidad/ERRORES_IDENTIDAD.md#err_ide_17) — La foto de perfil pesa más de 5 MB (400)
- [`ERR_IDE_18`](identidad/ERRORES_IDENTIDAD.md#err_ide_18) — El correo nuevo es igual al actual (409)
- [`ERR_IDE_19`](identidad/ERRORES_IDENTIDAD.md#err_ide_19) — El CI ya está registrado (409)

### Escuela (`ESC`)

- [`ERR_ESC_01`](escuela/ERRORES_ESCUELA.md#err_esc_01) — La escuela no existe o está inactiva (404)
- [`ERR_ESC_02`](escuela/ERRORES_ESCUELA.md#err_esc_02) — No existe la escuela con ese código (404)
- [`ERR_ESC_03`](escuela/ERRORES_ESCUELA.md#err_esc_03) — Ya existe una escuela con ese slug (409)
- [`ERR_ESC_04`](escuela/ERRORES_ESCUELA.md#err_esc_04) — La escuela ya estaba inactiva (409)
- [`ERR_ESC_05`](escuela/ERRORES_ESCUELA.md#err_esc_05) — La escuela ya estaba activa (409)
- [`ERR_ESC_06`](escuela/ERRORES_ESCUELA.md#err_esc_06) — La escuela está inactiva y no se puede editar (409)

### Académico (`ACA`)

- [`ERR_ACA_01`](academico/ERRORES_ACADEMICO.md#err_aca_01) — La hora de fin no es posterior a la de inicio (400)
- [`ERR_ACA_02`](academico/ERRORES_ACADEMICO.md#err_aca_02) — Duración de la clase inválida o tramo que no es múltiplo (400)
- [`ERR_ACA_03`](academico/ERRORES_ACADEMICO.md#err_aca_03) — Vigencia del horario inválida (400)
- [`ERR_ACA_04`](academico/ERRORES_ACADEMICO.md#err_aca_04) — El horario se cruza con otro horario activo del instructor (409)
- [`ERR_ACA_05`](academico/ERRORES_ACADEMICO.md#err_aca_05) — El horario no existe o no es del instructor (404)
- [`ERR_ACA_06`](academico/ERRORES_ACADEMICO.md#err_aca_06) — El horario ya estaba inactivo y no se puede desactivar de nuevo (409)
- [`ERR_ACA_07`](academico/ERRORES_ACADEMICO.md#err_aca_07) — La franja no existe o no es del instructor (404)
- [`ERR_ACA_08`](academico/ERRORES_ACADEMICO.md#err_aca_08) — La franja no está libre y no se puede bloquear (409)
- [`ERR_ACA_09`](academico/ERRORES_ACADEMICO.md#err_aca_09) — La franja no está bloqueada y no se puede desbloquear (409)
- [`ERR_ACA_10`](academico/ERRORES_ACADEMICO.md#err_aca_10) — La franja o el día ya empezó o ya pasó (409)
- [`ERR_ACA_11`](academico/ERRORES_ACADEMICO.md#err_aca_11) — El horario no está activo y no se puede pausar (409)
- [`ERR_ACA_12`](academico/ERRORES_ACADEMICO.md#err_aca_12) — El horario no está pausado y no se puede reanudar (409)
- [`ERR_ACA_13`](academico/ERRORES_ACADEMICO.md#err_aca_13) — Rango de fechas de franjas inválido (400)

<!--
  A medida que cada servicio lance su primer error de negocio, suma acá su
  sección "### Nombre (PREFIJO)" con la lista de sus códigos, igual que la
  de Sistema — ver GUIA_ERRORES_BACKEND.md.
-->
