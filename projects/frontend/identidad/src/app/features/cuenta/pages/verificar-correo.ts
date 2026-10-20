import { Component, OnInit, inject, signal } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { firstValueFrom } from 'rxjs'

import { CuentaService } from '../cuenta.service'

type Estado = 'verificando' | 'verificado' | 'invalido'

/**
 * Página a la que lleva el enlace del correo de verificación (CU04b). Es pública:
 * se abre con o sin sesión, en cualquier navegador, y verifica el correo al entrar.
 *
 * * El token sale del query string (?token=…). Sin token, o si el backend rechaza el
 * * enlace (inválido, vencido, usado o reemplazado), se explica sin distinguir el motivo.
 */
@Component({
  selector: 'app-verificar-correo',
  imports: [RouterLink],
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-md rounded-lg border p-6">
        @switch (estado()) {
          @case ('verificando') {
            <h1 class="mb-1 text-xl font-semibold">Verificando tu correo…</h1>
            <p class="text-muted-foreground text-sm" role="status">Un momento, por favor.</p>
          }
          @case ('verificado') {
            <h1 class="mb-1 text-xl font-semibold">Tu correo quedó verificado</h1>
            <p class="text-muted-foreground mb-4 text-sm" role="status">
              Ya puedes recuperar tu contraseña con este correo si la olvidas.
            </p>
            <a routerLink="/login" class="text-sm font-medium underline">Ir a iniciar sesión</a>
          }
          @case ('invalido') {
            <h1 class="mb-1 text-xl font-semibold">Enlace no válido</h1>
            <p class="text-destructive mb-4 text-sm" role="alert">
              Este enlace no es válido o ya venció. Inicia sesión y vuelve a pedir la verificación
              desde la barra superior.
            </p>
            <a routerLink="/login" class="text-sm font-medium underline">Ir a iniciar sesión</a>
          }
        }
      </div>
    </section>
  `,
})
export class VerificarCorreo implements OnInit {
  private readonly cuenta = inject(CuentaService)
  private readonly ruta = inject(ActivatedRoute)

  protected readonly estado = signal<Estado>('verificando')

  async ngOnInit(): Promise<void> {
    const token = this.ruta.snapshot.queryParamMap.get('token')
    if (!token) {
      this.estado.set('invalido')
      return
    }
    try {
      await firstValueFrom(this.cuenta.confirmarCorreo(token))
      this.estado.set('verificado')
    } catch {
      // * Cualquier rechazo (ERR_IDE_11 o un fallo de red) se explica igual: el enlace no sirvió
      this.estado.set('invalido')
    }
  }
}
