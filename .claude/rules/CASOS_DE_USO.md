---
globs: docs/CasosDeUso/**/*
---

# Casos de Uso

Esta regla define **cómo se escribe un Caso de Uso (CU)** en este
proyecto — el formato, las secciones obligatorias y el criterio de
granularidad. Se aplica siempre que se cree o edite un archivo dentro de
`docs/CasosDeUso/`.

La numeración (`CU01` ... `CU44`) viene del diseño funcional en
[`docs/DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf`](../../docs/DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf):
es la fuente de verdad de qué CU existe y cómo se llama. Un CU documenta
**un flujo de usuario coherente y acotado** — no es un documento de diseño
técnico (eso lo cubren las [Unidades de Trabajo](UNIDADES_DE_TRABAJO.md),
que se crean después y a partir del CU ya escrito).

## Dónde vive y cómo se nombra

`docs/CasosDeUso/CU{NN} - {Nombre corto}.md` — el número es el del diseño
(`CU01`, `CU02`, ..., `CU44`), nunca se renumera ni se reasigna a otro CU
aunque uno se descarte (mismo criterio que los códigos de error de
[EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md)). El nombre corto
del archivo coincide con el campo "Nombre" de la tabla interna.

**Excepción vigente: CU05.** El PDF lo llama "Gestionar roles y permisos"
y estaba equivocado: el CU05 del proyecto es **Gestionar escuelas** (el
Super admin crea escuelas con su Administrador, edita su nombre y las
desactiva o reactiva). La gestión de roles y permisos quedó sin CU asignado.
El archivo, las UT y los comentarios usan solo el significado nuevo.

**Excepción vigente: CU25.** El PDF lo llama "Gestionar plan de pagos y cuotas": un solo CU para el plan,
su contratación por el estudiante y las cuotas. El CU25 del proyecto es **Gestionar planes de horas** (el
Administrador define el nombre, las horas y el costo en bolivianos de cada plan, con su equivalente en
dólares como referencia). La contratación del plan por el estudiante y sus cuotas quedaron sin CU asignado:
cuando se documenten, toman número desde el CU26 en adelante. El archivo, las UT y los comentarios usan solo
el significado nuevo.

## Granularidad: un CU, un flujo

Si una funcionalidad agrupa más de un flujo de usuario independiente, se
separa en CU distintos. Señal de que hay que separar: cada flujo tiene su
propio disparador, su propio conjunto de criterios de aceptación que no
dependen de que el otro flujo se esté ejecutando, y podría entregarse y
probarse por separado. Ejemplo genérico: "iniciar sesión", "renovar la
sesión" y "cerrar sesión" son tres CU, no uno. Cuando un CU depende de
otro, se documenta en sus **precondiciones** con un link cruzado, no
fusionándolos.

## Formato del archivo (plantilla completa)

```markdown
# Caso de uso

| Id   | Nombre         | Paquete   | Prioridad       | Estado                      |
| ---- | -------------- | --------- | --------------- | --------------------------- |
| CUNN | {Nombre corto} | {Paquete} | Alta/Media/Baja | Pendiente/En progreso/Hecho |

**Actor principal:** {rol o tipo de usuario que dispara el CU}
**Actores secundarios:** {otros roles o sistemas involucrados, o "—"}
**Objetivo:** {qué busca lograr el actor, en una frase}

**Descripción:**
{Párrafo narrativo: qué hace el actor, qué responde el sistema y qué pasa
en los casos alternativos (error, permiso, estado inválido).}

## Precondiciones

- {Estado que debe cumplirse antes de ejecutar el CU. Si depende de otro
  CU, link relativo: `[CU-NN: Nombre](CU{NN}%20-%20Nombre.md)` — espacios
  codificados como `%20`.}

## Flujo principal

1. {Paso numerado del camino feliz, desde la acción inicial del actor
   hasta el resultado final.}
2. ...

## Flujos alternativos y excepciones

- **A1 (paso N):** {condición alternativa o de error} → {qué hace el
  sistema, incluido el código HTTP/error si aplica}.

## Reglas de negocio

- {Cada regla no obvia que condiciona el comportamiento, una por bullet.
  Si remite a una decisión técnica ya tomada en otra rule, se linkea.}

## Postcondiciones

- {Estado en que queda el sistema al terminar con éxito.}

## Criterios de aceptación

- **CA01:** Dado que {contexto}, el sistema debe {resultado verificable}.
- **CA02:** Dado que {condición alternativa/error}, el sistema debe
  {resultado esperado, con código HTTP/mensaje si aplica}.

## Desarrollador

{Responsable de implementar/dar seguimiento a este CU.}
```

## Reglas de contenido

- **Actor, Objetivo y Descripción** son obligatorios. La descripción cubre
  el camino feliz y menciona los errores principales (el detalle va en los
  flujos alternativos y los criterios).
- **Flujo principal** numerado y **Flujos alternativos** referenciados al
  paso que bifurcan (`A1`, `A2`...). Si el CU es muy simple puede tener
  solo flujo principal, pero un CU sin ningún flujo de error documentado
  se revisa dos veces.
- **Reglas de negocio**: no se dejan implícitas solo en la prosa.
- **Criterios de aceptación**: obligatorios siempre, numerados `CA01`,
  `CA02`... sin reusar un número si uno se elimina. Cada CA es verificable
  de forma binaria, formato "Dado que {contexto}, el sistema debe
  {resultado}" — nunca "el sistema debe funcionar bien".
- **Estado** se actualiza a mano a medida que avanza.
- Los links a otras rules o a otros CU usan rutas relativas correctas
  desde `docs/CasosDeUso/`.

## Qué NO va en un Caso de Uso

- Nombres de clases, endpoints exactos, esquemas de base de datos o
  cualquier detalle de implementación — eso es contenido de las
  [Unidades de Trabajo](UNIDADES_DE_TRABAJO.md). El CU describe
  **comportamiento observable por el actor**, no cómo se construye.
- Una lista de tareas técnicas ("crear el Model", "armar el controller").
