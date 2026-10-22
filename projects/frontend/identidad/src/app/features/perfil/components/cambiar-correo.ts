import { Component, DestroyRef, inject, input, output, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
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
  template: `
    <details class="bg-card border-border rounded-lg border p-4">
      <summary class="cursor-pointer text-base font-semibold">Cambiar correo</summary>

      <p class="text-muted-foreground mt-4 text-sm">
        Tu correo actual es <span data-testid="correo-actual">{{ perfil().email }}</span>
      </p>

      @if (aviso(); as mensaje) {
        <p class="mt-2 text-sm" role="status">{{ mensaje }}</p>
      }

      @if (pendiente(); as correo) {
        <p class="mt-2 text-sm" role="status" data-testid="pendiente">
          Te enviamos un correo a {{ correo }}. Tu correo no cambia hasta que abras el enlace (vale
          24 horas).
        </p>
        <button
          type="button"
          class="border-border mt-2 rounded-md border px-3 py-1 text-sm"
          (click)="pendiente.set(null)"
        >
          Pedir otro enlace
        </button>
      } @else {
        <form [formGroup]="form" (ngSubmit)="enviar()" class="mt-4 flex flex-col gap-4" novalidate>
          <div class="flex flex-col gap-1">
            <label for="correoPasswordActual" class="text-sm font-medium">Contraseña actual</label>
            <input
              id="correoPasswordActual"
              type="password"
              formControlName="passwordActual"
              autocomplete="current-password"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('passwordActual'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="flex flex-col gap-1">
            <label for="correoNuevo" class="text-sm font-medium">Correo nuevo</label>
            <input
              id="correoNuevo"
              type="email"
              formControlName="correoNuevo"
              autocomplete="email"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('correoNuevo'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>

          @if (errorGeneral(); as mensaje) {
            <p class="text-destructive text-sm" role="alert">{{ mensaje }}</p>
          }

          <button
            type="submit"
            [disabled]="enviando()"
            class="bg-primary text-primary-foreground self-start rounded-md px-4 py-2 text-sm font-medium disabled:opacity-50"
          >
            {{ enviando() ? 'Enviando…' : 'Enviar enlace' }}
          </button>
        </form>
      }
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
