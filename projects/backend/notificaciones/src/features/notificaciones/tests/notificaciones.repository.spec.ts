import type { DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb'
import { describe, expect, it, vi } from 'vitest'

import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import { NotificacionesRepository } from '../repositories/notificaciones.repository.js'

function crear(send: ReturnType<typeof vi.fn>) {
  return new NotificacionesRepository({ send } as unknown as DynamoDBDocumentClient)
}

describe('NotificacionesRepository (bandeja)', () => {
  it('lista solo las filas IN_APP de la persona, de la más reciente a la más antigua', async () => {
    const send = vi.fn().mockResolvedValue({ Items: [{ codigoNotificacion: 'n1' }] })

    const items = await crear(send).listarBandeja('u1')

    expect(items).toEqual([{ codigoNotificacion: 'n1' }])
    const input = send.mock.calls[0][0].input
    expect(input.KeyConditionExpression).toBe('usuarioCodigo = :u')
    expect(input.FilterExpression).toBe('#canal = :canal')
    expect(input.ExpressionAttributeValues).toEqual({ ':u': 'u1', ':canal': 'IN_APP' })
    expect(input.ScanIndexForward).toBe(false)
    expect(input.IndexName).toBeUndefined()
  })

  it('suma los filtros de leída y tipo cuando vienen', async () => {
    const send = vi.fn().mockResolvedValue({ Items: [] })

    await crear(send).listarBandeja('u1', { leida: false, tipo: TipoNotificacion.PAGO })

    const input = send.mock.calls[0][0].input
    expect(input.FilterExpression).toBe('#canal = :canal AND #leida = :leida AND #tipo = :tipo')
    expect(input.ExpressionAttributeValues).toMatchObject({ ':leida': false, ':tipo': 'PAGO' })
  })

  it('sigue LastEvaluatedKey hasta agotar las páginas de DynamoDB', async () => {
    const send = vi
      .fn()
      .mockResolvedValueOnce({ Items: [{ codigoNotificacion: 'n1' }], LastEvaluatedKey: { sk: 'k1' } })
      .mockResolvedValueOnce({ Items: [{ codigoNotificacion: 'n2' }] })

    const items = await crear(send).listarBandeja('u1')

    expect(items.map((i) => i.codigoNotificacion)).toEqual(['n1', 'n2'])
    expect(send.mock.calls[1][0].input.ExclusiveStartKey).toEqual({ sk: 'k1' })
  })

  it('busca un aviso por su codigo público filtrando también por canal IN_APP', async () => {
    const send = vi.fn().mockResolvedValue({ Items: [{ codigoNotificacion: 'n1' }] })

    const aviso = await crear(send).buscarEnBandeja('u1', 'n1')

    expect(aviso).toEqual({ codigoNotificacion: 'n1' })
    const input = send.mock.calls[0][0].input
    expect(input.FilterExpression).toBe('#canal = :canal AND codigoNotificacion = :codigo')
    expect(input.ExpressionAttributeValues[':codigo']).toBe('n1')
  })

  it('devuelve undefined si el aviso no está en la bandeja', async () => {
    const send = vi.fn().mockResolvedValue({ Items: [] })
    expect(await crear(send).buscarEnBandeja('u1', 'nx')).toBeUndefined()
  })

  it('cuenta las no leídas con el índice NoLeidas, sumando todas las páginas', async () => {
    const send = vi
      .fn()
      .mockResolvedValueOnce({ Count: 2, LastEvaluatedKey: { sk: 'k1' } })
      .mockResolvedValueOnce({ Count: 1 })

    expect(await crear(send).contarNoLeidas('u1')).toBe(3)
    const input = send.mock.calls[0][0].input
    expect(input.IndexName).toBe('NoLeidas')
    expect(input.Select).toBe('COUNT')
  })

  it('lista las no leídas desde el índice NoLeidas', async () => {
    const send = vi.fn().mockResolvedValue({ Items: [{ codigoNotificacion: 'n1' }] })

    expect(await crear(send).listarNoLeidas('u1')).toHaveLength(1)
    expect(send.mock.calls[0][0].input.IndexName).toBe('NoLeidas')
  })

  it('marcarLeida deja leida en true y quita noLeidaTs para sacarla del índice', async () => {
    const send = vi.fn().mockResolvedValue({})

    await crear(send).marcarLeida('u1', 'sk1')

    const input = send.mock.calls[0][0].input
    expect(input.Key).toEqual({ usuarioCodigo: 'u1', sk: 'sk1' })
    expect(input.UpdateExpression).toBe('SET leida = :leida REMOVE noLeidaTs')
    expect(input.ExpressionAttributeValues).toEqual({ ':leida': true })
  })
})
