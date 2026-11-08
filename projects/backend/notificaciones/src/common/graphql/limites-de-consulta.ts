import {
  type ASTVisitor,
  type FragmentDefinitionNode,
  GraphQLError,
  Kind,
  type SelectionSetNode,
  type ValidationContext,
  type ValidationRule,
} from 'graphql'

export const PROFUNDIDAD_MAXIMA_POR_DEFECTO = 6
export const CAMPOS_RAIZ_MAXIMOS_POR_DEFECTO = 10

/**
 * Reglas de validación que frenan las consultas abusivas (CU45).
 *
 * * A diferencia de REST, una sola petición GraphQL puede pedir muchísimo (relaciones anidadas, cientos de alias de la
 * * misma mutación). El rate limit del gateway cuenta peticiones, no consultas, así que el servicio pone su propio tope.
 * * Profundidad: los niveles de campos anidados, siguiendo los fragmentos. Campos raíz: cuántas consultas o
 * * mutaciones distintas (con sus alias) trae una operación.
 * * Se rechazan en la validación, antes de ejecutar nada, con el código genérico de validación `ERR_SYS_01`.
 */
export function limitesDeConsulta(
  profundidadMaxima: number = PROFUNDIDAD_MAXIMA_POR_DEFECTO,
  camposRaizMaximos: number = CAMPOS_RAIZ_MAXIMOS_POR_DEFECTO
): ValidationRule {
  return (context: ValidationContext): ASTVisitor => ({
    Document(documento) {
      const fragmentos = new Map<string, FragmentDefinitionNode>()
      for (const definicion of documento.definitions) {
        if (definicion.kind === Kind.FRAGMENT_DEFINITION) fragmentos.set(definicion.name.value, definicion)
      }

      for (const definicion of documento.definitions) {
        if (definicion.kind !== Kind.OPERATION_DEFINITION) continue

        const raiz = camposRaiz(definicion.selectionSet, fragmentos, new Set())
        if (raiz > camposRaizMaximos) {
          context.reportError(
            new GraphQLError(
              `La operación pide demasiadas cosas a la vez (${raiz} campos; el máximo es ${camposRaizMaximos})`,
              { nodes: definicion, extensions: { code: 'ERR_SYS_01' } }
            )
          )
        }

        const profundidad = profundidadDe(definicion.selectionSet, fragmentos, new Set())
        if (profundidad > profundidadMaxima) {
          context.reportError(
            new GraphQLError(
              `La consulta es demasiado profunda (${profundidad} niveles; el máximo es ${profundidadMaxima})`,
              { nodes: definicion, extensions: { code: 'ERR_SYS_01' } }
            )
          )
        }
      }
    },
  })
}

/**
 * Un campo de introspección (`__typename`, `__schema`, `__type`): no cuenta para ningún límite.
 *
 * * La consulta estándar de introspección que usan las herramientas para explorar el esquema tiene unos 15 niveles y
 * * rompería el tope de profundidad. No se pierde protección: con la introspección apagada (producción) Apollo la
 * * rechaza antes, y encendida (desarrollo) solo la usan esas herramientas.
 */
function esDeIntrospeccion(nombre: string): boolean {
  return nombre.startsWith('__')
}

/** Niveles de campos anidados. Un campo sin hijos cuenta 1; los de introspección no cuentan. */
function profundidadDe(
  seleccion: SelectionSetNode,
  fragmentos: Map<string, FragmentDefinitionNode>,
  enCurso: Set<string>
): number {
  let maxima = 0
  for (const nodo of seleccion.selections) {
    if (nodo.kind === Kind.FIELD) {
      if (esDeIntrospeccion(nodo.name.value)) continue
      const hijos = nodo.selectionSet ? profundidadDe(nodo.selectionSet, fragmentos, enCurso) : 0
      maxima = Math.max(maxima, 1 + hijos)
    } else if (nodo.kind === Kind.INLINE_FRAGMENT) {
      maxima = Math.max(maxima, profundidadDe(nodo.selectionSet, fragmentos, enCurso))
    } else {
      const nombre = nodo.name.value
      const fragmento = fragmentos.get(nombre)
      // * Un fragmento que se contiene a sí mismo lo rechaza otra regla de GraphQL; acá solo se evita el bucle
      if (!fragmento || enCurso.has(nombre)) continue
      enCurso.add(nombre)
      maxima = Math.max(maxima, profundidadDe(fragmento.selectionSet, fragmentos, enCurso))
      enCurso.delete(nombre)
    }
  }
  return maxima
}

/** Cuántos campos pide el primer nivel de la operación (cada alias cuenta), siguiendo los fragmentos. */
function camposRaiz(
  seleccion: SelectionSetNode,
  fragmentos: Map<string, FragmentDefinitionNode>,
  enCurso: Set<string>
): number {
  let total = 0
  for (const nodo of seleccion.selections) {
    if (nodo.kind === Kind.FIELD) {
      if (!esDeIntrospeccion(nodo.name.value)) total += 1
    } else if (nodo.kind === Kind.INLINE_FRAGMENT) {
      total += camposRaiz(nodo.selectionSet, fragmentos, enCurso)
    } else {
      const nombre = nodo.name.value
      const fragmento = fragmentos.get(nombre)
      if (!fragmento || enCurso.has(nombre)) continue
      enCurso.add(nombre)
      total += camposRaiz(fragmento.selectionSet, fragmentos, enCurso)
      enCurso.delete(nombre)
    }
  }
  return total
}
