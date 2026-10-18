import { Component } from '@angular/core'
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'

import { NAV_ITEMS } from './nav-items'

/**
 * Layout de toda la web: menú lateral, cabecera (con la campana de avisos)
 * y el área donde se monta cada micro-frontend.
 *
 * TODO: la campana de avisos se conecta con Notificaciones cuando exista
 * TODO: el caso de uso (hoy es solo el lugar reservado en la cabecera).
 */
@Component({
  selector: 'app-shell-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="flex min-h-svh">
      <aside
        class="bg-sidebar text-sidebar-foreground border-sidebar-border w-64 shrink-0 border-r p-4"
      >
        <a routerLink="/" class="mb-6 block text-lg font-semibold">DriveHub</a>
        <nav class="flex flex-col gap-1">
          @for (item of navItems; track item.ruta) {
            <a
              [routerLink]="item.ruta"
              routerLinkActive="bg-sidebar-accent text-sidebar-accent-foreground"
              class="hover:bg-sidebar-accent rounded-md px-3 py-2"
            >
              <span class="block text-sm font-medium">{{ item.etiqueta }}</span>
              <span class="text-muted-foreground block text-xs">{{ item.descripcion }}</span>
            </a>
          }
        </nav>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="border-border flex h-14 items-center justify-end gap-3 border-b px-6">
          <button
            type="button"
            class="hover:bg-accent rounded-md px-3 py-1 text-sm"
            aria-label="Avisos"
          >
            Avisos
          </button>
        </header>
        <main class="flex-1">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class ShellLayout {
  protected readonly navItems = NAV_ITEMS
}
