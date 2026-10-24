import { CUSTOM_ELEMENTS_SCHEMA, Component, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import '@drivehub/ui'

import { ApiError } from '../../../core/api/api.types'
import { destinoSeguro } from '../../../core/sesion/redireccion'
import { SesionService } from '../../../core/sesion/sesion.service'

/** Por qué se llegó al login sin que la persona lo pidiera (viene en `?motivo=`). */
const MENSAJES_DE_MOTIVO: Record<string, string> = {
  ERR_IDE_05: 'Tu cuenta está bloqueada temporalmente. Intenta de nuevo más tarde.',
  ERR_IDE_06: 'Tu cuenta está inactiva. Contacta a tu administrador.',
  ERR_IDE_07: 'La escuela de tu cuenta está suspendida.',
  ERR_IDE_08: 'Tu cuenta no puede usar la web. Ingresa desde la aplicación móvil.',
  ERR_IDE_09: 'Tu sesión terminó. Inicia sesión de nuevo.',
}

/**
 * Login único de la web (CU02). Solo entran Super admin y Administrador de
 * escuela; el estudiante y el instructor usan la app móvil.
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-auth-layout
      titulo="Bienvenido de vuelta"
      mensaje="Gestiona clases, alumnos y pagos de tu autoescuela desde un solo lugar."
    >
      <h1 class="dh-titulo-auth">Iniciar sesión</h1>
      <p class="dh-sub-auth">Si eres estudiante o instructor, ingresa desde la app móvil.</p>

      @if (motivo) {
        <p class="dh-notice dh-notice-info" role="status">
          <dh-icono nombre="info" [tam]="18" />
          <span>{{ motivo }}</span>
        </p>
      }

      <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
        <div class="dh-field" [class.dh-invalido]="error('email')">
          <label for="email" class="dh-label">Correo</label>
          <div class="dh-input-wrap">
            <dh-icono class="dh-input-ico" nombre="mail" [tam]="18" />
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="username"
              placeholder="tucorreo@escuela.com"
              class="dh-input"
            />
          </div>
          @if (error('email'); as mensaje) {
            <p class="dh-error" role="alert">{{ mensaje }}</p>
          }
        </div>

        <div class="dh-field" [class.dh-invalido]="error('password')">
          <label for="password" class="dh-label">Contraseña</label>
          <div class="dh-input-wrap">
            <dh-icono class="dh-input-ico" nombre="lock" [tam]="18" />
            <input
              id="password"
              [type]="verPassword() ? 'text' : 'password'"
              formControlName="password"
              autocomplete="current-password"
              placeholder="••••••••"
              class="dh-input"
            />
            <button
              type="button"
              class="dh-input-accion"
              [attr.aria-label]="verPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
              (click)="verPassword.set(!verPassword())"
            >
              <dh-icono [nombre]="verPassword() ? 'eye-off' : 'eye'" [tam]="18" />
            </button>
          </div>
          @if (error('password'); as mensaje) {
            <p class="dh-error" role="alert">{{ mensaje }}</p>
          }
        </div>

        <div class="dh-form-row">
          <span></span>
          <a routerLink="/account/forgot-password" class="dh-link">¿Olvidaste tu contraseña?</a>
        </div>

        @if (errorGeneral(); as mensaje) {
          <p class="dh-notice dh-notice-error" role="alert">
            <dh-icono nombre="alert" [tam]="18" />
            <span>{{ mensaje }}</span>
          </p>
        }

        <button type="submit" [disabled]="enviando()" class="dh-btn dh-btn-block">
          {{ enviando() ? 'Ingresando…' : 'Ingresar' }}
          <dh-icono nombre="arrow-right" [tam]="18" />
        </button>
      </form>
    </dh-auth-layout>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly router = inject(Router)
  private readonly sesion = inject(SesionService)
  private readonly query = inject(ActivatedRoute).snapshot.queryParamMap

  protected readonly enviando = signal(false)
  protected readonly verPassword = signal(false)
  protected readonly errorGeneral = signal<string | null>(null)
  protected readonly motivo = MENSAJES_DE_MOTIVO[this.query.get('motivo') ?? ''] ?? null

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  })

  protected error(campo: 'email' | 'password'): string | null {
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    if (control.errors?.['required']) {
      return campo === 'email' ? 'El correo es obligatorio' : 'La contraseña es obligatoria'
    }
    if (control.errors?.['email']) return 'El correo no es válido'
    return null
  }

  protected async enviar(): Promise<void> {
    this.errorGeneral.set(null)
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    const { email, password } = this.form.getRawValue()
    this.enviando.set(true)
    try {
      await this.sesion.iniciarSesion(email.trim(), password)
      await this.router.navigateByUrl(destinoSeguro(this.query.get('redirect')))
    } catch (error) {
      this.errorGeneral.set(this.mensajeDe(error))
    } finally {
      this.enviando.set(false)
    }
  }

  private mensajeDe(error: unknown): string {
    if (!(error instanceof ApiError)) {
      return 'No pudimos iniciar sesión. Inténtalo de nuevo más tarde.'
    }
    switch (error.code) {
      case 'ERR_IDE_04':
        return 'Correo o contraseña incorrectos'
      case 'ERR_IDE_06':
        return 'Tu cuenta está inactiva. Contacta a tu administrador'
      case 'ERR_IDE_07':
        return 'La escuela de tu cuenta está suspendida'
      case 'ERR_IDE_05':
      case 'ERR_IDE_08':
        // * El servidor dice cuándo reintentar (423) o qué plataforma usar (403)
        return error.message
      default:
        return 'No pudimos iniciar sesión. Inténtalo de nuevo más tarde.'
    }
  }
}
