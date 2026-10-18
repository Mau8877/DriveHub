# `docs/errors/` — Catálogo de códigos de error

Esta carpeta documenta, para humanos (soporte, quien consume la API desde
el frontend/mobile, o el propio equipo de backend meses después), qué
significa cada código de error (`ERR_{MODULO}_{NUMERO}`) que la API puede
devolver. El código en sí (la constante Java, `XxxErrorCodes`) le sirve al
`service` que lo lanza; esto de acá le sirve a la persona que lo recibe y
no quiere ponerse a leer código fuente para entender qué pasó.

El **proceso** de cuándo y cómo se documenta un código nuevo (obligatorio
en el mismo cambio que lo introduce) vive en
[GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
Este `README.md` es solo el índice — no repite esas reglas.

## Cómo está organizada

Una carpeta por feature/módulo (`system/`, y a futuro `usuarios/`,
`pedidos/`, etc. — se crean recién cuando ese módulo tiene su primer
error real, no antes), y dentro un único archivo `ERRORES_{MODULO}.md` con
todos los códigos de ese módulo, cada uno en su propia sección para poder
enlazarlo directo:

```
docs/errors/
├── README.md                          <- este archivo, el índice
├── system/
│   └── ERRORES_SISTEMA.md
├── usuarios/
│   └── ERRORES_USUARIOS.md            (cuando exista el primer error de usuarios)
└── pedidos/
    └── ERRORES_PEDIDOS.md             (cuando exista el primer error de pedidos)
```

## Índice de errores

### Sistema (`SYS`)

- [`ERR_SYS_00`](system/ERRORES_SISTEMA.md#err_sys_00) — Error interno no controlado (500)
- [`ERR_SYS_01`](system/ERRORES_SISTEMA.md#err_sys_01) — Validación de Bean Validation fallida (400)
- [`ERR_SYS_02`](system/ERRORES_SISTEMA.md#err_sys_02) — Límite de requests por IP superado (429)

<!--
  A medida que se agregan módulos de negocio (usuarios, pedidos...),
  cada uno suma acá su propia sección "### Nombre (PREFIJO)" con la lista
  de sus códigos, igual que la sección de Sistema de arriba — ver
  GUIA_ERRORES_BACKEND.md para el detalle exacto de qué hacer al agregar
  un código nuevo.
-->
