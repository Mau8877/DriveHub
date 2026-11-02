import { Inject, Injectable } from '@nestjs/common'
import { DynamoDBDocumentClient, GetCommand, PutCommand } from '@aws-sdk/lib-dynamodb'

import { TABLA_PREFERENCIAS } from '../../../common/dynamodb/definicion-tablas.js'
import { DYNAMODB_DOCUMENT_CLIENT } from '../../../common/dynamodb/dynamodb.constants.js'
import type { PreferenciasNotificacion } from '../models/preferencias-notificacion.model.js'

/**
 * Acceso a la tabla PreferenciasNotificacion: sin lógica de negocio.
 *
 * * CU36 consulta qué quiere la persona y CU37 la guarda.
 */
@Injectable()
export class PreferenciasRepository {
  constructor(@Inject(DYNAMODB_DOCUMENT_CLIENT) private readonly db: DynamoDBDocumentClient) {}

  async obtener(usuarioCodigo: string): Promise<PreferenciasNotificacion | null> {
    const respuesta = await this.db.send(
      new GetCommand({ TableName: TABLA_PREFERENCIAS, Key: { usuarioCodigo } })
    )
    return (respuesta.Item as PreferenciasNotificacion | undefined) ?? null
  }

  /** Reemplaza la configuración completa de la persona. */
  async guardar(preferencias: PreferenciasNotificacion): Promise<void> {
    await this.db.send(new PutCommand({ TableName: TABLA_PREFERENCIAS, Item: preferencias }))
  }
}
