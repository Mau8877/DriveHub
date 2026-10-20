import { Component, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'

import { ApiError } from '../core/api/api.types'
import { destinoSeguro } from '../core/sesion/redireccion'
import { SesionService } from '../core/sesion/sesion.service'

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
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-sm rounded-lg border p-6">
        <h1 class="mb-1 text-xl font-semibold">Iniciar sesión</h1>
        <p class="text-muted-foreground mb-4 text-sm">
          Si eres estudiante o instructor, ingresa desde la app móvil.
        </p>

        @if (motivo) {
          <p
            class="bg-secondary text-secondary-foreground mb-4 rounded-md px-3 py-2 text-sm"
            role="status"
          >
            {{ motivo }}
          </p>
        }

        <form [formGroup]="form" (ngSubmit)="enviar()" class="flex flex-col gap-4" novalidate>
          <div class="flex flex-col gap-1">
            <label for="email" class="text-sm font-medium">Correo</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="username"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('email'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="flex flex-col gap-1">
            <label for="password" class="text-sm font-medium">Contraseña</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('password'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>

          @if (errorGeneral(); as mensaje) {
            <p class="text-destructive text-sm" role="alert">{{ mensaje }}</p>
          }

          <button
            type="submit"
            [disabled]="enviando()"
            class="bg-primary text-primary-foreground rounded-md px-4 py-2 text-sm font-medium disabled:opacity-50"
          >
            {{ enviando() ? 'Ingresando…' : 'Ingresar' }}
          </button>
        </form>

        <a
          routerLink="/cuenta/recuperar"
          class="mt-4 block text-center text-sm font-medium underline"
        >
          ¿Olvidaste tu contraseña?
        </a>
      </div>
    </section>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly router = inject(Router)
  private readonly sesion = inject(SesionService)
  private readonly query = inject(ActivatedRoute).snapshot.queryParamMap

  protected readonly enviando = signal(false)
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
