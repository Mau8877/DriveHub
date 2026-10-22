import { Component, OnInit, inject, signal } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { firstValueFrom } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { CuentaService } from '../api/cuenta.service'

type Estado = 'confirmando' | 'cambiado' | 'invalido' | 'error'

/**
 * Página a la que lleva el enlace del correo de cambio de correo (CU06). Es pública: se abre con
 * o sin sesión, en cualquier navegador, y confirma el cambio al entrar. No inicia sesión.
 *
 * * El token sale del query string (?token=…). Sin token, o si el backend rechaza el enlace
 * * (ERR_IDE_11), se explica sin distinguir el motivo y se ofrece pedir otro desde el perfil.
 */
@Component({
  selector: 'app-cambiar-correo-confirmacion',
  imports: [RouterLink],
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-md rounded-lg border p-6">
        @switch (estado()) {
          @case ('confirmando') {
            <h1 class="mb-1 text-xl font-semibold">Confirmando tu correo…</h1>
            <p class="text-muted-foreground text-sm" role="status">Un momento, por favor.</p>
          }
          @case ('cambiado') {
            <h1 class="mb-1 text-xl font-semibold">Tu correo se cambió correctamente</h1>
            <p class="text-muted-foreground mb-4 text-sm" role="status">
              Desde ahora inicias sesión con tu correo nuevo.
            </p>
            <a routerLink="/" class="text-sm font-medium underline">Ir a la aplicación</a>
          }
          @case ('invalido') {
            <h1 class="mb-1 text-xl font-semibold">Enlace no válido</h1>
            <p class="text-destructive mb-4 text-sm" role="alert">
              El enlace no es válido, venció o ya se usó. Pide uno nuevo desde tu perfil.
            </p>
            <a routerLink="/perfil" class="text-sm font-medium underline">Ir a mi perfil</a>
          }
          @case ('error') {
            <h1 class="mb-1 text-xl font-semibold">No pudimos confirmar el cambio</h1>
            <p class="text-destructive mb-4 text-sm" role="alert">
              Ocurrió un error. Intenta de nuevo.
            </p>
            <button type="button" class="text-sm font-medium underline" (click)="confirmar()">
              Reintentar
            </button>
          }
        }
      </div>
    </section>
  `,
})
export class CambiarCorreoConfirmacion implements OnInit {
  private readonly cuenta = inject(CuentaService)
  private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token')

  protected readonly estado = signal<Estado>('confirmando')

  ngOnInit(): void {
    void this.confirmar()
  }

  protected async confirmar(): Promise<void> {
    if (!this.token) {
      this.estado.set('invalido')
      return
    }
    this.estado.set('confirmando')
    try {
      await firstValueFrom(this.cuenta.confirmarCambioCorreo(this.token))
      this.estado.set('cambiado')
    } catch (error) {
      this.estado.set(
        error instanceof ApiError && error.code === 'ERR_IDE_11' ? 'invalido' : 'error'
      )
    }
  }
}
