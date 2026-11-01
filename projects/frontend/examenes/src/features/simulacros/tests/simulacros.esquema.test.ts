import { describe, expect, it } from 'vitest'
import { validar } from '@drivehub/gestion'

import { ESQUEMA_SIMULACROS } from '../schemas/simulacros.esquema'

const camposDeCrear = () => ESQUEMA_SIMULACROS.crear!.secciones.flatMap((s) => s.campos)
const camposDeEditar = () => ESQUEMA_SIMULACROS.editar!.secciones.flatMap((s) => s.campos)
const campo = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!

describe('esquema de simulacros', () => {
  it('apunta al recurso simulacros y los filtros usan las claves del backend', () => {
    expect(ESQUEMA_SIMULACROS.recurso).toBe('simulacros')
    expect(ESQUEMA_SIMULACROS.filtros?.map((f) => f.clave)).toEqual(['filter[categoria]', 'filter[activo]'])
    expect(ESQUEMA_SIMULACROS.filtros?.[1].opciones.map((o) => o.valor)).toEqual(['true', 'false'])
  })

  it('crear y editar comparten los mismos campos, con el mismo orden', () => {
    expect(camposDeEditar().map((c) => c.campo)).toEqual(camposDeCrear().map((c) => c.campo))
    expect(camposDeCrear().map((c) => c.campo)).toEqual([
      'nombre',
      'categoria',
      'duracionMin',
      'nroPreguntas',
      'puntajeAprobacion',
      'reglas',
    ])
  })

  it('las acciones coinciden con los endpoints y solo desactivar pide confirmación', () => {
    const [desactivar, activar] = ESQUEMA_SIMULACROS.acciones!

    expect(desactivar).toMatchObject({ metodo: 'POST', ruta: '{id}/desactivar', confirmar: '¿Desactivar?' })
    expect(desactivar.visibleSi).toEqual({ campo: 'activo', igual: true })
    expect(activar).toMatchObject({ metodo: 'POST', ruta: '{id}/activar' })
    expect(activar.visibleSi).toEqual({ campo: 'activo', igual: false })
    expect(activar.confirmar).toBeUndefined()
  })

  it('el nombre duplicado del backend se muestra bajo el campo nombre', () => {
    expect(ESQUEMA_SIMULACROS.erroresDeCampo?.ERR_ACA_23.campo).toBe('nombre')
  })

  it('las reglas son filas de tema y cantidad, de 1 a 10', () => {
    expect(campo('reglas')).toMatchObject({ tipo: 'reglas', requerido: true, min: 1, max: 10 })
  })

  it('valida los rangos del backend: nombre 3 a 80, duración 5 a 180, preguntas 1 a 100, aprobación 1 a 100', () => {
    expect(validar(campo('nombre'), 'Ab')).not.toBeNull()
    expect(validar(campo('nombre'), 'Simulacro')).toBeNull()
    expect(validar(campo('duracionMin'), '4')).not.toBeNull()
    expect(validar(campo('duracionMin'), '181')).not.toBeNull()
    expect(validar(campo('duracionMin'), '20')).toBeNull()
    expect(validar(campo('nroPreguntas'), '0')).not.toBeNull()
    expect(validar(campo('nroPreguntas'), '101')).not.toBeNull()
    expect(validar(campo('puntajeAprobacion'), '0.5')).not.toBeNull()
    expect(validar(campo('puntajeAprobacion'), '70')).toBeNull()
    expect(validar(campo('puntajeAprobacion'), '100.5')).not.toBeNull()
  })

  it('las filas de reglas se validan con el tipo reglas del paquete', () => {
    expect(validar(campo('reglas'), [{ tema: 'Señales', cantidad: '5' }])).toBeNull()
    expect(validar(campo('reglas'), [])).not.toBeNull()
    expect(validar(campo('reglas'), [{ tema: 'Señales', cantidad: '0' }])).not.toBeNull()
  })
})
