import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core'
import { RouterLink } from '@angular/router'
import '@drivehub/ui'

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page grid min-h-[60svh] place-items-center">
      <div class="dh-state">
        <div class="dh-icono-grande"><dh-icono nombre="road" [tam]="34" /></div>
        <h1 class="dh-titulo-auth">Página no encontrada</h1>
        <p class="dh-sub-auth">La ruta que buscas no existe o se movió.</p>
        <a routerLink="/" class="dh-btn">Volver al inicio</a>
      </div>
    </section>
  `,
})
export class NoEncontrado {}
