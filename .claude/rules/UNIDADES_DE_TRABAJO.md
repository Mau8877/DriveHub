---
globs: docs/UnidadesTrabajo/**/*
---

# Unidades de Trabajo (UT)

Esta regla define **cómo se descompone una [Historia de Usuario](HISTORIAS_DE_USUARIO.md)
ya escrita en tareas técnicas ejecutables** — el formato de carpetas, el
formato de cada archivo, y las reglas estrictas de alcance que evitan que
una UT mezcle capas, invente algo no pedido, o construya por adelantado
trabajo que pertenece a otra HU.

Una UT es la unidad mínima de trabajo que se le puede pedir a alguien (o a
una IA) para implementar **una tarea acotada y verificable**, con su
alcance y su fuera-de-alcance explícitos — la idea central es reducir
ambigüedad: cuantas más decisiones queden ya tomadas en la UT, menos
errores por interpretación al implementarla.

## Cuándo se crean

Recién cuando la HU correspondiente ya está escrita y sus Criterios de
Aceptación están completos (ver [HISTORIAS_DE_USUARIO.md](HISTORIAS_DE_USUARIO.md))
— una UT nunca se escribe para una HU que todavía no tiene sus CA
definidos, porque no hay contra qué verificarla.

## Estructura de carpetas

```
docs/UnidadesTrabajo/
└── HU{NN} - {Nombre corto}/        <- mismo nombre que el archivo de la HU, sin ".md"
    ├── UT-BACKEND/
    │   ├── README.md
    │   ├── UT-HU{NN}-BE-01.md
    │   ├── UT-HU{NN}-BE-02.md
    │   └── ...
    ├── UT-FRONTEND/
    │   ├── README.md
    │   ├── UT-HU{NN}-FE-01.md
    │   └── ...
    └── UT-MOBILE/                   <- solo si la HU también toca mobile
        ├── README.md
        └── UT-HU{NN}-MOB-01.md
```

- Una subcarpeta **por capa** (`UT-BACKEND`, `UT-FRONTEND`, `UT-MOBILE`),
  nunca mezcladas en una sola carpeta — cada capa tiene su propio
  `README.md` índice y su propia numeración independiente.
- Si una HU no toca una capa (ej. una HU 100% de backend, sin cambio de
  UI), esa subcarpeta directamente no se crea — no se deja una carpeta
  vacía "por si acaso".

## Nomenclatura de código

**`UT-HU{NN}-{CAPA}-{NN}`**, ej. `UT-HU01-BE-03`, `UT-HU02-FE-01`.

- `HU{NN}`: mismo número que la Historia de Usuario de origen (dos
  dígitos, `01`, `02`...).
- `{CAPA}`: `BE` (backend), `FE` (frontend), `MOB` (mobile).
- `{NN}`: secuencial dentro de esa HU+capa, empieza en `01`. No se reusa
  un número aunque una UT se elimine durante la planificación.

El código es único en todo el repositorio (no solo dentro de su carpeta)
— sirve para citarlo en un commit o un PR sin ambigüedad
(`feat[BACK]: UT-HU01-BE-07 -- AuthService.login()`).

## El `README.md` de cada capa

Es el índice y el "mapa de diseño" de esa capa para esa HU puntual —
secciones obligatorias:

```markdown
# Unidades de trabajo — HU{NN}-{CAPA}

**Historia:** {resumen de una línea de qué cubre esta HU en esta capa}
**Fuente HU:** [`ruta relativa a la HU`](../../HistoriasDeUsuario/HU{NN}%20-%20Nombre.md)
**Recurso HTTP / contrato:** {endpoint(s) o pantalla(s) que esta HU expone}

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](ruta relativa) — la(s) rule(s) de la capa
correspondiente. Estas fichas **no** sustituyen esas reglas: si hay
conflicto, ganan las rules del proyecto.

## Decisiones de diseño

{Cualquier decisión técnica que no estaba resuelta en la HU original y
que hubo que tomar para poder desglosarla en UT concretas — con su
razonamiento. Si la HU ya definía todo sin ambigüedad, esta sección se
omite.}

## Contrato de datos / recurso HTTP

{Tabla de campos, tabla de endpoints con método/path/status/`data`, según
corresponda a la capa.}

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU{NN}-{CAPA}-01](./UT-HU{NN}-{CAPA}-01.md) | {nombre} | — |
| ... | ... | ... |

**Cadena:** resumen tipo `01, 02 → 03 → 04` de qué bloquea a qué.

## Criterios de aceptación cubiertos

| CA (HU{NN}) | Descripción | UTs |
|-------------|-------------|-----|
| CA01 | ... | 01, 03 |

## Follow-ups (no se implementan en estas UT)

- {Trabajo real y previsible que queda pendiente, pero que no bloquea que
  esta HU se dé por terminada.}

## Qué NO incluyen estas UT

- {Alcance de otras HU, otras capas, u otro trabajo relacionado que
  alguien podría asumir por error que está acá.}
```

## El formato de cada archivo `UT-HU{NN}-{CAPA}-{NN}.md`

```markdown
# UT-HU{NN}-{CAPA}-{NN} — {Nombre de la tarea}

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU{NN}-{CAPA}-{NN}` |
| **Nombre** | {nombre completo} |
| **Historia** | HU{NN}-{CAPA} |
| **Depende de** | {otra(s) UT de esta misma HU, o "—" si no depende de ninguna} |

## Antes de implementar

{Qué archivos de `.claude/rules/` leer puntualmente para ESTA tarea (no
toda la lista del README, solo lo relevante acá).}

## Solicitud de negocio

{Una o dos frases: el porqué de esta tarea, en términos de negocio —
no repite el "qué" del Objetivo.}

## Objetivo

{Una frase concreta: qué queda construido al terminar esta UT.}

## Alcance

{Lista exacta y sin ambigüedad de qué se crea/edita: nombres de
archivos/clases/funciones, firmas de métodos cuando ayuda, snippets de
referencia cuando el "cómo" no es obvio. Termina siempre con una lista
"Archivos esperados (crear/tocar)".}

## Fuera de alcance (no tocar)

{Explícito y no genérico — nombra archivos/carpetas/funcionalidad
puntual que alguien podría asumir que esta UT también cubre, y aclara
que no. Ver la regla estricta de abajo.}

## Resultado esperado

{Cómo se ve el sistema una vez aplicada esta UT, en 1-3 frases.}

## Validación

1. {Paso concreto y verificable — un test, un comando, una acción manual
   con su resultado esperado.}
2. ...
```

## Reglas estrictas

### Una UT nunca construye trabajo de otra HU, aunque sea trivial agregarlo ahora

Esta es la regla más importante de este documento. Si mientras se
implementa la UT de una HU aparece la tentación de "ya que estoy, dejo
también hecho X" porque X lo va a necesitar una HU futura relacionada —
**no se hace**. Cada HU tiene que poder considerarse terminada, verificada
y entregada de forma independiente, sin que su alcance real dependa de
adivinar qué construyó de más una UT anterior.

Ejemplo del error concreto que esta regla existe para evitar (genérico,
no ligado a ningún módulo puntual): si "HU-A" necesita un servicio con un
método `emitir(...)`, y una HU futura "HU-B" (todavía sin sus propias UT)
va a necesitar `revocar(...)` sobre ese mismo servicio, la UT de HU-A
**construye solo `emitir(...)`**. `revocar(...)` se agrega recién en la
propia UT de HU-B, cuando esa HU se desglosa — aunque HU-B ya esté
identificada de antemano y aunque agregar `revocar(...)` ahora sea
técnicamente sencillo. La UT de HU-B simplemente **edita** el archivo que
ya existe (lo declara en su "Alcance" como archivo a tocar, no a crear) en
vez de encontrarlo ya completo.

**Por qué:** si una UT de HU-A ya resolvió una porción de HU-B, la HU-B
deja de ser verificable por sí sola (parte de su comportamiento depende de
una UT que aparece documentada bajo otra HU), y si HU-B cambia de diseño
antes de implementarse, hay que volver a tocar UT de HU-A para
deshacerlo — quedan las dos HU acopladas en el cronograma sin necesidad.

### Alcance y fuera-de-alcance son ambos obligatorios

Una UT sin sección "Fuera de alcance" es una UT incompleta — no alcanza
con que el "Alcance" sea preciso, porque alguien igual puede asumir que
"ya que está tocando ese archivo, aprovecha para..." si no se le dice
explícitamente que no.

### Granularidad: una UT, una responsabilidad cohesiva

Una UT típica crea o edita entre 1 y 4 archivos estrechamente
relacionados (una entidad + su migración, un service + su test, un
componente + su schema). Si una UT necesita tocar más de una capa interna
del proyecto a la vez (ej. persistencia **y** controller **y**
seguridad), probablemente son 2-3 UT distintas, no una — mismo criterio
de "controllers delgados"/separación de capas que ya aplica el resto del
proyecto (ver [ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md) y
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md)).

### Las dependencias solo cruzan UT de la misma HU (o apuntan a algo que ya existe)

El campo "Depende de" de una UT nombra otra UT de **esa misma HU**, o un
archivo/módulo que **ya existe en el proyecto** antes de que esta HU
empezara. Nunca "esto lo necesita HU-B" como dependencia de salida — esa
relación, si existe, se documenta en la HU misma (ver
[HISTORIAS_DE_USUARIO.md](HISTORIAS_DE_USUARIO.md)), no en una UT.

### Toda UT termina en algo verificable

La sección "Validación" no es opcional ni puede ser una sola línea vaga —
tiene que permitir confirmar, sin ambigüedad, que la UT quedó bien
implementada (un comando de test, una secuencia de pasos con su resultado
esperado, una query que confirma un estado en base de datos, etc.).
