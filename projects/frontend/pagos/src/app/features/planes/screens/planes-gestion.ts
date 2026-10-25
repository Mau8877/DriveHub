import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_PLANES } from '../schemas/planes.esquema'

/**
 * Gestionar planes de horas (CU25, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado y el formulario los dibuja el Web Component a partir del esquema. Ver GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-planes-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class PlanesGestion {
  protected readonly esquema = ESQUEMA_PLANES
}
