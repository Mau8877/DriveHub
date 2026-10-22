import { afterEach, describe, expect, it, vi } from 'vitest'

import { iniciales } from './index.js'
import { montarShellSuelto as desdeDesarrollo } from './desarrollo.js'

const ITEMS = [
  { ruta: '/', etiqueta: 'Inicio', descripcion: 'Resumen', icono: 'home' },
  { ruta: '/escuelas', etiqueta: 'Gestionar escuelas', descripcion: 'Altas y estado', icono: 'school' },
  { ruta: '/pagos', etiqueta: 'Pagos', icono: 'wallet', grupo: 'Finanzas', insignia: 3 },
]

const USUARIO = { nombres: 'Ana María', apellidos: 'Pérez', fotoUrl: null, rol: 'Super admin' }
const ACCIONES = [
  { id: 'perfil', etiqueta: 'Editar perfil', icono: 'user', ruta: '/perfil' },
  { id: 'cerrar-sesion', etiqueta: 'Cerrar sesión', icono: 'logout', tono: 'peligro' },
]

async function crear(etiqueta, propiedades = {}, hijos = '') {
  const el = document.createElement(etiqueta)
  Object.assign(el, propiedades)
  el.innerHTML = hijos
  document.body.appendChild(el)
  await el.updateComplete
  return el
}
const q = (el, selector) => el.shadowRoot.querySelector(selector)
const qa = (el, selector) => [...el.shadowRoot.querySelectorAll(selector)]

afterEach(() => {
  document.body.innerHTML = ''
})

describe('dh-avatar', () => {
  it('sin foto muestra las iniciales', async () => {
    const el = await crear('dh-avatar', { nombres: 'Ana María', apellidos: 'Pérez' })
    expect(q(el, 'span').textContent).toBe('AP')
    expect(q(el, 'img')).toBeNull()
  })

  it('con foto muestra la imagen y si falla vuelve a las iniciales', async () => {
    const el = await crear('dh-avatar', { nombres: 'Ana', apellidos: 'Pérez', fotoUrl: 'http://minio/f.png' })
    expect(q(el, 'img').getAttribute('src')).toBe('http://minio/f.png')

    q(el, 'img').dispatchEvent(new Event('error'))
    await el.updateComplete

    expect(q(el, 'img')).toBeNull()
    expect(q(el, 'span').textContent).toBe('AP')
  })

  it('iniciales ignora espacios y pasa a mayúscula', () => {
    expect(iniciales(' ana', 'pérez')).toBe('AP')
    expect(iniciales()).toBe('')
  })
})

describe('dh-icono', () => {
  it('dibuja un icono conocido y nada con uno desconocido', async () => {
    const conocido = await crear('dh-icono', { nombre: 'school', tam: 24 })
    expect(q(conocido, 'svg')).not.toBeNull()
    expect(q(conocido, 'svg').getAttribute('width')).toBe('24')

    const desconocido = await crear('dh-icono', { nombre: 'no-existe' })
    expect(q(desconocido, 'svg')).toBeNull()
  })
})

describe('dh-sidebar', () => {
  it('dibuja un enlace por item con su etiqueta, descripción y destino', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS, activo: '/' })
    const enlaces = qa(el, 'a.item')
    expect(enlaces.map((a) => a.querySelector('strong').textContent)).toEqual(['Inicio', 'Gestionar escuelas', 'Pagos'])
    expect(enlaces[1].getAttribute('href')).toBe('/escuelas')
    expect(enlaces[1].querySelector('small').textContent).toBe('Altas y estado')
  })

  it('marca como activo el item de la ruta y de sus subrutas, y la raíz solo consigo misma', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS, activo: '/escuelas/nueva?x=1' })
    const activos = qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))
    expect(activos).toEqual(['/escuelas'])
    expect(q(el, 'a.activo').getAttribute('aria-current')).toBe('page')
  })

  it('un clic normal emite dh-navegar con la ruta y no navega; con Ctrl lo deja al navegador', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS })
    const alNavegar = vi.fn()
    el.addEventListener('dh-navegar', (e) => alNavegar(e.detail.ruta))

    const normal = new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 })
    qa(el, 'a.item')[1].dispatchEvent(normal)
    const conCtrl = new MouseEvent('click', { bubbles: true, cancelable: true, button: 0, ctrlKey: true })
    qa(el, 'a.item')[2].dispatchEvent(conCtrl)

    expect(alNavegar).toHaveBeenCalledTimes(1)
    expect(alNavegar).toHaveBeenCalledWith('/escuelas')
    expect(normal.defaultPrevented).toBe(true)
    expect(conCtrl.defaultPrevented).toBe(false)
  })

  it('con navegacion-nativa no intercepta el clic', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS, navegacionNativa: true })
    const alNavegar = vi.fn()
    el.addEventListener('dh-navegar', alNavegar)

    const clic = new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 })
    qa(el, 'a.item')[0].dispatchEvent(clic)

    expect(alNavegar).not.toHaveBeenCalled()
    expect(clic.defaultPrevented).toBe(false)
  })

  it('muestra el título de grupo, la insignia y la etiqueta sobre el menú', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS, etiquetaGrupo: 'Autoescuela Example' })
    expect(qa(el, '.grupo').map((g) => g.textContent.trim())).toEqual(['Autoescuela Example', 'Finanzas'])
    expect(q(el, '.insignia').textContent).toBe('3')
  })

  it('contraer emite dh-colapsar y refleja el atributo', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS })
    const alColapsar = vi.fn()
    el.addEventListener('dh-colapsar', (e) => alColapsar(e.detail.colapsado))

    q(el, 'button.contraer').click()
    await el.updateComplete

    expect(alColapsar).toHaveBeenCalledWith(true)
    expect(el.hasAttribute('colapsado')).toBe(true)
  })

  it('sin colapsable no hay botón de contraer', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS, colapsable: false })
    expect(q(el, 'button.contraer')).toBeNull()
  })

  it('un item sin icono o con un icono desconocido igual se dibuja', async () => {
    const el = await crear('dh-sidebar', { items: [{ ruta: '/x', etiqueta: 'X', icono: 'no-existe' }] })
    expect(qa(el, 'a.item')).toHaveLength(1)
  })
})

describe('dh-header', () => {
  it('dibuja las migas: enlaces salvo la última, que es la página actual', async () => {
    const el = await crear('dh-header', { migas: [{ etiqueta: 'Inicio', ruta: '/' }, { etiqueta: 'Escuelas' }] })
    expect(q(el, '.migas a').textContent).toBe('Inicio')
    expect(q(el, '.migas [aria-current="page"]').textContent).toBe('Escuelas')
  })

  it('sin usuario no hay menú de la cuenta', async () => {
    const el = await crear('dh-header')
    expect(q(el, '.cuenta')).toBeNull()
  })

  it('el menú de la cuenta abre con clic, muestra el nombre y el rol, y cierra con Escape', async () => {
    const el = await crear('dh-header', { usuario: USUARIO, accionesUsuario: ACCIONES })
    const boton = q(el, '.avatar-btn')
    expect(boton.getAttribute('aria-expanded')).toBe('false')
    expect(q(el, '.menu')).toBeNull()

    boton.click()
    await el.updateComplete
    expect(boton.getAttribute('aria-expanded')).toBe('true')
    expect(q(el, '.usuario b').textContent).toBe('Ana María Pérez')
    expect(q(el, '.usuario small').textContent).toBe('Super admin')
    expect(qa(el, '[role="menuitem"]').map((o) => o.textContent.trim())).toEqual(['Editar perfil', 'Cerrar sesión'])

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await el.updateComplete
    expect(q(el, '.menu')).toBeNull()
  })

  it('cierra el menú al hacer clic fuera', async () => {
    const el = await crear('dh-header', { usuario: USUARIO, accionesUsuario: ACCIONES })
    q(el, '.avatar-btn').click()
    await el.updateComplete

    document.body.click()
    await el.updateComplete

    expect(q(el, '.menu')).toBeNull()
  })

  it('una acción con ruta emite dh-navegar y una sin ruta emite dh-accion', async () => {
    const el = await crear('dh-header', { usuario: USUARIO, accionesUsuario: ACCIONES })
    const alNavegar = vi.fn()
    const alAccion = vi.fn()
    el.addEventListener('dh-navegar', (e) => alNavegar(e.detail.ruta))
    el.addEventListener('dh-accion', (e) => alAccion(e.detail.id))

    q(el, '.avatar-btn').click()
    await el.updateComplete
    qa(el, '[role="menuitem"]')[0].click()
    await el.updateComplete
    q(el, '.avatar-btn').click()
    await el.updateComplete
    qa(el, '[role="menuitem"]')[1].click()

    expect(alNavegar).toHaveBeenCalledWith('/perfil')
    expect(alAccion).toHaveBeenCalledWith('cerrar-sesion')
  })

  it('la hamburguesa emite dh-menu; la campana y el buscador solo si se piden', async () => {
    const el = await crear('dh-header')
    const alMenu = vi.fn()
    el.addEventListener('dh-menu', alMenu)
    q(el, '.hamburguesa').click()
    expect(alMenu).toHaveBeenCalled()
    expect(q(el, '[part="avisos"]')).toBeNull()
    expect(q(el, '.buscador')).toBeNull()

    el.avisos = 2
    el.buscador = true
    await el.updateComplete
    expect(q(el, '[part="avisos"]').classList.contains('punto')).toBe(true)
    expect(q(el, '.buscador')).not.toBeNull()
  })
})

describe('dh-shell', () => {
  it('compone el menú y la cabecera con las propiedades recibidas y deja pasar el contenido', async () => {
    const el = await crear(
      'dh-shell',
      { items: ITEMS, activo: '/escuelas', usuario: USUARIO, accionesUsuario: ACCIONES, migas: [{ etiqueta: 'Escuelas' }], etiquetaGrupo: 'Plataforma', recordarColapso: false },
      '<p id="pantalla">Contenido</p><button slot="header-acciones" id="extra">Extra</button>',
    )
    await new Promise((r) => setTimeout(r))
    const menu = q(el, 'dh-sidebar')
    const cabecera = q(el, 'dh-header')

    expect(menu.items).toBe(ITEMS)
    expect(menu.activo).toBe('/escuelas')
    expect(menu.etiquetaGrupo).toBe('Plataforma')
    expect(cabecera.usuario).toBe(USUARIO)
    expect(cabecera.migas).toEqual([{ etiqueta: 'Escuelas' }])
    expect(el.querySelector('#pantalla')).not.toBeNull()
    expect(q(el, 'slot:not([name])')).not.toBeNull()
    expect(q(el, 'slot[name="header-acciones"]')).not.toBeNull()
  })

  it('el menú muestra el logo y el nombre de la marca (el slot vacío no debe taparlos)', async () => {
    const el = await crear('dh-shell', { items: ITEMS, recordarColapso: false })
    await new Promise((r) => setTimeout(r))
    const menu = q(el, 'dh-sidebar')
    await menu.updateComplete

    expect(q(menu, '.marca svg')).not.toBeNull()
    expect(q(menu, '.marca-nombre').textContent).toBe('DriveHub')
  })

  it('los eventos del menú y de la cabecera llegan al <dh-shell>', async () => {
    const el = await crear('dh-shell', { items: ITEMS, usuario: USUARIO, accionesUsuario: ACCIONES, recordarColapso: false })
    await new Promise((r) => setTimeout(r))
    const alNavegar = vi.fn()
    const alAccion = vi.fn()
    el.addEventListener('dh-navegar', (e) => alNavegar(e.detail.ruta))
    el.addEventListener('dh-accion', (e) => alAccion(e.detail.id))

    qa(q(el, 'dh-sidebar'), 'a.item')[1].dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 }))
    const cabecera = q(el, 'dh-header')
    q(cabecera, '.avatar-btn').click()
    await cabecera.updateComplete
    qa(cabecera, '[role="menuitem"]')[1].click()

    expect(alNavegar).toHaveBeenCalledWith('/escuelas')
    expect(alAccion).toHaveBeenCalledWith('cerrar-sesion')
  })

  it('la hamburguesa abre el cajón y elegir un item lo cierra', async () => {
    const el = await crear('dh-shell', { items: ITEMS, recordarColapso: false })
    await new Promise((r) => setTimeout(r))

    q(q(el, 'dh-header'), '.hamburguesa').click()
    await el.updateComplete
    expect(el.hasAttribute('menu-abierto')).toBe(true)

    qa(q(el, 'dh-sidebar'), 'a.item')[0].dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 }))
    await el.updateComplete
    expect(el.hasAttribute('menu-abierto')).toBe(false)
  })

  it('recuerda si el menú está contraído', async () => {
    localStorage.removeItem('dh-menu-colapsado')
    const el = await crear('dh-shell', { items: ITEMS })
    await new Promise((r) => setTimeout(r))
    q(q(el, 'dh-sidebar'), 'button.contraer').click()
    await el.updateComplete
    expect(localStorage.getItem('dh-menu-colapsado')).toBe('1')

    document.body.innerHTML = ''
    const otro = await crear('dh-shell', { items: ITEMS })
    await new Promise((r) => setTimeout(r))
    expect(q(otro, 'dh-sidebar').colapsado).toBe(true)
    localStorage.removeItem('dh-menu-colapsado')
  })
})

describe('dh-auth-layout', () => {
  it('muestra el título, el mensaje, los puntos y deja pasar el formulario', async () => {
    const el = await crear('dh-auth-layout', { titulo: 'Hola', mensaje: 'Mensaje de prueba' }, '<form id="f"></form>')
    expect(q(el, 'h2').textContent).toBe('Hola')
    expect(q(el, '.copia p').textContent).toBe('Mensaje de prueba')
    expect(qa(el, 'li')).toHaveLength(3)
    expect(el.querySelector('#f')).not.toBeNull()
  })
})

describe('montarShellSuelto', () => {
  it('<dh-dialogo> no pinta nada cerrado; abierto avisa si se acepta o se cancela', async () => {
    const el = await crear('dh-dialogo', { titulo: '¿Quitar tu foto?', texto: 'Volverán las iniciales', aceptar: 'Quitar', peligro: true })
    expect(q(el, '.dialogo')).toBeNull()

    el.abierto = true
    await el.updateComplete
    expect(q(el, 'h2').textContent).toBe('¿Quitar tu foto?')
    expect(q(el, '.btn.peligro').textContent.trim()).toBe('Quitar')

    const aceptar = vi.fn()
    const cancelar = vi.fn()
    el.addEventListener('dh-aceptar', aceptar)
    el.addEventListener('dh-cancelar', cancelar)
    q(el, '.btn.peligro').click()
    q(el, '.btn.ghost').click()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(aceptar).toHaveBeenCalledTimes(1)
    expect(cancelar).toHaveBeenCalledTimes(2)
  })

  it('<dh-dialogo> ocupado no se puede cancelar ni aceptar', async () => {
    const el = await crear('dh-dialogo', { abierto: true, titulo: 'x', ocupado: true })
    const cancelar = vi.fn()
    el.addEventListener('dh-cancelar', cancelar)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(cancelar).not.toHaveBeenCalled()
    expect(qa(el, '.btn').every((b) => b.disabled)).toBe(true)
  })

  it('envuelve la app en un <dh-shell> y devuelve dónde montarla', () => {
    const raiz = document.createElement('div')
    document.body.appendChild(raiz)

    const { shell, contenido } = desdeDesarrollo(raiz, { items: ITEMS, activo: '/' })

    expect(shell.tagName).toBe('DH-SHELL')
    expect(shell.items).toBe(ITEMS)
    expect(shell.contains(contenido)).toBe(true)
    expect(raiz.firstElementChild).toBe(shell)
  })
})
