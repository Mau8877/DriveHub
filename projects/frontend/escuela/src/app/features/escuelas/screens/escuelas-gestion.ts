import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_ESCUELAS } from '../schemas/escuelas.esquema'

/**
 * Gestionar escuelas (CU05, Super admin). Es solo el anfitrión de <dh-gestion>: el listado, el
 * formulario y las acciones los dibuja el Web Component a partir del esquema. Ver
 * GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-escuelas-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class EscuelasGestion {
  protected readonly esquema = ESQUEMA_ESCUELAS
}
