import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'

import { mount, unmount } from './mount'

const props = {
  basePath: '/',
  apiBaseUrl: 'http://localhost:8080/api/v1',
  obtenerToken: () => null,
  navegar: () => undefined,
}

function nuevoElemento() {
  const elemento = document.createElement('div')
  document.body.appendChild(elemento)
  return elemento
}

describe('mount', () => {
  it('monta el remoto en el elemento que le pasa el shell', async () => {
    const elemento = nuevoElemento()

    mount(elemento, props)

    expect(await screen.findByRole('heading', { name: 'Contenido' })).toBeInTheDocument()
    unmount(elemento)
    await waitFor(() => expect(elemento).toBeEmptyDOMElement())
    elemento.remove()
  })

  it('unmount deja el elemento vacío', async () => {
    const elemento = nuevoElemento()
    mount(elemento, props)
    await screen.findByRole('heading', { name: 'Contenido' })

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
