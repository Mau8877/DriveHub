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

  it('seleccion con opciones fijas dibuja un select con sus etiquetas', async () => {
    await abrirCrearPerfil()

    const opciones = [...el.shadowRoot.querySelectorAll('#campo-sexo option')].map((o) => o.textContent)
    expect(opciones).toEqual(['Elige una opción', 'Masculino', 'Femenino'])
  })

  it('seleccion con opcionesDe pide el recurso con pageSize 100 y dibuja sus etiquetas', async () => {
    await abrirCrearPerfil()

    expect(fetch.mock.calls[1][0]).toBe('http://api.test/v1/categorias-licencia?page=1&pageSize=100')
    const opciones = [...el.shadowRoot.querySelectorAll('#campo-categoriaSigla option')].map((o) => o.textContent)
    expect(opciones).toEqual(['Elige una opción', 'Particular', 'Profesional B', 'Sin categoría'])
  })

  it('porDefecto preselecciona la opción al crear', async () => {
    await abrirCrearPerfil()

    expect(el.shadowRoot.querySelector('#campo-categoriaSigla').value).toBe('SIN')
  })

  it('mientras las opciones cargan, el select está deshabilitado con "Cargando…"', async () => {
    await montarPerfil()
    fetch.mockReturnValueOnce(new Promise(() => {}))
    boton('Nuevo estudiante').click()
    await asentar(el)

    const select = el.shadowRoot.querySelector('#campo-categoriaSigla')
    expect(select.disabled).toBe(true)
    expect(select.textContent).toContain('Cargando')
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

    expect(el.shadowRoot.querySelectorAll('#campo-categoriaSigla option')).toHaveLength(4)
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
    const select = el.shadowRoot.querySelector('#campo-sexo')
    select.value = 'FEMENINO'
    select.dispatchEvent(new Event('change', { bubbles: true }))
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
    const select = el.shadowRoot.querySelector('#campo-sexo')
    select.value = 'FEMENINO'
    select.dispatchEvent(new Event('change', { bubbles: true }))
    await asentar(el)

    boton('Crear').click()
    await asentar(el)

    expect(texto()).toContain('Elige la ubicación en el mapa')
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('un error del backend asignado a la seleccion se muestra bajo ese campo', async () => {
    await abrirCrearPerfil()
    const select = el.shadowRoot.querySelector('#campo-sexo')
    select.value = 'FEMENINO'
    select.dispatchEvent(new Event('change', { bubbles: true }))
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
    expect(el.shadowRoot.querySelector('#campo-categoriaSigla').value).toBe('B')

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
})
