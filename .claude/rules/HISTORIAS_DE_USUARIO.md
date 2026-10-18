---
globs: docs/HistoriasDeUsuario/**/*
---

# Historias de Usuario

Esta regla define **cómo se escribe una Historia de Usuario (HU)** en este
proyecto — el formato, las secciones obligatorias, y el criterio de
granularidad. Se aplica siempre que se cree o edite un archivo dentro de
`docs/HistoriasDeUsuario/`.

Una HU documenta **un flujo de usuario coherente y acotado** — no es un
documento de diseño técnico (eso lo cubren las
[Unidades de Trabajo](UNIDADES_DE_TRABAJO.md), que se crean después y a
partir de la HU ya escrita).

## Dónde vive y cómo se nombra

`docs/HistoriasDeUsuario/HU{NN} - {Nombre corto}.md` — numeración
secuencial de dos dígitos (`HU01`, `HU02`, ..., `HU10`, `HU11`...), sin
reusar un número aunque una HU se descarte (mismo criterio que los
códigos de error de
[EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md): un número usado
una vez no se reasigna a otra cosa). El nombre corto del archivo coincide
con el campo "Nombre corto de HU" de la tabla interna (ver abajo).

## Granularidad: una HU, un flujo

**Si una funcionalidad agrupa más de un flujo de usuario independiente,
se separa en HU distintas** — no se mete todo en una sola HU "grande"
porque conceptualmente pertenecen al mismo dominio. Señal de que hay que
separar: cada flujo tiene su propio disparador (una acción de usuario
distinta), su propio conjunto de criterios de aceptación que no dependen
de que el otro flujo se esté ejecutando en ese momento, y podría
entregarse/probarse por separado sin que el otro exista todavía.

Ejemplo del criterio (genérico, no ligado a ningún módulo puntual): una
funcionalidad de "gestión de sesión" no es una sola HU — "iniciar sesión",
"renovar la sesión automáticamente" y "cerrar sesión" son tres disparadores
de usuario distintos, con criterios de aceptación independientes entre sí
(se puede implementar y probar el login sin que el refresh automático
exista todavía) → son **tres HU separadas**, no una. Cuando una HU
depende del resultado de otra (ej. "renovar sesión" no tiene sentido sin
que "iniciar sesión" ya exista), esa relación se documenta con un link
cruzado en la descripción o en las reglas de negocio (ver plantilla
abajo), no fusionando ambas HU en una sola.

## Formato del archivo (plantilla completa)

```markdown
# Historia de usuario

| Id    | Nombre corto de HU | Prioridad        | PHU | Estado                              |
| ----- | ------------------- | ---------------- | --- | ------------------------------------ |
| HU-NN | {Nombre corto}       | Alta/Media/Baja  |     | Pendiente/En progreso/Hecha |

**Como:** {rol o tipo de usuario}
**Quiero:** {la acción/capacidad que el usuario busca}
**Para:** {el beneficio u objetivo de negocio detrás de esa acción}

**Descripción:**
{Párrafo narrativo: qué hace el usuario, qué responde el sistema, qué
pasa en los casos alternativos (error, permiso, estado inválido). Si esta
HU depende de otra o es prerequisito de una futura, se aclara acá con un
link relativo a ese otro archivo (`[HU-NN: Nombre](HU{NN}%20-%20Nombre.md)` —
espacios codificados como `%20` porque el nombre del archivo los tiene).}

---

## Conversación/Reglas (opcional)

**Flujo general**

1. {Paso a paso numerado del flujo feliz, desde la acción inicial del
   usuario hasta el resultado final.}
2. ...

**Reglas de negocio**

- {Cada regla de negocio no obvia que condiciona el comportamiento — una
  por bullet. Si una regla remite a una decisión técnica ya tomada en
  otra rule del proyecto, se linkea esa rule en vez de repetirla acá.}

---

## Tabla (opcional)

{Solo si hace falta una tabla de mapeo/referencia adicional (ej. estados
posibles y sus transiciones). Si no aporta nada, esta sección **se borra
del archivo** — no se deja vacía "por si acaso".}

---

## Criterios de aceptación

- **CA01:** Dado que {condición/contexto}, el sistema debe {resultado
  esperado, verificable}.
- **CA02:** Dado que {condición alternativa/error}, el sistema debe
  {resultado esperado, incluyendo el código HTTP/mensaje si aplica}.
- ... (uno por cada caso relevante: camino feliz, cada error específico,
  casos límite explícitos en la descripción)

## Desarrollador

{Nombre de la persona responsable de implementar/dar seguimiento a esta HU.}
```

## Reglas de contenido

- **`Como/Quiero/Para`** es obligatorio y siempre en esa forma exacta —
  no se reemplaza por prosa libre.
- **`Descripción`** es obligatoria; debe cubrir tanto el camino feliz como
  al menos una mención de qué pasa en los casos de error principales (el
  detalle exacto de cada error va en los Criterios de Aceptación, acá solo
  se anticipa que existen).
- **`Conversación/Reglas`** es opcional en el sentido de que una HU muy
  simple puede omitirla, pero si el flujo tiene más de 3-4 pasos o hay
  reglas de negocio no obvias, se completa — no se dejan las reglas de
  negocio implícitas solo en la prosa de la Descripción.
- **`Criterios de aceptación`** es **obligatorio siempre**, numerado
  `CA01`, `CA02`, ... (no se reinicia la numeración ni se reusa un número
  si un CA se elimina durante la revisión, mismo criterio que los códigos
  de error). Cada CA debe ser verificable de forma binaria (pasa/no pasa),
  formato "Dado que \{contexto\}, el sistema debe \{resultado\}" — no una
  frase vaga tipo "el sistema debe funcionar bien".
- **`PHU`** (Puntos de Historia de Usuario) puede quedar vacío hasta que
  el equipo lo estime — no bloquea la creación de la HU, pero si sigue
  vacío al momento de planificar el sprint, se estima antes de empezar a
  implementar sus Unidades de Trabajo.
- **`Estado`** se actualiza a medida que avanza (`Pendiente` → `En
  progreso` → `Hecha`) — esta rule no fuerza un mecanismo automático para
  eso, es responsabilidad de quien gestiona el trabajo mantenerlo al día.
- Los links a otras rules del proyecto (`.claude/rules/...`) o a otras HU
  usan rutas relativas correctas desde `docs/HistoriasDeUsuario/`. Un
  nombre de archivo de HU con espacios se referencia con `%20` en vez del
  espacio literal, para que el link funcione en cualquier visor Markdown.

## Qué NO va en una Historia de Usuario

- Nombres de clases, endpoints exactos, esquemas de base de datos, o
  cualquier detalle de implementación técnica — eso es contenido de las
  [Unidades de Trabajo](UNIDADES_DE_TRABAJO.md), no de la HU. La HU
  describe **comportamiento observable por el usuario/cliente de la API**,
  no cómo se construye por dentro.
- Una lista de tareas técnicas ("crear el Model", "armar el controller")
  — eso también es de las UT.
