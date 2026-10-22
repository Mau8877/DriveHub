import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import '@drivehub/ui'

import { SesionService } from '../../../core/sesion/sesion.service'
import { navItemsDeRoles } from '../../../layout/nav-items'

/**
 * Pantalla de bienvenida del panel: un acceso a cada módulo que ve el rol de la persona.
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page">
      <div class="dh-page-head">
        <div>
          <h1>Panel de la escuela</h1>
          <p>Elige un módulo para empezar.</p>
        </div>
      </div>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        @for (item of modulos(); track item.ruta) {
          <a
            [routerLink]="item.ruta"
            class="dh-card group flex items-start gap-4 p-5 transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <span
              class="bg-accent text-accent-foreground grid size-11 shrink-0 place-items-center rounded-xl"
            >
              <dh-icono [nombre]="item.icono" [tam]="22" />
            </span>
            <span class="min-w-0">
              <span class="block font-semibold">{{ item.etiqueta }}</span>
              <span class="dh-muted block text-sm">{{ item.descripcion }}</span>
            </span>
          </a>
        }
      </div>
    </section>
  `,
})
export class Inicio {
  private readonly sesion = inject(SesionService)

  // * "Inicio" es esta misma pantalla: no se ofrece como módulo
  protected readonly modulos = computed(() =>
    navItemsDeRoles(this.sesion.usuario()?.roles ?? []).filter((item) => item.ruta !== '/')
  )
}
