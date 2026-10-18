import { Component } from '@angular/core'
import { RouterOutlet } from '@angular/router'

/**
 * Raíz del remoto cuando corre suelto (ng serve en su propio puerto).
 * Dentro del shell no se usa: el shell monta directamente app.routes.ts.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {}
