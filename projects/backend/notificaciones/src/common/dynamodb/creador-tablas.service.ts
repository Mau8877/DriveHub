import { Inject, Injectable, Logger, OnModuleInit } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import {
  CreateTableCommand,
  type CreateTableCommandInput,
  DescribeTableCommand,
  DynamoDBClient,
  ListTablesCommand,
  UpdateTableCommand,
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
 * * En una tabla que ya existe solo agrega los índices secundarios (GSI) que le falten (ej. `PorToken` en
 * * DispositivosPush): sin eso, una base de desarrollo creada antes no los tendría.
 * ! Cualquier otro cambio de esquema en una tabla ya creada hay que aplicarlo a mano o recrear la tabla.
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
      if (existentes.includes(tabla.TableName!)) {
        await this.agregarIndicesFaltantes(tabla)
        continue
      }

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

  private async agregarIndicesFaltantes(tabla: CreateTableCommandInput): Promise<void> {
    const pedidos = tabla.GlobalSecondaryIndexes ?? []
    if (pedidos.length === 0) return

    const { Table } = await this.client.send(
      new DescribeTableCommand({ TableName: tabla.TableName })
    )
    const actuales = new Set((Table?.GlobalSecondaryIndexes ?? []).map((i) => i.IndexName))

    for (const indice of pedidos.filter((i) => !actuales.has(i.IndexName))) {
      const atributos = new Set(indice.KeySchema?.map((k) => k.AttributeName))
      await this.client.send(
        new UpdateTableCommand({
          TableName: tabla.TableName,
          AttributeDefinitions: tabla.AttributeDefinitions?.filter((a) =>
            atributos.has(a.AttributeName)
          ),
          GlobalSecondaryIndexUpdates: [{ Create: indice }],
        })
      )
      this.logger.log({
        module: 'notificaciones',
        message: `Índice ${indice.IndexName} agregado a la tabla ${tabla.TableName}`,
      })
    }
  }
}
