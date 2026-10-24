import { describe, expect, it } from 'vitest'
import { waitFor } from '@testing-library/react'

import { mount, unmount } from './mount'

const props = {
  basePath: '/',
  apiBaseUrl: 'http://localhost:8080/api/v1',
  obtenerToken: () => null,
  navegar: () => undefined,
  renovarSesion: async () => null,
}

/** Espera a que el remoto pinte su pantalla (la gestión de manuales, un <dh-gestion>). */
const pantalla = (elemento: HTMLElement) =>
  waitFor(() => expect(elemento.querySelector('dh-gestion')).not.toBeNull(), { timeout: 8000 })

function nuevoElemento() {
  const elemento = document.createElement('div')
  document.body.appendChild(elemento)
  return elemento
}

describe('mount', () => {
  // * La primera carga trae <dh-gestion> y @drivehub/ui: en frío tarda más que el tiempo por defecto
  it('monta el remoto en el elemento que le pasa el shell', { timeout: 20000 }, async () => {
    const elemento = nuevoElemento()

    mount(elemento, props)

    await pantalla(elemento)
    unmount(elemento)
    await waitFor(() => expect(elemento).toBeEmptyDOMElement())
    elemento.remove()
  })

  it('unmount deja el elemento vacío', async () => {
    const elemento = nuevoElemento()
    mount(elemento, props)
    await pantalla(elemento)

    unmount(elemento)

    await waitFor(() => expect(elemento).toBeEmptyDOMElement())
    elemento.remove()
  })

  it('si el shell desmonta antes de que termine la carga, no monta nada', async () => {
    const elemento = nuevoElemento()

    mount(elemento, props)
    unmount(elemento)

    await new Promise((resolver) => setTimeout(resolver, 50))
    expect(elemento).toBeEmptyDOMElement()
    elemento.remove()
  })
})
