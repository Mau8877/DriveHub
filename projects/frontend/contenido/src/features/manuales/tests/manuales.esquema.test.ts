import { describe, expect, it } from 'vitest'
import { validar } from '@drivehub/gestion'

import { ESQUEMA_MANUALES } from '../schemas/manuales.esquema'

const camposDeCrear = () => ESQUEMA_MANUALES.crear!.secciones.flatMap((s) => s.campos)
const camposDeEditar = () => ESQUEMA_MANUALES.editar!.secciones.flatMap((s) => s.campos)
const campo = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!

describe('esquema de manuales', () => {
  it('apunta al recurso manuales y los filtros usan las claves del backend', () => {
    expect(ESQUEMA_MANUALES.recurso).toBe('manuales')
    expect(ESQUEMA_MANUALES.filtros?.map((f) => f.clave)).toEqual([
      'filter[estado]',
      'filter[categoria]',
    ])
    expect(ESQUEMA_MANUALES.filtros?.[0].opciones.map((o) => o.valor)).toEqual([
      'BORRADOR',
      'PUBLICADO',
      'ARCHIVADO',
    ])
  })

  it('las acciones coinciden con los endpoints y solo archivar pide confirmación', () => {
    const [publicar, publicarDeNuevo, archivar] = ESQUEMA_MANUALES.acciones!

    expect(publicar).toMatchObject({ metodo: 'POST', ruta: '{id}/publicar' })
    expect(publicar.visibleSi).toEqual({ campo: 'estado', igual: 'BORRADOR' })
    expect(publicarDeNuevo).toMatchObject({ metodo: 'POST', ruta: '{id}/publicar' })
    expect(publicarDeNuevo.visibleSi).toEqual({ campo: 'estado', igual: 'ARCHIVADO' })
    expect(archivar).toMatchObject({ ruta: '{id}/archivar', confirmar: '¿Archivar?' })
    expect(archivar.visibleSi).toEqual({ campo: 'estado', igual: 'PUBLICADO' })
    expect(publicar.confirmar).toBeUndefined()
  })

  it('la columna PDF es un enlace al archivo', () => {
    expect(ESQUEMA_MANUALES.columnas.find((c) => c.titulo === 'PDF')).toMatchObject({
      campo: 'archivo.url',
      tipo: 'enlace',
    })
  })

  it('el PDF se sube a manuales/archivos: obligatorio al crear, opcional al editar', () => {
    const alEditar = camposDeEditar().find((c) => c.campo === 'archivo')!

    expect(campo('archivo')).toMatchObject({
      tipo: 'archivo',
      subida: 'manuales/archivos',
      accept: 'application/pdf',
      requerido: true,
    })
    expect(alEditar).toMatchObject({ tipo: 'archivo', subida: 'manuales/archivos' })
    expect(alEditar.requerido).toBeUndefined()
  })

  it('un manual archivado no se edita y su error de archivo cae bajo el campo', () => {
    expect(ESQUEMA_MANUALES.editar?.visibleSi).toEqual({ campo: 'estado', distinto: 'ARCHIVADO' })
    expect(ESQUEMA_MANUALES.editar?.bloqueo).toMatchObject({ campo: 'estado', valor: 'ARCHIVADO' })
    expect(ESQUEMA_MANUALES.erroresDeCampo).toMatchObject({ ERR_CON_03: { campo: 'archivo' } })
  })

  it('el título va de 3 a 150 caracteres y es obligatorio', () => {
    expect(validar(campo('titulo'), 'Señales')).toBeNull()
    expect(validar(campo('titulo'), 'ab')).not.toBeNull()
    expect(validar(campo('titulo'), 'a'.repeat(151))).not.toBeNull()
    expect(validar(campo('titulo'), '')).toBe('El título es obligatorio')
  })

  it('la categoría es una de las siglas ofrecidas', () => {
    expect(validar(campo('categoria'), 'GENERAL')).toBeNull()
    expect(validar(campo('categoria'), 'B')).toBeNull()
    expect(validar(campo('categoria'), 'Z')).not.toBeNull()
    expect(validar(campo('categoria'), '')).toBe('La categoría es obligatoria')
  })
})
