import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { analizarFecha, formatearFecha, iniciales } from './index.js'
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

  it('si dos items coinciden, solo queda activo el de la ruta más específica', async () => {
    const items = [
      { ruta: '/', etiqueta: 'Inicio' },
      { ruta: '/escuela/estudiantes', etiqueta: 'Estudiantes' },
      { ruta: '/escuela', etiqueta: 'Escuela' },
    ]
    const el = await crear('dh-sidebar', { items, activo: '/escuela/estudiantes' })
    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela/estudiantes'])

    el.activo = '/escuela/estudiantes/e1?x=1'
    await el.updateComplete
    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela/estudiantes'])

    // * Fuera de la ruta más específica, el item general vuelve a ser el activo
    el.activo = '/escuela'
    await el.updateComplete
    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela'])
    el.activo = '/escuela/otra-cosa'
    await el.updateComplete
    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela'])
  })

  it('una ruta que solo empieza igual no es una subruta: /escuelas no activa /escuela', async () => {
    const items = [{ ruta: '/escuela', etiqueta: 'Escuela' }, { ruta: '/escuelas', etiqueta: 'Gestionar escuelas' }]
    const el = await crear('dh-sidebar', { items, activo: '/escuelas' })

    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuelas'])
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

describe('dh-sidebar: grupos desplegables', () => {
  const ITEMS_GRUPOS = [
    { ruta: '/', etiqueta: 'Inicio', icono: 'home' },
    {
      etiqueta: 'Gestionar usuarios',
      icono: 'users',
      hijos: [
        { ruta: '/escuela/estudiantes', etiqueta: 'Estudiantes' },
        { ruta: '/escuela/instructores', etiqueta: 'Instructores' },
      ],
    },
    {
      etiqueta: 'Gestionar escuela',
      icono: 'school',
      hijos: [{ ruta: '/escuela/autos', etiqueta: 'Autos' }],
    },
  ]
  const botones = (el) => qa(el, 'button.item')
  const grupo = (el, etiqueta) => qa(el, '.padre').find((g) => g.querySelector('button.item strong').textContent === etiqueta)
  const hijosDe = (el, etiqueta) => [...grupo(el, etiqueta).querySelectorAll('a.item.hijo')]

  it('un item con hijos es un botón padre (no un enlace) con sus opciones adentro', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/' })

    expect(botones(el).map((b) => b.querySelector('strong').textContent)).toEqual(['Gestionar usuarios', 'Gestionar escuela'])
    expect(qa(el, 'a.item:not(.hijo)').map((a) => a.getAttribute('href'))).toEqual(['/'])
    expect(hijosDe(el, 'Gestionar usuarios').map((a) => a.textContent.trim())).toEqual(['Estudiantes', 'Instructores'])
    expect(hijosDe(el, 'Gestionar usuarios').map((a) => a.getAttribute('href'))).toEqual([
      '/escuela/estudiantes',
      '/escuela/instructores',
    ])
  })

  it('los grupos arrancan cerrados: aria-expanded en false y las opciones fuera del foco (inert)', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/' })

    for (const b of botones(el)) expect(b.getAttribute('aria-expanded')).toBe('false')
    expect(qa(el, '.hijos').every((h) => h.hasAttribute('inert'))).toBe(true)
  })

  it('pulsar el padre abre y cierra su grupo; los demás grupos no se tocan', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/' })

    botones(el)[0].click()
    await el.updateComplete
    expect(botones(el)[0].getAttribute('aria-expanded')).toBe('true')
    expect(grupo(el, 'Gestionar usuarios').classList.contains('abierto')).toBe(true)
    expect(grupo(el, 'Gestionar usuarios').querySelector('.hijos').hasAttribute('inert')).toBe(false)
    expect(botones(el)[1].getAttribute('aria-expanded')).toBe('false')

    botones(el)[1].click()
    await el.updateComplete
    expect(botones(el).map((b) => b.getAttribute('aria-expanded'))).toEqual(['true', 'true'])

    botones(el)[0].click()
    await el.updateComplete
    expect(botones(el).map((b) => b.getAttribute('aria-expanded'))).toEqual(['false', 'true'])
  })

  it('el grupo que contiene la ruta actual arranca abierto, con su opción activa y el padre resaltado', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/escuela/instructores' })

    expect(botones(el).map((b) => b.getAttribute('aria-expanded'))).toEqual(['true', 'false'])
    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela/instructores'])
    expect(q(el, 'a.activo').getAttribute('aria-current')).toBe('page')
    expect(grupo(el, 'Gestionar usuarios').classList.contains('con-activo')).toBe(true)
    expect(grupo(el, 'Gestionar escuela').classList.contains('con-activo')).toBe(false)
  })

  it('al navegar a otro grupo ese se abre solo y el anterior queda como estaba', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/escuela/estudiantes' })

    el.activo = '/escuela/autos/abc?x=1'
    await el.updateComplete

    expect(botones(el).map((b) => b.getAttribute('aria-expanded'))).toEqual(['true', 'true'])
    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela/autos'])
  })

  it('un grupo que la persona cerró a mano no se reabre mientras siga en una de sus opciones', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/escuela/estudiantes' })

    botones(el)[0].click()
    await el.updateComplete
    expect(botones(el)[0].getAttribute('aria-expanded')).toBe('false')

    el.requestUpdate()
    await el.updateComplete
    expect(botones(el)[0].getAttribute('aria-expanded')).toBe('false')
  })

  it('elegir una opción emite dh-navegar con su ruta y pide cerrar el cajón', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/' })
    botones(el)[0].click()
    await el.updateComplete
    const navegar = []
    let cerrar = 0
    el.addEventListener('dh-navegar', (e) => navegar.push(e.detail.ruta))
    el.addEventListener('dh-cerrar', () => (cerrar += 1))

    hijosDe(el, 'Gestionar usuarios')[1].click()

    expect(navegar).toEqual(['/escuela/instructores'])
    expect(cerrar).toBe(1)
  })

  it('con el menú contraído, pulsar un padre expande el menú, abre ese grupo y avisa', async () => {
    const el = await crear('dh-sidebar', { items: ITEMS_GRUPOS, activo: '/', colapsado: true })
    const colapsos = []
    el.addEventListener('dh-colapsar', (e) => colapsos.push(e.detail.colapsado))

    botones(el)[0].click()
    await el.updateComplete

    expect(el.colapsado).toBe(false)
    expect(colapsos).toEqual([false])
    expect(botones(el)[0].getAttribute('aria-expanded')).toBe('true')
  })

  it('los items sueltos y los grupos conviven: solo queda activo el de la ruta más específica', async () => {
    const items = [
      { ruta: '/', etiqueta: 'Inicio' },
      { ruta: '/escuela', etiqueta: 'Escuela' },
      { etiqueta: 'Gestionar usuarios', hijos: [{ ruta: '/escuela/estudiantes', etiqueta: 'Estudiantes' }] },
    ]
    const el = await crear('dh-sidebar', { items, activo: '/escuela/estudiantes' })

    expect(qa(el, 'a.item.activo').map((a) => a.getAttribute('href'))).toEqual(['/escuela/estudiantes'])
  })

  it('una opción con icono lo dibuja delante de su etiqueta; sin icono no deja hueco', async () => {
    const items = [
      {
        etiqueta: 'Gestionar usuarios',
        icono: 'users',
        hijos: [
          { ruta: '/escuela/estudiantes', etiqueta: 'Estudiantes', icono: 'user' },
          { ruta: '/escuela/instructores', etiqueta: 'Instructores' },
        ],
      },
    ]
    const el = await crear('dh-sidebar', { items, activo: '/' })
    const [conIcono, sinIcono] = qa(el, 'a.item.hijo')

    expect(conIcono.querySelector('.icono svg')).not.toBeNull()
    expect(conIcono.firstElementChild.classList.contains('icono')).toBe(true)
    expect(sinIcono.querySelector('.icono')).toBeNull()
  })

  it('una opción con un icono que no existe no rompe el menú', async () => {
    const items = [{ etiqueta: 'G', icono: 'users', hijos: [{ ruta: '/x', etiqueta: 'X', icono: 'no-existe' }] }]
    const el = await crear('dh-sidebar', { items, activo: '/' })

    expect(q(el, 'a.item.hijo strong').textContent).toBe('X')
    expect(q(el, 'a.item.hijo svg')).toBeNull()
  })

  it('un grupo sin hijos se dibuja como un enlace normal', async () => {
    const el = await crear('dh-sidebar', { items: [{ ruta: '/pagos', etiqueta: 'Pagos', hijos: [] }], activo: '/' })

    expect(qa(el, 'button.item')).toHaveLength(0)
    expect(q(el, 'a.item').getAttribute('href')).toBe('/pagos')
  })
})

describe('dh-select', () => {
  const OPCIONES = [
    { valor: 'M', etiqueta: 'Masculino' },
    { valor: 'F', etiqueta: 'Femenino' },
    { valor: 'O', etiqueta: 'Ñandú' },
  ]
  const disparador = (el) => q(el, 'button.disparador')
  const lista = (el) => q(el, 'ul[role="listbox"]')
  const items = (el) => qa(el, 'li[role="option"]')
  const teclear = (el, key, extra = {}) =>
    disparador(el).dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, composed: true, cancelable: true, ...extra }))
  const eventos = (el, nombre) => {
    const recibidos = []
    el.addEventListener(nombre, (e) => recibidos.push(e.detail))
    return recibidos
  }

  it('muestra la etiqueta de la opción elegida o, si no hay, el placeholder', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, valor: 'F' })
    expect(q(el, '.texto').textContent).toBe('Femenino')
    expect(q(el, '.texto').classList.contains('vacio')).toBe(false)

    el.valor = ''
    await el.updateComplete
    expect(q(el, '.texto').textContent).toBe('Elige una opción')
    expect(q(el, '.texto').classList.contains('vacio')).toBe(true)
  })

  it('es un combobox accesible: nombre, aria-expanded y lista con role listbox', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, etiqueta: 'Sexo' })
    expect(disparador(el).getAttribute('role')).toBe('combobox')
    expect(disparador(el).getAttribute('aria-label')).toBe('Sexo')
    expect(disparador(el).getAttribute('aria-expanded')).toBe('false')
    expect(lista(el)).toBeNull()

    disparador(el).click()
    await el.updateComplete

    expect(disparador(el).getAttribute('aria-expanded')).toBe('true')
    expect(lista(el)).not.toBeNull()
    expect(items(el).map((i) => i.textContent.trim())).toEqual(['Elige una opción', 'Masculino', 'Femenino', 'Ñandú'])
    expect(items(el)[0].getAttribute('aria-selected')).toBe('true')
  })

  it('no usa un <select> nativo', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    disparador(el).click()
    await el.updateComplete

    expect(el.shadowRoot.querySelector('select, option')).toBeNull()
  })

  it('elegir con el clic emite dh-cambio, cierra la lista y muestra la elegida', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const cambios = eventos(el, 'dh-cambio')
    disparador(el).click()
    await el.updateComplete

    items(el)[2].click()
    await el.updateComplete

    expect(cambios).toEqual([{ valor: 'F' }])
    expect(el.valor).toBe('F')
    expect(lista(el)).toBeNull()
    expect(q(el, '.texto').textContent).toBe('Femenino')
  })

  it('elegir lo que ya estaba elegido no emite nada', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, valor: 'M' })
    const cambios = eventos(el, 'dh-cambio')
    disparador(el).click()
    await el.updateComplete

    items(el)[1].click()
    await el.updateComplete

    expect(cambios).toEqual([])
  })

  it('la primera opción (el placeholder) quita la elección', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, valor: 'M' })
    const cambios = eventos(el, 'dh-cambio')
    disparador(el).click()
    await el.updateComplete

    items(el)[0].click()
    await el.updateComplete

    expect(cambios).toEqual([{ valor: '' }])
    expect(q(el, '.texto').textContent).toBe('Elige una opción')
  })

  it('con sin-vacio no ofrece quitar la elección', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, sinVacio: true })
    disparador(el).click()
    await el.updateComplete

    expect(items(el).map((i) => i.textContent.trim())).toEqual(['Masculino', 'Femenino', 'Ñandú'])
  })

  it('marca la opción elegida y resalta la activa al abrir', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, valor: 'F' })
    disparador(el).click()
    await el.updateComplete

    expect(items(el)[2].getAttribute('aria-selected')).toBe('true')
    expect(items(el)[2].classList.contains('activa')).toBe(true)
    expect(items(el)[2].querySelector('.marca')).not.toBeNull()
    expect(items(el)[1].getAttribute('aria-selected')).toBe('false')
  })

  it('teclado: flechas abren y mueven, Enter elige', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const cambios = eventos(el, 'dh-cambio')

    teclear(el, 'ArrowDown')
    await el.updateComplete
    expect(lista(el)).not.toBeNull()
    expect(items(el)[0].classList.contains('activa')).toBe(true)

    teclear(el, 'ArrowDown')
    teclear(el, 'ArrowDown')
    await el.updateComplete
    expect(items(el)[2].classList.contains('activa')).toBe(true)

    teclear(el, 'ArrowUp')
    await el.updateComplete
    expect(items(el)[1].classList.contains('activa')).toBe(true)

    teclear(el, 'Enter')
    await el.updateComplete
    expect(cambios).toEqual([{ valor: 'M' }])
    expect(lista(el)).toBeNull()
  })

  it('teclado: Home y End van a la primera y a la última; las flechas no se pasan de los extremos', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    teclear(el, 'ArrowDown')
    await el.updateComplete

    teclear(el, 'End')
    await el.updateComplete
    expect(items(el)[3].classList.contains('activa')).toBe(true)
    teclear(el, 'ArrowDown')
    await el.updateComplete
    expect(items(el)[3].classList.contains('activa')).toBe(true)

    teclear(el, 'Home')
    await el.updateComplete
    expect(items(el)[0].classList.contains('activa')).toBe(true)
    teclear(el, 'ArrowUp')
    await el.updateComplete
    expect(items(el)[0].classList.contains('activa')).toBe(true)
  })

  it('Enter y Espacio abren la lista cerrada', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })

    teclear(el, 'Enter')
    await el.updateComplete
    expect(lista(el)).not.toBeNull()
    teclear(el, 'Escape')
    await el.updateComplete
    teclear(el, ' ')
    await el.updateComplete
    expect(lista(el)).not.toBeNull()
  })

  it('Escape cierra la lista sin que llegue a quien escucha Escape más arriba (como un panel)', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const arriba = vi.fn()
    document.addEventListener('keydown', arriba)
    disparador(el).click()
    await el.updateComplete

    teclear(el, 'Escape')
    await el.updateComplete

    expect(lista(el)).toBeNull()
    expect(arriba).not.toHaveBeenCalled()

    // * Con la lista cerrada, Escape sigue su camino
    teclear(el, 'Escape')
    expect(arriba).toHaveBeenCalledTimes(1)
    document.removeEventListener('keydown', arriba)
  })

  it('Tab cierra la lista', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    disparador(el).click()
    await el.updateComplete

    teclear(el, 'Tab')
    await el.updateComplete

    expect(lista(el)).toBeNull()
  })

  it('escribir salta a la opción que empieza con esas letras, sin tildes ni mayúsculas', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const cambios = eventos(el, 'dh-cambio')

    teclear(el, 'f')
    await el.updateComplete
    expect(cambios).toEqual([{ valor: 'F' }])

    // * Pasan más de 600 ms entre una letra y otra: es una búsqueda nueva
    await new Promise((r) => setTimeout(r, 700))
    teclear(el, 'n')
    await el.updateComplete
    expect(cambios).toEqual([{ valor: 'F' }, { valor: 'O' }])
  })

  it('escribir con la lista abierta mueve la opción activa en vez de elegir', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const cambios = eventos(el, 'dh-cambio')
    teclear(el, 'ArrowDown')
    await el.updateComplete

    teclear(el, 'm')
    await el.updateComplete

    expect(cambios).toEqual([])
    expect(items(el)[1].classList.contains('activa')).toBe(true)
  })

  it('deshabilitado no se abre ni responde al teclado', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, deshabilitado: true })
    expect(disparador(el).disabled).toBe(true)

    disparador(el).click()
    teclear(el, 'ArrowDown')
    await el.updateComplete

    expect(lista(el)).toBeNull()
  })

  it('un clic fuera cierra la lista y uno dentro de la lista no', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    disparador(el).click()
    await el.updateComplete

    lista(el).dispatchEvent(new Event('pointerdown', { bubbles: true, composed: true }))
    await el.updateComplete
    expect(lista(el)).not.toBeNull()

    document.body.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await el.updateComplete
    expect(lista(el)).toBeNull()
  })

  it('cambiar el tamaño de la ventana cierra la lista (queda mal posicionada)', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    disparador(el).click()
    await el.updateComplete

    window.dispatchEvent(new Event('resize'))
    await el.updateComplete

    expect(lista(el)).toBeNull()
  })

  it('un segundo clic en el control cierra la lista', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    disparador(el).click()
    await el.updateComplete
    disparador(el).click()
    await el.updateComplete

    expect(lista(el)).toBeNull()
  })

  it('cuando el foco sale emite dh-blur y cierra la lista', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const salidas = []
    el.addEventListener('dh-blur', () => salidas.push(true))
    disparador(el).click()
    await el.updateComplete

    disparador(el).dispatchEvent(new FocusEvent('focusout', { bubbles: true, composed: true, relatedTarget: null }))
    await el.updateComplete

    expect(salidas).toHaveLength(1)
    expect(lista(el)).toBeNull()
  })

  it('invalido pone el borde de error y avisa con aria-invalid', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, invalido: true })

    expect(el.hasAttribute('invalido')).toBe(true)
    expect(disparador(el).getAttribute('aria-invalid')).toBe('true')
  })

  it('la lista se posiciona con fixed según el control y se abre hacia abajo si entra', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    vi.spyOn(disparador(el), 'getBoundingClientRect').mockReturnValue({ top: 100, bottom: 144, left: 20, width: 300, height: 44, right: 320 })
    disparador(el).click()
    await el.updateComplete

    const estilo = lista(el).getAttribute('style')
    expect(lista(el).classList.contains('arriba')).toBe(false)
    expect(estilo).toContain('left: 20px')
    expect(estilo).toContain('width: 300px')
    expect(estilo).toContain('top: 148px')
  })

  it('se abre hacia arriba cuando abajo no entra y arriba hay más lugar', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES })
    const alto = window.innerHeight
    vi.spyOn(disparador(el), 'getBoundingClientRect').mockReturnValue({ top: alto - 60, bottom: alto - 16, left: 20, width: 300, height: 44, right: 320 })
    disparador(el).click()
    await el.updateComplete

    expect(lista(el).classList.contains('arriba')).toBe(true)
    expect(lista(el).getAttribute('style')).toContain('bottom: 64px')
  })

  it('sin opciones y sin opción vacía muestra "Sin opciones"', async () => {
    const el = await crear('dh-select', { opciones: [], sinVacio: true })
    disparador(el).click()
    await el.updateComplete

    expect(lista(el).textContent).toContain('Sin opciones')
  })

  it('un valor numérico se compara como texto con el de las opciones', async () => {
    const el = await crear('dh-select', { opciones: [{ valor: 1, etiqueta: 'Uno' }], valor: '1' })

    expect(q(el, '.texto').textContent).toBe('Uno')
  })
})

describe('dh-select multiple', () => {
  const OPCIONES = [
    { valor: 'A', etiqueta: 'Moto' },
    { valor: 'B', etiqueta: 'Auto' },
    { valor: 'C', etiqueta: 'Camión' },
    { valor: 'D', etiqueta: 'Bus' },
  ]
  const disparador = (el) => q(el, '.disparador')
  const lista = (el) => q(el, 'ul[role="listbox"]')
  const items = (el) => qa(el, 'li[role="option"]')
  const fichas = (el) => qa(el, '.ficha:not(.mas)').map((f) => f.textContent.trim())
  const teclear = (el, key) =>
    disparador(el).dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, composed: true, cancelable: true }))
  const eventos = (el) => {
    const recibidos = []
    el.addEventListener('dh-cambio', (e) => recibidos.push(e.detail))
    return recibidos
  }

  it('elegir opciones las alterna y el menú sigue abierto', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true })
    const cambios = eventos(el)
    disparador(el).click()
    await el.updateComplete

    items(el)[1].click()
    await el.updateComplete
    expect(lista(el)).not.toBeNull()
    expect(fichas(el)).toEqual(['Auto'])

    items(el)[1].click()
    await el.updateComplete
    expect(fichas(el)).toEqual([])
    expect(cambios).toEqual([{ valores: ['B'] }, { valores: [] }])
  })

  it('no ofrece la opción vacía y marca el estado de cada opción', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, valores: ['C'] })
    disparador(el).click()
    await el.updateComplete

    expect(items(el).map((i) => i.textContent.trim())).toEqual(['Moto', 'Auto', 'Camión', 'Bus'])
    expect(items(el).map((i) => i.getAttribute('aria-selected'))).toEqual(['false', 'false', 'true', 'false'])
    expect(lista(el).getAttribute('aria-multiselectable')).toBe('true')
    expect(items(el)[2].querySelector('.casilla.marcada')).not.toBeNull()
  })

  it('el evento trae los valores en el orden de las opciones, no en el de elección', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true })
    const cambios = eventos(el)
    disparador(el).click()
    await el.updateComplete

    items(el)[3].click()
    items(el)[0].click()
    await el.updateComplete

    expect(cambios.at(-1)).toEqual({ valores: ['A', 'D'] })
    expect(fichas(el)).toEqual(['Moto', 'Bus'])
  })

  it('la × de una ficha la quita sin abrir el menú', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, valores: ['A', 'B'] })
    const cambios = eventos(el)

    qa(el, '.ficha button')[0].click()
    await el.updateComplete

    expect(lista(el)).toBeNull()
    expect(fichas(el)).toEqual(['Auto'])
    expect(cambios).toEqual([{ valores: ['B'] }])
  })

  it('sin nada elegido muestra el placeholder; con más de tres fichas corta con +N', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, placeholder: 'Categorías' })
    expect(q(el, '.texto').textContent).toBe('Categorías')

    el.valores = ['A', 'B', 'C', 'D']
    await el.updateComplete
    expect(fichas(el)).toEqual(['Moto', 'Auto', 'Camión'])
    expect(q(el, '.ficha.mas').textContent.trim()).toBe('+1')
  })

  it('los valores que no están en las opciones se ignoran', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, valores: ['B', 'X'] })
    const cambios = eventos(el)
    expect(fichas(el)).toEqual(['Auto'])

    disparador(el).click()
    await el.updateComplete
    items(el)[0].click()
    await el.updateComplete

    expect(cambios).toEqual([{ valores: ['A', 'B'] }])
  })

  it('con el teclado, Espacio y Enter alternan la opción activa y Backspace quita la última ficha', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true })
    const cambios = eventos(el)

    teclear(el, 'ArrowDown')
    await el.updateComplete
    expect(lista(el)).not.toBeNull()

    teclear(el, ' ')
    teclear(el, 'ArrowDown')
    teclear(el, 'Enter')
    await el.updateComplete
    expect(fichas(el)).toEqual(['Moto', 'Auto'])

    teclear(el, 'Escape')
    await el.updateComplete
    expect(lista(el)).toBeNull()

    teclear(el, 'Backspace')
    await el.updateComplete
    expect(fichas(el)).toEqual(['Moto'])
    expect(cambios.at(-1)).toEqual({ valores: ['A'] })
  })

  it('Backspace con el menú abierto no quita fichas', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, valores: ['A'] })
    disparador(el).click()
    await el.updateComplete

    teclear(el, 'Backspace')
    await el.updateComplete

    expect(fichas(el)).toEqual(['Moto'])
  })

  it('deshabilitado no se abre ni deja quitar fichas', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, valores: ['A'], deshabilitado: true })
    const cambios = eventos(el)

    disparador(el).click()
    teclear(el, 'Backspace')
    qa(el, '.ficha button')[0].click()
    await el.updateComplete

    expect(lista(el)).toBeNull()
    expect(disparador(el).getAttribute('aria-disabled')).toBe('true')
    expect(cambios).toEqual([])
    expect(fichas(el)).toEqual(['Moto'])
  })

  it('es un combobox accesible y no usa un <select> nativo', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true, etiqueta: 'Categorías', invalido: true })

    expect(disparador(el).getAttribute('role')).toBe('combobox')
    expect(disparador(el).getAttribute('aria-label')).toBe('Categorías')
    expect(disparador(el).getAttribute('aria-invalid')).toBe('true')
    expect(el.shadowRoot.querySelector('select, option')).toBeNull()
  })

  it('cuando el foco sale emite dh-blur', async () => {
    const el = await crear('dh-select', { opciones: OPCIONES, multiple: true })
    const salidas = []
    el.addEventListener('dh-blur', () => salidas.push(true))
    disparador(el).click()
    await el.updateComplete

    disparador(el).dispatchEvent(new FocusEvent('focusout', { bubbles: true, composed: true, relatedTarget: null }))
    await el.updateComplete

    expect(salidas).toHaveLength(1)
    expect(lista(el)).toBeNull()
  })
})

describe('dh-fecha', () => {
  const entrada = (el) => q(el, 'input')
  const calendario = (el) => q(el, '.calendario')
  const dias = (el) => qa(el, 'button.dia')
  const titulo = (el) => q(el, 'button.titulo').textContent.trim()
  const celdas = (el) => qa(el, 'button.celda')
  const boton = (el, texto) => qa(el, 'button').find((b) => b.textContent.trim() === texto)
  const diaNumero = (el, n) => dias(el).find((d) => !d.classList.contains('otro-mes') && d.textContent.trim() === String(n))
  const eventos = (el, nombre) => {
    const recibidos = []
    el.addEventListener(nombre, (e) => recibidos.push(e.detail))
    return recibidos
  }
  const escribirTexto = async (el, texto) => {
    entrada(el).value = texto
    entrada(el).dispatchEvent(new Event('input', { bubbles: true, composed: true }))
    await el.updateComplete
  }
  const teclear = async (el, key, extra = {}) => {
    entrada(el).dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, composed: true, cancelable: true, ...extra }))
    await el.updateComplete
  }
  const abrir = async (el) => {
    q(el, 'button.abrir').click()
    await el.updateComplete
  }

  beforeEach(() => {
    // * Hoy fijo: jueves 23 de octubre de 2026 (solo el reloj; los temporizadores siguen reales)
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 9, 23, 12) })
  })
  afterEach(() => vi.useRealTimers())

  it('formatearFecha y analizarFecha convierten entre yyyy-MM-dd y dd/MM/yyyy y rechazan fechas que no existen', () => {
    expect(formatearFecha('2000-05-04')).toBe('04/05/2000')
    expect(formatearFecha('')).toBe('')
    expect(formatearFecha('no-es-fecha')).toBe('')
    expect(analizarFecha('04/05/2000')).toBe('2000-05-04')
    expect(analizarFecha('29/02/2024')).toBe('2024-02-29')
    expect(analizarFecha('29/02/2023')).toBeNull()
    expect(analizarFecha('31/04/2000')).toBeNull()
    expect(analizarFecha('00/01/2000')).toBeNull()
    expect(analizarFecha('1/1/2000')).toBeNull()
  })

  it('muestra la fecha como dd/mm/aaaa o el placeholder si no hay', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    expect(entrada(el).value).toBe('04/05/2000')

    el.valor = ''
    await el.updateComplete
    expect(entrada(el).value).toBe('')
    expect(entrada(el).placeholder).toBe('dd/mm/aaaa')

    el.valor = '1999-12-31'
    await el.updateComplete
    expect(entrada(el).value).toBe('31/12/1999')
  })

  it('no usa un <input type="date"> nativo', async () => {
    const el = await crear('dh-fecha', {})
    await abrir(el)

    expect(el.shadowRoot.querySelector('input[type="date"]')).toBeNull()
    expect(entrada(el).type).toBe('text')
  })

  it('escribir pone las barras solas y emite dh-cambio solo con una fecha completa y real', async () => {
    const el = await crear('dh-fecha', {})
    const cambios = eventos(el, 'dh-cambio')

    await escribirTexto(el, '0405')
    expect(entrada(el).value).toBe('04/05')
    expect(cambios).toEqual([])

    await escribirTexto(el, '04/05/20ab00')
    expect(entrada(el).value).toBe('04/05/2000')
    expect(cambios).toEqual([{ valor: '2000-05-04' }])
    expect(el.valor).toBe('2000-05-04')
  })

  it('una fecha que no existe no se acepta', async () => {
    const el = await crear('dh-fecha', {})
    const cambios = eventos(el, 'dh-cambio')

    await escribirTexto(el, '31022000')

    expect(cambios).toEqual([])
    expect(el.valor).toBe('')
  })

  it('una fecha real escrita fuera de min/max se acepta: el formulario dice qué regla no cumple', async () => {
    const el = await crear('dh-fecha', { max: '2008-10-23' })
    const cambios = eventos(el, 'dh-cambio')

    await escribirTexto(el, '01012020')

    expect(cambios).toEqual([{ valor: '2020-01-01' }])
  })

  it('al salir con un texto incompleto vuelve a la última fecha buena, y con el texto vacío borra la fecha', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    const cambios = eventos(el, 'dh-cambio')

    await escribirTexto(el, '1203')
    entrada(el).dispatchEvent(new FocusEvent('focusout', { bubbles: true, composed: true, relatedTarget: null }))
    await el.updateComplete
    expect(entrada(el).value).toBe('04/05/2000')
    expect(cambios).toEqual([])

    await escribirTexto(el, '')
    entrada(el).dispatchEvent(new FocusEvent('focusout', { bubbles: true, composed: true, relatedTarget: null }))
    await el.updateComplete
    expect(cambios).toEqual([{ valor: '' }])
    expect(el.valor).toBe('')
  })

  it('el botón del calendario lo abre y muestra el mes de la fecha elegida con sus días', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    expect(calendario(el)).toBeNull()

    await abrir(el)

    expect(calendario(el)).not.toBeNull()
    expect(calendario(el).getAttribute('role')).toBe('dialog')
    expect(entrada(el).getAttribute('aria-expanded')).toBe('true')
    expect(titulo(el)).toBe('mayo 2000')
    expect(qa(el, '.semana span').map((s) => s.textContent)).toEqual(['L', 'M', 'X', 'J', 'V', 'S', 'D'])
    // * Mayo de 2000 empieza un lunes: 31 días + 4 de junio para completar la última semana = 35 celdas
    expect(dias(el)).toHaveLength(35)
    expect(diaNumero(el, 4).classList.contains('elegido')).toBe(true)
    expect(diaNumero(el, 4).getAttribute('aria-selected')).toBe('true')
    expect(diaNumero(el, 5).getAttribute('aria-selected')).toBe('false')
  })

  it('sin fecha abre en el mes de hoy y marca el día de hoy', async () => {
    const el = await crear('dh-fecha', {})
    await abrir(el)

    expect(titulo(el)).toBe('octubre 2026')
    expect(diaNumero(el, 23).classList.contains('hoy')).toBe(true)
  })

  it('elegir un día emite dh-cambio, cierra el calendario y escribe la fecha', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    const cambios = eventos(el, 'dh-cambio')
    await abrir(el)

    diaNumero(el, 18).click()
    await el.updateComplete

    expect(cambios).toEqual([{ valor: '2000-05-18' }])
    expect(calendario(el)).toBeNull()
    expect(entrada(el).value).toBe('18/05/2000')
  })

  it('los días de los meses vecinos también se pueden elegir', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    const cambios = eventos(el, 'dh-cambio')
    await abrir(el)

    const junio = dias(el).find((d) => d.classList.contains('otro-mes'))
    expect(junio.getAttribute('aria-label')).toBe('01/06/2000')
    junio.click()
    await el.updateComplete

    expect(cambios).toEqual([{ valor: '2000-06-01' }])
  })

  it('las flechas del encabezado cambian de mes, también entre años', async () => {
    const el = await crear('dh-fecha', { valor: '2000-01-15' })
    await abrir(el)

    q(el, 'button.nav[aria-label="Anterior"]').click()
    await el.updateComplete
    expect(titulo(el)).toBe('diciembre 1999')

    q(el, 'button.nav[aria-label="Siguiente"]').click()
    q(el, 'button.nav[aria-label="Siguiente"]').click()
    await el.updateComplete
    expect(titulo(el)).toBe('febrero 2000')
  })

  it('el título sube de días a meses y a años; elegir un año y un mes baja de vuelta a los días', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    await abrir(el)

    q(el, 'button.titulo').click()
    await el.updateComplete
    expect(titulo(el)).toBe('2000')
    expect(celdas(el).map((c) => c.textContent.trim())).toEqual(['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'])

    q(el, 'button.titulo').click()
    await el.updateComplete
    expect(titulo(el)).toBe('1992 – 2003')
    expect(celdas(el)).toHaveLength(12)

    celdas(el).find((c) => c.textContent.trim() === '1995').click()
    await el.updateComplete
    expect(titulo(el)).toBe('1995')

    celdas(el).find((c) => c.textContent.trim() === 'mar').click()
    await el.updateComplete
    expect(titulo(el)).toBe('marzo 1995')
    expect(dias(el).length).toBeGreaterThanOrEqual(28)
  })

  it('en la vista de años, las flechas pasan de a 12 años', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    await abrir(el)
    q(el, 'button.titulo').click()
    await el.updateComplete
    q(el, 'button.titulo').click()
    await el.updateComplete

    q(el, 'button.nav[aria-label="Anterior"]').click()
    await el.updateComplete

    expect(titulo(el)).toBe('1980 – 1991')
  })

  it('un día fuera de min/max no se puede elegir', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10', min: '2000-05-05', max: '2000-05-20' })
    const cambios = eventos(el, 'dh-cambio')
    await abrir(el)

    expect(diaNumero(el, 4).disabled).toBe(true)
    expect(diaNumero(el, 5).disabled).toBe(false)
    expect(diaNumero(el, 20).disabled).toBe(false)
    expect(diaNumero(el, 21).disabled).toBe(true)
    diaNumero(el, 4).click()
    await el.updateComplete
    expect(cambios).toEqual([])
    expect(calendario(el)).not.toBeNull()
  })

  it('no se puede navegar a meses, ni a años, donde no hay ningún día permitido', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10', min: '2000-05-05', max: '2000-05-20' })
    await abrir(el)

    expect(q(el, 'button.nav[aria-label="Anterior"]').disabled).toBe(true)
    expect(q(el, 'button.nav[aria-label="Siguiente"]').disabled).toBe(true)

    q(el, 'button.titulo').click()
    await el.updateComplete
    expect(celdas(el).filter((c) => !c.disabled).map((c) => c.textContent.trim())).toEqual(['may'])
  })

  it('sin fecha y con hoy fuera del rango, abre en el límite más cercano', async () => {
    const el = await crear('dh-fecha', { max: '2008-10-23' })
    await abrir(el)

    expect(titulo(el)).toBe('octubre 2008')
  })

  it('Hoy elige el día de hoy y se apaga si hoy está fuera del rango', async () => {
    const el = await crear('dh-fecha', {})
    const cambios = eventos(el, 'dh-cambio')
    await abrir(el)

    boton(el, 'Hoy').click()
    await el.updateComplete
    expect(cambios).toEqual([{ valor: '2026-10-23' }])

    const limitado = await crear('dh-fecha', { max: '2008-10-23' })
    await abrir(limitado)
    expect(boton(limitado, 'Hoy').disabled).toBe(true)
  })

  it('Borrar quita la fecha y solo está activo si hay una', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-04' })
    const cambios = eventos(el, 'dh-cambio')
    await abrir(el)

    boton(el, 'Borrar').click()
    await el.updateComplete

    expect(cambios).toEqual([{ valor: '' }])
    expect(entrada(el).value).toBe('')
    await abrir(el)
    expect(boton(el, 'Borrar').disabled).toBe(true)
  })

  it('teclado: la flecha abajo abre; las flechas mueven el día; Enter elige', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10' })
    const cambios = eventos(el, 'dh-cambio')

    await teclear(el, 'ArrowDown')
    expect(calendario(el)).not.toBeNull()
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('10/05/2000')

    await teclear(el, 'ArrowRight')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('11/05/2000')
    await teclear(el, 'ArrowDown')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('18/05/2000')
    await teclear(el, 'ArrowLeft')
    await teclear(el, 'ArrowUp')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('10/05/2000')

    await teclear(el, 'Enter')
    expect(cambios).toEqual([])
    expect(calendario(el)).toBeNull()
  })

  it('teclado: Enter sobre otro día lo elige', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10' })
    const cambios = eventos(el, 'dh-cambio')
    await teclear(el, 'ArrowDown')

    await teclear(el, 'ArrowRight')
    await teclear(el, 'Enter')

    expect(cambios).toEqual([{ valor: '2000-05-11' }])
  })

  it('teclado: RePág y AvPág cambian de mes, con Mayús de año; Inicio y Fin van al lunes y al domingo', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10' })
    await teclear(el, 'ArrowDown')

    await teclear(el, 'PageDown')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('10/06/2000')
    expect(titulo(el)).toBe('junio 2000')
    await teclear(el, 'PageUp', { shiftKey: true })
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('10/06/1999')

    // * 10/06/1999 fue jueves: el lunes de esa semana es el 7 y el domingo el 13
    await teclear(el, 'Home')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('07/06/1999')
    await teclear(el, 'End')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('13/06/1999')
  })

  it('teclado: el foco no se sale del rango permitido', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10', min: '2000-05-05', max: '2000-05-20' })
    await teclear(el, 'ArrowDown')

    await teclear(el, 'PageDown')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('20/05/2000')
    await teclear(el, 'PageUp')
    expect(q(el, 'button.dia.foco').getAttribute('aria-label')).toBe('05/05/2000')
  })

  it('Escape cierra el calendario sin llegar a quien escucha Escape más arriba (como un panel)', async () => {
    const el = await crear('dh-fecha', { valor: '2000-05-10' })
    const arriba = vi.fn()
    document.addEventListener('keydown', arriba)
    await abrir(el)

    await teclear(el, 'Escape')
    expect(calendario(el)).toBeNull()
    expect(arriba).not.toHaveBeenCalled()

    await teclear(el, 'Escape')
    expect(arriba).toHaveBeenCalledTimes(1)
    document.removeEventListener('keydown', arriba)
  })

  it('Tab cierra el calendario', async () => {
    const el = await crear('dh-fecha', {})
    await abrir(el)

    await teclear(el, 'Tab')

    expect(calendario(el)).toBeNull()
  })

  it('deshabilitado no se escribe ni se abre', async () => {
    const el = await crear('dh-fecha', { deshabilitado: true })

    expect(entrada(el).disabled).toBe(true)
    expect(q(el, 'button.abrir').disabled).toBe(true)
    await teclear(el, 'ArrowDown')
    expect(calendario(el)).toBeNull()
  })

  it('un clic fuera lo cierra y uno dentro del calendario no', async () => {
    const el = await crear('dh-fecha', {})
    await abrir(el)

    calendario(el).dispatchEvent(new Event('pointerdown', { bubbles: true, composed: true }))
    await el.updateComplete
    expect(calendario(el)).not.toBeNull()

    document.body.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await el.updateComplete
    expect(calendario(el)).toBeNull()
  })

  it('cambiar el tamaño de la ventana lo cierra (queda mal posicionado)', async () => {
    const el = await crear('dh-fecha', {})
    await abrir(el)

    window.dispatchEvent(new Event('resize'))
    await el.updateComplete

    expect(calendario(el)).toBeNull()
  })

  it('al salir emite dh-blur y cierra el calendario', async () => {
    const el = await crear('dh-fecha', {})
    const salidas = []
    el.addEventListener('dh-blur', () => salidas.push(true))
    await abrir(el)

    entrada(el).dispatchEvent(new FocusEvent('focusout', { bubbles: true, composed: true, relatedTarget: null }))
    await el.updateComplete

    expect(salidas).toHaveLength(1)
    expect(calendario(el)).toBeNull()
  })

  it('invalido pone el borde de error y avisa con aria-invalid', async () => {
    const el = await crear('dh-fecha', { invalido: true, etiqueta: 'Fecha de nacimiento' })

    expect(el.hasAttribute('invalido')).toBe(true)
    expect(entrada(el).getAttribute('aria-invalid')).toBe('true')
    expect(entrada(el).getAttribute('aria-label')).toBe('Fecha de nacimiento')
  })

  it('se posiciona con fixed según el campo y se abre hacia arriba si abajo no entra', async () => {
    const el = await crear('dh-fecha', {})
    vi.spyOn(q(el, '.campo'), 'getBoundingClientRect').mockReturnValue({ top: 100, bottom: 144, left: 20, width: 300, height: 44, right: 320 })
    await abrir(el)
    expect(calendario(el).classList.contains('arriba')).toBe(false)
    expect(calendario(el).getAttribute('style')).toContain('top: 148px')
    q(el, 'button.abrir').click()
    await el.updateComplete

    const alto = window.innerHeight
    q(el, '.campo').getBoundingClientRect.mockReturnValue({ top: alto - 60, bottom: alto - 16, left: 20, width: 300, height: 44, right: 320 })
    await abrir(el)
    expect(calendario(el).classList.contains('arriba')).toBe(true)
    expect(calendario(el).getAttribute('style')).toContain('bottom: 64px')
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
