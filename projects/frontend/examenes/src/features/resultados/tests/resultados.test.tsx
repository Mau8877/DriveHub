import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'

import { ESQUEMA_RESULTADOS } from '../schemas/resultados.esquema'
import { ResultadosGestion } from '../screens/ResultadosGestion'

describe('esquema de resultados', () => {
  it('apunta al recurso resultados, es de solo lectura y filtra por tipo de evaluación', () => {
    expect(ESQUEMA_RESULTADOS.recurso).toBe('resultados')
    expect(ESQUEMA_RESULTADOS.crear).toBeUndefined()
    expect(ESQUEMA_RESULTADOS.editar).toBeUndefined()
    expect(ESQUEMA_RESULTADOS.acciones).toBeUndefined()
    expect(ESQUEMA_RESULTADOS.filtros?.map((f) => f.clave)).toEqual(['filter[origen]'])
    expect(ESQUEMA_RESULTADOS.filtros?.[0].opciones.map((o) => o.valor)).toEqual([
      'SIMULACRO',
      'PRUEBA_CLASE',
      'EXAMEN_CONDUCCION',
    ])
  })

  it('las columnas usan los campos de la respuesta del backend', () => {
    expect(ESQUEMA_RESULTADOS.columnas.map((c) => c.campo)).toEqual([
      'estudianteNombre',
      'titulo',
      'origen',
      'puntaje',
      'aprobado',
      'dia',
    ])
    expect(ESQUEMA_RESULTADOS.columnas.find((c) => c.campo === 'aprobado')?.insignias).toMatchObject({
      true: { etiqueta: 'Aprobado' },
      false: { etiqueta: 'No aprobado' },
    })
  })
})

describe('ResultadosGestion', () => {
  it('monta <dh-gestion> con el esquema de resultados como propiedad', () => {
    const { container } = render(<ResultadosGestion />)

    const elemento = container.querySelector('dh-gestion') as
      (HTMLElement & { esquema: unknown }) | null
    expect(elemento).not.toBeNull()
    expect(elemento!.esquema).toBe(ESQUEMA_RESULTADOS)
  })
})
