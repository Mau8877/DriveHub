import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { registrarContextoShell } from '@drivehub/mf-contract'

import './index.js'

const ESQUEMA = {
  recurso: 'escuelas',
  titulo: 'Gestionar escuelas',
  descripcion: 'Escuelas de la plataforma',
  nombreSingular: 'escuela',
  columnas: [
    { campo: 'nombre', titulo: 'Nombre' },
    { campo: 'slug', titulo: 'Slug', secundaria: true },
    {
      campo: 'estado',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        ACTIVA: { etiqueta: 'Activa', tono: 'exito' },
        INACTIVA: { etiqueta: 'Inactiva' },
      },
    },
  ],
  busqueda: { placeholder: 'Buscar por nombre o slug' },
  filtros: [
    {
      clave: 'filter[estado]',
      titulo: 'Estado',
      opciones: [
        { valor: 'ACTIVA', etiqueta: 'Activas' },
        { valor: 'INACTIVA', etiqueta: 'Inactivas' },
      ],
    },
  ],
  crear: {
    etiqueta: 'Nueva escuela',
    mensajeExito: 'Escuela creada correctamente.',
    secciones: [
      {
        titulo: 'Escuela',
        campos: [
          { campo: 'nombre', titulo: 'Nombre', requerido: true, min: 3, max: 120, mensajes: { requerido: 'El nombre es obligatorio' } },
          { campo: 'slug', titulo: 'Slug', requerido: true, min: 3, max: 60, patron: 'slug', sugeridoDe: 'nombre' },
        ],
      },
      {
        titulo: 'Administrador',
        campos: [
          { campo: 'administrador.email', titulo: 'Correo', tipo: 'email', requerido: true },
          { campo: 'administrador.password', titulo: 'Contraseña', tipo: 'password', requerido: true, min: 9 },
          { campo: 'administrador.telefono', titulo: 'Teléfono', tipo: 'telefono', max: 20 },
        ],
      },
    ],
  },
  editar: {
    cargar: 'por-codigo/{id}',
    mensajeExito: 'Escuela actualizada correctamente.',
    visibleSi: { campo: 'estado', igual: 'ACTIVA' },
    bloqueo: { campo: 'activa', valor: false, mensaje: 'La escuela está inactiva. Reactívala para editarla.' },
    secciones: [
      {
        campos: [
          { campo: 'slug', titulo: 'Slug', soloLectura: true },
          { campo: 'nombre', titulo: 'Nombre', requerido: true, min: 3, max: 120 },
        ],
      },
    ],
  },
  acciones: [
    {
      id: 'desactivar',
      etiqueta: 'Desactivar',
      metodo: 'DELETE',
      ruta: '{id}',
      confirmar: '¿Desactivar?',
      mensajeExito: 'Escuela desactivada correctamente.',
      visibleSi: { campo: 'estado', igual: 'ACTIVA' },
      tono: 'peligro',
    },
    {
      id: 'reactivar',
      etiqueta: 'Reactivar',
      metodo: 'POST',
      ruta: '{id}/reactivar',
      mensajeExito: 'Escuela reactivada correctamente.',
      visibleSi: { campo: 'estado', igual: 'INACTIVA' },
    },
  ],
  erroresDeCampo: {
    ERR_ESC_03: { campo: 'slug', mensaje: 'Ese slug ya está en uso' },
    ERR_IDE_01: { campo: 'administrador.email', mensaje: 'Este correo ya está registrado' },
  },
}

const VIVA = { codigo: 'c1', slug: 'autos-sur', nombre: 'Autos del Sur', estado: 'ACTIVA', creadoEn: '2026-10-21T10:00:00Z' }
const MUERTA = { codigo: 'c2', slug: 'viejo', nombre: 'Viejo', estado: 'INACTIVA', creadoEn: '2026-10-20T10:00:00Z' }

const sobre = (data, meta) => new Response(JSON.stringify({ status: 'success', data, ...(meta ? { meta } : {}) }), { status: 200 })
const lista = (filas) => sobre(filas, { page: 1, pageSize: 20, totalItems: filas.length, totalPages: filas.length ? 1 : 0 })
const falla = (estado, error, message = 'x') => new Response(JSON.stringify({ status: 'failed', error, message }), { status: estado })
const esperar = (ms = 0) => new Promise((r) => setTimeout(r, ms))

/** Espera a que el componente termine de pintar y de resolver sus promesas pendientes. */
async function asentar(el) {
  for (let i = 0; i < 4; i++) {
    await esperar()
    await el.updateComplete
  }
}

describe('<dh-gestion>', () => {
  let el

  const texto = () => el.shadowRoot.textContent.replace(/\s+/g, ' ')
  const boton = (t) => Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === t)
  const escribir = async (selector, valor) => {
    const campo = el.shadowRoot.querySelector(selector)
    campo.value = valor
    campo.dispatchEvent(new Event('input', { bubbles: true }))
    campo.dispatchEvent(new Event('blur'))
    await asentar(el)
  }

  async function montar(respuestaInicial = lista([VIVA, MUERTA])) {
    fetch.mockResolvedValueOnce(respuestaInicial)
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA
    await asentar(el)
  }

  beforeEach(() => {
    registrarContextoShell({
      apiBaseUrl: 'http://api.test/v1',
      obtenerToken: () => 't',
      navegar: () => {},
      renovarSesion: async () => null,
    })
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    el?.remove()
    vi.useRealTimers()
    vi.unstubAllGlobals()
    globalThis.__drivehubShell__ = undefined
  })

  it('se registra como custom element', () => {
    expect(customElements.get('dh-gestion')).toBeDefined()
  })

  it('muestra el título, la descripción y una fila por registro con su insignia', async () => {
    await montar()

    expect(texto()).toContain('Gestionar escuelas')
    expect(texto()).toContain('Escuelas de la plataforma')
    const filas = el.shadowRoot.querySelectorAll('tbody tr')
    expect(filas).toHaveLength(2)
    expect(filas[0].textContent).toContain('Autos del Sur')
    expect(filas[0].querySelector('.insignia.exito').textContent).toBe('Activa')
    expect(filas[1].querySelector('.insignia').textContent).toBe('Inactiva')
  })

  it('la primera columna es Nro y numera las filas de 1 a N', async () => {
    await montar()

    const titulos = [...el.shadowRoot.querySelectorAll('thead th')].map((th) => th.textContent)
    const numeros = [...el.shadowRoot.querySelectorAll('tbody td.nro')].map((td) => td.textContent)
    expect(titulos[0]).toBe('Nro')
    expect(numeros).toEqual(['1', '2'])
  })

  it('pide el listado con page, pageSize y los parámetros del esquema', async () => {
    await montar()

    expect(fetch.mock.calls[0][0]).toBe('http://api.test/v1/escuelas?page=1&pageSize=20')
  })

  it('las acciones dependen de visibleSi: Editar y Desactivar en la activa, Reactivar en la inactiva', async () => {
    await montar()
    const [viva, muerta] = Array.from(el.shadowRoot.querySelectorAll('tbody tr'))

    expect(viva.textContent).toContain('Editar')
    expect(viva.textContent).toContain('Desactivar')
    expect(viva.textContent).not.toContain('Reactivar')
    expect(muerta.textContent).toContain('Reactivar')
    expect(muerta.textContent).not.toContain('Editar')
    expect(muerta.textContent).not.toContain('Desactivar')
  })

  it('el filtro de estado consulta con filter[estado] codificado y vuelve a la página 1', async () => {
    await montar()
    fetch.mockResolvedValueOnce(lista([MUERTA]))
    const select = el.shadowRoot.querySelector('select')

    select.value = 'INACTIVA'
    select.dispatchEvent(new Event('change'))
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/escuelas?page=1&pageSize=20&filter%5Bestado%5D=INACTIVA')
    expect(el.shadowRoot.querySelectorAll('tbody tr')).toHaveLength(1)
  })

  it('la búsqueda espera 300 ms de calma antes de consultar', async () => {
    await montar()
    fetch.mockResolvedValue(lista([VIVA]))
    vi.useFakeTimers()
    const buscador = el.shadowRoot.querySelector('input[type="search"]')

    for (const t of ['s', 'su', 'sur']) {
      buscador.value = t
      buscador.dispatchEvent(new Event('input'))
    }
    vi.advanceTimersByTime(299)
    expect(fetch).toHaveBeenCalledTimes(1)
    vi.advanceTimersByTime(1)

    expect(fetch).toHaveBeenCalledTimes(2)
    expect(fetch.mock.calls[1][0]).toContain('search=sur')
  })

  it('desactivar pide confirmación en la fila y no llama hasta confirmar; luego recarga y avisa', async () => {
    await montar()
    boton('Desactivar').click()
    await asentar(el)

    expect(texto()).toContain('¿Desactivar?')
    expect(fetch).toHaveBeenCalledTimes(1)

    fetch.mockResolvedValueOnce(sobre(null)).mockResolvedValueOnce(lista([MUERTA]))
    boton('Confirmar').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/escuelas/c1')
    expect(fetch.mock.calls[1][1].method).toBe('DELETE')
    expect(texto()).toContain('Escuela desactivada correctamente.')
  })

  it('cancelar la confirmación no desactiva nada', async () => {
    await montar()
    boton('Desactivar').click()
    await asentar(el)

    boton('Cancelar').click()
    await asentar(el)

    expect(texto()).not.toContain('¿Desactivar?')
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('reactivar no pide confirmación (no la define el esquema)', async () => {
    await montar()
    fetch.mockResolvedValueOnce(sobre(VIVA)).mockResolvedValueOnce(lista([VIVA]))

    boton('Reactivar').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/escuelas/c2/reactivar')
    expect(fetch.mock.calls[1][1].method).toBe('POST')
    expect(texto()).toContain('Escuela reactivada correctamente.')
  })

  it('si una acción falla, muestra el mensaje del servidor', async () => {
    await montar()
    boton('Desactivar').click()
    await asentar(el)
    fetch.mockResolvedValueOnce(falla(409, 'ERR_ESC_04', 'La escuela ya estaba inactiva')).mockResolvedValueOnce(lista([MUERTA]))

    boton('Confirmar').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('.aviso.error').textContent).toContain('ya estaba inactiva')
  })

  it('estados vacío, sin resultados y error con reintento', async () => {
    await montar(lista([]))
    expect(texto()).toContain('Todavía no hay registros.')

    el.remove()
    await montar(falla(500, 'ERR_SYS_00', 'Ocurrió un error inesperado'))
    expect(texto()).toContain('Ocurrió un error inesperado')

    fetch.mockResolvedValueOnce(lista([VIVA]))
    boton('Reintentar').click()
    await asentar(el)
    expect(el.shadowRoot.querySelectorAll('tbody tr')).toHaveLength(1)
  })

  it('la paginación usa meta: deshabilita Anterior en la primera y pide la siguiente', async () => {
    await montar(sobre([VIVA], { page: 1, pageSize: 20, totalItems: 25, totalPages: 2 }))

    expect(texto()).toContain('Página 1 de 2')
    expect(boton('Anterior').disabled).toBe(true)
    fetch.mockResolvedValueOnce(lista([MUERTA]))
    boton('Siguiente').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toContain('page=2')
  })

  // ------------------------------------------------------------------ crear

  async function abrirCrear() {
    await montar()
    boton('Nueva escuela').click()
    await asentar(el)
  }

  it('crear abre el formulario con sus secciones; sugiere el slug desde el nombre hasta que se edita', async () => {
    await abrirCrear()

    expect(el.shadowRoot.querySelectorAll('legend')).toHaveLength(2)
    await escribir('#campo-nombre', 'Autos del Sur')
    expect(el.shadowRoot.querySelector('#campo-slug').value).toBe('autos-del-sur')

    await escribir('#campo-slug', 'mi-slug')
    await escribir('#campo-nombre', 'Otro Nombre')
    expect(el.shadowRoot.querySelector('#campo-slug').value).toBe('mi-slug')
  })

  it('un formulario inválido no se envía y muestra los errores bajo cada campo', async () => {
    await abrirCrear()

    boton('Crear').click()
    await asentar(el)

    expect(fetch).toHaveBeenCalledTimes(1)
    expect(texto()).toContain('El nombre es obligatorio')
    expect(texto()).toContain('Este campo es obligatorio')
    await escribir('#campo-administrador-email', 'no-es-correo')
    expect(texto()).toContain('El correo no es válido')
  })

  it('envía el cuerpo anidado por los puntos, sin recortar la contraseña y sin campos opcionales vacíos', async () => {
    await abrirCrear()
    await escribir('#campo-nombre', '  Autos del Sur ')
    await escribir('#campo-administrador-email', 'marta@gmail.com')
    await escribir('#campo-administrador-password', ' 123456789 ')
    fetch.mockResolvedValueOnce(sobre({ codigo: 'c9' })).mockResolvedValueOnce(lista([VIVA]))

    boton('Crear').click()
    await asentar(el)

    const [url, init] = fetch.mock.calls[1]
    expect(url).toBe('http://api.test/v1/escuelas')
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body)).toEqual({
      nombre: 'Autos del Sur',
      slug: 'autos-del-sur',
      administrador: { email: 'marta@gmail.com', password: ' 123456789 ' },
    })
    expect(texto()).toContain('Escuela creada correctamente.')
    expect(el.shadowRoot.querySelector('form')).toBeNull()
  })

  it.each([
    ['ERR_ESC_03', 'Ese slug ya está en uso'],
    ['ERR_IDE_01', 'Este correo ya está registrado'],
  ])('el error %s se muestra bajo su campo y no se pierde lo escrito', async (codigo, mensaje) => {
    await abrirCrear()
    await escribir('#campo-nombre', 'Autos del Sur')
    await escribir('#campo-administrador-email', 'marta@gmail.com')
    await escribir('#campo-administrador-password', '123456789')
    fetch.mockResolvedValueOnce(falla(409, codigo))

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain(mensaje)
    expect(el.shadowRoot.querySelector('#campo-nombre').value).toBe('Autos del Sur')
  })

  it('cualquier otro error se muestra como aviso general del formulario', async () => {
    await abrirCrear()
    await escribir('#campo-nombre', 'Autos del Sur')
    await escribir('#campo-administrador-email', 'marta@gmail.com')
    await escribir('#campo-administrador-password', '123456789')
    fetch.mockResolvedValueOnce(falla(500, 'ERR_SYS_00'))

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('No pudimos guardar')
  })

  it('la contraseña está oculta salvo que se pida verla', async () => {
    await abrirCrear()
    const campo = el.shadowRoot.querySelector('#campo-administrador-password')
    expect(campo.type).toBe('password')

    boton('Mostrar').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('#campo-administrador-password').type).toBe('text')
  })

  it('Cancelar vuelve al listado sin guardar', async () => {
    await abrirCrear()

    fetch.mockResolvedValueOnce(lista([VIVA]))
    boton('Cancelar').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('form')).toBeNull()
    expect(texto()).toContain('Gestionar escuelas')
  })

  // ------------------------------------------------------------------ editar

  it('editar carga el registro por la ruta del esquema, muestra el slug sin poder editarlo y guarda solo lo editable', async () => {
    await montar()
    fetch.mockResolvedValueOnce(sobre({ codigo: 'c1', slug: 'autos-sur', nombre: 'Autos del Sur', activa: true }))

    boton('Editar').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/escuelas/por-codigo/c1')
    expect(el.shadowRoot.querySelector('#campo-slug').readOnly).toBe(true)
    expect(el.shadowRoot.querySelector('#campo-nombre').value).toBe('Autos del Sur')

    await escribir('#campo-nombre', ' Nombre Nuevo ')
    fetch.mockResolvedValueOnce(sobre({})).mockResolvedValueOnce(lista([VIVA]))
    boton('Guardar').click()
    await asentar(el)

    const [url, init] = fetch.mock.calls[2]
    expect(url).toBe('http://api.test/v1/escuelas/c1')
    expect(init.method).toBe('PATCH')
    expect(JSON.parse(init.body)).toEqual({ nombre: 'Nombre Nuevo' })
    expect(texto()).toContain('Escuela actualizada correctamente.')
  })

  it('si el registro cumple el bloqueo no muestra el formulario sino el mensaje', async () => {
    await montar()
    fetch.mockResolvedValueOnce(sobre({ codigo: 'c1', slug: 'autos-sur', nombre: 'Autos', activa: false }))

    boton('Editar').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('form')).toBeNull()
    expect(texto()).toContain('La escuela está inactiva. Reactívala para editarla.')
  })

  it('si no se puede cargar el registro, muestra el error del servidor', async () => {
    await montar()
    fetch.mockResolvedValueOnce(falla(404, 'ERR_ESC_02', 'La escuela no existe'))

    boton('Editar').click()
    await asentar(el)

    expect(texto()).toContain('La escuela no existe')
  })

  it('un esquema nuevo reinicia la pantalla y vuelve a cargar', async () => {
    await montar()
    fetch.mockResolvedValueOnce(lista([]))

    el.esquema = { ...ESQUEMA, recurso: 'vehiculos', titulo: 'Vehículos' }
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toContain('/vehiculos?')
    expect(texto()).toContain('Vehículos')
  })
})

describe('<dh-gestion> campos numero y seleccion', () => {
  let el

  beforeEach(() => {
    registrarContextoShell({
      apiBaseUrl: 'http://api.test/v1',
      obtenerToken: () => 't',
      navegar: () => {},
      renovarSesion: async () => null,
    })
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    el?.remove()
    vi.unstubAllGlobals()
    globalThis.__drivehubShell__ = undefined
  })

  it('el desplegable ofrece sus opciones y el número viaja como número en el cuerpo', async () => {
    fetch.mockResolvedValueOnce(lista([]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = {
      recurso: 'vehiculos',
      titulo: 'Autos',
      columnas: [{ campo: 'placa', titulo: 'Placa' }],
      crear: {
        etiqueta: 'Nuevo auto',
        mensajeExito: 'Listo',
        etiquetaEnviar: 'Registrar',
        secciones: [
          {
            campos: [
              { campo: 'placa', titulo: 'Placa', requerido: true },
              {
                campo: 'transmision',
                titulo: 'Transmisión',
                tipo: 'seleccion',
                requerido: true,
                opciones: [
                  { valor: 'MANUAL', etiqueta: 'Manual' },
                  { valor: 'AUTOMATICO', etiqueta: 'Automática' },
                ],
              },
              { campo: 'kilometraje', titulo: 'Kilometraje', tipo: 'numero', min: 0 },
            ],
          },
        ],
      },
    }
    await asentar(el)
    Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === 'Nuevo auto').click()
    await asentar(el)

    const select = el.shadowRoot.querySelector('#campo-transmision')
    expect(Array.from(select.options).map((o) => o.value)).toEqual(['', 'MANUAL', 'AUTOMATICO'])
    expect(el.shadowRoot.querySelector('#campo-kilometraje').type).toBe('number')

    const poner = async (selector, valor, evento) => {
      const campo = el.shadowRoot.querySelector(selector)
      campo.value = valor
      campo.dispatchEvent(new Event(evento, { bubbles: true }))
      campo.dispatchEvent(new Event('blur'))
      await asentar(el)
    }
    await poner('#campo-placa', 'ABC-123', 'input')
    await poner('#campo-transmision', 'AUTOMATICO', 'change')
    await poner('#campo-kilometraje', '1500', 'input')
    fetch.mockResolvedValueOnce(sobre({ codigo: 'v1' })).mockResolvedValueOnce(lista([]))

    Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === 'Registrar').click()
    await asentar(el)

    const [, init] = fetch.mock.calls[1]
    expect(JSON.parse(init.body)).toEqual({ placa: 'ABC-123', transmision: 'AUTOMATICO', kilometraje: 1500 })
  })
})

describe('<dh-gestion> campo y columna imagen', () => {
  let el
  const FOTO = 'https://res.cloudinary.com/nube/image/upload/v1/drivehub/e1/vehiculos/auto.jpg'
  const boton = (t) => Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === t)

  const ESQUEMA_AUTOS = {
    recurso: 'vehiculos',
    titulo: 'Autos',
    columnas: [
      { campo: 'imagenUrl', titulo: 'Foto', tipo: 'imagen' },
      { campo: 'placa', titulo: 'Placa' },
    ],
    crear: {
      etiqueta: 'Nuevo auto',
      mensajeExito: 'Listo',
      etiquetaEnviar: 'Registrar',
      secciones: [
        {
          campos: [
            { campo: 'placa', titulo: 'Placa', requerido: true },
            { campo: 'imagenUrl', titulo: 'Foto', tipo: 'imagen', firma: 'vehiculos/firma-imagen' },
          ],
        },
      ],
    },
  }

  beforeEach(() => {
    registrarContextoShell({
      apiBaseUrl: 'http://api.test/v1',
      obtenerToken: () => 't',
      navegar: () => {},
      renovarSesion: async () => null,
    })
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    el?.remove()
    vi.unstubAllGlobals()
    globalThis.__drivehubShell__ = undefined
  })

  async function montar() {
    fetch.mockResolvedValueOnce(lista([{ codigo: 'v1', placa: 'ABC-123', imagenUrl: FOTO }]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_AUTOS
    await asentar(el)
  }

  async function abrirCrearYElegir(archivo) {
    await montar()
    boton('Nuevo auto').click()
    await asentar(el)
    const entrada = el.shadowRoot.querySelector('#campo-imagenUrl')
    Object.defineProperty(entrada, 'files', { value: [archivo], configurable: true })
    entrada.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)
  }

  it('la columna imagen muestra una miniatura recortada de Cloudinary', async () => {
    await montar()

    const img = el.shadowRoot.querySelector('img.miniatura')
    expect(img.getAttribute('src')).toContain('/image/upload/c_fill,w_96,h_64,f_auto,q_auto/v1/')
  })

  it('sube la foto con la firma del backend y deja la URL en el cuerpo del formulario', async () => {
    fetch.mockImplementation(async () => lista([]))
    const archivo = new File(['x'], 'auto.png', { type: 'image/png' })
    await montar()
    boton('Nuevo auto').click()
    await asentar(el)
    fetch.mockReset()
    fetch
      .mockResolvedValueOnce(
        sobre({ cloudName: 'nube', apiKey: 'k', timestamp: 1700000000, folder: 'drivehub/e1/vehiculos', signature: 'sig' })
      )
      .mockResolvedValueOnce(new Response(JSON.stringify({ secure_url: FOTO }), { status: 200 }))

    const entrada = el.shadowRoot.querySelector('#campo-imagenUrl')
    Object.defineProperty(entrada, 'files', { value: [archivo], configurable: true })
    entrada.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)

    expect(fetch.mock.calls[0][0]).toBe('http://api.test/v1/vehiculos/firma-imagen')
    expect(fetch.mock.calls[0][1].method).toBe('POST')
    const [urlNube, init] = fetch.mock.calls[1]
    expect(urlNube).toBe('https://api.cloudinary.com/v1_1/nube/image/upload')
    expect(init.body.get('signature')).toBe('sig')
    expect(init.body.get('api_key')).toBe('k')
    expect(init.body.get('folder')).toBe('drivehub/e1/vehiculos')
    expect(init.body.get('file').name).toBe('auto.png')
    expect(el.shadowRoot.querySelector('img.vista-previa')).not.toBeNull()

    const placa = el.shadowRoot.querySelector('#campo-placa')
    placa.value = 'ABC-123'
    placa.dispatchEvent(new Event('input', { bubbles: true }))
    fetch.mockResolvedValueOnce(sobre({ codigo: 'v2' })).mockResolvedValueOnce(lista([]))
    boton('Registrar').click()
    await asentar(el)
    expect(JSON.parse(fetch.mock.calls[2][1].body)).toEqual({ placa: 'ABC-123', imagenUrl: FOTO })
  })

  it('rechaza lo que no es imagen o pesa más de 5 MB sin llamar a nadie', async () => {
    await abrirCrearYElegir(new File(['x'], 'datos.pdf', { type: 'application/pdf' }))
    expect(el.shadowRoot.textContent).toContain('El archivo debe ser una imagen')

    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.png', { type: 'image/png' })
    const entrada = el.shadowRoot.querySelector('#campo-imagenUrl')
    Object.defineProperty(entrada, 'files', { value: [grande], configurable: true })
    entrada.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)

    expect(el.shadowRoot.textContent).toContain('no puede pesar más de 5 MB')
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('si Cloudinary rechaza la subida, avisa y no deja URL', async () => {
    await montar()
    boton('Nuevo auto').click()
    await asentar(el)
    fetch
      .mockResolvedValueOnce(sobre({ cloudName: 'nube', apiKey: 'k', timestamp: 1, folder: 'f', signature: 's' }))
      .mockResolvedValueOnce(new Response('{}', { status: 400 }))

    const entrada = el.shadowRoot.querySelector('#campo-imagenUrl')
    Object.defineProperty(entrada, 'files', {
      value: [new File(['x'], 'a.png', { type: 'image/png' })],
      configurable: true,
    })
    entrada.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)

    expect(el.shadowRoot.textContent).toContain('No pudimos subir la imagen')
    expect(el.shadowRoot.querySelector('img.vista-previa')).toBeNull()
  })
})
