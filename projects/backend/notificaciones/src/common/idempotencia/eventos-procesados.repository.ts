import { ConditionalCheckFailedException } from '@aws-sdk/client-dynamodb'
import { DeleteCommand, DynamoDBDocumentClient, PutCommand } from '@aws-sdk/lib-dynamodb'
import { Inject, Injectable } from '@nestjs/common'

import { TABLA_EVENTOS_PROCESADOS } from '../dynamodb/definicion-tablas.js'
import { DYNAMODB_DOCUMENT_CLIENT } from '../dynamodb/dynamodb.constants.js'
import { DIAS_RETENCION_EVENTOS_PROCESADOS, type EventoProcesado } from './evento-procesado.model.js'

/**
 * Idempotencia de los consumidores: el broker puede entregar un mismo evento
 * dos veces, así que antes de procesarlo se registra su codigo con una
 * escritura condicional. Si ya estaba, es un duplicado.
 */
@Injectable()
export class EventosProcesadosRepository {
  constructor(@Inject(DYNAMODB_DOCUMENT_CLIENT) private readonly db: DynamoDBDocumentClient) {}

  /** Devuelve true si el evento era nuevo y false si ya se había procesado. */
  async registrar(eventoCodigo: string): Promise<boolean> {
    const ahora = new Date()
    const item: EventoProcesado = {
      eventoCodigo,
      procesadoEn: ahora.toISOString(),
      ttl: Math.floor(ahora.getTime() / 1000) + DIAS_RETENCION_EVENTOS_PROCESADOS * 24 * 60 * 60,
    }
    try {
      await this.db.send(
        new PutCommand({
          TableName: TABLA_EVENTOS_PROCESADOS,
          Item: item,
          ConditionExpression: 'attribute_not_exists(eventoCodigo)',
        })
      )
      return true
    } catch (error) {
      if (error instanceof ConditionalCheckFailedException) return false
      throw error
    }
  }

  /** Deshace el registro para poder reintentar un evento que falló antes de enviar nada. */
  async liberar(eventoCodigo: string): Promise<void> {
    await this.db.send(
      new DeleteCommand({ TableName: TABLA_EVENTOS_PROCESADOS, Key: { eventoCodigo } })
    )
  }
}
