---
globs: projects/backend/**/*
---

# Guía de errores — Backend

Complementa a [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md) (que define
la jerarquía de excepciones y el **formato** del código, `ERR_{MODULO}_{NUMERO}`)
con el **proceso obligatorio** para cuando se crea un código de error
nuevo: dónde se documenta, con qué formato, y por qué.

## Estructura de `docs/errors/`

Una carpeta por feature/módulo, y dentro un único archivo
`ERRORES_{MODULO}.md` con **todos** los códigos de ese módulo, cada uno en
su propia sección (`### \`ERR_XXX_NN\``) para poder enlazarlo directo por
ancla:

```
docs/errors/
├── README.md                  <- índice: un link por código, agrupado por módulo
├── system/
│   └── ERRORES_SISTEMA.md
├── usuarios/
│   └── ERRORES_USUARIOS.md
└── pedidos/
    └── ERRORES_PEDIDOS.md
```

- Nombre de la carpeta: el mismo nombre del feature tal como aparece en
  `features/{feature}/` (`usuarios`, `pedidos`...), en minúscula.
  Para los códigos transversales de `SYS` (ver
  [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)),
  la carpeta es `system/` (no hay un `features/system/` real, es la
  excepción reservada).
- La carpeta de un módulo **no se crea de antemano** — se crea recién
  cuando ese módulo lanza su primer error real, mismo criterio de "no
  anticipar estructura vacía" que ya usa el proyecto para
  `shared/`/`utils/` en el frontend.
- Un módulo, un archivo (`ERRORES_{MODULO}.md`) — no un archivo por código
  ni todos los módulos mezclados en un único archivo gigante.

## Regla estricta: todo código nuevo se documenta antes de mergear

Cuando se lanza una excepción de negocio con un `errorCode` que todavía no
existía (una constante nueva en el `XxxErrorCodes` del módulo), en el
**mismo cambio** se hacen las dos cosas siguientes — ninguna es opcional
ni "para después":

1. **Agregar la sección del error** en
   `docs/errors/{modulo}/ERRORES_{MODULO}.md` (crear la carpeta y el
   archivo si es el primer error de ese módulo), con este formato exacto:

   ```markdown
   ## `ERR_{MODULO}_{NUMERO}`

   - **HTTP:** {código, ej. 404}
   - **Excepción:** {clase que lo lanza, ej. `NotFoundException`}
   - **Significado:** {frase corta y concreta de CUÁNDO pasa — la causa
     real de negocio, no el texto del mensaje que ve el usuario}
   - **Dónde se lanza:** {Clase.método(), ej. `PedidoService.aprobar()`}
   ```

2. **Agregar el link** a esa sección nueva en
   [`docs/errors/README.md`](../../../docs/errors/README.md), dentro de
   la sección del módulo que corresponda (se crea la sección `### Nombre
   (PREFIJO)` si es el primer código de ese módulo en el índice) —
   apuntando al ancla que generó el heading del paso 1
   (`{modulo}/ERRORES_{MODULO}.md#err_{modulo}_{numero}`, todo en
   minúscula).

Un código que existe en el `XxxErrorCodes.java` pero no en
`docs/errors/` (o que está en `docs/errors/` pero sin su link desde el
índice del `README.md`) es, a todo efecto práctico, un código sin
documentar — no cumple con esta regla.

### Por qué un catálogo aparte, si el código ya vive en `XxxErrorCodes.java`

La constante Java le sirve al código (el `service` que la lanza). La
documentación en `docs/errors/` le sirve a las **personas** que no van a
andar grepeando el código fuente para entender qué significa `ERR_PED_04`
cuando lo ven en un log de producción o en la respuesta de un endpoint:
soporte, quien está armando el frontend/mobile contra un endpoint que
todavía no conoce a fondo, o el mismo equipo de backend seis meses
después. El índice del `README.md` existe para que, dado un código
suelto (ej. visto en un log), alguien pueda llegar en un clic a su
explicación exacta sin tener que abrir varios archivos a adivinar.

### Qué se documenta de cada código

Las cuatro líneas del formato de arriba son obligatorias:

| Campo | Contenido |
| --- | --- |
| HTTP | El status code que devuelve (lo determina qué excepción lo lanza — ver la tabla de [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)) |
| Excepción | La clase que lo lanza (`NotFoundException`, `ConflictException`, etc., o "ninguna" si es un caso especial como el rate limit) |
| Significado | Una frase corta y concreta de cuándo pasa — no el mensaje textual que ve el usuario, sino la causa real |
| Dónde se lanza | Clase y método donde vive el `throw` — para no tener que buscarlo a mano |

No se documentan los `400` genéricos que dispara Bean Validation
automático (`@NotBlank`, `@Email`, etc. sobre un DTO) — esos no pasan por
un `errorCode` propio, caen en `ERR_SYS_01` (ver
[EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md#manejo-global)), que ya
tiene su propia sección única en `system/ERRORES_SISTEMA.md`.

### Cuándo se actualiza una sección existente

Si cambia el significado real de un código ya documentado (no el texto
del mensaje al usuario, sino la condición de negocio que lo dispara), se
actualiza esa misma sección en el mismo cambio que modifica el `service`.
No se agrega una sección nueva para lo mismo, y no se deja la sección
vieja describiendo un comportamiento que ya no existe.

### Cuándo se borra una sección

Nunca. Igual que un `NUMERO` de código no se reutiliza aunque el error que
lo usaba desaparezca (ver
[EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)),
la sección tampoco se borra — se marca como **retirada** en el campo
**Significado** (ej. "Retirado: el módulo de pedidos ya no soporta este
estado desde 2026-XX") y su link se deja en el índice del `README.md`.
Alguien que encuentre ese código viejo en un log histórico necesita poder
buscarlo y encontrar qué significaba, aunque ya no se produzca más.
