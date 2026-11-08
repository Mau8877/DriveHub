import { ApolloDriver, type ApolloDriverConfig } from '@nestjs/apollo'
import { ConfigService } from '@nestjs/config'
import { GraphQLModule } from '@nestjs/graphql'
import { Module } from '@nestjs/common'
import type { GraphQLFormattedError } from 'graphql'

import {
  CAMPOS_RAIZ_MAXIMOS_POR_DEFECTO,
  limitesDeConsulta,
  PROFUNDIDAD_MAXIMA_POR_DEFECTO,
} from './limites-de-consulta.js'

/** La ruta del endpoint GraphQL. Cuelga de `/api/v1` como el resto de la API de negocio (ver ENDPOINTS_BACKEND.md). */
export const RUTA_GRAPHQL = 'api/v1/graphql'

/**
 * Los códigos con que Apollo marca los errores de la propia petición (sintaxis, campo inexistente, argumento de
 * tipo incorrecto, límites): no son de negocio, son «el pedido no es válido».
 */
const CODIGOS_DE_PETICION_INVALIDA = new Set([
  'GRAPHQL_PARSE_FAILED',
  'GRAPHQL_VALIDATION_FAILED',
  'BAD_USER_INPUT',
  'BAD_REQUEST',
  'OPERATION_RESOLUTION_FAILURE',
  'PERSISTED_QUERY_NOT_SUPPORTED',
])

/**
 * Deja TODO error de GraphQL con un código catalogado en `extensions.code` (regla estricta de EXCEPCIONES_BACKEND.md):
 * los de negocio y los de validación ya lo traen (`ERR_...`); los que Apollo marca como petición inválida pasan a
 * `ERR_SYS_01` y cualquier otro a `ERR_SYS_00`, sin su detalle.
 *
 * ! Los mensajes de la validación de GraphQL (campo inexistente, sintaxis) los escribe la librería y salen en inglés.
 */
export function errorCatalogado(formateado: GraphQLFormattedError): GraphQLFormattedError {
  const codigo = formateado.extensions?.code
  if (typeof codigo === 'string' && codigo.startsWith('ERR_')) return formateado
  if (typeof codigo === 'string' && CODIGOS_DE_PETICION_INVALIDA.has(codigo)) {
    return { ...formateado, extensions: { code: 'ERR_SYS_01' } }
  }
  return { ...formateado, message: 'Ocurrió un error inesperado', extensions: { code: 'ERR_SYS_00' } }
}

/**
 * Configuración de GraphQL (CU45): una puerta más al mismo servicio, al lado de REST.
 *
 * * Código primero: el esquema se arma en memoria desde los decoradores (`autoSchemaFile: true`), sin escribir
 * * ningún archivo `.gql` en disco.
 * * Cada resolver vive en su feature y llama a los mismos services que los controllers de REST.
 * ! No hay lotes de consultas, y la introspección y la página de pruebas solo se activan en desarrollo
 * ! (`GRAPHQL_INTROSPECTION=true`): en producción el esquema no se publica.
 */
@Module({
  imports: [
    GraphQLModule.forRootAsync<ApolloDriverConfig>({
      driver: ApolloDriver,
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        autoSchemaFile: true,
        path: RUTA_GRAPHQL,
        // * Los guards leen la persona del request de Express: se lo pasamos al contexto de GraphQL
        context: ({ req }: { req: unknown }) => ({ req }),
        introspection: config.get<string>('GRAPHQL_INTROSPECTION') === 'true',
        playground: false,
        allowBatchedHttpRequests: false,
        includeStacktraceInErrorResponses: false,
        formatError: errorCatalogado,
        validationRules: [
          limitesDeConsulta(
            Number(config.get<string>('GRAPHQL_MAX_DEPTH') ?? PROFUNDIDAD_MAXIMA_POR_DEFECTO),
            Number(config.get<string>('GRAPHQL_MAX_ROOT_FIELDS') ?? CAMPOS_RAIZ_MAXIMOS_POR_DEFECTO)
          ),
        ],
      }),
    }),
  ],
})
export class GraphqlApiModule {}

/**
 * Los módulos de GraphQL que se suman al módulo raíz: ninguno si `GRAPHQL_ENABLED=false`.
 *
 * * Es el interruptor de apagado: sin GraphQL el servicio queda exactamente como antes (solo REST).
 */
export function modulosGraphql(entorno: NodeJS.ProcessEnv = process.env) {
  return entorno.GRAPHQL_ENABLED === 'false' ? [] : [GraphqlApiModule]
}
