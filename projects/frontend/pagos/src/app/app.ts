import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import type { ItemNavegacion } from '@drivehub/ui'
import '@drivehub/ui'

/** Menú del remoto cuando corre suelto: el mismo armazón que tiene dentro del shell. */
const ITEMS: ItemNavegacion[] = [
  { ruta: '/', etiqueta: 'Pagos', descripcion: 'Planes de pago y cuotas.', icono: 'wallet' },
]

/**
 * Raíz del remoto cuando corre suelto (ng serve en su propio puerto): lo envuelve en el armazón
 * compartido (`<dh-shell>` de @drivehub/ui) para que se vea igual que dentro del shell.
 * Dentro del shell no se usa: el shell monta directamente app.routes.ts.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-shell [items]="items" activo="/" [migas]="migas">
      <router-outlet />
    </dh-shell>
  `,
})
export class App {
  protected readonly items = ITEMS
  protected readonly migas = [{ etiqueta: 'Pagos' }]
}
