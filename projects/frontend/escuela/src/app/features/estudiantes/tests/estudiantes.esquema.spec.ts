import { validar } from '@drivehub/gestion'

import { ESQUEMA_ESTUDIANTES } from '../schemas/estudiantes.esquema'

const camposDe = (secciones: { campos: { campo: string }[] }[]) =>
  secciones.flatMap((s) => s.campos)
const camposDeCrear = () => ESQUEMA_ESTUDIANTES.crear!.secciones.flatMap((s) => s.campos)
const crear = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!
const camposDeEditar = () => ESQUEMA_ESTUDIANTES.editar!.secciones.flatMap((s) => s.campos)
const camposDeDetalle = () => ESQUEMA_ESTUDIANTES.detalle!.secciones.flatMap((s) => s.campos)

describe('esquema de estudiantes', () => {
  it('apunta al recurso estudiantes (lo atiende Identidad, pero el cliente solo conoce el gateway)', () => {
    expect(ESQUEMA_ESTUDIANTES.recurso).toBe('estudiantes')
    expect(ESQUEMA_ESTUDIANTES.titulo).toBe('Gestionar estudiantes')
    expect(ESQUEMA_ESTUDIANTES.nombreSingular).toBe('estudiante')
  })

  it('las columnas son las del contrato del listado: nombre completo, edad, categoría, incorporación y estado', () => {
    expect(ESQUEMA_ESTUDIANTES.columnas.map((c) => c.campo)).toEqual([
      'nombreCompleto',
      'edad',
      'categoriaSigla',
      'fechaIncorporacion',
      'estado',
    ])
    const [nombre, , categoria, incorporacion, estado] = ESQUEMA_ESTUDIANTES.columnas
    expect(nombre).toMatchObject({ principal: true, icono: 'user', titulo: 'Nombre' })
    expect(categoria).toMatchObject({ codigo: true, titulo: 'Categoría' })
    expect(incorporacion).toMatchObject({ tipo: 'fecha', titulo: 'Incorporación' })
    expect(estado.tipo).toBe('insignia')
    expect(estado.insignias).toEqual({
      ACTIVO: { etiqueta: 'Activo', tono: 'exito' },
      INACTIVO: { etiqueta: 'Inactivo' },
    })
  })

  it('solo el nombre se estira: las columnas de datos son cortas y quedan juntas', () => {
    const [nombre, ...datos] = ESQUEMA_ESTUDIANTES.columnas

    expect(nombre.ancho).toBeUndefined()
    expect(datos.map((c) => c.ancho)).toEqual(['5rem', '7rem', '9.5rem', '7rem'])
  })

  it('busca por nombre y filtra por estado con la clave filter[estado] del backend', () => {
    expect(ESQUEMA_ESTUDIANTES.busqueda?.placeholder).toBe('Buscar por nombre')
    expect(ESQUEMA_ESTUDIANTES.filtros).toHaveLength(1)
    expect(ESQUEMA_ESTUDIANTES.filtros![0]).toMatchObject({
      clave: 'filter[estado]',
      titulo: 'Estado',
      todas: 'Todos',
    })
    expect(ESQUEMA_ESTUDIANTES.filtros![0].opciones).toEqual([
      { valor: 'ACTIVO', etiqueta: 'Activos' },
      { valor: 'INACTIVO', etiqueta: 'Inactivos' },
    ])
  })

  it('el detalle muestra todos los datos del contrato, con fechas e insignias', () => {
    expect(ESQUEMA_ESTUDIANTES.detalle?.titulo).toBe('Datos del estudiante')
    expect(camposDeDetalle().map((c) => c.campo)).toEqual([
      'nombreCompleto',
      'edad',
      'fechaNacimiento',
      'sexo',
      'ci',
      'email',
      'telefono',
      'direccion',
      'categoriaSigla',
      'estado',
      'fechaIncorporacion',
    ])
    const porCampo = (nombre: string) => camposDeDetalle().find((c) => c.campo === nombre)!
    expect(porCampo('fechaNacimiento').tipo).toBe('fecha')
    expect(porCampo('fechaIncorporacion').tipo).toBe('fecha')
    expect(porCampo('sexo').tipo).toBe('insignia')
    expect(porCampo('sexo').insignias).toEqual({
      MASCULINO: { etiqueta: 'Masculino' },
      FEMENINO: { etiqueta: 'Femenino' },
    })
    expect(porCampo('estado').insignias?.['ACTIVO']).toEqual({ etiqueta: 'Activo', tono: 'exito' })
  })

  it('los campos del alta son los del contrato de POST /estudiantes', () => {
    expect(camposDeCrear().map((c) => c.campo)).toEqual([
      'nombres',
      'apellidos',
      'fechaNacimiento',
      'sexo',
      'ci',
      'telefono',
      'email',
      'password',
      'ubicacion',
      'categoriaSigla',
    ])
    expect(ESQUEMA_ESTUDIANTES.crear).toMatchObject({
      etiqueta: 'Nuevo estudiante',
      etiquetaEnviar: 'Crear estudiante',
      mensajeExito: 'Estudiante creado correctamente.',
    })
    expect(crear('ubicacion')).toMatchObject({ tipo: 'ubicacion', requerido: true })
    expect(crear('categoriaSigla')).toMatchObject({
      tipo: 'seleccion',
      porDefecto: 'SIN',
      opcionesDe: { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion' },
    })
    expect(crear('password').ayuda).toBe('La escribes tú y se la comunicas al estudiante.')
  })

  it('el CI exige de 5 a 15 caracteres con letras, números y guion', () => {
    expect(validar(crear('ci'), 'AB12345')).toBeNull()
    expect(validar(crear('ci'), 'AB-123')).toBeNull()
    expect(validar(crear('ci'), 'AB12')).not.toBeNull()
    expect(validar(crear('ci'), 'AB 12345')).not.toBeNull()
    expect(validar(crear('ci'), 'A'.repeat(16))).not.toBeNull()
    expect(validar(crear('ci'), '')).toBe('El CI es obligatorio')
  })

  it('la contraseña exige de 9 a 64 caracteres', () => {
    expect(validar(crear('password'), '12345678')).toBe(
      'La contraseña debe tener entre 9 y 64 caracteres'
    )
    expect(validar(crear('password'), '123456789')).toBeNull()
    expect(validar(crear('password'), 'x'.repeat(65))).not.toBeNull()
  })

  it('un menor de 18 años no pasa y una fecha futura tampoco', () => {
    const hoy = new Date(2026, 9, 23)
    expect(validar(crear('fechaNacimiento'), '2009-10-24', hoy)).toBe(
      'El estudiante debe tener al menos 18 años'
    )
    expect(validar(crear('fechaNacimiento'), '2008-10-23', hoy)).toBeNull()
    expect(validar(crear('fechaNacimiento'), '2999-01-01', hoy)).toBe(
      'La fecha de nacimiento debe estar en el pasado'
    )
    expect(validar(crear('fechaNacimiento'), '', hoy)).toBe('La fecha de nacimiento es obligatoria')
  })

  it('nombres y apellidos son obligatorios y llegan hasta 80 caracteres', () => {
    expect(validar(crear('nombres'), '')).toBe('Los nombres son obligatorios')
    expect(validar(crear('apellidos'), '')).toBe('Los apellidos son obligatorios')
    expect(validar(crear('nombres'), 'x'.repeat(81))).not.toBeNull()
    expect(validar(crear('nombres'), 'Ana María')).toBeNull()
  })

  it('el sexo, el correo y la ubicación son obligatorios; el teléfono es opcional', () => {
    expect(validar(crear('sexo'), '')).toBe('Elige el sexo')
    expect(validar(crear('email'), '')).toBe('El correo es obligatorio')
    expect(validar(crear('ubicacion'), null)).toBe('Elige la ubicación en el mapa')
    expect(validar(crear('telefono'), '')).toBeNull()
  })

  it('al editar solo son editables la categoría y la ubicación; lo demás es de solo lectura', () => {
    const editar = ESQUEMA_ESTUDIANTES.editar!
    expect(editar.metodo).toBe('PATCH')
    expect(editar.cargar).toBeUndefined()
    expect(editar.ruta).toBeUndefined()
    expect(editar.mensajeExito).toBe('Estudiante actualizado correctamente.')

    const editables = camposDeEditar()
      .filter((c) => !('soloLectura' in c && c.soloLectura))
      .map((c) => c.campo)
    expect(editables).toEqual(['categoriaSigla', 'ubicacion'])
    for (const nombre of ['nombreCompleto', 'ci', 'email']) {
      expect(camposDeEditar().find((c) => c.campo === nombre)).toMatchObject({ soloLectura: true })
    }
    expect(camposDeEditar().find((c) => c.campo === 'ubicacion')).toMatchObject({
      tipo: 'ubicacion',
      direccionDe: 'direccion',
    })
    expect(camposDeEditar().find((c) => c.campo === 'categoriaSigla')).toMatchObject({
      tipo: 'seleccion',
      opcionesDe: { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion' },
    })
  })

  it('los errores del backend caen en su campo', () => {
    expect(ESQUEMA_ESTUDIANTES.erroresDeCampo).toEqual({
      ERR_IDE_01: { campo: 'email', mensaje: 'Este correo ya está registrado' },
      ERR_IDE_02: {
        campo: 'fechaNacimiento',
        mensaje: 'El estudiante debe tener al menos 18 años',
      },
      ERR_IDE_19: { campo: 'ci', mensaje: 'Este CI ya está registrado' },
      ERR_IDE_21: { campo: 'categoriaSigla', mensaje: 'La categoría ya no existe' },
    })
    const camposQueExisten = new Set([...camposDeCrear(), ...camposDeEditar()].map((c) => c.campo))
    for (const regla of Object.values(ESQUEMA_ESTUDIANTES.erroresDeCampo!)) {
      expect(camposQueExisten.has(regla.campo)).toBe(true)
    }
  })

  it('los mensajes de la pantalla están en español', () => {
    expect(ESQUEMA_ESTUDIANTES.mensajes).toEqual({
      vacio: 'Todavía no hay estudiantes en tu escuela.',
      sinResultados: 'Sin resultados.',
      errorGuardar: 'No pudimos guardar el estudiante. Inténtalo de nuevo más tarde.',
    })
  })

  it('no tiene acciones ni nada de saldo (Pagos todavía no existe)', () => {
    expect(ESQUEMA_ESTUDIANTES.acciones).toBeUndefined()
    const todo = JSON.stringify(ESQUEMA_ESTUDIANTES).toLowerCase()
    expect(todo).not.toContain('saldo')
    expect(todo).not.toContain('pendiente')
    expect(todo).not.toContain('monto')
    expect(camposDe(ESQUEMA_ESTUDIANTES.detalle!.secciones).map((c) => c.campo)).not.toContain(
      'saldo'
    )
  })
})
