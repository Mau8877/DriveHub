import { describe, expect, it } from 'vitest'
import { validar } from '@drivehub/gestion'

import { ESQUEMA_PREGUNTAS } from '../schemas/preguntas.esquema'

const camposDeCrear = () => ESQUEMA_PREGUNTAS.crear!.secciones.flatMap((s) => s.campos)
const camposDeEditar = () => ESQUEMA_PREGUNTAS.editar!.secciones.flatMap((s) => s.campos)
const campo = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!

describe('esquema de preguntas', () => {
  it('apunta al recurso preguntas y los filtros usan las claves del backend', () => {
    expect(ESQUEMA_PREGUNTAS.recurso).toBe('preguntas')
    expect(ESQUEMA_PREGUNTAS.filtros?.map((f) => f.clave)).toEqual([
      'filter[categoria]',
      'filter[tipo]',
      'filter[dificultad]',
      'filter[activa]',
    ])
    expect(ESQUEMA_PREGUNTAS.filtros?.[3].opciones.map((o) => o.valor)).toEqual(['true', 'false'])
  })

  it('crear y editar comparten los mismos campos, con el mismo orden', () => {
    expect(camposDeEditar().map((c) => c.campo)).toEqual(camposDeCrear().map((c) => c.campo))
    expect(camposDeCrear().map((c) => c.campo)).toEqual([
      'categoria',
      'tema',
      'tipo',
      'dificultad',
      'enunciado',
      'opciones',
      'explicacion',
    ])
  })

  it('las acciones coinciden con los endpoints y solo desactivar pide confirmación', () => {
    const [desactivar, activar] = ESQUEMA_PREGUNTAS.acciones!

    expect(desactivar).toMatchObject({
      metodo: 'POST',
      ruta: '{id}/desactivar',
      confirmar: '¿Desactivar?',
    })
    expect(desactivar.visibleSi).toEqual({ campo: 'activa', igual: true })
    expect(activar).toMatchObject({ metodo: 'POST', ruta: '{id}/activar' })
    expect(activar.visibleSi).toEqual({ campo: 'activa', igual: false })
    expect(activar.confirmar).toBeUndefined()
  })

  it('el enunciado y la explicación son de texto largo, las opciones son filas y la dificultad viaja como número', () => {
    expect(campo('enunciado')).toMatchObject({ tipo: 'textarea', requerido: true })
    expect(campo('explicacion')).toMatchObject({ tipo: 'textarea' })
    expect(campo('explicacion').requerido).toBeUndefined()
    expect(campo('opciones')).toMatchObject({ tipo: 'opciones', requerido: true, min: 2, max: 6 })
    expect(campo('dificultad')).toMatchObject({ tipo: 'seleccion', numerico: true })
  })

  it('la tabla muestra la dificultad y el estado como insignias y el estado booleano por sus claves', () => {
    const dificultad = ESQUEMA_PREGUNTAS.columnas.find((c) => c.campo === 'dificultad')!
    const activa = ESQUEMA_PREGUNTAS.columnas.find((c) => c.campo === 'activa')!

    expect(Object.keys(dificultad.insignias!)).toEqual(['1', '2', '3'])
    expect(Object.keys(activa.insignias!).sort()).toEqual(['false', 'true'])
  })

  it('el enunciado va de 5 a 500 caracteres y el tema de 2 a 80', () => {
    expect(validar(campo('enunciado'), '¿Qué indica el PARE?')).toBeNull()
    expect(validar(campo('enunciado'), 'Hola')).not.toBeNull()
    expect(validar(campo('enunciado'), 'a'.repeat(501))).not.toBeNull()
    expect(validar(campo('enunciado'), '')).toBe('El enunciado es obligatorio')
    expect(validar(campo('tema'), 'Señales')).toBeNull()
    expect(validar(campo('tema'), 'a')).not.toBeNull()
    expect(validar(campo('tema'), 'a'.repeat(81))).not.toBeNull()
  })

  it('la categoría, el tipo y la dificultad son una de las opciones ofrecidas', () => {
    expect(validar(campo('categoria'), 'B')).toBeNull()
    expect(validar(campo('categoria'), 'Z')).not.toBeNull()
    expect(validar(campo('tipo'), 'VERDADERO_FALSO')).toBeNull()
    expect(validar(campo('tipo'), 'ADIVINAR')).not.toBeNull()
    expect(validar(campo('dificultad'), '3')).toBeNull()
    expect(validar(campo('dificultad'), '4')).not.toBeNull()
    expect(validar(campo('dificultad'), '')).toBe('La dificultad es obligatoria')
  })

  it('las opciones piden de 2 a 6 filas con texto y al menos una correcta', () => {
    const fila = (texto: string, correcta = false) => ({ texto, correcta })

    expect(validar(campo('opciones'), [fila('Detenerse', true), fila('Seguir')])).toBeNull()
    expect(validar(campo('opciones'), [fila('Solo una', true)])).toBe('Agrega al menos 2 opciones')
    expect(validar(campo('opciones'), [fila('Uno'), fila('Dos')])).toBe(
      'Marca al menos una opción correcta'
    )
    expect(validar(campo('opciones'), [fila('Uno', true), fila('')])).toBe(
      'Todas las opciones necesitan texto'
    )
  })
})
