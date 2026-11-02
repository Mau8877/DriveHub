import { Inject, Injectable } from '@nestjs/common'
import { DynamoDBDocumentClient, PutCommand, QueryCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb'
import type { QueryCommandInput } from '@aws-sdk/lib-dynamodb'

import { INDICE_NO_LEIDAS, TABLA_NOTIFICACIONES } from '../../../common/dynamodb/definicion-tablas.js'
import { DYNAMODB_DOCUMENT_CLIENT } from '../../../common/dynamodb/dynamodb.constants.js'
import { CanalNotificacion } from '../enums/canal-notificacion.enum.js'
import type { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import type { Notificacion } from '../models/notificacion.model.js'

export interface FiltrosBandeja {
  leida?: boolean
  tipo?: TipoNotificacion
}

/**
 * Acceso a la tabla Notificaciones: sin lógica de negocio.
 *
 * * La bandeja de la persona son solo las filas IN_APP (CU38): las de EMAIL y PUSH
 * * son la constancia del envío y nunca se listan.
 */
@Injectable()
export class NotificacionesRepository {
  constructor(@Inject(DYNAMODB_DOCUMENT_CLIENT) private readonly db: DynamoDBDocumentClient) {}

  async guardar(notificacion: Notificacion): Promise<void> {
    await this.db.send(new PutCommand({ TableName: TABLA_NOTIFICACIONES, Item: notificacion }))
  }

  /** La bandeja de la persona, del aviso más reciente al más antiguo. */
  async listarBandeja(usuarioCodigo: string, filtros: FiltrosBandeja = {}): Promise<Notificacion[]> {
    return this.consultarBandeja(usuarioCodigo, filtros)
  }

  /** Un aviso de la bandeja por su codigo público (la clave de la tabla es `sk`, no el codigo). */
  async buscarEnBandeja(usuarioCodigo: string, codigoNotificacion: string): Promise<Notificacion | undefined> {
    const encontradas = await this.consultarBandeja(usuarioCodigo, {}, codigoNotificacion)
    return encontradas[0]
  }

  async contarNoLeidas(usuarioCodigo: string): Promise<number> {
    let total = 0
    let siguiente: QueryCommandInput['ExclusiveStartKey']
    do {
      const res = await this.db.send(
        new QueryCommand({
          TableName: TABLA_NOTIFICACIONES,
          IndexName: INDICE_NO_LEIDAS,
          KeyConditionExpression: 'usuarioCodigo = :u',
          ExpressionAttributeValues: { ':u': usuarioCodigo },
          Select: 'COUNT',
          ExclusiveStartKey: siguiente,
        })
      )
      total += res.Count ?? 0
      siguiente = res.LastEvaluatedKey
    } while (siguiente)
    return total
  }

  /** Las no leídas: el índice NoLeidas es disperso y solo tiene filas IN_APP sin leer. */
  async listarNoLeidas(usuarioCodigo: string): Promise<Notificacion[]> {
    const items: Notificacion[] = []
    let siguiente: QueryCommandInput['ExclusiveStartKey']
    do {
      const res = await this.db.send(
        new QueryCommand({
          TableName: TABLA_NOTIFICACIONES,
          IndexName: INDICE_NO_LEIDAS,
          KeyConditionExpression: 'usuarioCodigo = :u',
          ExpressionAttributeValues: { ':u': usuarioCodigo },
          ScanIndexForward: false,
          ExclusiveStartKey: siguiente,
        })
      )
      items.push(...((res.Items ?? []) as Notificacion[]))
      siguiente = res.LastEvaluatedKey
    } while (siguiente)
    return items
  }

  /** Marca la fila como leída y la saca del índice NoLeidas. Sobre una ya leída no cambia nada. */
  async marcarLeida(usuarioCodigo: string, sk: string): Promise<void> {
    await this.db.send(
      new UpdateCommand({
        TableName: TABLA_NOTIFICACIONES,
        Key: { usuarioCodigo, sk },
        UpdateExpression: 'SET leida = :leida REMOVE noLeidaTs',
        ExpressionAttributeValues: { ':leida': true },
      })
    )
  }

  private async consultarBandeja(
    usuarioCodigo: string,
    filtros: FiltrosBandeja,
    codigoNotificacion?: string
  ): Promise<Notificacion[]> {
    const condiciones = ['#canal = :canal']
    const nombres: Record<string, string> = { '#canal': 'canal' }
    const valores: Record<string, unknown> = { ':u': usuarioCodigo, ':canal': CanalNotificacion.IN_APP }
    if (filtros.leida !== undefined) {
      condiciones.push('#leida = :leida')
      nombres['#leida'] = 'leida'
      valores[':leida'] = filtros.leida
    }
    if (filtros.tipo !== undefined) {
      condiciones.push('#tipo = :tipo')
      nombres['#tipo'] = 'tipo'
      valores[':tipo'] = filtros.tipo
    }
    if (codigoNotificacion !== undefined) {
      condiciones.push('codigoNotificacion = :codigo')
      valores[':codigo'] = codigoNotificacion
    }

    const items: Notificacion[] = []
    let siguiente: QueryCommandInput['ExclusiveStartKey']
    do {
      const res = await this.db.send(
        new QueryCommand({
          TableName: TABLA_NOTIFICACIONES,
          KeyConditionExpression: 'usuarioCodigo = :u',
          FilterExpression: condiciones.join(' AND '),
          ExpressionAttributeNames: nombres,
          ExpressionAttributeValues: valores,
          ScanIndexForward: false,
          ExclusiveStartKey: siguiente,
        })
      )
      items.push(...((res.Items ?? []) as Notificacion[]))
      siguiente = res.LastEvaluatedKey
    } while (siguiente)
    return items
  }
}
