import { describe, expect, it } from 'vitest'
import { validar } from '@drivehub/gestion'

import { ESQUEMA_CONDUCCION } from '../schemas/conduccion.esquema'

const camposDeCrear = () => ESQUEMA_CONDUCCION.crear!.secciones.flatMap((s) => s.campos)
const camposDeEditar = () => ESQUEMA_CONDUCCION.editar!.secciones.flatMap((s) => s.campos)
const campo = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!

describe('esquema de examen de conducción', () => {
  it('apunta al recurso examenes-conduccion y los filtros usan las claves del backend', () => {
    expect(ESQUEMA_CONDUCCION.recurso).toBe('examenes-conduccion')
    expect(ESQUEMA_CONDUCCION.filtros?.map((f) => f.clave)).toEqual(['filter[categoria]', 'filter[estado]'])
    expect(ESQUEMA_CONDUCCION.filtros?.[1].opciones.map((o) => o.valor)).toEqual([
      'PROGRAMADO',
      'REALIZADO',
      'CANCELADO',
      'NO_ASISTIO',
    ])
  })

  it('programar pide estudiante, categoría, fecha, hora, evaluador y vehículo, elegidos de las listas de la escuela', () => {
    expect(camposDeCrear().map((c) => c.campo)).toEqual([
      'estudianteCodigo',
      'categoria',
      'fecha',
      'hora',
      'evaluadorCodigo',
      'vehiculoCodigo',
    ])
    expect(campo('estudianteCodigo').opcionesDe).toMatchObject({ recurso: 'estudiantes', valor: 'codigo' })
    expect(campo('evaluadorCodigo').opcionesDe).toMatchObject({ recurso: 'instructores', valor: 'codigo' })
    expect(campo('vehiculoCodigo').opcionesDe).toMatchObject({ recurso: 'vehiculos', valor: 'codigo' })
  })

  it('reprogramar solo cambia fecha, hora, evaluador y vehículo (sin obligatorios) y solo en uno programado', () => {
    expect(camposDeEditar().map((c) => c.campo)).toEqual(['fecha', 'hora', 'evaluadorCodigo', 'vehiculoCodigo'])
    expect(camposDeEditar().every((c) => !c.requerido)).toBe(true)
    expect(ESQUEMA_CONDUCCION.editar!.visibleSi).toEqual({ campo: 'estado', igual: 'PROGRAMADO' })
  })

  it('las acciones coinciden con los endpoints, son solo de un examen programado y las que cierran piden confirmación', () => {
    const [registrar, noAsistio, cancelar] = ESQUEMA_CONDUCCION.acciones!

    expect(registrar).toMatchObject({ metodo: 'POST', ruta: '{id}/registrar-resultado' })
    expect(registrar.formulario!.secciones[0].campos.map((c) => c.campo)).toEqual(['puntaje', 'observaciones'])
    expect(noAsistio).toMatchObject({ ruta: '{id}/no-asistio', confirmar: '¿Marcar que no asistió?' })
    expect(cancelar).toMatchObject({ ruta: '{id}/cancelar', confirmar: '¿Cancelar el examen?', tono: 'peligro' })
    for (const accion of [registrar, noAsistio, cancelar]) {
      expect(accion.visibleSi).toEqual({ campo: 'estado', igual: 'PROGRAMADO' })
    }
  })

  it('los errores del backend se muestran bajo su campo', () => {
    expect(ESQUEMA_CONDUCCION.erroresDeCampo?.ERR_ACA_26.campo).toBe('categoria')
    expect(ESQUEMA_CONDUCCION.erroresDeCampo?.ERR_ACA_27.campo).toBe('estudianteCodigo')
  })

  it('la hora debe ser HH:mm y la fecha posterior a hoy', () => {
    const hoy = new Date(2026, 10, 2)

    expect(validar(campo('hora'), '09:30')).toBeNull()
    expect(validar(campo('hora'), '25:00')).not.toBeNull()
    expect(validar(campo('hora'), '9:30')).not.toBeNull()
    expect(validar(campo('fecha'), '2026-11-02', hoy)).not.toBeNull()
    expect(validar(campo('fecha'), '2026-11-03', hoy)).toBeNull()
  })

  it('el puntaje va de 0 a 100', () => {
    const puntaje = ESQUEMA_CONDUCCION.acciones![0].formulario!.secciones[0].campos[0]

    expect(validar(puntaje, '85,5')).toBeNull()
    expect(validar(puntaje, '101')).not.toBeNull()
    expect(validar(puntaje, '')).not.toBeNull()
  })
})
