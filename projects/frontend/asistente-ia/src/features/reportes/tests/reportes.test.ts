import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { nombreDeArchivo } from '../api/useReportes'
import { type ReconocimientoVoz, useDictado } from '../components/useDictado'
import { especificacionSchema, pedidoSchema } from '../schemas/especificacionSchema'

const valida = {
  fuente: 'pagos',
  titulo: 'Ventas de octubre',
  columnas: ['fecha', 'montoBs'],
  filtros: { desde: '2026-10-01', hasta: '2026-10-31', estado: null, tipo: null },
  orden: null,
  formato: 'pdf' as const,
}

describe('especificacionSchema', () => {
  it('acepta una especificación completa', () => {
    expect(especificacionSchema.safeParse(valida).success).toBe(true)
  })

  it('pide al menos una columna y un título', () => {
    const r = especificacionSchema.safeParse({ ...valida, columnas: [], titulo: '  ' })

    expect(r.success).toBe(false)
    const mensajes = r.error!.issues.map((i) => i.message)
    expect(mensajes).toContain('Elige al menos una columna')
    expect(mensajes).toContain('El título es obligatorio')
  })

  it('rechaza un rango de fechas invertido, marcado en «hasta»', () => {
    const r = especificacionSchema.safeParse({
      ...valida,
      filtros: { ...valida.filtros, desde: '2026-11-01', hasta: '2026-10-01' },
    })

    expect(r.success).toBe(false)
    expect(r.error!.issues[0].path).toEqual(['filtros', 'hasta'])
  })

  it('solo admite PDF o Excel', () => {
    expect(especificacionSchema.safeParse({ ...valida, formato: 'csv' }).success).toBe(false)
  })
})

describe('pedidoSchema', () => {
  it('pide un pedido de entre 3 y 500 caracteres', () => {
    expect(pedidoSchema.safeParse('ok').success).toBe(false)
    expect(pedidoSchema.safeParse('a'.repeat(501)).success).toBe(false)
    expect(pedidoSchema.safeParse('ventas de octubre').success).toBe(true)
  })
})

describe('nombreDeArchivo', () => {
  it('toma el nombre codificado de Content-Disposition', () => {
    expect(nombreDeArchivo("attachment; filename*=UTF-8''reporte-pagos-2026-11-10.xlsx", 'xlsx')).toBe(
      'reporte-pagos-2026-11-10.xlsx',
    )
  })

  it('sin cabecera usa un nombre por defecto con el formato', () => {
    expect(nombreDeArchivo(undefined, 'pdf')).toBe('reporte.pdf')
  })
})

class ReconocimientoFalso implements ReconocimientoVoz {
  static ultimo: ReconocimientoFalso | null = null
  lang = ''
  continuous = true
  interimResults = true
  onresult: ReconocimientoVoz['onresult'] = null
  onend: ReconocimientoVoz['onend'] = null
  onerror: ReconocimientoVoz['onerror'] = null
  iniciado = false

  constructor() {
    ReconocimientoFalso.ultimo = this
  }

  start() {
    this.iniciado = true
  }

  stop() {
    this.onend?.()
  }

  decir(texto: string) {
    this.onresult?.({ resultIndex: 0, results: [{ 0: { transcript: ` ${texto} ` }, isFinal: true }] })
  }
}

describe('useDictado', () => {
  it('sin reconocimiento en el navegador no se ofrece', () => {
    const { result } = renderHook(() => useDictado(() => {}, undefined))

    expect(result.current.soportado).toBe(false)
  })

  it('escucha en español y entrega la frase terminada', () => {
    const frases: string[] = []
    const { result } = renderHook(() => useDictado((t) => frases.push(t), ReconocimientoFalso))

    act(() => result.current.iniciar())
    expect(result.current.escuchando).toBe(true)
    expect(ReconocimientoFalso.ultimo!.lang).toBe('es-BO')

    act(() => ReconocimientoFalso.ultimo!.decir('pagos de octubre'))
    act(() => result.current.detener())

    expect(frases).toEqual(['pagos de octubre'])
    expect(result.current.escuchando).toBe(false)
  })

  it('sin permiso de micrófono lo explica', () => {
    const { result } = renderHook(() => useDictado(() => {}, ReconocimientoFalso))

    act(() => result.current.iniciar())
    act(() => ReconocimientoFalso.ultimo!.onerror?.({ error: 'not-allowed' }))

    expect(result.current.error).toContain('permiso')
  })
})
