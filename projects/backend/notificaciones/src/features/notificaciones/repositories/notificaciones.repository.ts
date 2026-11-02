import { Inject, Injectable } from '@nestjs/common'
import { DynamoDBDocumentClient, PutCommand } from '@aws-sdk/lib-dynamodb'

import { TABLA_NOTIFICACIONES } from '../../../common/dynamodb/definicion-tablas.js'
import { DYNAMODB_DOCUMENT_CLIENT } from '../../../common/dynamodb/dynamodb.constants.js'
import type { Notificacion } from '../models/notificacion.model.js'

/**
 * Acceso a la tabla Notificaciones: sin lógica de negocio.
 *
 * * Hoy solo guarda (CU36). Listar y marcar leídas lo agrega CU38.
 */
@Injectable()
export class NotificacionesRepository {
  constructor(@Inject(DYNAMODB_DOCUMENT_CLIENT) private readonly db: DynamoDBDocumentClient) {}

  async guardar(notificacion: Notificacion): Promise<void> {
    await this.db.send(new PutCommand({ TableName: TABLA_NOTIFICACIONES, Item: notificacion }))
  }
}
