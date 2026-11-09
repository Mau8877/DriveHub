import { Inject, Injectable } from '@nestjs/common'
import {
  DeleteCommand,
  DynamoDBDocumentClient,
  PutCommand,
  QueryCommand,
  UpdateCommand,
} from '@aws-sdk/lib-dynamodb'

import { DYNAMODB_DOCUMENT_CLIENT } from '../../../common/dynamodb/dynamodb.constants.js'
import { INDICE_POR_TOKEN, TABLA_DISPOSITIVOS } from '../../../common/dynamodb/definicion-tablas.js'
import type { DispositivoPush } from '../models/dispositivo-push.model.js'

/** Acceso a la tabla DispositivosPush: sin lógica de negocio. */
@Injectable()
export class DispositivosRepository {
  constructor(@Inject(DYNAMODB_DOCUMENT_CLIENT) private readonly db: DynamoDBDocumentClient) {}

  async listarPorUsuario(usuarioCodigo: string): Promise<DispositivoPush[]> {
    const respuesta = await this.db.send(
      new QueryCommand({
        TableName: TABLA_DISPOSITIVOS,
        KeyConditionExpression: 'usuarioCodigo = :u',
        ExpressionAttributeValues: { ':u': usuarioCodigo },
      })
    )
    return (respuesta.Items ?? []) as DispositivoPush[]
  }

  /** Las claves (persona + dispositivo) de todos los que tienen registrado ese token, de cualquier persona. */
  async listarPorToken(
    tokenFcm: string
  ): Promise<Pick<DispositivoPush, 'usuarioCodigo' | 'codigoDispositivoPush'>[]> {
    const respuesta = await this.db.send(
      new QueryCommand({
        TableName: TABLA_DISPOSITIVOS,
        IndexName: INDICE_POR_TOKEN,
        KeyConditionExpression: 'tokenFcm = :t',
        ExpressionAttributeValues: { ':t': tokenFcm },
      })
    )
    return (respuesta.Items ?? []) as Pick<
      DispositivoPush,
      'usuarioCodigo' | 'codigoDispositivoPush'
    >[]
  }

  async listarActivos(usuarioCodigo: string): Promise<DispositivoPush[]> {
    return (await this.listarPorUsuario(usuarioCodigo)).filter((d) => d.activo)
  }

  async guardar(dispositivo: DispositivoPush): Promise<void> {
    await this.db.send(new PutCommand({ TableName: TABLA_DISPOSITIVOS, Item: dispositivo }))
  }

  async desactivar(usuarioCodigo: string, codigoDispositivoPush: string): Promise<void> {
    await this.db.send(
      new UpdateCommand({
        TableName: TABLA_DISPOSITIVOS,
        Key: { usuarioCodigo, codigoDispositivoPush },
        UpdateExpression: 'SET activo = :falso',
        ExpressionAttributeValues: { ':falso': false },
      })
    )
  }

  async eliminar(usuarioCodigo: string, codigoDispositivoPush: string): Promise<void> {
    await this.db.send(
      new DeleteCommand({
        TableName: TABLA_DISPOSITIVOS,
        Key: { usuarioCodigo, codigoDispositivoPush },
      })
    )
  }
}
