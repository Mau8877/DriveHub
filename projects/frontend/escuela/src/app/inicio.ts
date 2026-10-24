import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
import '@drivehub/ui'

/**
 * Pantalla inicial del remoto Escuela.
 *
 * TODO: se reemplaza por las pantallas reales a medida que se implementen
 * TODO: los casos de uso de este micro-frontend (CU07 a CU11).
 */
@Component({
  selector: 'app-inicio',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page">
      <div class="dh-page-head">
        <div>
          <h1>Escuela</h1>
          <p>Autos, estudiantes, docentes y asignaciones.</p>
        </div>
      </div>
      <div class="dh-card dh-pendiente">
        <span class="dh-pendiente-ico">
          <dh-icono nombre="car" [tam]="22" />
        </span>
        <p class="dh-muted dh-texto-sm">Casos de uso pendientes: CU07 a CU11.</p>
      </div>
    </section>
  `,
})
export class Inicio {}
