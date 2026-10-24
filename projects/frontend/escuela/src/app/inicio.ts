import { Component } from '@angular/core'
import { RouterLink } from '@angular/router'

/**
 * Pantalla inicial del remoto Escuela: la puerta a sus secciones.
 *
 * TODO: se suman los accesos a estudiantes, docentes y asignaciones a medida que se implementen
 * TODO: sus casos de uso (CU08 a CU11).
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  template: `
    <section class="p-6">
      <h1 class="mb-1 text-2xl font-semibold">Escuela</h1>
      <p class="text-muted-foreground">Autos, estudiantes, docentes y asignaciones.</p>
      <nav class="mt-4 flex gap-3">
        <a
          routerLink="autos"
          class="border-border bg-card hover:bg-muted rounded-lg border px-4 py-3 text-sm font-medium"
        >
          Autos
        </a>
      </nav>
    </section>
  `,
})
export class Inicio {}
