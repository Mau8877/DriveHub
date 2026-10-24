import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, inject, signal } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import '@drivehub/ui'
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
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-auth-layout
      titulo="Cambio de correo"
      mensaje="Confirma la dirección nueva de tu cuenta de DriveHub."
    >
      @switch (estado()) {
        @case ('confirmando') {
          <div class="dh-state">
            <div class="dh-icono-grande"><dh-icono nombre="mail" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Confirmando tu correo…</h1>
            <p class="dh-sub-auth" role="status">Un momento, por favor.</p>
          </div>
        }
        @case ('cambiado') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-ok"><dh-icono nombre="check-circle" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Tu correo se cambió correctamente</h1>
            <p class="dh-sub-auth" role="status">Desde ahora inicias sesión con tu correo nuevo.</p>
            <a routerLink="/" class="dh-btn dh-btn-block">Ir a la aplicación</a>
          </div>
        }
        @case ('invalido') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-mal"><dh-icono nombre="alert" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Enlace no válido</h1>
            <p class="dh-sub-auth" role="alert">
              El enlace no es válido, venció o ya se usó. Pide uno nuevo desde tu perfil.
            </p>
            <a routerLink="/perfil" class="dh-btn dh-btn-soft dh-btn-block">Ir a mi perfil</a>
          </div>
        }
        @case ('error') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-mal"><dh-icono nombre="alert" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">No pudimos confirmar el cambio</h1>
            <p class="dh-sub-auth" role="alert">Ocurrió un error. Intenta de nuevo.</p>
            <button type="button" class="dh-btn dh-btn-soft dh-btn-block" (click)="confirmar()">
              Reintentar
            </button>
          </div>
        }
      }
    </dh-auth-layout>
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
