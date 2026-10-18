import { flushSync } from 'react-dom'

/**
 * Corre `toggle` (el cambio de tema) envuelto en la View Transitions API del
 * navegador, con un reveal circular que arranca desde (x, y) -- pensado
 * para el punto donde el usuario clickeó "Cambiar tema" (ver AppHeader.tsx).
 * El keyframe real vive en index.css (`::view-transition-new(root)`), acá
 * solo se calculan y setean las variables CSS que ese keyframe usa
 * (posición + radio necesario para cubrir toda la pantalla) y se dispara la
 * transición.
 *
 * Sin soporte del navegador (Firefox, Safari viejo) o con
 * `prefers-reduced-motion: reduce`, cae a `toggle()` directo, sin animación
 * -- el cambio de tema en sí nunca depende de esto, solo la animación es
 * progresiva.
 */
export function runThemeTransition(x: number, y: number, toggle: () => void) {
  const supportsViewTransition = typeof document.startViewTransition === 'function'
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (!supportsViewTransition || prefersReducedMotion) {
    toggle()
    return
  }

  const root = document.documentElement
  const radius = Math.hypot(
    Math.max(x, window.innerWidth - x),
    Math.max(y, window.innerHeight - y),
  )
  root.style.setProperty('--theme-transition-x', `${x}px`)
  root.style.setProperty('--theme-transition-y', `${y}px`)
  root.style.setProperty('--theme-transition-radius', `${radius}px`)

  const transition = document.startViewTransition(() => {
    flushSync(() => toggle())
  })

  transition.finished.finally(() => {
    root.style.removeProperty('--theme-transition-x')
    root.style.removeProperty('--theme-transition-y')
    root.style.removeProperty('--theme-transition-radius')
  })
}
