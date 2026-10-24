import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
import '@drivehub/ui'

/**
 * Pantalla inicial del remoto Identidad.
 *
 * TODO: se reemplaza por las pantallas reales a medida que se implementen
 * TODO: los casos de uso de este micro-frontend (CU01, CU06).
 */
@Component({
  selector: 'app-inicio',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page">
      <div class="dh-page-head">
        <div>
          <h1>Identidad</h1>
          <p>Usuarios, roles y perfil.</p>
        </div>
      </div>
      <div class="dh-card dh-pendiente">
        <span class="dh-pendiente-ico">
          <dh-icono nombre="users" [tam]="22" />
        </span>
        <p class="dh-muted dh-texto-sm">Casos de uso pendientes: CU01, CU06.</p>
      </div>
    </section>
  `,
})
export class Inicio {}
