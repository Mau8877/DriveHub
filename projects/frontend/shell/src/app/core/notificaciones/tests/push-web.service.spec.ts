import '@angular/compiler'
import { Injector, runInInjectionContext } from '@angular/core'

import { GraphqlClient } from '../../api/graphql.client'
import { FirebaseWeb } from '../firebase-web'
import { PushWebService } from '../push-web.service'

const CONFIG_COMPLETA = {
  apiKey: 'k',
  authDomain: 'd',
  projectId: 'p',
  messagingSenderId: 's',
  appId: 'a',
  vapidKey: 'v',
}

describe('PushWebService', () => {
  let graphql: { ejecutar: ReturnType<typeof vi.fn> }
  let firebase: { obtenerToken: ReturnType<typeof vi.fn> }
  let notificacion: { permission: string; requestPermission: ReturnType<typeof vi.fn> }

  const crear = () => {
    const injector = Injector.create({
      providers: [
        { provide: GraphqlClient, useValue: graphql },
        { provide: FirebaseWeb, useValue: firebase },
      ],
    })
    return runInInjectionContext(injector, () => new PushWebService())
  }

  const configurar = (config: Record<string, string> | null, ok = true) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok, json: async () => config }))
  }

  beforeEach(() => {
    localStorage.clear()
    graphql = {
      ejecutar: vi
        .fn()
        .mockResolvedValue({ registrarDispositivo: { codigoDispositivoPush: 'd1' } }),
    }
    firebase = { obtenerToken: vi.fn().mockResolvedValue('token-fcm') }
    notificacion = { permission: 'granted', requestPermission: vi.fn() }
    vi.stubGlobal('Notification', notificacion)
    Object.defineProperty(navigator, 'serviceWorker', { value: {}, configurable: true })
    configurar(CONFIG_COMPLETA)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('registra el navegador como WEB y guarda el código para quitarlo luego', async () => {
    await crear().activar()

    expect(graphql.ejecutar).toHaveBeenCalledWith(expect.stringContaining('registrarDispositivo'), {
      entrada: { tokenFcm: 'token-fcm', plataforma: 'WEB' },
    })
    expect(localStorage.getItem('dh.push.dispositivo')).toBe('d1')
  })

  it('no pide el permiso si ya estaba concedido', async () => {
    await crear().activar()
    expect(notificacion.requestPermission).not.toHaveBeenCalled()
  })

  it('pide el permiso si aún no se decidió y registra si lo conceden', async () => {
    notificacion.permission = 'default'
    notificacion.requestPermission.mockResolvedValue('granted')

    await crear().activar()

    expect(notificacion.requestPermission).toHaveBeenCalledOnce()
    expect(graphql.ejecutar).toHaveBeenCalledOnce()
  })

  it('no registra si la persona niega el permiso al pedírselo', async () => {
    notificacion.permission = 'default'
    notificacion.requestPermission.mockResolvedValue('denied')

    await crear().activar()

    expect(firebase.obtenerToken).not.toHaveBeenCalled()
    expect(graphql.ejecutar).not.toHaveBeenCalled()
  })

  it('no hace nada si el permiso ya estaba negado, sin volver a pedirlo', async () => {
    notificacion.permission = 'denied'

    await crear().activar()

    expect(notificacion.requestPermission).not.toHaveBeenCalled()
    expect(graphql.ejecutar).not.toHaveBeenCalled()
  })

  it('no hace nada con la configuración de Firebase vacía o incompleta', async () => {
    configurar({ ...CONFIG_COMPLETA, vapidKey: '' })
    await crear().activar()
    configurar(null, false)
    await crear().activar()

    expect(firebase.obtenerToken).not.toHaveBeenCalled()
    expect(graphql.ejecutar).not.toHaveBeenCalled()
  })

  it('no hace nada si el navegador no soporta notificaciones', async () => {
    vi.stubGlobal('Notification', undefined)
    await crear().activar()
    expect(graphql.ejecutar).not.toHaveBeenCalled()
  })

  it('no se registra dos veces en la misma página', async () => {
    const servicio = crear()
    await servicio.activar()
    await servicio.activar()
    await Promise.all([servicio.activar(), servicio.activar()])

    expect(graphql.ejecutar).toHaveBeenCalledOnce()
  })

  it('un error del backend o de Firebase no se propaga', async () => {
    graphql.ejecutar.mockRejectedValue(new Error('500'))
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    await expect(crear().activar()).resolves.toBeUndefined()

    firebase.obtenerToken.mockRejectedValue(new Error('sin red'))
    await expect(crear().activar()).resolves.toBeUndefined()
  })

  it('desactivar quita el dispositivo guardado y limpia el código', async () => {
    localStorage.setItem('dh.push.dispositivo', 'd1')

    await crear().desactivar()

    expect(graphql.ejecutar).toHaveBeenCalledWith(expect.stringContaining('quitarDispositivo'), {
      codigo: 'd1',
    })
    expect(localStorage.getItem('dh.push.dispositivo')).toBeNull()
  })

  it('desactivar sin dispositivo registrado no llama al backend', async () => {
    await crear().desactivar()
    expect(graphql.ejecutar).not.toHaveBeenCalled()
  })

  it('desactivar no lanza si el backend falla y igual limpia el código', async () => {
    localStorage.setItem('dh.push.dispositivo', 'd1')
    graphql.ejecutar.mockRejectedValue(new Error('401'))

    await expect(crear().desactivar()).resolves.toBeUndefined()
    expect(localStorage.getItem('dh.push.dispositivo')).toBeNull()
  })

  it('tras desactivar permite registrar de nuevo', async () => {
    const servicio = crear()
    await servicio.activar()
    await servicio.desactivar()
    await servicio.activar()

    expect(graphql.ejecutar).toHaveBeenCalledTimes(3)
  })
})
