import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { EventosProcesadosRepository } from '../../../common/idempotencia/eventos-procesados.repository.js'
import { Plataforma } from '../../dispositivos/enums/plataforma.enum.js'
import type { DispositivoPush } from '../../dispositivos/models/dispositivo-push.model.js'
import type { DispositivosRepository } from '../../dispositivos/repositories/dispositivos.repository.js'
import type { PreferenciasNotificacion } from '../../preferencias/models/preferencias-notificacion.model.js'
import type { PreferenciasRepository } from '../../preferencias/repositories/preferencias.repository.js'
import type { SolicitudNotificacionRequest } from '../dto/solicitud-notificacion.request.js'
import { CanalNotificacion } from '../enums/canal-notificacion.enum.js'
import { EstadoNotificacion } from '../enums/estado-notificacion.enum.js'
import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import type { Notificacion } from '../models/notificacion.model.js'
import type { NotificacionesRepository } from '../repositories/notificaciones.repository.js'
import { EnvioNotificacionService } from '../services/envio-notificacion.service.js'
import type { ProveedorCorreo } from '../services/proveedor-correo.js'
import type { ProveedorPush } from '../services/proveedor-push.js'

const solicitud: SolicitudNotificacionRequest = {
  eventoCodigo: '0192f7a0-0000-7000-8000-000000000001',
  usuarioCodigo: '0192f7a0-0000-7000-8000-000000000002',
  tipo: TipoNotificacion.PAGO,
  titulo: 'Pago confirmado',
  mensaje: 'Ya tienes 10 horas',
  datos: { pagoCodigo: 'p1' },
  email: 'ana@correo.com',
}

const mediodia = new Date('2026-11-02T12:00:00-04:00')

function dispositivo(token: string, codigo = `d-${token}`): DispositivoPush {
  return {
    usuarioCodigo: solicitud.usuarioCodigo,
    codigoDispositivoPush: codigo,
    tokenFcm: token,
    plataforma: Plataforma.ANDROID,
    activo: true,
    ultimoUso: '2026-01-01T00:00:00.000Z',
  }
}

describe('EnvioNotificacionService', () => {
  let eventos: { registrar: ReturnType<typeof vi.fn>; liberar: ReturnType<typeof vi.fn> }
  let preferencias: { obtener: ReturnType<typeof vi.fn> }
  let dispositivos: { listarActivos: ReturnType<typeof vi.fn>; desactivar: ReturnType<typeof vi.fn> }
  let guardadas: Notificacion[]
  let historial: { guardar: ReturnType<typeof vi.fn> }
  let correo: { enviar: ReturnType<typeof vi.fn> }
  let push: { enviar: ReturnType<typeof vi.fn> }
  let service: EnvioNotificacionService

  beforeEach(() => {
    guardadas = []
    eventos = { registrar: vi.fn().mockResolvedValue(true), liberar: vi.fn().mockResolvedValue(undefined) }
    preferencias = { obtener: vi.fn().mockResolvedValue(null) }
    dispositivos = {
      listarActivos: vi.fn().mockResolvedValue([dispositivo('t1')]),
      desactivar: vi.fn().mockResolvedValue(undefined),
    }
    historial = {
      guardar: vi.fn(async (n: Notificacion) => {
        guardadas.push(n)
      }),
    }
    correo = { enviar: vi.fn().mockResolvedValue(undefined) }
    push = { enviar: vi.fn().mockResolvedValue({ aceptados: 1, tokensInvalidos: [] }) }
    service = new EnvioNotificacionService(
      eventos as unknown as EventosProcesadosRepository,
      preferencias as unknown as PreferenciasRepository,
      dispositivos as unknown as DispositivosRepository,
      historial as unknown as NotificacionesRepository,
      correo as unknown as ProveedorCorreo,
      push as unknown as ProveedorPush
    )
  })

  const canales = () => guardadas.map((n) => `${n.canal}:${n.estado}`).sort()

  it('CA01: sin preferencias guarda IN_APP no leída, manda el correo y el push', async () => {
    const r = await service.enviar(solicitud, mediodia)

    expect(r.resultado).toBe('PROCESADA')
    expect(canales()).toEqual(['EMAIL:ENVIADA', 'IN_APP:ENVIADA', 'PUSH:ENVIADA'])
    const bandeja = guardadas.find((n) => n.canal === CanalNotificacion.IN_APP)!
    expect(bandeja.leida).toBe(false)
    expect(bandeja.noLeidaTs).toBe(mediodia.toISOString())
    expect(bandeja.sk).toBe(`${mediodia.toISOString()}#${bandeja.codigoNotificacion}`)
    expect(guardadas.find((n) => n.canal === CanalNotificacion.EMAIL)!.noLeidaTs).toBeUndefined()
    expect(correo.enviar).toHaveBeenCalledWith(
      expect.objectContaining({ para: 'ana@correo.com', asunto: 'Pago confirmado' })
    )
    expect(push.enviar).toHaveBeenCalledWith(
      ['t1'],
      'Pago confirmado',
      'Ya tienes 10 horas',
      expect.objectContaining({ pagoCodigo: 'p1', tipo: 'PAGO' })
    )
  })

  it('CA02: un pedido repetido no envía nada', async () => {
    eventos.registrar.mockResolvedValue(false)
    const r = await service.enviar(solicitud, mediodia)
    expect(r).toEqual({ resultado: 'DUPLICADA', notificaciones: [] })
    expect(preferencias.obtener).not.toHaveBeenCalled()
    expect(correo.enviar).not.toHaveBeenCalled()
  })

  it('CA03: un tipo desactivado no envía ni guarda nada', async () => {
    const prefs = {
      canales: { email: true, push: true, inApp: true },
      tipos: { PAGO: false, RESERVA: true, EXAMEN: true, RECORDATORIO: true },
      email: 'ana@correo.com',
    } as PreferenciasNotificacion
    preferencias.obtener.mockResolvedValue(prefs)
    const r = await service.enviar(solicitud, mediodia)
    expect(r.resultado).toBe('OMITIDA')
    expect(guardadas).toHaveLength(0)
    expect(correo.enviar).not.toHaveBeenCalled()
    expect(push.enviar).not.toHaveBeenCalled()
  })

  it('CA04: con el correo desactivado no manda correo pero sí los demás canales', async () => {
    preferencias.obtener.mockResolvedValue({
      canales: { email: false, push: true, inApp: true },
      tipos: { PAGO: true, RESERVA: true, EXAMEN: true, RECORDATORIO: true },
      email: 'ana@correo.com',
    } as PreferenciasNotificacion)
    await service.enviar(solicitud, mediodia)
    expect(correo.enviar).not.toHaveBeenCalled()
    expect(canales()).toEqual(['IN_APP:ENVIADA', 'PUSH:ENVIADA'])
  })

  it('CA05: si el correo falla queda FALLIDA y los demás canales se completan', async () => {
    correo.enviar.mockRejectedValue(new Error('SMTP caído'))
    const r = await service.enviar(solicitud, mediodia)
    expect(r.resultado).toBe('PROCESADA')
    expect(canales()).toEqual(['EMAIL:FALLIDA', 'IN_APP:ENVIADA', 'PUSH:ENVIADA'])
  })

  it('CA06: dentro del silencio no manda push pero sí correo y bandeja', async () => {
    preferencias.obtener.mockResolvedValue({
      canales: { email: true, push: true, inApp: true },
      tipos: { PAGO: true, RESERVA: true, EXAMEN: true, RECORDATORIO: true },
      silencioDesde: '22:00',
      silencioHasta: '06:00',
      email: 'ana@correo.com',
    } as PreferenciasNotificacion)
    await service.enviar(solicitud, new Date('2026-11-02T23:30:00-04:00'))
    expect(push.enviar).not.toHaveBeenCalled()
    expect(canales()).toEqual(['EMAIL:ENVIADA', 'IN_APP:ENVIADA'])
  })

  it('CA07: desactiva el dispositivo cuyo token Firebase rechaza', async () => {
    dispositivos.listarActivos.mockResolvedValue([dispositivo('t1'), dispositivo('t2')])
    push.enviar.mockResolvedValue({ aceptados: 1, tokensInvalidos: ['t2'] })
    await service.enviar(solicitud, mediodia)
    expect(dispositivos.desactivar).toHaveBeenCalledOnce()
    expect(dispositivos.desactivar).toHaveBeenCalledWith(solicitud.usuarioCodigo, 'd-t2')
    expect(canales()).toContain('PUSH:ENVIADA')
  })

  it('el push queda FALLIDA si ningún dispositivo lo acepta o Firebase falla', async () => {
    push.enviar.mockResolvedValue({ aceptados: 0, tokensInvalidos: [] })
    await service.enviar(solicitud, mediodia)
    expect(canales()).toContain('PUSH:FALLIDA')

    guardadas.length = 0
    push.enviar.mockRejectedValue(new Error('Firebase no está configurado'))
    await service.enviar(solicitud, mediodia)
    expect(canales()).toContain('PUSH:FALLIDA')
  })

  it('sin dispositivos activos no intenta el push ni deja fila PUSH', async () => {
    dispositivos.listarActivos.mockResolvedValue([])
    await service.enviar(solicitud, mediodia)
    expect(push.enviar).not.toHaveBeenCalled()
    expect(canales()).toEqual(['EMAIL:ENVIADA', 'IN_APP:ENVIADA'])
  })

  it('sin correo en el pedido ni en las preferencias no manda correo', async () => {
    await service.enviar(Object.assign({}, solicitud, { email: undefined }), mediodia)
    expect(correo.enviar).not.toHaveBeenCalled()
  })

  it('CA12: si falla al leer las preferencias libera el pedido y relanza el error', async () => {
    preferencias.obtener.mockRejectedValue(new Error('DynamoDB caído'))
    await expect(service.enviar(solicitud, mediodia)).rejects.toThrow('DynamoDB caído')
    expect(eventos.liberar).toHaveBeenCalledWith(solicitud.eventoCodigo)
    expect(correo.enviar).not.toHaveBeenCalled()
  })

  it('CA12: si falla al guardar la bandeja libera el pedido y no envía nada', async () => {
    historial.guardar.mockRejectedValueOnce(new Error('DynamoDB caído'))
    await expect(service.enviar(solicitud, mediodia)).rejects.toThrow()
    expect(eventos.liberar).toHaveBeenCalledOnce()
    expect(correo.enviar).not.toHaveBeenCalled()
    expect(push.enviar).not.toHaveBeenCalled()
  })

  it('si falla guardar la constancia de un canal ya enviado no libera ni lanza', async () => {
    historial.guardar.mockImplementation(async (n: Notificacion) => {
      if (n.canal === CanalNotificacion.EMAIL) throw new Error('fallo')
      guardadas.push(n)
    })
    const r = await service.enviar(solicitud, mediodia)
    expect(r.resultado).toBe('PROCESADA')
    expect(eventos.liberar).not.toHaveBeenCalled()
    expect(guardadas.some((n) => n.estado === EstadoNotificacion.FALLIDA)).toBe(false)
  })

  it('el ttl de cada fila es de 180 días', async () => {
    await service.enviar(solicitud, mediodia)
    const dias = (guardadas[0].ttl - mediodia.getTime() / 1000) / 86400
    expect(dias).toBe(180)
  })
})
