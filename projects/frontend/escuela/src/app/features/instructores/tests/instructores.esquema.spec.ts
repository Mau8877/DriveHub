import { validar } from '@drivehub/gestion'

import { ESQUEMA_INSTRUCTORES } from '../schemas/instructores.esquema'

const aplanar = (secciones: { campos: { campo: string }[] }[]) => secciones.flatMap((s) => s.campos)
const camposDeCrear = () => ESQUEMA_INSTRUCTORES.crear!.secciones.flatMap((s) => s.campos)
const crear = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!
const camposDeEditar = () => ESQUEMA_INSTRUCTORES.editar!.secciones.flatMap((s) => s.campos)
const editar = (nombre: string) => camposDeEditar().find((c) => c.campo === nombre)!
const camposDeDetalle = () => aplanar(ESQUEMA_INSTRUCTORES.detalle!.secciones)

// * Fija para no depender del día en que corren las pruebas
const HOY = new Date(2026, 9, 24)

describe('esquema de instructores', () => {
  it('apunta al recurso instructores y se llama Instructores', () => {
    expect(ESQUEMA_INSTRUCTORES.recurso).toBe('instructores')
    expect(ESQUEMA_INSTRUCTORES.titulo).toBe('Instructores')
    expect(ESQUEMA_INSTRUCTORES.nombreSingular).toBe('instructor')
    expect(ESQUEMA_INSTRUCTORES.crear?.etiqueta).toBe('Nuevo instructor')
  })

  it('las columnas son las del contrato del listado, con etiquetas para las categorías', () => {
    expect(ESQUEMA_INSTRUCTORES.columnas.map((c) => c.campo)).toEqual([
      'fotoUrl',
      'nombreCompleto',
      'nroLicencia',
      'licenciaVence',
      'categorias',
      'estado',
    ])
    const [foto, nombre, licencia, vence, categorias, estado] = ESQUEMA_INSTRUCTORES.columnas
    expect(foto).toMatchObject({ tipo: 'foto', nombre: 'nombreCompleto' })
    expect(nombre).toMatchObject({ principal: true, titulo: 'Nombre' })
    expect(licencia).toMatchObject({ codigo: true })
    expect(vence).toMatchObject({ tipo: 'fecha' })
    expect(categorias).toMatchObject({ tipo: 'etiquetas', etiqueta: 'sigla' })
    expect(estado.tipo).toBe('insignia')
    expect(estado.insignias).toEqual({
      ACTIVO: { etiqueta: 'Activo', tono: 'exito' },
      LICENCIA: { etiqueta: 'Licencia', tono: 'ambar' },
      INACTIVO: { etiqueta: 'Inactivo' },
    })
  })

  it('busca por nombre y filtra por los tres estados con filter[estado]', () => {
    expect(ESQUEMA_INSTRUCTORES.busqueda?.placeholder).toBe('Buscar por nombre')
    expect(ESQUEMA_INSTRUCTORES.filtros).toHaveLength(1)
    expect(ESQUEMA_INSTRUCTORES.filtros![0]).toMatchObject({
      clave: 'filter[estado]',
      todas: 'Todos',
    })
    expect(ESQUEMA_INSTRUCTORES.filtros![0].opciones.map((o) => o.valor)).toEqual([
      'ACTIVO',
      'LICENCIA',
      'INACTIVO',
    ])
  })

  it('el detalle muestra la persona, la ubicación y la ficha profesional', () => {
    expect(camposDeDetalle().map((c) => c.campo)).toEqual([
      'fotoUrl',
      'nombreCompleto',
      'edad',
      'fechaNacimiento',
      'sexo',
      'ci',
      'email',
      'telefono',
      'direccion',
      'latitud',
      'longitud',
      'nroLicencia',
      'licenciaVence',
      'fechaContratacion',
      'fechaAlta',
      'categorias',
      'estado',
    ])
    const categorias = camposDeDetalle().find((c) => c.campo === 'categorias')!
    expect(categorias).toMatchObject({ tipo: 'etiquetas', etiqueta: 'sigla' })
  })

  it('las acciones por fila son Ver detalles y Editar: no se elimina', () => {
    expect(ESQUEMA_INSTRUCTORES.detalle).toBeDefined()
    expect(ESQUEMA_INSTRUCTORES.editar).toBeDefined()
    expect(ESQUEMA_INSTRUCTORES.acciones).toBeUndefined()
  })

  it('mapea los errores del backend a su campo, con el mensaje del backend', () => {
    expect(ESQUEMA_INSTRUCTORES.erroresDeCampo).toEqual({
      ERR_IDE_01: { campo: 'email', mensaje: 'Este correo ya está registrado' },
      ERR_IDE_02: {
        campo: 'fechaNacimiento',
        mensaje: 'El instructor debe tener al menos 18 años',
      },
      ERR_IDE_19: { campo: 'ci', mensaje: 'Este CI ya está registrado' },
      ERR_ESC_12: { campo: 'nroLicencia', mensaje: 'Este número de licencia ya está en uso' },
      ERR_ESC_14: { campo: 'categorias', mensaje: 'Una de las categorías no es válida' },
    })
  })
})

describe('alta de un instructor', () => {
  it('tiene las cuatro secciones: datos personales, acceso, ubicación y ficha profesional', () => {
    expect(ESQUEMA_INSTRUCTORES.crear!.secciones.map((s) => s.titulo)).toEqual([
      'Datos personales',
      'Acceso',
      'Ubicación',
      'Ficha profesional',
    ])
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
      'nroLicencia',
      'licenciaVence',
      'fechaContratacion',
      'categorias',
    ])
  })

  it('el número de licencia es obligatorio, de 3 a 30 caracteres y solo letras, números y guion', () => {
    const licencia = crear('nroLicencia')

    expect(validar(licencia, '')).toBe('El número de licencia es obligatorio')
    expect(validar(licencia, 'AB')).toContain('entre 3 y 30')
    expect(validar(licencia, 'A'.repeat(31))).toContain('entre 3 y 30')
    expect(validar(licencia, 'AB 12')).toContain('entre 3 y 30')
    expect(validar(licencia, 'AB#12')).toContain('entre 3 y 30')
    expect(validar(licencia, 'lic-123')).toBeNull()
    expect(validar(licencia, 'A'.repeat(30))).toBeNull()
  })

  it('el vencimiento debe ser posterior a hoy: hoy se rechaza y mañana se acepta', () => {
    const vence = crear('licenciaVence')

    expect(validar(vence, '', HOY)).toBe('El vencimiento de la licencia es obligatorio')
    expect(validar(vence, '2026-10-24', HOY)).toBe('La licencia debe vencer después de hoy')
    expect(validar(vence, '2026-10-23', HOY)).toBe('La licencia debe vencer después de hoy')
    expect(validar(vence, '2026-10-25', HOY)).toBeNull()
  })

  it('la contratación empieza siendo hoy, hoy es válido y una fecha futura se rechaza', () => {
    const contratacion = crear('fechaContratacion')

    expect(contratacion.porDefecto).toBe('hoy')
    expect(validar(contratacion, '2026-10-24', HOY)).toBeNull()
    expect(validar(contratacion, '2020-01-01', HOY)).toBeNull()
    expect(validar(contratacion, '2026-10-25', HOY)).toBe(
      'La fecha de contratación no puede ser futura'
    )
    expect(validar(contratacion, '', HOY)).toBeNull()
  })

  it('las categorías son una selección múltiple del catálogo, obligatoria y sin SIN', () => {
    const categorias = crear('categorias')

    expect(categorias).toMatchObject({
      tipo: 'seleccion',
      multiple: true,
      requerido: true,
      valorDe: 'sigla',
      opcionesDe: {
        recurso: 'categorias-licencia',
        valor: 'sigla',
        etiqueta: 'descripcion',
        excluir: ['SIN'],
      },
    })
    expect(validar(categorias, [])).toBe('Elige al menos una categoría')
    expect(validar(categorias, ['B'])).toBeNull()
  })

  it('exige ser mayor de 18 años y que la fecha de nacimiento esté en el pasado', () => {
    const nacimiento = crear('fechaNacimiento')

    expect(validar(nacimiento, '2010-01-01', HOY)).toBe('El instructor debe tener al menos 18 años')
    expect(validar(nacimiento, '2026-10-24', HOY)).toBe(
      'La fecha de nacimiento debe estar en el pasado'
    )
    expect(validar(nacimiento, '2000-01-01', HOY)).toBeNull()
  })

  it('el acceso usa las mismas reglas que los estudiantes: CI de 5 a 15 y contraseña de 9 a 64', () => {
    expect(validar(crear('ci'), 'AB')).toContain('entre 5 y 15')
    expect(validar(crear('ci'), 'AB12345')).toBeNull()
    expect(validar(crear('password'), '12345678')).toBe(
      'La contraseña debe tener entre 9 y 64 caracteres'
    )
    expect(validar(crear('password'), '123456789')).toBeNull()
    expect(validar(crear('email'), 'no-es-correo')).toBe('El correo no es válido')
  })

  it('la ubicación es obligatoria', () => {
    expect(validar(crear('ubicacion'), { latitud: null, longitud: null })).toBe(
      'Elige la ubicación en el mapa'
    )
  })
})

describe('edición de un instructor', () => {
  it('es un PATCH y solo la ficha profesional y la ubicación son editables', () => {
    expect(ESQUEMA_INSTRUCTORES.editar!.metodo).toBe('PATCH')
    const editables = camposDeEditar()
      .filter((c) => !c.soloLectura)
      .map((c) => c.campo)

    expect(editables).toEqual([
      'nroLicencia',
      'licenciaVence',
      'fechaContratacion',
      'estado',
      'categorias',
      'ubicacion',
    ])
  })

  it('el nombre, el CI y el correo se muestran de solo lectura (no viajan)', () => {
    for (const nombre of ['nombreCompleto', 'ci', 'email']) {
      expect(editar(nombre).soloLectura).toBe(true)
    }
    for (const personal of [
      'nombres',
      'apellidos',
      'fechaNacimiento',
      'sexo',
      'telefono',
      'password',
    ]) {
      expect(camposDeEditar().some((c) => c.campo === personal)).toBe(false)
    }
  })

  it('el vencimiento solo se valida si cambia: una licencia vencida no impide guardar', () => {
    const vence = editar('licenciaVence')

    expect(vence.soloSiCambia).toBe(true)
    expect(validar(vence, '2020-01-01', HOY, { valorInicial: '2020-01-01' })).toBeNull()
    expect(validar(vence, '2021-01-01', HOY, { valorInicial: '2020-01-01' })).toBe(
      'La licencia debe vencer después de hoy'
    )
    expect(validar(vence, '2027-01-01', HOY, { valorInicial: '2020-01-01' })).toBeNull()
  })

  it('el estado ofrece los tres valores', () => {
    expect(editar('estado').opciones!.map((o) => o.valor)).toEqual([
      'ACTIVO',
      'LICENCIA',
      'INACTIVO',
    ])
  })

  it('las categorías y la licencia se validan igual que en el alta', () => {
    expect(validar(editar('categorias'), [])).toBe('Elige al menos una categoría')
    expect(validar(editar('nroLicencia'), 'AB')).toContain('entre 3 y 30')
  })

  it('la ubicación muestra la dirección ya guardada', () => {
    expect(editar('ubicacion')).toMatchObject({
      tipo: 'ubicacion',
      requerido: true,
      direccionDe: 'direccion',
    })
  })
})
