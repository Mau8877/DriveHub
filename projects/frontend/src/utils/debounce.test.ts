import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { debounce } from './debounce'

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('solo llama a la función una vez, después del delay, si se invoca varias veces seguidas', () => {
    const fn = vi.fn()
    const debounced = debounce(fn, 300)

    debounced()
    debounced()
    debounced()

    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(300)

    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('pasa los argumentos de la última llamada', () => {
    const fn = vi.fn()
    const debounced = debounce(fn, 300)

    debounced('primero')
    debounced('segundo')

    vi.advanceTimersByTime(300)

    expect(fn).toHaveBeenCalledWith('segundo')
  })

  it('reinicia el timer si se llama de nuevo antes de que pase el delay', () => {
    const fn = vi.fn()
    const debounced = debounce(fn, 300)

    debounced()
    vi.advanceTimersByTime(200)
    debounced()
    vi.advanceTimersByTime(200)

    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(100)

    expect(fn).toHaveBeenCalledTimes(1)
  })
})
