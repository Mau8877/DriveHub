import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import '@drivehub/ui'
import { firstValueFrom } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { EMAIL_MAX, obligatorioConTexto } from '../../registro'
import { PerfilService } from '../api/perfil.service'
import { codigoDe, mensajeDeErrorPerfil } from '../schemas/perfil.errores'
import type { Perfil } from '../types/perfil.types'

type Campo = 'passwordActual' | 'correoNuevo'

/**
 * Sección de cambiar el correo (CU06). Pide la contraseña actual y el correo nuevo; el enlace
 * llega al correo NUEVO y el de la cuenta no cambia hasta abrirlo.
 *
 * * Con una solicitud pendiente, al volver a la pestaña vuelve a pedir el perfil: si el correo
 * * cambió, lo muestra y avisa (CA18). El shell renueva su propia sesión al volver (CU04b).
 * ! La contraseña nunca se guarda ni se escribe en la consola.
 */
@Component({
  selector: 'app-cambiar-correo',
  imports: [ReactiveFormsModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <details class="dh-card dh-acordeon">
      <summary>
        <span class="dh-acordeon-ico"><dh-icono nombre="mail" [tam]="18" /></span>
        <span class="dh-acordeon-texto">
          <b>Cambiar correo</b>
          <small>Lo confirmas con un enlace en el correo nuevo</small>
        </span>
        <dh-icono nombre="chevron-down" [tam]="18" />
      </summary>

      <div class="dh-acordeon-cuerpo">
        <p class="dh-muted dh-texto-sm dh-mb">
          Tu correo actual es <b data-testid="correo-actual">{{ perfil().email }}</b>
        </p>

        @if (aviso(); as mensaje) {
          <p class="dh-notice dh-notice-ok" role="status">
            <dh-icono nombre="check-circle" [tam]="18" />
            <span>{{ mensaje }}</span>
          </p>
        }

        @if (pendiente(); as correo) {
          <p class="dh-notice dh-notice-info" role="status" data-testid="pendiente">
            <dh-icono nombre="mail" [tam]="18" />
            <span>
              Te enviamos un correo a {{ correo }}. Tu correo no cambia hasta que abras el enlace
              (vale 24 horas).
            </span>
          </p>
          <button type="button" class="dh-btn dh-btn-ghost dh-btn-sm" (click)="pendiente.set(null)">
            Pedir otro enlace
          </button>
        } @else {
          <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
            <div class="dh-field">
              <label for="correoPasswordActual" class="dh-label">Contraseña actual</label>
              <input
                id="correoPasswordActual"
                type="password"
                formControlName="passwordActual"
                autocomplete="current-password"
                class="dh-input"
              />
              @if (error('passwordActual'); as mensaje) {
                <p class="dh-error" role="alert">{{ mensaje }}</p>
              }
            </div>

            <div class="dh-field">
              <label for="correoNuevo" class="dh-label">Correo nuevo</label>
              <input
                id="correoNuevo"
                type="email"
                formControlName="correoNuevo"
                autocomplete="email"
                class="dh-input"
              />
              @if (error('correoNuevo'); as mensaje) {
                <p class="dh-error" role="alert">{{ mensaje }}</p>
              }
            </div>

            @if (errorGeneral(); as mensaje) {
              <p class="dh-notice dh-notice-error" role="alert">{{ mensaje }}</p>
            }

            <div class="dh-form-foot">
              <button type="submit" [disabled]="enviando()" class="dh-btn">
                {{ enviando() ? 'Enviando…' : 'Enviar enlace' }}
              </button>
            </div>
          </form>
        }
      </div>
    </details>
  `,
})
export class CambiarCorreo {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly api = inject(PerfilService)

  readonly perfil = input.required<Perfil>()
  readonly perfilCambiado = output<Perfil>()

  protected readonly enviando = signal(false)
  /** El correo nuevo al que se envió el enlace, mientras no se haya abierto. */
  protected readonly pendiente = signal<string | null>(null)
  protected readonly aviso = signal<string | null>(null)
  protected readonly errorGeneral = signal<string | null>(null)
  private readonly errorDeServidor = signal<Partial<Record<Campo, string>>>({})

  protected readonly form = this.fb.group({
    passwordActual: ['', [Validators.required]],
    correoNuevo: ['', [obligatorioConTexto, Validators.email, Validators.maxLength(EMAIL_MAX)]],
  })

  constructor() {
    if (typeof document !== 'undefined') {
      const alVolver = () => {
        if (document.visibilityState === 'visible' && this.pendiente()) void this.revisarCorreo()
      }
      document.addEventListener('visibilitychange', alVolver)
      inject(DestroyRef).onDestroy(() => document.removeEventListener('visibilitychange', alVolver))
    }
  }

  protected error(campo: Campo): string | null {
    const delServidor = this.errorDeServidor()[campo]
    if (delServidor) return delServidor
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    if (campo === 'passwordActual') {
      return control.errors?.['required'] ? 'La contraseña actual es obligatoria' : null
    }
    if (control.errors?.['required']) return 'El correo nuevo es obligatorio'
    if (control.errors?.['email']) return 'El correo no es válido'
    if (control.errors?.['maxlength']) return `El correo no puede superar ${EMAIL_MAX} caracteres`
    return null
  }

  protected async enviar(): Promise<void> {
    this.aviso.set(null)
    this.errorGeneral.set(null)
    this.errorDeServidor.set({})
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    const { passwordActual, correoNuevo } = this.form.getRawValue()
    this.enviando.set(true)
    try {
      await firstValueFrom(this.api.solicitarCambioCorreo({ passwordActual, correoNuevo }))
      this.pendiente.set(correoNuevo.trim().toLowerCase())
      this.form.reset()
    } catch (error) {
      const codigo = codigoDe(error)
      if (codigo === 'ERR_IDE_14') {
        this.errorDeServidor.set({ passwordActual: mensajeDeErrorPerfil(error) })
      } else if (codigo === 'ERR_IDE_01' || codigo === 'ERR_IDE_18') {
        this.errorDeServidor.set({ correoNuevo: mensajeDeErrorPerfil(error) })
      } else if (error instanceof ApiError && codigo === 'ERR_SYS_01') {
        this.errorGeneral.set(error.message)
      } else {
        this.errorGeneral.set('No pudimos enviar el correo. Intenta más tarde.')
      }
    } finally {
      this.enviando.set(false)
    }
  }

  /** Al volver a la pestaña con una solicitud pendiente: ¿ya se abrió el enlace? */
  private async revisarCorreo(): Promise<void> {
    try {
      const actualizado = await firstValueFrom(this.api.obtener())
      if (actualizado.email !== this.perfil().email) {
        this.pendiente.set(null)
        this.perfilCambiado.emit(actualizado)
        this.aviso.set(`Tu correo ahora es ${actualizado.email}`)
      }
    } catch {
      // * Sin red o sesión vencida: se vuelve a intentar la próxima vez que se vuelva a la pestaña
    }
  }
}
