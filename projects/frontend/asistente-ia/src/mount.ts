import type { MountProps } from '@drivehub/mf-contract'

/**
 * Punto de entrada del remoto Asistente IA dentro del shell (módulo './mount'
 * de Module Federation, ver vite.config.ts y @drivehub/mf-contract).
 *
 * * El shell llama mount(elemento, props) al entrar a /asistente-ia y unmount
 * * al salir. Props trae la URL de la API, cómo pedir el token (el remoto
 * * nunca hace login) y la ruta base para el router.
 * * Este archivo es .ts (sin JSX) a propósito: en desarrollo, todo .tsx que
 * * transforma @vitejs/plugin-react exige el "preamble" de React Refresh, que
 * * normalmente inyecta el index.html del remoto. Dentro del shell ese
 * * index.html nunca se carga, así que acá se instala el preamble a mano
 * * ANTES de importar el primer .tsx (montar.tsx).
 */
type Montador = typeof import('./montar')

const pendientes = new WeakMap<HTMLElement, Promise<Montador>>()

export function mount(elemento: HTMLElement, props: MountProps): void {
  const carga = instalarPreambuloReactRefresh().then(() => import('./montar'))
  pendientes.set(elemento, carga)
  void carga.then((montador) => {
    // Si el shell desmontó antes de que terminara la carga, no se monta.
    if (pendientes.get(elemento) === carga) montador.montar(elemento, props)
  })
}

export function unmount(elemento: HTMLElement): void {
  const carga = pendientes.get(elemento)
  pendientes.delete(elemento)
  void carga?.then((montador) => montador.desmontar(elemento))
}

declare global {
  interface Window {
    __vite_plugin_react_preamble_installed__?: boolean
    $RefreshReg$?: () => void
    $RefreshSig$?: () => (tipo: unknown) => unknown
  }
}

async function instalarPreambuloReactRefresh(): Promise<void> {
  // import.meta.hot solo existe servido por Vite en desarrollo (no en el build
  // de producción). Vitest también lo define, pero ahí no hay dev server.
  const enDevServer = import.meta.hot && import.meta.env.MODE !== 'test'
  if (!enDevServer || window.__vite_plugin_react_preamble_installed__) return

  const url = new URL('/@react-refresh', import.meta.url).href
  const { default: RefreshRuntime } = await import(/* @vite-ignore */ url)
  RefreshRuntime.injectIntoGlobalHook(window)
  window.$RefreshReg$ = () => {}
  window.$RefreshSig$ = () => (tipo) => tipo
  window.__vite_plugin_react_preamble_installed__ = true
}
