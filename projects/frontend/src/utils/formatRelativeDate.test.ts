import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import dayjs from 'dayjs'
import { formatRelativeDate } from './formatRelativeDate'

describe('formatRelativeDate', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-03T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('formatea una fecha de hace unos minutos en español', () => {
    const fecha = dayjs().subtract(5, 'minute').toISOString()

    expect(formatRelativeDate(fecha)).toBe('hace 5 minutos')
  })

  it('formatea una fecha de hace unos días en español', () => {
    const fecha = dayjs().subtract(2, 'day').toISOString()

    expect(formatRelativeDate(fecha)).toBe('hace 2 días')
  })

  it('formatea una fecha futura como relativa hacia adelante', () => {
    const fecha = dayjs().add(3, 'hour').toISOString()

    expect(formatRelativeDate(fecha)).toBe('en 3 horas')
  })
})
