import { describe, it, expect, vi, afterEach } from 'vitest'
import { runThemeTransition } from './runThemeTransition'

describe('runThemeTransition', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    // @ts-expect-error -- limpiar el stub de startViewTransition entre tests
    delete document.startViewTransition
  })

  it('sin soporte de startViewTransition, llama a toggle directo', () => {
    const toggle = vi.fn()
    vi.stubGlobal('matchMedia', () => ({ matches: false }))

    runThemeTransition(10, 10, toggle)

    expect(toggle).toHaveBeenCalledOnce()
  })

  it('con prefers-reduced-motion, llama a toggle directo aunque haya soporte', () => {
    const toggle = vi.fn()
    const startViewTransition = vi.fn()
    document.startViewTransition = startViewTransition
    vi.stubGlobal('matchMedia', () => ({ matches: true }))

    runThemeTransition(10, 10, toggle)

    expect(toggle).toHaveBeenCalledOnce()
    expect(startViewTransition).not.toHaveBeenCalled()
  })

  it('con soporte y sin reduced-motion, dispara la transición con flushSync adentro', () => {
    const toggle = vi.fn()
    const finished = Promise.resolve()
    const startViewTransition = vi.fn((callback: () => void) => {
      callback()
      return { finished }
    })
    // @ts-expect-error -- solo para el test, la API real no está en jsdom
    document.startViewTransition = startViewTransition
    vi.stubGlobal('matchMedia', () => ({ matches: false }))

    runThemeTransition(20, 30, toggle)

    expect(startViewTransition).toHaveBeenCalledOnce()
    expect(toggle).toHaveBeenCalledOnce()
    expect(document.documentElement.style.getPropertyValue('--theme-transition-x')).toBe('20px')
    expect(document.documentElement.style.getPropertyValue('--theme-transition-y')).toBe('30px')
  })
})
