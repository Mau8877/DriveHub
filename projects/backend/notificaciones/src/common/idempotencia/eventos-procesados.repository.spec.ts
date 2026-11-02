import { ConditionalCheckFailedException } from '@aws-sdk/client-dynamodb'
import type { DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb'
import { describe, expect, it, vi } from 'vitest'

import { EventosProcesadosRepository } from './eventos-procesados.repository.js'

function crear(send: ReturnType<typeof vi.fn>) {
  return new EventosProcesadosRepository({ send } as unknown as DynamoDBDocumentClient)
}

describe('EventosProcesadosRepository', () => {
  it('registra un evento nuevo con TTL de 7 días y devuelve true', async () => {
    const send = vi.fn().mockResolvedValue({})
    expect(await crear(send).registrar('e1')).toBe(true)
    const { Item, ConditionExpression } = send.mock.calls[0][0].input
    expect(ConditionExpression).toBe('attribute_not_exists(eventoCodigo)')
    const dias = (Item.ttl - Date.now() / 1000) / 86400
    expect(dias).toBeGreaterThan(6.99)
    expect(dias).toBeLessThan(7.01)
  })

  it('devuelve false si el evento ya estaba registrado', async () => {
    const send = vi.fn().mockRejectedValue(
      new ConditionalCheckFailedException({ message: 'duplicado', $metadata: {} })
    )
    expect(await crear(send).registrar('e1')).toBe(false)
  })

  it('propaga cualquier otro error', async () => {
    const send = vi.fn().mockRejectedValue(new Error('base caída'))
    await expect(crear(send).registrar('e1')).rejects.toThrow('base caída')
  })

  it('liberar borra el registro del evento', async () => {
    const send = vi.fn().mockResolvedValue({})
    await crear(send).liberar('e1')
    expect(send.mock.calls[0][0].input.Key).toEqual({ eventoCodigo: 'e1' })
  })
})
