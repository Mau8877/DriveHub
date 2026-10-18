import { Component } from '@angular/core'

/**
 * Login único de la web (CU02).
 *
 * TODO: implementar con el caso de uso CU02 (formulario, llamada a
 * TODO: POST /api/v1/auth/login y guardado del token en SesionService).
 */
@Component({
  selector: 'app-login',
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-sm rounded-lg border p-6">
        <h1 class="mb-1 text-xl font-semibold">Iniciar sesión</h1>
        <p class="text-muted-foreground text-sm">Pendiente de implementar (CU02).</p>
      </div>
    </section>
  `,
})
export class Login {}
