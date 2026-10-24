import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_AUTOS } from '../schemas/autos.esquema'

/**
 * Gestionar autos (CU07, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado, el formulario y las acciones los dibuja el Web Component a partir del esquema. Ver
 * GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-autos-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class AutosGestion {
  protected readonly esquema = ESQUEMA_AUTOS
}
