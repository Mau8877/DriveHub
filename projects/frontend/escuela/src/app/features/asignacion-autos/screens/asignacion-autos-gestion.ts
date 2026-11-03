import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_ASIGNACION_AUTOS } from '../schemas/asignacion-autos.esquema'

/**
 * Asignación de autos a instructores (CU10, Administrador de escuela). Es solo el anfitrión de
 * <dh-gestion>: el listado, los formularios de las acciones y el historial los dibuja el Web Component
 * a partir del esquema. Ver GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-asignacion-autos-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class AsignacionAutosGestion {
  protected readonly esquema = ESQUEMA_ASIGNACION_AUTOS
}
