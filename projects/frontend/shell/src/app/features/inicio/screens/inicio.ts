import { Component } from '@angular/core'
import { RouterLink } from '@angular/router'

import { NAV_ITEMS } from '../../../layout/nav-items'

/**
 * Pantalla de bienvenida del panel de la escuela.
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  template: `
    <section class="p-6">
      <h1 class="mb-1 text-2xl font-semibold">Panel de la escuela</h1>
      <p class="text-muted-foreground mb-6">Elegí un módulo para empezar.</p>
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        @for (item of navItems; track item.ruta) {
          <a
            [routerLink]="item.ruta"
            class="bg-card border-border hover:bg-accent rounded-lg border p-4"
          >
            <span class="block font-medium">{{ item.etiqueta }}</span>
            <span class="text-muted-foreground text-sm">{{ item.descripcion }}</span>
          </a>
        }
      </div>
    </section>
  `,
})
export class Inicio {
  protected readonly navItems = NAV_ITEMS
}
