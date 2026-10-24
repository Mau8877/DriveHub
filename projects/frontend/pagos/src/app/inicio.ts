import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
import '@drivehub/ui'

/**
 * Pantalla inicial del remoto Pagos.
 *
 * TODO: se reemplaza por las pantallas reales a medida que se implementen
 * TODO: los casos de uso de este micro-frontend (CU25).
 */
@Component({
  selector: 'app-inicio',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page">
      <div class="dh-page-head">
        <div>
          <h1>Pagos</h1>
          <p>Planes de pago y cuotas.</p>
        </div>
      </div>
      <div class="dh-card dh-pendiente">
        <span class="dh-pendiente-ico">
          <dh-icono nombre="wallet" [tam]="22" />
        </span>
        <p class="dh-muted dh-texto-sm">Casos de uso pendientes: CU25.</p>
      </div>
    </section>
  `,
})
export class Inicio {}
