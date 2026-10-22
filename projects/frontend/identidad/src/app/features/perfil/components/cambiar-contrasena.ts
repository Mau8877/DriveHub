import { Component, inject, signal } from '@angular/core'
import {
  type AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  type ValidationErrors,
  Validators,
} from '@angular/forms'
import { firstValueFrom } from 'rxjs'

import { PASSWORD_MAX, PASSWORD_MIN } from '../../registro'
import { PerfilService } from '../api/perfil.service'
import { codigoDe, mensajeDeErrorPerfil } from '../schemas/perfil.errores'

type Campo = 'passwordActual' | 'passwordNueva' | 'confirmacion'

const MENSAJE_LARGO = `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres`

/** La confirmación igual a la nueva y la nueva distinta de la actual. */
function coherencia(grupo: AbstractControl): ValidationErrors | null {
  const actual = grupo.get('passwordActual')?.value as string
  const nueva = grupo.get('passwordNueva')?.value as string
  const confirmacion = grupo.get('confirmacion')?.value as string
  const errores: ValidationErrors = {}
  if (confirmacion && nueva !== confirmacion) errores['noCoincide'] = true
  if (actual && nueva && actual === nueva) errores['igualALaActual'] = true
  return Object.keys(errores).length ? errores : null
}

/**
 * Sección de cambiar la contraseña con sesión (CU06). La actual es obligatoria y la nueva sigue la
 * regla del registro. Al terminar se cierran las sesiones de los demás dispositivos, pero la de
 * esta pestaña no se toca.
 *
 * ! Las contraseñas nunca se muestran, se guardan ni se escriben en la consola.
 */
@Component({
  selector: 'app-cambiar-contrasena',
  imports: [ReactiveFormsModule],
  template: `
    <details class="bg-card border-border rounded-lg border p-4">
      <summary class="cursor-pointer text-base font-semibold">Cambiar contraseña</summary>

      <form [formGroup]="form" (ngSubmit)="enviar()" class="mt-4 flex flex-col gap-4" novalidate>
        <div class="flex flex-col gap-1">
          <label for="passwordActual" class="text-sm font-medium">Contraseña actual</label>
          <input
            id="passwordActual"
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
          <label for="passwordNueva" class="text-sm font-medium">Contraseña nueva</label>
          <input
            id="passwordNueva"
            type="password"
            formControlName="passwordNueva"
            autocomplete="new-password"
            class="border-border bg-background rounded-md border px-3 py-2 text-sm"
          />
          @if (error('passwordNueva'); as mensaje) {
            <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
          }
        </div>

        <div class="flex flex-col gap-1">
          <label for="confirmacion" class="text-sm font-medium">Confirma la contraseña nueva</label>
          <input
            id="confirmacion"
            type="password"
            formControlName="confirmacion"
            autocomplete="new-password"
            class="border-border bg-background rounded-md border px-3 py-2 text-sm"
          />
          @if (error('confirmacion'); as mensaje) {
            <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
          }
        </div>

        @if (errorGeneral(); as mensaje) {
          <p class="text-destructive text-sm" role="alert">{{ mensaje }}</p>
        }
        @if (aviso(); as mensaje) {
          <p class="text-sm" role="status">{{ mensaje }}</p>
        }

        <button
          type="submit"
          [disabled]="enviando()"
          class="bg-primary text-primary-foreground self-start rounded-md px-4 py-2 text-sm font-medium disabled:opacity-50"
        >
          {{ enviando() ? 'Guardando…' : 'Cambiar contraseña' }}
        </button>
      </form>
    </details>
  `,
})
export class CambiarContrasena {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly api = inject(PerfilService)

  protected readonly enviando = signal(false)
  protected readonly aviso = signal<string | null>(null)
  protected readonly errorGeneral = signal<string | null>(null)
  private readonly errorDeServidor = signal<Partial<Record<Campo, string>>>({})

  protected readonly form = this.fb.group(
    {
      passwordActual: ['', [Validators.required]],
      passwordNueva: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN),
          Validators.maxLength(PASSWORD_MAX),
        ],
      ],
      confirmacion: ['', [Validators.required]],
    },
    { validators: coherencia }
  )

  protected error(campo: Campo): string | null {
    const delServidor = this.errorDeServidor()[campo]
    if (delServidor) return delServidor
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    if (campo === 'passwordActual') {
      return control.errors?.['required'] ? 'La contraseña actual es obligatoria' : null
    }
    if (campo === 'passwordNueva') {
      if (control.errors?.['required']) return 'La contraseña nueva es obligatoria'
      if (control.errors?.['minlength'] || control.errors?.['maxlength']) return MENSAJE_LARGO
      return this.form.errors?.['igualALaActual']
        ? 'La contraseña nueva debe ser distinta de la actual'
        : null
    }
    if (control.errors?.['required']) return 'Confirma la contraseña nueva'
    return this.form.errors?.['noCoincide'] ? 'Las contraseñas no coinciden' : null
  }

  protected async enviar(): Promise<void> {
    this.aviso.set(null)
    this.errorGeneral.set(null)
    this.errorDeServidor.set({})
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    const { passwordActual, passwordNueva } = this.form.getRawValue()
    this.enviando.set(true)
    try {
      await firstValueFrom(
        this.api.cambiarContrasena({ passwordActual, passwordNueva, dispositivo: 'WEB' })
      )
      this.form.reset()
      this.aviso.set('Contraseña actualizada. Cerramos tus otras sesiones.')
    } catch (error) {
      const mensaje = mensajeDeErrorPerfil(error)
      const codigo = codigoDe(error)
      if (codigo === 'ERR_IDE_14') {
        this.errorDeServidor.set({ passwordActual: mensaje })
      } else if (codigo === 'ERR_IDE_15') {
        this.errorDeServidor.set({ passwordNueva: mensaje })
      } else {
        this.errorGeneral.set(mensaje)
      }
    } finally {
      this.enviando.set(false)
    }
  }
}
