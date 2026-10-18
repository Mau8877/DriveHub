import { Global, Module } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { DynamoDBClient } from '@aws-sdk/client-dynamodb'
import { DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb'

import { CreadorTablasService } from './creador-tablas.service.js'
import { DYNAMODB_CLIENT, DYNAMODB_DOCUMENT_CLIENT } from './dynamodb.constants.js'

/**
 * Cliente de DynamoDB compartido por todo el servicio.
 *
 * * DYNAMODB_ENDPOINT apunta a dynamodb-local en desarrollo; en AWS real se
 * * deja vacío y el SDK resuelve el endpoint de la región.
 * * Los repositories usan el DocumentClient (traduce objetos JS <-> atributos
 * * de DynamoDB); el cliente crudo queda para operaciones de tabla.
 */
@Global()
@Module({
  providers: [
    {
      provide: DYNAMODB_CLIENT,
      inject: [ConfigService],
      useFactory: (config: ConfigService) =>
        new DynamoDBClient({
          region: config.get<string>('AWS_REGION', 'us-east-1'),
          endpoint: config.get<string>('DYNAMODB_ENDPOINT') || undefined,
        }),
    },
    {
      provide: DYNAMODB_DOCUMENT_CLIENT,
      inject: [DYNAMODB_CLIENT],
      useFactory: (client: DynamoDBClient) =>
        DynamoDBDocumentClient.from(client, {
          marshallOptions: { removeUndefinedValues: true },
        }),
    },
    CreadorTablasService,
  ],
  exports: [DYNAMODB_CLIENT, DYNAMODB_DOCUMENT_CLIENT],
})
export class DynamoDbModule {}
