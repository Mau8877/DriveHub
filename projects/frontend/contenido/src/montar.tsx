import { StrictMode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { RouterProvider } from '@tanstack/react-router'
import type { MountProps } from '@drivehub/mf-contract'

import './index.css'
import { crearRouter } from './router'
import { configurarApi, QueryProvider } from './store'

const raices = new WeakMap<HTMLElement, Root>()

/** Monta el árbol de React del remoto. Lo llama mount.ts (nunca el shell directo). */
export function montar(elemento: HTMLElement, props: MountProps) {
  configurarApi({ apiBaseUrl: props.apiBaseUrl, obtenerToken: props.obtenerToken })
  const raiz = createRoot(elemento)
  raiz.render(
    <StrictMode>
      <QueryProvider>
        <RouterProvider router={crearRouter(props.basePath)} />
      </QueryProvider>
    </StrictMode>,
  )
  raices.set(elemento, raiz)
}

export function desmontar(elemento: HTMLElement) {
  raices.get(elemento)?.unmount()
  raices.delete(elemento)
}
