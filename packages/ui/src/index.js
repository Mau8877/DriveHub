import { DhAuthLayout } from './dh-auth-layout.js'
import { DhAvatar } from './dh-avatar.js'
import { DhDialogo } from './dh-dialogo.js'
import { DhHeader } from './dh-header.js'
import { DhIcono } from './dh-icono.js'
import { DhShell } from './dh-shell.js'
import { DhSidebar } from './dh-sidebar.js'

const COMPONENTES = {
  'dh-avatar': DhAvatar,
  'dh-dialogo': DhDialogo,
  'dh-icono': DhIcono,
  'dh-sidebar': DhSidebar,
  'dh-header': DhHeader,
  'dh-shell': DhShell,
  'dh-auth-layout': DhAuthLayout,
}

/**
 * Registra los componentes. Se hace una sola vez aunque dos remotos incluyan su propia copia del
 * paquete: gana la primera (customElements no admite redefinir un nombre).
 */
export function registrarUi() {
  for (const [nombre, clase] of Object.entries(COMPONENTES)) {
    if (!customElements.get(nombre)) customElements.define(nombre, clase)
  }
}

registrarUi()

export { DhAuthLayout, DhAvatar, DhDialogo, DhHeader, DhIcono, DhShell, DhSidebar }
export { ICONOS, icono } from './iconos.js'
export { iniciales } from './dh-avatar.js'
