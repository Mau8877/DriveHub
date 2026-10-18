/**
 * Devuelve una versión "debounced" de `fn`: si se la vuelve a llamar antes
 * de que pasen `delayMs`, cancela la ejecución pendiente y reinicia el
 * temporizador.
 *
 * * Pensado para UX (evitar doble-submit por doble-click, o dispararse en
 * * cada tecla de un buscador) -- no es protección de seguridad. El rate
 * * limit real contra abuso vive en el backend
 * * (.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit), porque
 * * esto es JS del navegador y cualquiera lo puede saltear pegándole
 * * directo a la API.
 * ! No se usa como wrapper global de apiClient -- debounce-ar todas las
 * ! requests por igual rompería llamadas legítimas en paralelo (varias
 * ! queries de TanStack Query pidiendo cosas distintas al mismo tiempo).
 * ! Se aplica puntual, en el call site que lo necesite (ej. el onClick de
 * ! un botón de submit).
 */
export function debounce<Args extends unknown[]>(
  fn: (...args: Args) => void,
  delayMs: number,
): (...args: Args) => void {
  let timeoutId: ReturnType<typeof setTimeout> | undefined

  return (...args: Args) => {
    if (timeoutId !== undefined) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => fn(...args), delayMs)
  }
}
