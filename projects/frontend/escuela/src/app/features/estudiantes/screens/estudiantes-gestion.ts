import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_ESTUDIANTES } from '../schemas/estudiantes.esquema'

/**
 * Gestionar estudiantes (CU08, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado, el detalle y el formulario los dibuja el Web Component a partir del esquema. Ver
 * GESTION_FRONTEND.md.
 */
@Component({
  selector: 'app-estudiantes-gestion',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<dh-gestion [esquema]="esquema"></dh-gestion>`,
})
export class EstudiantesGestion {
  protected readonly esquema = ESQUEMA_ESTUDIANTES
}
