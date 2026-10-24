import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router, RouterOutlet } from '@angular/router'
import type { ItemNavegacion } from '@drivehub/ui'
import '@drivehub/ui'
import { filter, map } from 'rxjs'

/** Menú del remoto cuando corre suelto: el mismo armazón que tiene dentro del shell. */
const ITEMS: ItemNavegacion[] = [
  { ruta: '/', etiqueta: 'Identidad', descripcion: 'Usuarios, roles y perfil.', icono: 'users' },
]

/** Páginas sin sesión (registro, enlaces del correo): van sin menú ni cabecera. */
const PREFIJOS_PUBLICOS = ['/register', '/account']

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
    @if (!esPublica()) {
      <dh-shell [items]="items" activo="/" [migas]="migas">
        <router-outlet />
      </dh-shell>
    } @else {
      <router-outlet />
    }
  `,
})
export class App {
  protected readonly items = ITEMS
  protected readonly migas = [{ etiqueta: 'Identidad' }]
  private readonly router = inject(Router)
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  )

  protected readonly esPublica = computed(() =>
    PREFIJOS_PUBLICOS.some((prefijo) => this.url().startsWith(prefijo))
  )
}
