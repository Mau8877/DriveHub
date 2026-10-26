import { validar } from '@drivehub/gestion'

import { ESQUEMA_ASIGNACION_AUTOS } from '../schemas/asignacion-autos.esquema'

const accion = (id: string) => ESQUEMA_ASIGNACION_AUTOS.acciones!.find((a) => a.id === id)!
const camposDe = (id: string) => accion(id).formulario!.secciones.flatMap((s) => s.campos)
const campo = (id: string, nombre: string) => camposDe(id).find((c) => c.campo === nombre)!

describe('esquema de asignación de autos', () => {
  it('apunta al recurso asignaciones-vehiculo e identifica cada fila por el código del auto', () => {
    expect(ESQUEMA_ASIGNACION_AUTOS.recurso).toBe('asignaciones-vehiculo')
    expect(ESQUEMA_ASIGNACION_AUTOS.idCampo).toBe('vehiculoCodigo')
    expect(ESQUEMA_ASIGNACION_AUTOS.crear).toBeUndefined()
    expect(ESQUEMA_ASIGNACION_AUTOS.editar).toBeUndefined()
  })

  it('un auto sin instructor se ve como "Sin asignar"', () => {
    const instructor = ESQUEMA_ASIGNACION_AUTOS.columnas.find(
      (c) => c.campo === 'instructor.nombreCompleto'
    )

    expect(instructor?.vacioTexto).toBe('Sin asignar')
  })

  it('el filtro de asignación usa filter[asignacion] con asignados y sin asignar', () => {
    const filtro = ESQUEMA_ASIGNACION_AUTOS.filtros![0]

    expect(filtro.clave).toBe('filter[asignacion]')
    expect(filtro.opciones.map((o) => o.valor)).toEqual(['ASIGNADO', 'SIN_ASIGNAR'])
  })

  it('las acciones coinciden con los endpoints y su visibilidad depende de si hay instructor', () => {
    expect(accion('asignar')).toMatchObject({ metodo: 'POST', ruta: '{id}/asignar' })
    expect(accion('asignar').visibleSi).toEqual({ campo: 'instructor', igual: null })
    expect(accion('cambiar-instructor')).toMatchObject({
      metodo: 'POST',
      ruta: '{id}/cambiar-instructor',
    })
    expect(accion('cambiar-instructor').visibleSi).toEqual({ campo: 'instructor', distinto: null })
    expect(accion('liberar')).toMatchObject({ metodo: 'POST', ruta: '{id}/liberar' })
    expect(accion('liberar').visibleSi).toEqual({ campo: 'instructor', distinto: null })
  })

  it('asignar y cambiar piden un formulario; solo liberar pide confirmación', () => {
    expect(accion('asignar').formulario).toBeDefined()
    expect(accion('cambiar-instructor').formulario).toBeDefined()
    expect(accion('liberar').formulario).toBeUndefined()
    expect(accion('liberar')).toMatchObject({ confirmar: '¿Liberar el auto?', tono: 'peligro' })
    expect(accion('asignar').confirmar).toBeUndefined()
  })

  it('el instructor se elige entre los activos de la escuela y es obligatorio', () => {
    for (const id of ['asignar', 'cambiar-instructor']) {
      const instructor = campo(id, 'instructorCodigo')

      expect(instructor.tipo).toBe('seleccion')
      expect(instructor.opcionesDe).toMatchObject({
        recurso: 'instructores',
        valor: 'codigo',
        etiqueta: 'nombreCompleto',
        params: { 'filter[estado]': 'ACTIVO' },
      })
      expect(validar(instructor, '')).toBe('Elige un instructor')
      expect(validar(instructor, 'i1')).toBeNull()
    }
  })

  it('la fecha arranca en hoy y no puede ser futura', () => {
    const hoy = new Date(2026, 9, 26)
    for (const id of ['asignar', 'cambiar-instructor']) {
      const fecha = campo(id, 'fecha')

      expect(fecha.porDefecto).toBe('hoy')
      expect(validar(fecha, '2026-10-26', hoy)).toBeNull()
      expect(validar(fecha, '2026-10-01', hoy)).toBeNull()
      expect(validar(fecha, '2026-10-27', hoy)).toBe('La fecha no puede ser futura')
      expect(validar(fecha, '', hoy)).toBe('La fecha es obligatoria')
    }
  })

  it('el historial es un detalle en lista que carga una sola página grande y marca "Vigente"', () => {
    const detalle = ESQUEMA_ASIGNACION_AUTOS.detalle!

    expect(detalle.etiqueta).toBe('Ver historial')
    expect(detalle.cargar).toBe('{id}/historial?pageSize=100')
    expect(detalle.lista?.columnas.map((c) => c.campo)).toEqual([
      'instructor.nombreCompleto',
      'fechaInicio',
      'fechaFin',
    ])
    expect(detalle.lista?.columnas[2].vacioTexto).toBe('Vigente')
  })

  it('los errores del backend con campo van bajo el instructor', () => {
    const errores = ESQUEMA_ASIGNACION_AUTOS.erroresDeCampo!

    for (const codigo of ['ERR_ESC_13', 'ERR_ESC_16', 'ERR_ESC_18']) {
      expect(errores[codigo].campo).toBe('instructorCodigo')
    }
    expect(errores['ERR_ESC_17']).toBeUndefined()
    expect(errores['ERR_ESC_19']).toBeUndefined()
  })
})
