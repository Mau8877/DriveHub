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

  // ------------------------------------------------------------------ campo fecha (<dh-fecha>)

  const ESQUEMA_FECHA = {
    recurso: 'estudiantes',
    titulo: 'Gestionar estudiantes',
    nombreSingular: 'estudiante',
    columnas: [{ campo: 'nombreCompleto', titulo: 'Nombre' }],
    crear: {
      etiqueta: 'Nuevo estudiante',
      mensajeExito: 'Estudiante creado correctamente.',
      secciones: [
        {
          campos: [
            {
              campo: 'fechaNacimiento',
              titulo: 'Fecha de nacimiento',
              tipo: 'fecha',
              requerido: true,
              validaciones: ['fechaNoFutura', 'edadMinima:18'],
              mensajes: { requerido: 'La fecha de nacimiento es obligatoria', edadMinima: 'Debe tener al menos 18 años' },
            },
            { campo: 'fechaAlta', titulo: 'Fecha de alta', tipo: 'fecha', validaciones: ['fechaNoFutura'] },
            { campo: 'fechaLibre', titulo: 'Otra fecha', tipo: 'fecha' },
          ],
        },
      ],
    },
  }

  async function abrirCrearFecha() {
    // * Hoy fijo: jueves 23 de octubre de 2026 (solo el reloj)
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 9, 23, 12) })
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_FECHA
    await asentar(el)
    boton('Nuevo estudiante').click()
    await asentar(el)
  }

  it('un campo de tipo fecha dibuja <dh-fecha>, no un input de fecha nativo', async () => {
    await abrirCrearFecha()

    expect(listaDe('campo-fechaNacimiento').tagName).toBe('DH-FECHA')
    expect(el.shadowRoot.querySelector('input[type="date"]')).toBeNull()
    expect(listaDe('campo-fechaNacimiento').etiqueta).toBe('Fecha de nacimiento')
  })

  it('el calendario deja elegir solo hasta lo que permiten las reglas: edad mínima, fecha no futura o nada', async () => {
    await abrirCrearFecha()

    expect(listaDe('campo-fechaNacimiento').max).toBe('2008-10-23')
    expect(listaDe('campo-fechaAlta').max).toBe('2026-10-22')
    expect(listaDe('campo-fechaLibre').max).toBe('')
  })

  it('el límite por edad mínima cae el 28 de febrero si el año de destino no es bisiesto', async () => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2028, 1, 29, 12) })
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_FECHA
    await asentar(el)
    boton('Nuevo estudiante').click()
    await asentar(el)

    // * 18 años antes del 29/02/2028 es el 29/02/2010, que no existe: el último día válido es el 28
    expect(listaDe('campo-fechaNacimiento').max).toBe('2010-02-28')
  })

  it('elegir una fecha la guarda en el formulario como yyyy-MM-dd y se envía así', async () => {
    await abrirCrearFecha()

    await elegir('campo-fechaNacimiento', '2000-05-04')
    expect(listaDe('campo-fechaNacimiento').valor).toBe('2000-05-04')

    fetch.mockResolvedValueOnce(sobre({ codigo: 'e2' })).mockResolvedValueOnce(lista([ESTUDIANTE]))
    boton('Crear').click()
    await asentar(el)

    const [url, init] = fetch.mock.calls[1]
    expect(url).toBe('http://api.test/v1/estudiantes')
    expect(JSON.parse(init.body)).toEqual({ fechaNacimiento: '2000-05-04' })
  })

  it('una fecha que no cumple la edad mínima muestra su mensaje bajo el campo y no se envía', async () => {
    await abrirCrearFecha()

    await elegir('campo-fechaNacimiento', '2015-01-01')

    expect(texto()).toContain('Debe tener al menos 18 años')
    expect(listaDe('campo-fechaNacimiento').invalido).toBe(true)
    boton('Crear').click()
    await asentar(el)
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('una fecha requerida sin valor muestra su mensaje al intentar enviar', async () => {
    await abrirCrearFecha()

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('La fecha de nacimiento es obligatoria')
    expect(listaDe('campo-fechaNacimiento').invalido).toBe(true)
  })

  it('al editar, la fecha del registro llega al calendario', async () => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 9, 23, 12) })
    fetch.mockResolvedValueOnce(lista([ESTUDIANTE]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = {
      ...ESQUEMA_FECHA,
      editar: { mensajeExito: 'ok', secciones: [{ campos: [{ campo: 'fechaNacimiento', titulo: 'Fecha de nacimiento', tipo: 'fecha' }] }] },
    }
    await asentar(el)
    fetch.mockResolvedValueOnce(sobre({ codigo: 'e1', fechaNacimiento: '1999-12-31' }))

    boton('Editar').click()
    await asentar(el)

    expect(listaDe('campo-fechaNacimiento').valor).toBe('1999-12-31')
  })

  // ------------------------------------------------------------------ columna y dato de tipo foto

  const ESQUEMA_FOTO = {
    recurso: 'estudiantes',
    titulo: 'Gestionar estudiantes',
    nombreSingular: 'estudiante',
    columnas: [
      { campo: 'fotoUrl', titulo: 'Foto', tipo: 'foto', nombre: 'nombreCompleto' },
      { campo: 'nombreCompleto', titulo: 'Nombre' },
    ],
    detalle: {
      secciones: [{ campos: [{ campo: 'fotoUrl', titulo: 'Foto', tipo: 'foto', nombre: 'nombreCompleto' }, { campo: 'ci', titulo: 'CI' }] }],
    },
  }
  const CON_FOTO = { codigo: 'e1', nombreCompleto: 'Ana María Pérez', fotoUrl: 'http://minio/ana.png', ci: 'AB1' }
  const SIN_FOTO = { codigo: 'e2', nombreCompleto: 'Leonel Barrios Mayorga', fotoUrl: null, ci: 'AB2' }

  async function montarFotos() {
    fetch.mockResolvedValueOnce(lista([CON_FOTO, SIN_FOTO]))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA_FOTO
    await asentar(el)
  }

  it('la columna de tipo foto dibuja un <dh-avatar> por fila con la URL de la foto', async () => {
    await montarFotos()

    const avatares = [...el.shadowRoot.querySelectorAll('tbody dh-avatar')]
    expect(avatares).toHaveLength(2)
    expect(avatares[0].fotoUrl).toBe('http://minio/ana.png')
    expect(avatares[0].tam).toBe(36)
    expect(el.shadowRoot.querySelector('thead').textContent).toContain('Foto')
  })

  it('sin foto el avatar lleva las iniciales: primer nombre y último apellido', async () => {
    await montarFotos()

    const [conFoto, sinFoto] = [...el.shadowRoot.querySelectorAll('tbody dh-avatar')]
    expect([conFoto.nombres, conFoto.apellidos]).toEqual(['Ana', 'Pérez'])
    expect(sinFoto.fotoUrl).toBeNull()
    expect([sinFoto.nombres, sinFoto.apellidos]).toEqual(['Leonel', 'Mayorga'])
  })

  it('la columna foto es angosta por defecto', async () => {
    await montarFotos()

    const columna = el.shadowRoot.querySelector('thead th:nth-child(2)')
    expect(columna.getAttribute('style')).toContain('width: 5rem')
  })

  it('un campo de tipo foto en el detalle muestra el avatar grande', async () => {
    await montarFotos()
    fetch.mockResolvedValueOnce(sobre(CON_FOTO))
    // * Esta pantalla no tiene "Editar": el único botón de la fila es "Ver detalles"
    boton('Ver detalles').click()
    await asentar(el)

    const avatar = el.shadowRoot.querySelector('aside.drawer .dato.foto dh-avatar')
    expect(avatar).not.toBeNull()
    expect(avatar.fotoUrl).toBe('http://minio/ana.png')
    expect(avatar.tam).toBe(72)
  })
})

// ---------------------------------------------------------------------- CU09: múltiple, etiquetas, fechas

describe('<dh-gestion> selección múltiple, etiquetas y fechas', () => {
  let el

  const hoy = new Date()
  const dos = (n) => String(n).padStart(2, '0')
  const texto = () => el.shadowRoot.textContent.replace(/\s+/g, ' ')
  const boton = (t) => Array.from(el.shadowRoot.querySelectorAll('button')).find((b) => b.textContent.trim() === t)
  const campo = (id) => el.shadowRoot.querySelector(`#${id}`)
  const fechaIso = (dias) => {
    const f = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + dias)
    return `${f.getFullYear()}-${dos(f.getMonth() + 1)}-${dos(f.getDate())}`
  }

  const CATEGORIAS = [
    { sigla: 'A', descripcion: 'Moto' },
    { sigla: 'B', descripcion: 'Auto' },
    { sigla: 'SIN', descripcion: 'Sin categoría' },
  ]
  const INSTRUCTOR = {
    codigo: 'i1',
    nombreCompleto: 'Leo Mayorga',
    nroLicencia: 'LIC-1',
    licenciaVence: '2020-01-01',
    fechaContratacion: '2019-05-05',
    categorias: [
      { sigla: 'A', descripcion: 'Moto' },
      { sigla: 'B', descripcion: 'Auto' },
    ],
    estado: 'ACTIVO',
  }

  const CAMPO_CATEGORIAS = {
    campo: 'categorias',
    titulo: 'Categorías',
    tipo: 'seleccion',
    multiple: true,
    requerido: true,
    valorDe: 'sigla',
    opcionesDe: { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion', excluir: ['SIN'] },
    mensajes: { requerido: 'Elige al menos una categoría' },
  }
  const CAMPO_VENCE = {
    campo: 'licenciaVence',
    titulo: 'Vencimiento',
    tipo: 'fecha',
    requerido: true,
    validaciones: ['fechaFutura'],
  }
  const CAMPO_CONTRATACION = {
    campo: 'fechaContratacion',
    titulo: 'Contratación',
    tipo: 'fecha',
    porDefecto: 'hoy',
    validaciones: ['hastaHoy'],
  }

  const ESQUEMA = {
    recurso: 'instructores',
    titulo: 'Instructores',
    nombreSingular: 'instructor',
    columnas: [
      { campo: 'nombreCompleto', titulo: 'Nombre' },
      { campo: 'categorias', titulo: 'Categorías', tipo: 'etiquetas', etiqueta: 'sigla' },
    ],
    crear: {
      etiqueta: 'Nuevo instructor',
      mensajeExito: 'Instructor creado correctamente.',
      secciones: [{ titulo: 'Ficha', campos: [CAMPO_VENCE, CAMPO_CONTRATACION, CAMPO_CATEGORIAS] }],
    },
    editar: {
      mensajeExito: 'Instructor actualizado correctamente.',
      secciones: [
        {
          campos: [
            { ...CAMPO_VENCE, soloSiCambia: true },
            CAMPO_CONTRATACION,
            CAMPO_CATEGORIAS,
            { campo: 'estado', titulo: 'Estado', tipo: 'seleccion', opciones: [{ valor: 'ACTIVO', etiqueta: 'Activo' }, { valor: 'INACTIVO', etiqueta: 'Inactivo' }] },
          ],
        },
      ],
    },
    detalle: {
      secciones: [
        {
          campos: [
            { campo: 'categorias', titulo: 'Categorías', tipo: 'etiquetas', etiqueta: 'sigla' },
            { campo: 'otras', titulo: 'Otras', tipo: 'etiquetas' },
          ],
        },
      ],
    },
    erroresDeCampo: { ERR_ESC_10: { campo: 'categorias', mensaje: 'Una categoría no es válida' } },
  }

  async function montar(filas = [INSTRUCTOR]) {
    fetch.mockResolvedValueOnce(lista(filas))
    el = document.createElement('dh-gestion')
    document.body.append(el)
    el.esquema = ESQUEMA
    await asentar(el)
  }

  async function abrirCrear() {
    await montar()
    fetch.mockResolvedValueOnce(lista(CATEGORIAS))
    boton('Nuevo instructor').click()
    await asentar(el)
  }

  async function abrirEditar() {
    await montar()
    fetch.mockResolvedValueOnce(lista(CATEGORIAS)).mockResolvedValueOnce(sobre(INSTRUCTOR))
    boton('Editar').click()
    await asentar(el)
  }

  const elegir = async (id, valores) => {
    campo(id).dispatchEvent(new CustomEvent('dh-cambio', { detail: { valores }, bubbles: true, composed: true }))
    await asentar(el)
  }
  const cambiarFecha = async (id, valor) => {
    campo(id).dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor }, bubbles: true, composed: true }))
    await asentar(el)
  }
  const cuerpoEnviado = (n) => JSON.parse(fetch.mock.calls[n][1].body)

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

  // ---------------------------------------------------------------- seleccion multiple

  it('dibuja un <dh-select multiple> con las opciones del catálogo menos las excluidas', async () => {
    await abrirCrear()

    const select = campo('campo-categorias')
    expect(select.tagName).toBe('DH-SELECT')
    expect(select.multiple).toBe(true)
    expect(select.opciones.map((o) => o.valor)).toEqual(['A', 'B'])
    expect(select.opciones.map((o) => o.etiqueta)).toEqual(['Moto', 'Auto'])
    expect(select.valores).toEqual([])
    expect(el.shadowRoot.querySelector('select')).toBeNull()
  })

  it('elegir dos categorías envía el arreglo de siglas', async () => {
    await abrirCrear()
    await cambiarFecha('campo-licenciaVence', fechaIso(30))
    await elegir('campo-categorias', ['A', 'B'])
    fetch.mockResolvedValueOnce(sobre({ codigo: 'i2' })).mockResolvedValueOnce(lista([INSTRUCTOR]))

    boton('Crear').click()
    await asentar(el)

    const [url, opciones] = fetch.mock.calls[2]
    expect(url).toBe('http://api.test/v1/instructores')
    expect(opciones.method).toBe('POST')
    expect(cuerpoEnviado(2).categorias).toEqual(['A', 'B'])
    expect(texto()).toContain('Instructor creado correctamente.')
  })

  it('requerido sin ninguna categoría muestra el mensaje bajo el campo y no envía', async () => {
    await abrirCrear()
    await cambiarFecha('campo-licenciaVence', fechaIso(30))

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('Elige al menos una categoría')
    expect(campo('campo-categorias').invalido).toBe(true)
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('al editar, las categorías del registro (objetos con sigla) se normalizan a siglas', async () => {
    await abrirEditar()

    expect(campo('campo-categorias').valores).toEqual(['A', 'B'])
  })

  it('al editar se envía el arreglo actual de siglas', async () => {
    await abrirEditar()
    await elegir('campo-categorias', ['B'])
    fetch.mockResolvedValueOnce(sobre(INSTRUCTOR)).mockResolvedValueOnce(lista([INSTRUCTOR]))

    boton('Guardar').click()
    await asentar(el)

    const [url, opciones] = fetch.mock.calls[3]
    expect(url).toBe('http://api.test/v1/instructores/i1')
    expect(opciones.method).toBe('PATCH')
    expect(cuerpoEnviado(3).categorias).toEqual(['B'])
  })

  it('un error del backend mapeado al campo múltiple se muestra bajo ese campo', async () => {
    await abrirCrear()
    await cambiarFecha('campo-licenciaVence', fechaIso(30))
    await elegir('campo-categorias', ['A'])
    fetch.mockResolvedValueOnce(falla(400, 'ERR_ESC_10', 'Categoría inválida'))

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('Una categoría no es válida')
  })

  // ---------------------------------------------------------------- etiquetas

  it('la columna etiquetas dibuja una insignia por elemento, con la propiedad indicada', async () => {
    await montar()

    const etiquetas = [...el.shadowRoot.querySelectorAll('tbody td .etiquetas .etiqueta')].map((e) => e.textContent.trim())
    expect(etiquetas).toEqual(['A', 'B'])
  })

  it('acepta un arreglo de textos y corta con +N cuando hay más de tres', async () => {
    await montar([{ ...INSTRUCTOR, categorias: ['A', 'B', 'C', 'D', 'E'] }])

    const etiquetas = [...el.shadowRoot.querySelectorAll('tbody td .etiquetas .etiqueta')].map((e) => e.textContent.trim())
    expect(etiquetas).toEqual(['A', 'B', 'C', '+2'])
  })

  it('una lista vacía se muestra con un guion', async () => {
    await montar([{ ...INSTRUCTOR, categorias: [] }])

    expect(el.shadowRoot.querySelector('tbody td .etiquetas')).toBeNull()
    expect(el.shadowRoot.querySelectorAll('tbody td')[2].textContent.trim()).toBe('—')
  })

  it('el detalle muestra todas las etiquetas y "—" si el dato falta', async () => {
    await montar([{ ...INSTRUCTOR, categorias: ['A', 'B', 'C', 'D', 'E'] }])
    fetch.mockResolvedValueOnce(sobre({ categorias: ['A', 'B', 'C', 'D', 'E'] }))
    boton('Ver detalles').click()
    await asentar(el)

    const datos = [...el.shadowRoot.querySelectorAll('aside.drawer .dato')]
    expect([...datos[0].querySelectorAll('.etiqueta')].map((e) => e.textContent.trim())).toEqual(['A', 'B', 'C', 'D', 'E'])
    expect(datos[1].querySelector('.dato-valor').textContent.trim()).toBe('—')
    expect(datos[1].querySelector('.dato-valor').classList.contains('sin-valor')).toBe(true)
  })

  // ---------------------------------------------------------------- fechas

  it('fechaFutura rechaza hoy y acepta mañana; el calendario arranca mañana', async () => {
    await abrirCrear()
    expect(campo('campo-licenciaVence').min).toBe(fechaIso(1))

    await cambiarFecha('campo-licenciaVence', fechaIso(0))
    expect(texto()).toContain('La fecha debe ser posterior a hoy')
    expect(campo('campo-licenciaVence').invalido).toBe(true)

    await cambiarFecha('campo-licenciaVence', fechaIso(1))
    expect(texto()).not.toContain('La fecha debe ser posterior a hoy')
  })

  it('porDefecto "hoy" precarga la fecha de hoy al crear y la envía', async () => {
    await abrirCrear()
    await cambiarFecha('campo-licenciaVence', fechaIso(30))
    await elegir('campo-categorias', ['A'])
    expect(campo('campo-fechaContratacion').valor).toBe(fechaIso(0))
    fetch.mockResolvedValueOnce(sobre({ codigo: 'i2' })).mockResolvedValueOnce(lista([INSTRUCTOR]))

    boton('Crear').click()
    await asentar(el)

    expect(cuerpoEnviado(2).fechaContratacion).toBe(fechaIso(0))
  })

  it('al editar, porDefecto "hoy" no pisa el valor del registro', async () => {
    await abrirEditar()

    expect(campo('campo-fechaContratacion').valor).toBe('2019-05-05')
  })

  it('soloSiCambia: editar con el vencimiento ya pasado sin tocarlo guarda; si lo cambia a otra fecha pasada, no', async () => {
    await abrirEditar()
    fetch.mockResolvedValueOnce(sobre(INSTRUCTOR)).mockResolvedValueOnce(lista([INSTRUCTOR]))

    boton('Guardar').click()
    await asentar(el)

    expect(fetch.mock.calls[3][1].method).toBe('PATCH')
    expect(cuerpoEnviado(3).licenciaVence).toBe('2020-01-01')

    el.remove()
    await abrirEditar()
    await cambiarFecha('campo-licenciaVence', '2021-01-01')
    const llamadas = fetch.mock.calls.length
    boton('Guardar').click()
    await asentar(el)

    expect(texto()).toContain('La fecha debe ser posterior a hoy')
    expect(fetch.mock.calls.length).toBe(llamadas)
  })

  it('soloSiCambia no se aplica en el alta: un vencimiento pasado se rechaza', async () => {
    await abrirCrear()
    await cambiarFecha('campo-licenciaVence', '2020-01-01')
    await elegir('campo-categorias', ['A'])

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('La fecha debe ser posterior a hoy')
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
