import type { DynamoDBClient } from '@aws-sdk/client-dynamodb'
import type { ConfigService } from '@nestjs/config'
import { describe, expect, it, vi } from 'vitest'

import { CreadorTablasService } from './creador-tablas.service.js'
import { DEFINICION_TABLAS, INDICE_NO_LEIDAS, INDICE_POR_TOKEN } from './definicion-tablas.js'

type Comando = { constructor: { name: string }; input: Record<string, unknown> }

/** Un cliente falso: `existentes` son las tablas ya creadas y `indices` los GSI que tiene cada una. */
function crear(existentes: string[], indices: Record<string, string[]> = {}) {
  const enviados: Comando[] = []
  const client = {
    send: vi.fn(async (comando: Comando) => {
      enviados.push(comando)
      const tipo = comando.constructor.name
      if (tipo === 'ListTablesCommand') return { TableNames: existentes }
      if (tipo === 'DescribeTableCommand') {
        const tabla = comando.input.TableName as string
        return {
          Table: {
            // * `waitUntilTableExists` también consulta DescribeTable y espera ACTIVE
            TableStatus: 'ACTIVE',
            GlobalSecondaryIndexes: (indices[tabla] ?? []).map((IndexName) => ({ IndexName })),
          },
        }
      }
      return { Table: { TableStatus: 'ACTIVE' } }
    }),
    config: { region: async () => 'us-east-1' },
  }
  const config = { get: vi.fn().mockReturnValue('true') }
  const servicio = new CreadorTablasService(
    client as unknown as DynamoDBClient,
    config as unknown as ConfigService
  )
  const de = (tipo: string) => enviados.filter((c) => c.constructor.name === tipo)
  return { servicio, de, config }
}

describe('CreadorTablasService', () => {
  it('agrega el índice PorToken a una DispositivosPush que ya existía sin él', async () => {
    const todas = DEFINICION_TABLAS.map((d) => d.tabla.TableName as string)
    const { servicio, de } = crear(todas, { Notificaciones: [INDICE_NO_LEIDAS] })

    await servicio.onModuleInit()

    const cambios = de('UpdateTableCommand')
    expect(cambios).toHaveLength(1)
    expect(cambios[0].input.TableName).toBe('DispositivosPush')
    expect(cambios[0].input.GlobalSecondaryIndexUpdates).toMatchObject([
      { Create: { IndexName: INDICE_POR_TOKEN } },
    ])
    // * Solo declara el atributo que usa el índice (tokenFcm), no los de la clave de la tabla
    expect(cambios[0].input.AttributeDefinitions).toEqual([
      { AttributeName: 'tokenFcm', AttributeType: 'S' },
    ])
    expect(de('CreateTableCommand')).toHaveLength(0)
  })

  it('no toca las tablas que ya tienen todos sus índices', async () => {
    const todas = DEFINICION_TABLAS.map((d) => d.tabla.TableName as string)
    const { servicio, de } = crear(todas, {
      Notificaciones: [INDICE_NO_LEIDAS],
      DispositivosPush: [INDICE_POR_TOKEN],
    })

    await servicio.onModuleInit()

    expect(de('UpdateTableCommand')).toHaveLength(0)
    expect(de('CreateTableCommand')).toHaveLength(0)
  })

  it('crea con el índice las tablas que no existen', async () => {
    const { servicio, de } = crear([])

    await servicio.onModuleInit()

    const dispositivos = de('CreateTableCommand').find(
      (c) => c.input.TableName === 'DispositivosPush'
    )
    expect(dispositivos?.input.GlobalSecondaryIndexes).toMatchObject([
      { IndexName: INDICE_POR_TOKEN },
    ])
    expect(de('UpdateTableCommand')).toHaveLength(0)
  })

  it('no hace nada con DYNAMODB_CREAR_TABLAS=false', async () => {
    const { servicio, de, config } = crear([])
    config.get.mockReturnValue('false')

    await servicio.onModuleInit()

    expect(de('ListTablesCommand')).toHaveLength(0)
  })
})
