import { Component } from '@angular/core'
import { RouterLink } from '@angular/router'

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  template: `
    <section class="p-6">
      <h1 class="mb-2 text-2xl font-semibold">Página no encontrada</h1>
      <a routerLink="/" class="text-primary underline">Volver al inicio</a>
    </section>
  `,
})
export class NoEncontrado {}
