import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_INSTRUCTORES } from '../schemas/instructores.esquema'

/**
 * Gestionar instructores (CU09, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado, el detalle y el formulario los dibuja el Web Component a partir del esquema. Ver
 * GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-instructores-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class InstructoresGestion {
  protected readonly esquema = ESQUEMA_INSTRUCTORES
}
