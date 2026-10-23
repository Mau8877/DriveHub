import { afterEach, describe, expect, it, vi } from 'vitest'
import * as Leaflet from 'leaflet/dist/leaflet-src.esm.js'

import { DhMapaUbicacion } from './index.js'

const L = Leaflet

async function crear(propiedades = {}) {
  const el = document.createElement('dh-mapa-ubicacion')
  Object.assign(el, propiedades)
  document.body.appendChild(el)
  await el.updateComplete
  return el
}

const q = (el, selector) => el.shadowRoot.querySelector(selector)

/** Simula un toque en el mapa en ese punto. */
function tocar(el, latitud, longitud) {
  el._mapa.fire('click', { latlng: L.latLng(latitud, longitud) })
}

function escuchar(el) {
  const eventos = []
  el.addEventListener('dh-ubicacion', (e) => eventos.push(e.detail))
  return eventos
}

afterEach(() => {
  document.body.innerHTML = ''
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('<dh-mapa-ubicacion>', () => {
  it('se registra una sola vez', () => {
    expect(customElements.get('dh-mapa-ubicacion')).toBe(DhMapaUbicacion)
    expect(() => import('./index.js')).not.toThrow()
  })

  it('muestra el mapa con la atribución de OpenStreetMap siempre a la vista', async () => {
    const el = await crear()

    expect(el._mapa).toBeTruthy()
    expect(q(el, '.leaflet-control-attribution').textContent).toContain('OpenStreetMap')
  })

  it('sin punto, centra en La Paz y no hay marcador', async () => {
    const el = await crear()

    const centro = el._mapa.getCenter()
    expect(centro.lat).toBeCloseTo(-17.7833, 3)
    expect(centro.lng).toBeCloseTo(-63.1821, 3)
    expect(el._marcador).toBeNull()
  })

  it('con un punto inicial, coloca el marcador y centra en él', async () => {
    const el = await crear({ latitud: -17.7833, longitud: -63.1821 })

    expect(el._marcador).toBeTruthy()
    expect(el._marcador.getLatLng().lat).toBeCloseTo(-17.7833, 4)
    expect(el._mapa.getCenter().lng).toBeCloseTo(-63.1821, 3)
  })

  it('un toque emite dh-ubicacion con 6 decimales y coloca el marcador', async () => {
    const el = await crear()
    const eventos = escuchar(el)

    tocar(el, -16.48970012345, -68.11930098765)

    expect(eventos).toEqual([{ latitud: -16.4897, longitud: -68.119301 }])
    expect(el.latitud).toBe(-16.4897)
    expect(el._marcador.getLatLng().lat).toBeCloseTo(-16.4897, 4)
  })

  it('un segundo toque mueve el único marcador', async () => {
    const el = await crear()

    tocar(el, -16.5, -68.1)
    const marcador = el._marcador
    tocar(el, -16.6, -68.2)

    expect(el._marcador).toBe(marcador)
    expect(marcador.getLatLng().lat).toBeCloseTo(-16.6, 4)
  })

  it('arrastrar el marcador emite dh-ubicacion', async () => {
    const el = await crear({ latitud: -16.5, longitud: -68.1 })
    const eventos = escuchar(el)

    el._marcador.setLatLng([-16.7, -68.3])
    el._marcador.fire('dragend')

    expect(eventos).toEqual([{ latitud: -16.7, longitud: -68.3 }])
  })

  it('deshabilitado no emite ni mueve el marcador', async () => {
    const el = await crear({ latitud: -16.5, longitud: -68.1, deshabilitado: true })
    const eventos = escuchar(el)

    tocar(el, -16.9, -68.9)

    expect(eventos).toEqual([])
    expect(el.latitud).toBe(-16.5)
    expect(q(el, 'button').disabled).toBe(true)
  })

  it('muestra la dirección guardada cuando existe', async () => {
    const el = await crear({ direccion: 'Av. Arce 2345, La Paz' })

    expect(q(el, '.direccion').textContent).toContain('Av. Arce 2345, La Paz')
  })

  it('no muestra el bloque de dirección cuando está en blanco', async () => {
    const el = await crear({ direccion: '' })

    expect(q(el, '.direccion')).toBeNull()
  })

  it('"Mi ubicación" emite el punto del dispositivo', async () => {
    vi.stubGlobal('navigator', {
      geolocation: {
        getCurrentPosition: (ok) => ok({ coords: { latitude: -16.5001, longitude: -68.1502 } }),
      },
    })
    const el = await crear()
    const eventos = escuchar(el)

    q(el, 'button').click()

    expect(eventos).toEqual([{ latitud: -16.5001, longitud: -68.1502 }])
  })

  it('"Mi ubicación" con el permiso denegado avisa y no emite', async () => {
    vi.stubGlobal('navigator', {
      geolocation: { getCurrentPosition: (_ok, error) => error({ code: 1 }) },
    })
    const el = await crear()
    const eventos = escuchar(el)

    q(el, 'button').click()
    await el.updateComplete

    expect(eventos).toEqual([])
    expect(q(el, '.aviso').textContent).toContain('No pudimos obtener tu ubicación')
  })

  it('"Mi ubicación" sin geolocalización en el dispositivo avisa', async () => {
    vi.stubGlobal('navigator', {})
    const el = await crear()

    q(el, 'button').click()
    await el.updateComplete

    expect(q(el, '.aviso').textContent).toContain('no permite obtener la ubicación')
  })

  it('Alt + flecha ajusta el marcador y emite', async () => {
    const el = await crear({ latitud: -16.5, longitud: -68.1 })
    const eventos = escuchar(el)

    q(el, '.mapa').dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp', altKey: true, bubbles: true }))

    expect(eventos).toHaveLength(1)
    expect(eventos[0].latitud).toBeGreaterThan(-16.5)
    expect(eventos[0].longitud).toBe(-68.1)
  })

  it('una flecha sin Alt no mueve el marcador', async () => {
    const el = await crear({ latitud: -16.5, longitud: -68.1 })
    const eventos = escuchar(el)

    q(el, '.mapa').dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true }))

    expect(eventos).toEqual([])
  })

  it('al cambiar latitud y longitud desde afuera, mueve el marcador', async () => {
    const el = await crear({ latitud: -16.5, longitud: -68.1 })

    el.latitud = -17.0
    el.longitud = -68.5
    await el.updateComplete

    expect(el._marcador.getLatLng().lat).toBeCloseTo(-17.0, 4)
  })

  it('al quitar el punto desde afuera, quita el marcador', async () => {
    const el = await crear({ latitud: -16.5, longitud: -68.1 })

    el.latitud = null
    el.longitud = null
    await el.updateComplete

    expect(el._marcador).toBeNull()
  })

  it('al salir del documento libera el mapa', async () => {
    const el = await crear()
    const mapa = el._mapa
    const remover = vi.spyOn(mapa, 'remove')

    el.remove()

    expect(remover).toHaveBeenCalled()
    expect(el._mapa).toBeNull()
  })
})
