import { Inject, Injectable, Logger, OnModuleInit } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import {
  CreateTableCommand,
  DynamoDBClient,
  ListTablesCommand,
  UpdateTimeToLiveCommand,
  waitUntilTableExists,
} from '@aws-sdk/client-dynamodb'

import { DEFINICION_TABLAS } from './definicion-tablas.js'
import { DYNAMODB_CLIENT } from './dynamodb.constants.js'

/**
 * Crea al arrancar las tablas de DEFINICION_TABLAS que todavía no existan
 * (y activa su TTL). Es idempotente: si la tabla ya existe, no la toca.
 *
 * * Activo por defecto (DYNAMODB_CREAR_TABLAS=true) para desarrollo con
 * * dynamodb-local. En AWS real las tablas se provisionan por infraestructura
 * * y esto se apaga con DYNAMODB_CREAR_TABLAS=false.
 * ! No modifica tablas existentes: un cambio de esquema (ej. un GSI nuevo)
 * ! en una tabla ya creada hay que aplicarlo a mano o recrear la tabla.
 */
@Injectable()
export class CreadorTablasService implements OnModuleInit {
  private readonly logger = new Logger(CreadorTablasService.name)

  constructor(
    @Inject(DYNAMODB_CLIENT) private readonly client: DynamoDBClient,
    private readonly config: ConfigService
  ) {}

  async onModuleInit(): Promise<void> {
    if (this.config.get<string>('DYNAMODB_CREAR_TABLAS', 'true') !== 'true') return

    const { TableNames: existentes = [] } = await this.client.send(new ListTablesCommand({}))

    for (const { tabla, atributoTtl } of DEFINICION_TABLAS) {
      if (existentes.includes(tabla.TableName!)) continue

      await this.client.send(new CreateTableCommand(tabla))
      await waitUntilTableExists(
        { client: this.client, maxWaitTime: 30 },
        { TableName: tabla.TableName }
      )
      if (atributoTtl) {
        await this.client.send(
          new UpdateTimeToLiveCommand({
            TableName: tabla.TableName,
            TimeToLiveSpecification: { AttributeName: atributoTtl, Enabled: true },
          })
        )
      }
      this.logger.log({ module: 'notificaciones', message: `Tabla ${tabla.TableName} creada` })
    }
  }
}
