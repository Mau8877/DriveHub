import { Component, inject } from '@angular/core'
import { ActivatedRoute } from '@angular/router'

/**
 * Login único de la web (CU02).
 *
 * * Ya muestra el aviso de "cuenta creada" cuando llega desde el registro
 * * del estudiante (CU01) con ?registrado=1.
 * TODO: implementar con el caso de uso CU02 (formulario, llamada a
 * TODO: POST /api/v1/auth/login y guardado del token en SesionService).
 */
@Component({
  selector: 'app-login',
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-sm rounded-lg border p-6">
        @if (cuentaCreada) {
          <p
            class="bg-secondary text-secondary-foreground mb-4 rounded-md px-3 py-2 text-sm"
            role="status"
          >
            Tu cuenta fue creada. Inicia sesión para continuar.
          </p>
        }
        <h1 class="mb-1 text-xl font-semibold">Iniciar sesión</h1>
        <p class="text-muted-foreground text-sm">Pendiente de implementar (CU02).</p>
      </div>
    </section>
  `,
})
export class Login {
  protected readonly cuentaCreada =
    inject(ActivatedRoute).snapshot.queryParamMap.get('registrado') === '1'
}
