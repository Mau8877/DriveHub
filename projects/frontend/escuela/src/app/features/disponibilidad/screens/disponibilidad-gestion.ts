import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_DISPONIBILIDAD } from '../schemas/disponibilidad.esquema'

/**
 * Disponibilidad de los instructores (CU11, Administrador de escuela). Es solo el anfitrión de <dh-gestion>:
 * el listado, los filtros y el calendario del día los dibuja el Web Component a partir del esquema. Ver
 * GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-disponibilidad-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class DisponibilidadGestion {
  protected readonly esquema = ESQUEMA_DISPONIBILIDAD
}
