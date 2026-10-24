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
      icono: 'power',
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

    boton('Inactivas').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/escuelas?page=1&pageSize=20&filter%5Bestado%5D=INACTIVA')
    expect(el.shadowRoot.querySelectorAll('tbody tr')).toHaveLength(1)
    expect(boton('Inactivas').getAttribute('aria-pressed')).toBe('true')
    expect(boton('Todas').getAttribute('aria-pressed')).toBe('false')
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

  it('desactivar pide confirmación en un diálogo y no llama hasta confirmar; luego recarga y avisa', async () => {
    await montar()
    boton('Desactivar').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('.dialogo').textContent).toContain('¿Desactivar?')
    expect(fetch).toHaveBeenCalledTimes(1)

    fetch.mockResolvedValueOnce(sobre(null)).mockResolvedValueOnce(lista([MUERTA]))
    el.shadowRoot.querySelector('.dialogo .boton.peligro').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/escuelas/c1')
    expect(fetch.mock.calls[1][1].method).toBe('DELETE')
    expect(texto()).toContain('Escuela desactivada correctamente.')
  })

  it('cancelar la confirmación no desactiva nada', async () => {
    await montar()
    boton('Desactivar').click()
    await asentar(el)

    el.shadowRoot.querySelector('.dialogo .boton.secundario').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('.dialogo')).toBeNull()
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

    el.shadowRoot.querySelector('.dialogo .boton.peligro').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('.toast.error').textContent).toContain('ya estaba inactiva')
  })

  it('estados vacío, sin resultados y error con reintento', async () => {
    await montar(lista([]))
    expect(texto()).toContain('Todavía no hay registros')

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

    expect(texto()).toContain('Mostrando 1–20 de 25')
    const anterior = el.shadowRoot.querySelector('[aria-label="Página anterior"]')
    expect(anterior.disabled).toBe(true)
    fetch.mockResolvedValueOnce(lista([MUERTA]))
    el.shadowRoot.querySelector('[aria-label="Página siguiente"]').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toContain('page=2')
  })

  it('las acciones y el botón de crear llevan su icono', async () => {
    await montar()
    const raiz = el.shadowRoot
    expect(raiz.querySelector('.cabecera .boton svg')).not.toBeNull()
    expect(boton('Editar').querySelector('svg')).not.toBeNull()
    expect(boton('Desactivar').querySelector('svg')).not.toBeNull()
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

    el.shadowRoot.querySelector('[aria-label="Mostrar contraseña"]').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('#campo-administrador-password').type).toBe('text')
  })

  it('crear y editar se abren en un panel lateral sobre el listado, que sigue visible', async () => {
    await abrirCrear()

    const drawer = el.shadowRoot.querySelector('aside.drawer')
    expect(drawer).not.toBeNull()
    expect(drawer.getAttribute('role')).toBe('dialog')
    expect(drawer.querySelector('form')).not.toBeNull()
    expect(el.shadowRoot.querySelectorAll('tbody tr')).toHaveLength(2)
    expect(drawer.querySelectorAll('.paso')).toHaveLength(2)
  })

  it('Escape y el clic en el fondo cierran el panel', async () => {
    await abrirCrear()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()

    boton('Nueva escuela').click()
    await asentar(el)
    const fondo = el.shadowRoot.querySelector('.fondo')
    fondo.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()
  })

  it('Cancelar vuelve al listado sin guardar', async () => {
    await abrirCrear()

    boton('Cancelar').click()
    await asentar(el)

    expect(fetch).toHaveBeenCalledTimes(1)
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

  // ------------------------------------------------------------------ campos seleccion y ubicacion

  const ESQUEMA_PERFIL = {
    recurso: 'estudiantes',
    titulo: 'Gestionar estudiantes',
    nombreSingular: 'estudiante',
    columnas: [{ campo: 'nombreCompleto', titulo: 'Nombre' }],
    crear: {
      etiqueta: 'Nuevo estudiante',
      mensajeExito: 'Estudiante creado correctamente.',
      secciones: [
        {
          titulo: 'Datos',
          campos: [
            {
              campo: 'sexo',
              titulo: 'Sexo',
              tipo: 'seleccion',
              requerido: true,
              opciones: [
                { valor: 'MASCULINO', etiqueta: 'Masculino' },
                { valor: 'FEMENINO', etiqueta: 'Femenino' },
              ],
              mensajes: { requerido: 'Elige el sexo' },
            },
            {
              campo: 'categoriaSigla',
              titulo: 'Categoría',
              tipo: 'seleccion',
              porDefecto: 'SIN',
              opcionesDe: { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion' },
            },
            {
              campo: 'administrador.ubicacion',
              titulo: 'Ubicación',
              tipo: 'ubicacion',
              requerido: true,
              mensajes: { requerido: 'Elige la ubicación en el mapa' },
            },
          ],
        },
      ],
    },
    editar: {
      mensajeExito: 'Estudiante actualizado correctamente.',
      secciones: [
        {
          campos: [
            { campo: 'ci', titulo: 'CI', soloLectura: true },
            { campo: 'categoriaSigla', titulo: 'Categoría', tipo: 'seleccion', opcionesDe: { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion' } },
            { campo: 'ubicacion', titulo: 'Ubicación', tipo: 'ubicacion', requerido: true, direccionDe: 'direccion' },
          ],
        },
      ],
    },
    erroresDeCampo: { ERR_IDE_21: { campo: 'categoriaSigla', mensaje: 'La categoría ya no existe' } },
  }

  const CATEGORIAS = [
    { sigla: 'P', descripcion: 'Particular' },
    { sigla: 'B', descripcion: 'Profesional B' },
    { sigla: 'SIN', descripcion: 'Sin categoría' },
  ]
  const ESTUDIANTE = { codigo: 'e1', nombreCompleto: 'Ana Pérez', ci: 'AB12345', categoriaSigla: 'B', latitud: -16.5, longitud: -68.1, direccion: 'Av. Arce 2345, La Paz' }

  async function montarPerfil() {
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_PERFIL
    await asentar(el)
  }

  async function abrirCrearPerfil() {
    await montarPerfil()
    fetch.mockResolvedValueOnce(lista(CATEGORIAS))
    boton('Nuevo estudiante').click()
    await asentar(el)
  }

  const mapa = () => el.shadowRoot.querySelector('dh-mapa-ubicacion')
  const listaDe = (id) => el.shadowRoot.querySelector(`#${id}`)
  const etiquetasDe = (id) => listaDe(id).opciones.map((o) => o.etiqueta)
  // * Lo que hace <dh-select> al elegir una opción (el componente tiene sus propios tests en @drivehub/ui)
  const elegir = async (id, valor) => {
    listaDe(id).dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor }, bubbles: true, composed: true }))
    await asentar(el)
  }

  it('seleccion con opciones fijas dibuja una lista <dh-select> con sus etiquetas, no un select nativo', async () => {
    await abrirCrearPerfil()

    expect(listaDe('campo-sexo').tagName).toBe('DH-SELECT')
    expect(el.shadowRoot.querySelector('select')).toBeNull()
    expect(etiquetasDe('campo-sexo')).toEqual(['Masculino', 'Femenino'])
    expect(listaDe('campo-sexo').placeholder).toBe('Elige una opción')
    expect(listaDe('campo-sexo').etiqueta).toBe('Sexo')
  })

  it('seleccion con opcionesDe pide el recurso con pageSize 100 y dibuja sus etiquetas', async () => {
    await abrirCrearPerfil()

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/categorias-licencia?page=1&pageSize=100')
    expect(etiquetasDe('campo-categoriaSigla')).toEqual(['Particular', 'Profesional B', 'Sin categoría'])
  })

  it('porDefecto preselecciona la opción al crear', async () => {
    await abrirCrearPerfil()

    expect(listaDe('campo-categoriaSigla').valor).toBe('SIN')
  })

  it('mientras las opciones cargan, la lista está deshabilitada con "Cargando…"', async () => {
    await montarPerfil()
    fetch.mockReturnValueOnce(new Promise(() => {}))
    boton('Nuevo estudiante').click()
    await asentar(el)

    const select = listaDe('campo-categoriaSigla')
    expect(select.deshabilitado).toBe(true)
    expect(select.placeholder).toBe('Cargando…')
  })

  it('si las opciones fallan, avisa y permite reintentar', async () => {
    await montarPerfil()
    fetch.mockResolvedValueOnce(falla(500, 'ERR_SYS_00', 'x'))
    boton('Nuevo estudiante').click()
    await asentar(el)
    expect(texto()).toContain('No pudimos cargar las opciones')

    fetch.mockResolvedValueOnce(lista(CATEGORIAS))
    boton('Reintentar').click()
    await asentar(el)

    expect(listaDe('campo-categoriaSigla').opciones).toHaveLength(3)
    expect(listaDe('campo-categoriaSigla').deshabilitado).toBe(false)
  })

  it('seleccion requerida sin valor muestra su mensaje y no envía', async () => {
    await abrirCrearPerfil()

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('Elige el sexo')
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('ubicacion monta el mapa; al elegir un punto, el formulario lo guarda y envía latitud y longitud con el prefijo del campo', async () => {
    await abrirCrearPerfil()
    await elegir('campo-sexo', 'FEMENINO')
    await asentar(el)

    mapa().dispatchEvent(new CustomEvent('dh-ubicacion', { detail: { latitud: -16.4897, longitud: -68.1193 }, bubbles: true, composed: true }))
    await asentar(el)
    fetch.mockResolvedValueOnce(sobre({ codigo: 'e2' })).mockResolvedValueOnce(lista([ESTUDIANTE]))
    boton('Crear').click()
    await asentar(el)

    const [url, opciones] = fetch.mock.calls[2]
    expect(url).toBe('http://api.test/v1/estudiantes')
    expect(JSON.parse(opciones.body)).toEqual({
      sexo: 'FEMENINO',
      categoriaSigla: 'SIN',
      administrador: { latitud: -16.4897, longitud: -68.1193 },
    })
    expect(texto()).toContain('Estudiante creado correctamente.')
  })

  it('ubicacion requerida sin punto muestra su mensaje bajo el mapa y no envía', async () => {
    await abrirCrearPerfil()
    await elegir('campo-sexo', 'FEMENINO')
    await asentar(el)

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('Elige la ubicación en el mapa')
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('un error del backend asignado a la seleccion se muestra bajo ese campo', async () => {
    await abrirCrearPerfil()
    await elegir('campo-sexo', 'FEMENINO')
    mapa().dispatchEvent(new CustomEvent('dh-ubicacion', { detail: { latitud: -16.5, longitud: -68.1 }, bubbles: true, composed: true }))
    await asentar(el)
    fetch.mockResolvedValueOnce(falla(400, 'ERR_IDE_21', 'La categoría no existe'))

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('La categoría ya no existe')
  })

  it('al editar, carga el punto del registro, muestra la dirección y solo envía lo editable', async () => {
    await montarPerfil()
    fetch.mockResolvedValueOnce(lista(CATEGORIAS))
    fetch.mockResolvedValueOnce(sobre(ESTUDIANTE))
    boton('Editar').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/categorias-licencia?page=1&pageSize=100')
    expect(fetch.mock.calls[2][0]).toBe('http://api.test/v1/estudiantes/e1')
    expect(mapa().latitud).toBe(-16.5)
    expect(mapa().direccion).toBe('Av. Arce 2345, La Paz')
    expect(listaDe('campo-categoriaSigla').valor).toBe('B')

    mapa().dispatchEvent(new CustomEvent('dh-ubicacion', { detail: { latitud: -17, longitud: -68 }, bubbles: true, composed: true }))
    await asentar(el)
    fetch.mockResolvedValueOnce(sobre(ESTUDIANTE)).mockResolvedValueOnce(lista([ESTUDIANTE]))
    boton('Guardar').click()
    await asentar(el)

    const [url, opciones] = fetch.mock.calls[3]
    expect(url).toBe('http://api.test/v1/estudiantes/e1')
    expect(opciones.method).toBe('PATCH')
    expect(JSON.parse(opciones.body)).toEqual({ categoriaSigla: 'B', latitud: -17, longitud: -68 })
  })

  it('el campo ubicacion solo de lectura deshabilita el mapa', async () => {
    const esquema = structuredClone(ESQUEMA_PERFIL)
    esquema.editar.secciones[0].campos[2].soloLectura = true
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = esquema
    await asentar(el)
    fetch.mockResolvedValueOnce(lista(CATEGORIAS))
    fetch.mockResolvedValueOnce(sobre(ESTUDIANTE))
    boton('Editar').click()
    await asentar(el)

    expect(mapa().deshabilitado).toBe(true)
  })

  it('un filtro con más de 4 opciones usa la lista <dh-select> y consulta al elegir', async () => {
    const opciones = ['A', 'B', 'C', 'D', 'E'].map((v) => ({ valor: v, etiqueta: 'Categoría ' + v }))
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = { ...structuredClone(ESQUEMA_PERFIL), filtros: [{ clave: 'filter[categoria]', titulo: 'Categoría', opciones }] }
    await asentar(el)

    const filtro = el.shadowRoot.querySelector('dh-select.filtro-lista')
    expect(filtro).not.toBeNull()
    expect(el.shadowRoot.querySelector('select')).toBeNull()
    expect(filtro.opciones).toHaveLength(5)
    expect(filtro.placeholder).toBe('Categoría: todos')

    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    filtro.dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor: 'C' }, bubbles: true, composed: true }))
    await asentar(el)
    expect(fetch.mock.calls[1][0]).toContain('filter%5Bcategoria%5D=C')

    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    filtro.dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor: '' }, bubbles: true, composed: true }))
    await asentar(el)
    expect(fetch.mock.calls[2][0]).not.toContain('filter')
  })

  // ------------------------------------------------------------------ detalle de solo lectura

  const DETALLE = {
    titulo: 'Datos del estudiante',
    secciones: [
      {
        campos: [
          { campo: 'nombreCompleto', titulo: 'Nombre' },
          { campo: 'fechaNacimiento', titulo: 'Nacimiento', tipo: 'fecha', mitad: true },
          { campo: 'edad', titulo: 'Edad', mitad: true },
          { campo: 'estado', titulo: 'Estado', tipo: 'insignia', insignias: { ACTIVO: { etiqueta: 'Activo', tono: 'exito' } } },
          { campo: 'telefono', titulo: 'Teléfono' },
        ],
      },
    ],
  }
  const FICHA = { codigo: 'e1', nombreCompleto: 'Ana Pérez', fechaNacimiento: '2000-05-04', edad: 26, estado: 'ACTIVO', telefono: null }

  async function montarConDetalle(detalle = DETALLE, esquema = {}) {
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = { ...structuredClone(ESQUEMA_PERFIL), detalle, ...esquema }
    await asentar(el)
  }

  async function abrirDetalle(ficha = FICHA) {
    await montarConDetalle()
    fetch.mockResolvedValueOnce(sobre(ficha))
    boton('Ver detalles').click()
    await asentar(el)
  }

  it('sin detalle en el esquema no hay botón "Ver detalles"', async () => {
    await montarPerfil()

    expect(boton('Ver detalles')).toBeUndefined()
  })

  it('con detalle aparece "Ver detalles" en cada fila, con el icono del esquema', async () => {
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE, { ...ESTUDIANTE, codigo: 'e2' }]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = { ...structuredClone(ESQUEMA_PERFIL), detalle: DETALLE }
    await asentar(el)

    const botones = Array.from(el.shadowRoot.querySelectorAll('button')).filter((b) => b.textContent.trim() === 'Ver detalles')
    expect(botones).toHaveLength(2)
    expect(botones[0].querySelector('svg')).not.toBeNull()
  })

  it('visibleSi limita el botón a las filas que lo cumplen', async () => {
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE, { ...ESTUDIANTE, codigo: 'e2', categoriaSigla: 'SIN' }]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = { ...structuredClone(ESQUEMA_PERFIL), detalle: { ...DETALLE, visibleSi: { campo: 'categoriaSigla', igual: 'B' } } }
    await asentar(el)

    const botones = Array.from(el.shadowRoot.querySelectorAll('button')).filter((b) => b.textContent.trim() === 'Ver detalles')
    expect(botones).toHaveLength(1)
  })

  it('la etiqueta del botón se puede cambiar desde el esquema', async () => {
    await montarConDetalle({ ...DETALLE, etiqueta: 'Ver ficha' })

    expect(boton('Ver ficha')).toBeDefined()
    expect(boton('Ver detalles')).toBeUndefined()
  })

  it('al pulsarlo pide {recurso}/{id} y abre el panel lateral con el título del esquema', async () => {
    await abrirDetalle()

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/estudiantes/e1')
    expect(fetch.mock.calls[1][1].method).toBe('GET')
    const drawer = el.shadowRoot.querySelector('aside.drawer')
    expect(drawer).not.toBeNull()
    expect(drawer.querySelector('h2').textContent).toBe('Datos del estudiante')
    expect(el.shadowRoot.querySelectorAll('tbody tr')).toHaveLength(1)
  })

  it('cargar cambia la ruta con {id}', async () => {
    await montarConDetalle({ ...DETALLE, cargar: 'por-codigo/{id}' })
    fetch.mockResolvedValueOnce(sobre(FICHA))

    boton('Ver detalles').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/estudiantes/por-codigo/e1')
  })

  it('muestra cada dato con su formato: fecha 04 may 2026, insignia con su etiqueta y "—" si está vacío', async () => {
    await abrirDetalle({ ...FICHA, fechaNacimiento: '2026-05-04' })

    const t = el.shadowRoot.querySelector('aside.drawer').textContent.replace(/\s+/g, ' ')
    expect(t).toContain('Nombre Ana Pérez')
    expect(t).toContain('Nacimiento 04 may 2026')
    expect(t).toContain('Edad 26')
    expect(t).toContain('Estado Activo')
    expect(t).toContain('Teléfono —')
    expect(el.shadowRoot.querySelector('aside.drawer .insignia.exito').textContent.trim()).toBe('Activo')
  })

  it('un valor sin etiqueta de insignia se muestra tal cual', async () => {
    await abrirDetalle({ ...FICHA, estado: 'OTRO' })

    expect(el.shadowRoot.querySelector('aside.drawer .insignia').textContent.trim()).toBe('OTRO')
  })

  it('los campos "mitad" consecutivos comparten fila', async () => {
    await abrirDetalle()

    const par = el.shadowRoot.querySelector('aside.drawer .grid-2')
    expect(par.querySelectorAll('.dato')).toHaveLength(2)
    expect(par.textContent).toContain('Nacimiento')
    expect(par.textContent).toContain('Edad')
  })

  it('es de solo lectura: sin formulario, sin Guardar y sin campos de entrada', async () => {
    await abrirDetalle()

    const drawer = el.shadowRoot.querySelector('aside.drawer')
    expect(drawer.querySelector('form')).toBeNull()
    expect(drawer.querySelector('input, select, textarea, dh-select')).toBeNull()
    expect(drawer.querySelector('button[type="submit"]')).toBeNull()
    expect(Array.from(drawer.querySelectorAll('button')).map((b) => b.textContent.trim()).filter(Boolean)).toEqual(['Cerrar'])
  })

  it('mientras carga muestra el esqueleto y después los datos', async () => {
    await montarConDetalle()
    let resolver
    fetch.mockReturnValueOnce(new Promise((r) => (resolver = r)))

    boton('Ver detalles').click()
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer .brillo')).not.toBeNull()
    expect(el.shadowRoot.querySelector('aside.drawer .dato')).toBeNull()

    resolver(sobre(FICHA))
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer .brillo')).toBeNull()
    expect(el.shadowRoot.querySelector('aside.drawer .dato')).not.toBeNull()
  })

  it('se cierra con Cerrar, con la X y con Escape', async () => {
    await abrirDetalle()
    boton('Cerrar').click()
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()

    fetch.mockResolvedValueOnce(sobre(FICHA))
    boton('Ver detalles').click()
    await asentar(el)
    el.shadowRoot.querySelector('aside.drawer button[aria-label="Cerrar"]').click()
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()

    fetch.mockResolvedValueOnce(sobre(FICHA))
    boton('Ver detalles').click()
    await asentar(el)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()
  })

  it('un clic en el fondo lo cierra', async () => {
    await abrirDetalle()

    el.shadowRoot.querySelector('.fondo').dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
    await asentar(el)

    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()
  })

  it('si la carga falla muestra el aviso de error y cierra el panel', async () => {
    await montarConDetalle()
    fetch.mockResolvedValueOnce(falla(404, 'ERR_IDE_20', 'El estudiante no existe'))

    boton('Ver detalles').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()
    expect(el.shadowRoot.querySelector('.toast.error').textContent).toContain('El estudiante no existe')
  })

  it('si se cierra antes de que llegue la respuesta, esa respuesta no reabre ni pisa nada', async () => {
    await montarConDetalle()
    let resolver
    fetch.mockReturnValueOnce(new Promise((r) => (resolver = r)))

    boton('Ver detalles').click()
    await asentar(el)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await asentar(el)
    resolver(sobre(FICHA))
    await asentar(el)

    expect(el.shadowRoot.querySelector('aside.drawer')).toBeNull()
  })

  it('convive con crear y editar en el mismo esquema: cada botón abre lo suyo', async () => {
    await montarConDetalle()
    expect(boton('Ver detalles')).toBeDefined()
    expect(boton('Editar')).toBeDefined()
    expect(boton('Nuevo estudiante')).toBeDefined()

    fetch.mockResolvedValueOnce(sobre(FICHA))
    boton('Ver detalles').click()
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer form')).toBeNull()
    boton('Cerrar').click()
    await asentar(el)

    fetch.mockResolvedValueOnce(lista(CATEGORIAS))
    fetch.mockResolvedValueOnce(sobre(ESTUDIANTE))
    boton('Editar').click()
    await asentar(el)
    expect(el.shadowRoot.querySelector('aside.drawer form')).not.toBeNull()
  })
})

// * Lo que hace <dh-select> al elegir una opción (el componente tiene sus propios tests en @drivehub/ui)
const elegirOpcion = async (el, id, valor) => {
  el.shadowRoot
    .querySelector(`#${id}`)
    .dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor }, bubbles: true, composed: true }))
  await asentar(el)
}

describe('<dh-gestion> filas sin acciones', () => {
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

  it('una fila sin ninguna acción disponible dice "Sin acciones" en vez de quedar vacía', async () => {
    fetch.mockResolvedValueOnce(lista([{ ...MUERTA, codigo: 'c3', estado: 'OTRO' }]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = { ...ESQUEMA, editar: { ...ESQUEMA.editar, visibleSi: { campo: 'estado', igual: 'ACTIVA' } } }
    await asentar(el)

    expect(el.shadowRoot.textContent).toContain('Sin acciones')
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
    expect(select.tagName).toBe('DH-SELECT')
    expect(select.opciones.map((o) => o.valor)).toEqual(['MANUAL', 'AUTOMATICO'])
    expect(el.shadowRoot.querySelector('#campo-kilometraje').type).toBe('number')

    const poner = async (selector, valor, evento) => {
      const campo = el.shadowRoot.querySelector(selector)
      campo.value = valor
      campo.dispatchEvent(new Event(evento, { bubbles: true }))
      campo.dispatchEvent(new Event('blur'))
      await asentar(el)
    }
    await poner('#campo-placa', 'ABC-123', 'input')
    await elegirOpcion(el, 'campo-transmision', 'AUTOMATICO')
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

describe('<dh-gestion> campos archivo y lista, columna enlace', () => {
  let el
  const DESCRIPTOR = {
    url: 'http://localhost:9000/drivehub-contenido/escuelas/e1/manuales/a.pdf',
    nombre: 'm.pdf',
    mime: 'application/pdf',
    sha256: 'a'.repeat(64),
    tamanoBytes: 193,
  }
  const FILA = { codigo: 'm1', titulo: 'Señales', etiquetas: ['vial', 'señales'], archivo: { ...DESCRIPTOR } }
  const boton = (t) => Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === t)
  const pdf = (nombre = 'm.pdf', tipo = 'application/pdf') => new File(['%PDF'], nombre, { type: tipo })

  const ESQUEMA_MANUALES = {
    recurso: 'manuales',
    titulo: 'Manuales',
    columnas: [
      { campo: 'titulo', titulo: 'Título' },
      { campo: 'archivo.url', titulo: 'PDF', tipo: 'enlace', etiqueta: 'Ver PDF' },
    ],
    crear: {
      etiqueta: 'Nuevo manual',
      mensajeExito: 'Listo',
      etiquetaEnviar: 'Crear',
      secciones: [
        {
          campos: [
            { campo: 'titulo', titulo: 'Título', requerido: true },
            { campo: 'etiquetas', titulo: 'Etiquetas', tipo: 'lista' },
            {
              campo: 'archivo',
              titulo: 'PDF',
              tipo: 'archivo',
              requerido: true,
              subida: 'manuales/archivos',
              accept: 'application/pdf',
              maxBytes: 1024,
              mensajes: { requerido: 'Sube el PDF', tipo: 'Debe ser un PDF', max: 'Muy pesado' },
            },
          ],
        },
      ],
    },
    editar: {
      etiquetaEnviar: 'Guardar',
      mensajeExito: 'Guardado',
      secciones: [
        {
          campos: [
            { campo: 'titulo', titulo: 'Título', requerido: true },
            { campo: 'etiquetas', titulo: 'Etiquetas', tipo: 'lista' },
            { campo: 'archivo', titulo: 'PDF', tipo: 'archivo', subida: 'manuales/archivos' },
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

  async function montar(filas = [FILA]) {
    fetch.mockResolvedValueOnce(lista(filas))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_MANUALES
    await asentar(el)
  }

  async function elegir(archivo) {
    const entrada = el.shadowRoot.querySelector('#campo-archivo')
    Object.defineProperty(entrada, 'files', { value: [archivo], configurable: true })
    entrada.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)
  }

  async function escribirTexto(selector, valor) {
    const campo = el.shadowRoot.querySelector(selector)
    campo.value = valor
    campo.dispatchEvent(new Event('input', { bubbles: true }))
    await asentar(el)
  }

  it('la columna enlace abre el archivo en otra pestaña con su etiqueta', async () => {
    await montar()

    const enlace = el.shadowRoot.querySelector('a.enlace-archivo')
    expect(enlace.textContent.trim()).toBe('Ver PDF')
    expect(enlace.getAttribute('href')).toBe(DESCRIPTOR.url)
    expect(enlace.getAttribute('target')).toBe('_blank')
    expect(enlace.getAttribute('rel')).toContain('noopener')
  })

  it('sube el archivo como multipart al elegirlo y al enviar manda su descriptor y la lista como array', async () => {
    await montar([])
    boton('Nuevo manual').click()
    await asentar(el)
    fetch.mockReset()
    fetch.mockResolvedValueOnce(sobre(DESCRIPTOR))

    await elegir(pdf())

    const [urlSubida, initSubida] = fetch.mock.calls[0]
    expect(urlSubida).toBe('http://api.test/v1/manuales/archivos')
    expect(initSubida.method).toBe('POST')
    expect(initSubida.body.get('archivo').name).toBe('m.pdf')
    expect(initSubida.headers['Content-Type']).toBeUndefined()
    expect(el.shadowRoot.textContent).toContain('m.pdf')

    await escribirTexto('#campo-titulo', 'Señales')
    await escribirTexto('#campo-etiquetas', ' vial, señales ,vial,, ')
    fetch.mockResolvedValueOnce(sobre({ codigo: 'm2' })).mockResolvedValueOnce(lista([]))
    boton('Crear').click()
    await asentar(el)

    expect(JSON.parse(fetch.mock.calls[1][1].body)).toEqual({
      titulo: 'Señales',
      etiquetas: ['vial', 'señales'],
      archivo: DESCRIPTOR,
    })
  })

  it('sin archivo no se envía y pide subirlo; un tipo o tamaño no válido avisa sin llamar a nadie', async () => {
    await montar([])
    boton('Nuevo manual').click()
    await asentar(el)
    await escribirTexto('#campo-titulo', 'Señales')
    boton('Crear').click()
    await asentar(el)
    expect(el.shadowRoot.textContent).toContain('Sube el PDF')

    fetch.mockReset()
    await elegir(pdf('a.png', 'image/png'))
    expect(el.shadowRoot.textContent).toContain('Debe ser un PDF')
    await elegir(new File([new Uint8Array(2048)], 'grande.pdf', { type: 'application/pdf' }))
    expect(el.shadowRoot.textContent).toContain('Muy pesado')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('si el backend rechaza el archivo, muestra su mensaje y no deja valor', async () => {
    await montar([])
    boton('Nuevo manual').click()
    await asentar(el)
    fetch.mockReset()
    fetch.mockResolvedValueOnce(falla(400, 'ERR_SYS_01', 'El manual debe ser un archivo PDF'))

    await elegir(pdf())

    expect(el.shadowRoot.textContent).toContain('El manual debe ser un archivo PDF')
    expect(el.shadowRoot.querySelector('.archivo-actual')).toBeNull()
  })

  it('al editar muestra el archivo actual y las etiquetas como texto, y sin cambiar el archivo no lo reenvía', async () => {
    await montar()
    fetch.mockResolvedValueOnce(sobre(FILA))
    boton('Editar').click()
    await asentar(el)

    expect(el.shadowRoot.querySelector('#campo-etiquetas').value).toBe('vial, señales')
    // * El listado sigue visible detrás del panel: el enlace del archivo actual es el del panel
    expect(el.shadowRoot.querySelector('.drawer a.enlace-archivo').textContent.trim()).toBe('m.pdf')

    await escribirTexto('#campo-titulo', 'Señales 2')
    fetch.mockResolvedValueOnce(sobre(FILA)).mockResolvedValueOnce(lista([FILA]))
    boton('Guardar').click()
    await asentar(el)

    const cuerpo = JSON.parse(fetch.mock.calls[2][1].body)
    expect(cuerpo).toEqual({ titulo: 'Señales 2', etiquetas: ['vial', 'señales'] })
    expect(cuerpo.archivo).toBeUndefined()
  })
})

describe('<dh-gestion> campos textarea, opciones y seleccion numerica, y datos booleanos', () => {
  let el
  const boton = (t) => Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === t)
  const filas = () => Array.from(el.shadowRoot.querySelectorAll('.fila-opcion'))
  const PREGUNTA = {
    codigo: 'p1',
    enunciado: '¿Qué indica el PARE?',
    dificultad: 2,
    activa: true,
    opciones: [
      { id: 'A', texto: 'Detenerse', correcta: true },
      { id: 'B', texto: 'Seguir', correcta: false },
      { id: 'C', texto: 'Tocar bocina', correcta: false },
    ],
  }
  const INACTIVA = { ...PREGUNTA, codigo: 'p2', activa: false }

  const ESQUEMA_PREGUNTAS = {
    recurso: 'preguntas',
    titulo: 'Preguntas',
    columnas: [
      { campo: 'enunciado', titulo: 'Enunciado' },
      {
        campo: 'activa',
        titulo: 'Estado',
        tipo: 'insignia',
        insignias: { true: { etiqueta: 'Activa', tono: 'exito' }, false: { etiqueta: 'Inactiva' } },
      },
    ],
    filtros: [
      {
        clave: 'filter[activa]',
        titulo: 'Estado',
        opciones: [
          { valor: 'true', etiqueta: 'Activas' },
          { valor: 'false', etiqueta: 'Inactivas' },
        ],
      },
    ],
    crear: {
      etiqueta: 'Nueva pregunta',
      mensajeExito: 'Listo',
      etiquetaEnviar: 'Crear',
      secciones: [
        {
          campos: [
            { campo: 'enunciado', titulo: 'Enunciado', tipo: 'textarea', requerido: true, max: 500 },
            {
              campo: 'dificultad',
              titulo: 'Dificultad',
              tipo: 'seleccion',
              numerico: true,
              requerido: true,
              opciones: [
                { valor: '1', etiqueta: 'Fácil' },
                { valor: '2', etiqueta: 'Media' },
                { valor: '3', etiqueta: 'Difícil' },
              ],
            },
            { campo: 'opciones', titulo: 'Opciones', tipo: 'opciones', requerido: true, min: 2, max: 3 },
          ],
        },
      ],
    },
    editar: {
      etiquetaEnviar: 'Guardar',
      mensajeExito: 'Guardado',
      secciones: [
        {
          campos: [
            {
              campo: 'dificultad',
              titulo: 'Dificultad',
              tipo: 'seleccion',
              numerico: true,
              opciones: [{ valor: '2', etiqueta: 'Media' }],
            },
            { campo: 'opciones', titulo: 'Opciones', tipo: 'opciones', requerido: true },
          ],
        },
      ],
    },
    acciones: [
      {
        id: 'desactivar',
        etiqueta: 'Desactivar',
        metodo: 'POST',
        ruta: '{id}/desactivar',
        mensajeExito: 'Listo',
        visibleSi: { campo: 'activa', igual: true },
      },
      {
        id: 'activar',
        etiqueta: 'Activar',
        metodo: 'POST',
        ruta: '{id}/activar',
        mensajeExito: 'Listo',
        visibleSi: { campo: 'activa', igual: false },
      },
    ],
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

  async function montar(datos = [PREGUNTA, INACTIVA]) {
    fetch.mockResolvedValueOnce(lista(datos))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_PREGUNTAS
    await asentar(el)
  }

  async function escribirEn(selector, valor, evento = 'input') {
    const campo = el.shadowRoot.querySelector(selector)
    campo.value = valor
    campo.dispatchEvent(new Event(evento, { bubbles: true }))
    await asentar(el)
  }

  async function llenarFila(i, texto, correcta = false) {
    const entrada = filas()[i].querySelector('input[type="text"]')
    entrada.value = texto
    entrada.dispatchEvent(new Event('input', { bubbles: true }))
    await asentar(el)
    const casilla = filas()[i].querySelector('input[type="checkbox"]')
    casilla.checked = correcta
    casilla.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)
  }

  it('un dato booleano se muestra con su insignia y decide las acciones visibles', async () => {
    await montar()

    expect(el.shadowRoot.textContent).toContain('Activa')
    expect(el.shadowRoot.textContent).toContain('Inactiva')
    const botones = Array.from(el.shadowRoot.querySelectorAll('tbody tr')).map((tr) =>
      Array.from(tr.querySelectorAll('button')).map((b) => b.textContent.trim())
    )
    expect(botones[0]).toContain('Desactivar')
    expect(botones[0]).not.toContain('Activar')
    expect(botones[1]).toContain('Activar')
    expect(botones[1]).not.toContain('Desactivar')
  })

  it('el filtro de un booleano manda filter[activa]=false', async () => {
    await montar()
    fetch.mockResolvedValueOnce(lista([INACTIVA]))

    boton('Inactivas').click()
    await asentar(el)

    expect(fetch.mock.calls[1][0]).toContain('filter%5Bactiva%5D=false')
  })

  it('el campo opciones arranca con las filas mínimas y respeta el mínimo y el máximo', async () => {
    await montar([])
    boton('Nueva pregunta').click()
    await asentar(el)

    expect(filas()).toHaveLength(2)
    expect(filas()[0].querySelector('button').disabled).toBe(true)
    boton('Agregar opción').click()
    await asentar(el)
    expect(filas()).toHaveLength(3)
    expect(boton('Agregar opción').disabled).toBe(true)
    filas()[2].querySelector('button').click()
    await asentar(el)
    expect(filas()).toHaveLength(2)
  })

  it('envía el textarea, la dificultad como número y las opciones como arreglo de objetos', async () => {
    await montar([])
    boton('Nueva pregunta').click()
    await asentar(el)
    expect(el.shadowRoot.querySelector('textarea#campo-enunciado')).not.toBeNull()
    await escribirEn('#campo-enunciado', '  ¿Qué indica el PARE?  ')
    await elegirOpcion(el, 'campo-dificultad', '2')
    await llenarFila(0, ' Detenerse ', true)
    await llenarFila(1, 'Seguir', false)
    fetch.mockResolvedValueOnce(sobre({ codigo: 'p9' })).mockResolvedValueOnce(lista([]))

    boton('Crear').click()
    await asentar(el)

    expect(JSON.parse(fetch.mock.calls[1][1].body)).toEqual({
      enunciado: '¿Qué indica el PARE?',
      dificultad: 2,
      opciones: [
        { texto: 'Detenerse', correcta: true },
        { texto: 'Seguir', correcta: false },
      ],
    })
  })

  it('sin texto en una opción o sin ninguna correcta no se envía y muestra el error bajo el campo', async () => {
    await montar([])
    boton('Nueva pregunta').click()
    await asentar(el)
    await escribirEn('#campo-enunciado', 'Enunciado válido')
    await elegirOpcion(el, 'campo-dificultad', '1')
    await llenarFila(0, 'Uno', false)

    boton('Crear').click()
    await asentar(el)
    expect(el.shadowRoot.textContent).toContain('Todas las opciones necesitan texto')

    await llenarFila(1, 'Dos', false)
    boton('Crear').click()
    await asentar(el)
    expect(el.shadowRoot.textContent).toContain('Marca al menos una opción correcta')
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('al editar carga las opciones del backend como filas y reenvía el conjunto completo', async () => {
    await montar()
    fetch.mockResolvedValueOnce(sobre(PREGUNTA))
    boton('Editar').click()
    await asentar(el)

    expect(filas()).toHaveLength(3)
    expect(filas()[0].querySelector('input[type="text"]').value).toBe('Detenerse')
    expect(filas()[0].querySelector('input[type="checkbox"]').checked).toBe(true)
    expect(filas()[1].querySelector('input[type="checkbox"]').checked).toBe(false)
    expect(el.shadowRoot.querySelector('#campo-dificultad').valor).toBe('2')

    await llenarFila(1, 'Avanzar', false)
    fetch.mockResolvedValueOnce(sobre(PREGUNTA)).mockResolvedValueOnce(lista([PREGUNTA]))
    boton('Guardar').click()
    await asentar(el)

    expect(JSON.parse(fetch.mock.calls[2][1].body)).toEqual({
      dificultad: 2,
      opciones: [
        { texto: 'Detenerse', correcta: true },
        { texto: 'Avanzar', correcta: false },
        { texto: 'Tocar bocina', correcta: false },
      ],
    })
  })
})
