import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, inject, signal } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import '@drivehub/ui'
import { firstValueFrom } from 'rxjs'

import { CuentaService } from '../api/cuenta.service'

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
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-auth-layout
      titulo="Verifica tu correo"
      mensaje="Con el correo verificado puedes recuperar tu contraseña cuando la olvides."
    >
      @switch (estado()) {
        @case ('verificando') {
          <div class="dh-state">
            <div class="dh-icono-grande"><dh-icono nombre="mail" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Verificando tu correo…</h1>
            <p class="dh-sub-auth" role="status">Un momento, por favor.</p>
          </div>
        }
        @case ('verificado') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-ok"><dh-icono nombre="check-circle" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Tu correo quedó verificado</h1>
            <p class="dh-sub-auth" role="status">
              Ya puedes recuperar tu contraseña con este correo si la olvidas.
            </p>
            <a routerLink="/login" class="dh-btn dh-btn-block">Ir a iniciar sesión</a>
          </div>
        }
        @case ('invalido') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-mal"><dh-icono nombre="alert" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Enlace no válido</h1>
            <p class="dh-sub-auth" role="alert">
              Este enlace no es válido o ya venció. Inicia sesión y vuelve a pedir la verificación
              desde la barra superior.
            </p>
            <a routerLink="/login" class="dh-btn dh-btn-soft dh-btn-block">Ir a iniciar sesión</a>
          </div>
        }
      }
    </dh-auth-layout>
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
