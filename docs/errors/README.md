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
| `PAG` | Pagos | [`pagos/`](pagos/ERRORES_PAGOS.md) |
| `ACA` | Académico | [`academico/`](academico/ERRORES_ACADEMICO.md) |
| `CON` | Contenido | [`contenido/`](contenido/ERRORES_CONTENIDO.md) |
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
- [`ERR_IDE_20`](identidad/ERRORES_IDENTIDAD.md#err_ide_20) — El estudiante no existe en la escuela (404)
- [`ERR_IDE_21`](identidad/ERRORES_IDENTIDAD.md#err_ide_21) — La categoría de licencia no existe (400)
- [`ERR_IDE_22`](identidad/ERRORES_IDENTIDAD.md#err_ide_22) — El instructor no existe en la escuela (404)
- [`ERR_IDE_23`](identidad/ERRORES_IDENTIDAD.md#err_ide_23) — La cuenta del instructor ya se usó y no se revierte (409)

### Escuela (`ESC`)

- [`ERR_ESC_01`](escuela/ERRORES_ESCUELA.md#err_esc_01) — La escuela no existe o está inactiva (404)
- [`ERR_ESC_02`](escuela/ERRORES_ESCUELA.md#err_esc_02) — No existe la escuela con ese código (404)
- [`ERR_ESC_03`](escuela/ERRORES_ESCUELA.md#err_esc_03) — Ya existe una escuela con ese slug (409)
- [`ERR_ESC_04`](escuela/ERRORES_ESCUELA.md#err_esc_04) — La escuela ya estaba inactiva (409)
- [`ERR_ESC_05`](escuela/ERRORES_ESCUELA.md#err_esc_05) — La escuela ya estaba activa (409)
- [`ERR_ESC_06`](escuela/ERRORES_ESCUELA.md#err_esc_06) — La escuela está inactiva y no se puede editar (409)
- [`ERR_ESC_07`](escuela/ERRORES_ESCUELA.md#err_esc_07) — La categoría de licencia no existe (404)
- [`ERR_ESC_08`](escuela/ERRORES_ESCUELA.md#err_esc_08) — Ya existe un auto con esa placa (409)
- [`ERR_ESC_09`](escuela/ERRORES_ESCUELA.md#err_esc_09) — El auto no existe en la escuela (404)
- [`ERR_ESC_10`](escuela/ERRORES_ESCUELA.md#err_esc_10) — Cambio no permitido desde el estado actual del auto (409)
- [`ERR_ESC_11`](escuela/ERRORES_ESCUELA.md#err_esc_11) — El token no trae la escuela de la cuenta (403)
- [`ERR_ESC_12`](escuela/ERRORES_ESCUELA.md#err_esc_12) — El número de licencia ya está en uso (409)
- [`ERR_ESC_13`](escuela/ERRORES_ESCUELA.md#err_esc_13) — El instructor no existe en la escuela (404)
- [`ERR_ESC_14`](escuela/ERRORES_ESCUELA.md#err_esc_14) — Una categoría del instructor no es válida (400)
- [`ERR_ESC_15`](escuela/ERRORES_ESCUELA.md#err_esc_15) — El estudiante no es alumno de este instructor (404)

### Pagos (`PAG`)

- [`ERR_PAG_01`](pagos/ERRORES_PAGOS.md#err_pag_01) — Ya existe un plan con ese nombre en la escuela (409)
- [`ERR_PAG_02`](pagos/ERRORES_PAGOS.md#err_pag_02) — El plan no existe en la escuela (404)
- [`ERR_PAG_03`](pagos/ERRORES_PAGOS.md#err_pag_03) — El plan ya estaba en ese estado (409)
- [`ERR_PAG_04`](pagos/ERRORES_PAGOS.md#err_pag_04) — El token no trae la escuela de la cuenta (403)
- [`ERR_PAG_05`](pagos/ERRORES_PAGOS.md#err_pag_05) — El plan no está disponible para comprar (404)
- [`ERR_PAG_06`](pagos/ERRORES_PAGOS.md#err_pag_06) — La pasarela de pago no está disponible (503)
- [`ERR_PAG_07`](pagos/ERRORES_PAGOS.md#err_pag_07) — El pago no existe o es de otra persona (404)
- [`ERR_PAG_08`](pagos/ERRORES_PAGOS.md#err_pag_08) — El aviso de la pasarela no se pudo verificar (400)
- [`ERR_PAG_09`](pagos/ERRORES_PAGOS.md#err_pag_09) — No hay horas suficientes en la billetera (409)
- [`ERR_PAG_10`](pagos/ERRORES_PAGOS.md#err_pag_10) — La compra no se puede reembolsar: sin confirmar o con una devolución vigente (409)
- [`ERR_PAG_11`](pagos/ERRORES_PAGOS.md#err_pag_11) — Ya se usó al menos una hora de la compra: no hay reembolso parcial (409)
- [`ERR_PAG_12`](pagos/ERRORES_PAGOS.md#err_pag_12) — El estudiante no es alumno de este instructor (404)
- [`ERR_PAG_13`](pagos/ERRORES_PAGOS.md#err_pag_13) — El comprobante no existe o no es del que lo pide (404)
- [`ERR_PAG_14`](pagos/ERRORES_PAGOS.md#err_pag_14) — El PDF del comprobante todavía se está preparando (409)
- [`ERR_PAG_15`](pagos/ERRORES_PAGOS.md#err_pag_15) — Un servicio del que depende la operación no está disponible (503)
- [`ERR_PAG_16`](pagos/ERRORES_PAGOS.md#err_pag_16) — No hay horas gastadas en esa clase para devolver (404)
- [`ERR_PAG_17`](pagos/ERRORES_PAGOS.md#err_pag_17) — La compra con esa clave todavía se está procesando (409)
- [`ERR_PAG_18`](pagos/ERRORES_PAGOS.md#err_pag_18) — La clave de compra ya se usó para otra compra (409)

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
- [`ERR_ACA_14`](academico/ERRORES_ACADEMICO.md#err_aca_14) — La pregunta no existe en la escuela (404)
- [`ERR_ACA_15`](academico/ERRORES_ACADEMICO.md#err_aca_15) — La pregunta ya estaba en ese estado (409)
- [`ERR_ACA_16`](academico/ERRORES_ACADEMICO.md#err_aca_16) — El token no trae la escuela de la cuenta (403)

### Contenido (`CON`)

- [`ERR_CON_01`](contenido/ERRORES_CONTENIDO.md#err_con_01) — El manual no existe en la escuela (404)
- [`ERR_CON_02`](contenido/ERRORES_CONTENIDO.md#err_con_02) — Cambio no permitido desde el estado actual del manual (409)
- [`ERR_CON_03`](contenido/ERRORES_CONTENIDO.md#err_con_03) — El archivo del manual no es válido (400)
- [`ERR_CON_04`](contenido/ERRORES_CONTENIDO.md#err_con_04) — El token no trae la escuela de la cuenta (403)

<!--
  A medida que cada servicio lance su primer error de negocio, suma acá su
  sección "### Nombre (PREFIJO)" con la lista de sus códigos, igual que la
  de Sistema — ver GUIA_ERRORES_BACKEND.md.
-->
