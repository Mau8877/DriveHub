import { CUSTOM_ELEMENTS_SCHEMA, Component, inject, signal } from '@angular/core'
import {
  type AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  type ValidationErrors,
  Validators,
} from '@angular/forms'
import { ActivatedRoute, RouterLink } from '@angular/router'
import '@drivehub/ui'
import { firstValueFrom } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { PASSWORD_MAX, PASSWORD_MIN } from '../../registro'
import { CuentaService } from '../api/cuenta.service'

type Estado = 'formulario' | 'hecho' | 'invalido'
type Campo = 'password' | 'confirmacion'

const MENSAJE_LARGO = `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres`

/** La confirmación debe ser igual a la contraseña nueva. */
function confirmacionIgual(grupo: AbstractControl): ValidationErrors | null {
  const password = grupo.get('password')?.value as string
  const confirmacion = grupo.get('confirmacion')?.value as string
  return confirmacion && password !== confirmacion ? { noCoincide: true } : null
}

/**
 * Elegir la contraseña nueva con el enlace del correo (CU04). Pantalla pública: se abre con o
 * sin sesión, también desde el navegador de un teléfono. Al terminar no inicia sesión.
 *
 * * El token sale del query string (?token=…). Sin token, o si el backend rechaza el enlace
 * * (ERR_IDE_11), se explica sin distinguir el motivo y se ofrece pedir otro.
 * * La regla de la contraseña es la misma del registro (registro.reglas.ts).
 * ! La contraseña y el token nunca se guardan ni se escriben en la consola.
 */
@Component({
  selector: 'app-restablecer',
  imports: [ReactiveFormsModule, RouterLink],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-auth-layout
      titulo="Una contraseña nueva"
      mensaje="Elige una contraseña segura para tu cuenta."
    >
      @switch (estado()) {
        @case ('hecho') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-ok"><dh-icono nombre="check-circle" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Contraseña actualizada</h1>
            <p class="dh-sub-auth" role="status">
              Tu contraseña se actualizó. Ya puedes iniciar sesión.
            </p>
            <a routerLink="/login" class="dh-btn dh-btn-block">Ir a iniciar sesión</a>
          </div>
        }
        @case ('invalido') {
          <div class="dh-state">
            <div class="dh-icono-grande dh-mal"><dh-icono nombre="alert" [tam]="34" /></div>
            <h1 class="dh-titulo-auth">Enlace no válido</h1>
            <p class="dh-sub-auth" role="alert">Este enlace no es válido o ya venció.</p>
            <a routerLink="/account/forgot-password" class="dh-btn dh-btn-soft dh-btn-block">
              Pedir un enlace nuevo
            </a>
          </div>
        }
        @default {
          <h1 class="dh-titulo-auth">Nueva contraseña</h1>
          <p class="dh-sub-auth">Elige la contraseña nueva de tu cuenta.</p>

          <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
            <div class="dh-field">
              <label for="password" class="dh-label">Contraseña nueva</label>
              <input
                id="password"
                type="password"
                formControlName="password"
                autocomplete="new-password"
                class="dh-input"
              />
              @if (error('password'); as mensaje) {
                <p class="dh-error" role="alert">{{ mensaje }}</p>
              }
            </div>

            <div class="dh-field">
              <label for="confirmacion" class="dh-label">Confirma la contraseña</label>
              <input
                id="confirmacion"
                type="password"
                formControlName="confirmacion"
                autocomplete="new-password"
                class="dh-input"
              />
              @if (error('confirmacion'); as mensaje) {
                <p class="dh-error" role="alert">{{ mensaje }}</p>
              }
            </div>

            @if (errorGeneral(); as mensaje) {
              <p class="dh-notice dh-notice-error" role="alert">{{ mensaje }}</p>
            }

            <button type="submit" [disabled]="enviando()" class="dh-btn dh-btn-block">
              {{ enviando() ? 'Guardando…' : 'Cambiar contraseña' }}
            </button>
          </form>
        }
      }
    </dh-auth-layout>
  `,
})
export class Restablecer {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly cuenta = inject(CuentaService)
  private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token')

  protected readonly estado = signal<Estado>(this.token ? 'formulario' : 'invalido')
  protected readonly enviando = signal(false)
  protected readonly errorGeneral = signal<string | null>(null)

  protected readonly form = this.fb.group(
    {
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN),
          Validators.maxLength(PASSWORD_MAX),
        ],
      ],
      confirmacion: ['', [Validators.required]],
    },
    { validators: confirmacionIgual }
  )

  protected error(campo: Campo): string | null {
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    if (campo === 'password') {
      if (control.errors?.['required']) return 'La contraseña es obligatoria'
      if (control.errors?.['minlength'] || control.errors?.['maxlength']) return MENSAJE_LARGO
      return null
    }
    if (control.errors?.['required']) return 'Confirma la contraseña'
    return this.form.errors?.['noCoincide'] ? 'Las contraseñas no coinciden' : null
  }

  protected async enviar(): Promise<void> {
    this.errorGeneral.set(null)
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched()
      return
    }

    this.enviando.set(true)
    try {
      await firstValueFrom(
        this.cuenta.restablecerContrasena(this.token, this.form.getRawValue().password)
      )
      this.form.reset()
      this.estado.set('hecho')
    } catch (error) {
      this.errorGeneral.set(null)
      if (error instanceof ApiError && error.code === 'ERR_IDE_11') {
        this.estado.set('invalido')
      } else if (error instanceof ApiError && error.code === 'ERR_SYS_01') {
        this.errorGeneral.set(error.message)
      } else {
        this.errorGeneral.set('No pudimos cambiar la contraseña. Intenta más tarde.')
      }
    } finally {
      this.enviando.set(false)
    }
  }
}
