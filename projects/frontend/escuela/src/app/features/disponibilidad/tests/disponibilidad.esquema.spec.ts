import { ESQUEMA_DISPONIBILIDAD } from '../schemas/disponibilidad.esquema'

const columna = (campo: string) => ESQUEMA_DISPONIBILIDAD.columnas.find((c) => c.campo === campo)!
const filtro = (clave: string) => ESQUEMA_DISPONIBILIDAD.filtros!.find((f) => f.clave === clave)!

describe('esquema de disponibilidad', () => {
  it('apunta a disponibilidad-instructores e identifica cada fila por el código de la ficha', () => {
    expect(ESQUEMA_DISPONIBILIDAD.recurso).toBe('disponibilidad-instructores')
    expect(ESQUEMA_DISPONIBILIDAD.idCampo).toBe('instructorCodigo')
  })

  it('es de solo lectura: sin crear, editar, acciones ni detalle', () => {
    expect(ESQUEMA_DISPONIBILIDAD.crear).toBeUndefined()
    expect(ESQUEMA_DISPONIBILIDAD.editar).toBeUndefined()
    expect(ESQUEMA_DISPONIBILIDAD.acciones).toBeUndefined()
    expect(ESQUEMA_DISPONIBILIDAD.detalle).toBeUndefined()
  })

  it('las columnas son las del contrato, con el instructor como columna principal', () => {
    expect(ESQUEMA_DISPONIBILIDAD.columnas.map((c) => c.campo)).toEqual([
      'nombreCompleto',
      'disponibilidad',
      'libres',
      'reservadas',
      'bloqueadas',
      'autos',
      'estadoAutos',
    ])
    expect(columna('nombreCompleto').principal).toBe(true)
  })

  it('la disponibilidad se ve como insignia con los cuatro valores del backend', () => {
    const disponibilidad = columna('disponibilidad')

    expect(disponibilidad.tipo).toBe('insignia')
    expect(Object.keys(disponibilidad.insignias!)).toEqual([
      'DISPONIBLE',
      'COMPLETO',
      'SIN_HORARIO',
      'NO_DISPONIBLE',
    ])
    expect(disponibilidad.insignias!['DISPONIBLE']).toEqual({
      etiqueta: 'Disponible',
      tono: 'exito',
    })
    expect(disponibilidad.insignias!['NO_DISPONIBLE'].etiqueta).toBe('No disponible')
  })

  it('el estado de los autos se ve como insignia con los tres valores del backend', () => {
    const estado = columna('estadoAutos')

    expect(estado.tipo).toBe('insignia')
    expect(Object.keys(estado.insignias!)).toEqual(['DISPONIBLE', 'NO_DISPONIBLE', 'SIN_AUTO'])
  })

  it('las placas se dibujan como etiquetas y un instructor sin autos dice "Sin auto"', () => {
    const autos = columna('autos')

    expect(autos.tipo).toBe('etiquetas')
    expect(autos.etiqueta).toBe('placa')
    expect(autos.vacioTexto).toBe('Sin auto')
  })

  it('la búsqueda cubre el instructor y los datos de su auto', () => {
    expect(ESQUEMA_DISPONIBILIDAD.busqueda?.placeholder).toBe(
      'Buscar por instructor, placa, marca o modelo'
    )
  })

  it('los filtros son los del contrato y el día va primero', () => {
    expect(ESQUEMA_DISPONIBILIDAD.filtros!.map((f) => f.clave)).toEqual([
      'filter[fecha]',
      'filter[momento]',
      'filter[disponibilidad]',
      'filter[auto]',
    ])
  })

  it('el día es un filtro de fecha que arranca en hoy y no deja elegir días pasados', () => {
    expect(filtro('filter[fecha]')).toMatchObject({
      tipo: 'fecha',
      porDefecto: 'hoy',
      desdeHoy: true,
    })
  })

  it('el momento del día ofrece mañana, tarde y noche, y "Todo el día" lo quita', () => {
    const momento = filtro('filter[momento]')

    expect(momento.opciones.map((o) => o.valor)).toEqual(['MANANA', 'TARDE', 'NOCHE'])
    expect(momento.todas).toBe('Todo el día')
  })

  it('los filtros de disponibilidad y de autos usan los valores del backend', () => {
    expect(filtro('filter[disponibilidad]').opciones.map((o) => o.valor)).toEqual([
      'DISPONIBLE',
      'COMPLETO',
      'SIN_HORARIO',
      'NO_DISPONIBLE',
    ])
    expect(filtro('filter[auto]').opciones.map((o) => o.valor)).toEqual([
      'DISPONIBLE',
      'NO_DISPONIBLE',
      'SIN_AUTO',
    ])
  })

  it('los mensajes de vacío y de sin resultados están en español', () => {
    expect(ESQUEMA_DISPONIBILIDAD.mensajes?.vacio).toContain('instructores')
    expect(ESQUEMA_DISPONIBILIDAD.mensajes?.sinResultados).toContain('coincide')
  })
})
