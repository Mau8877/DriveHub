import { CUSTOM_ELEMENTS_SCHEMA, Component, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { RouterLink } from '@angular/router'
import '@drivehub/ui'
import { firstValueFrom } from 'rxjs'

import { EMAIL_MAX } from '../../registro'
import { CuentaService } from '../api/cuenta.service'

/** Mismo texto siempre: no revela si el correo existe ni si está verificado. */
const MENSAJE_ENVIADO =
  'Si el correo está registrado y verificado, te enviamos un enlace. Revisa tu bandeja.'

/**
 * Pedir el enlace para recuperar la contraseña (CU04). Pantalla pública, con o sin sesión.
 *
 * * El mensaje de éxito es fijo y no depende de la respuesta del backend; la advertencia de
 * * que hace falta el correo verificado se muestra siempre, sin revelar si la cuenta existe.
 */
@Component({
  selector: 'app-recuperar',
  imports: [ReactiveFormsModule, RouterLink],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-auth-layout
      titulo="Recupera tu acceso"
      mensaje="Te enviamos un enlace seguro para elegir una contraseña nueva."
    >
      <h1 class="dh-titulo-auth">Recuperar contraseña</h1>
      <p class="dh-sub-auth">
        Ingresa tu correo y te enviaremos un enlace para elegir una contraseña nueva.
      </p>

      @if (enviado()) {
        <p class="dh-notice dh-notice-ok" role="status">
          <dh-icono nombre="mail" [tam]="18" />
          <span>{{ mensajeEnviado }}</span>
        </p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <div class="dh-field">
            <label for="email" class="dh-label">Correo</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="email"
              class="dh-input"
            />
            @if (error(); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          @if (errorGeneral(); as mensaje) {
            <p class="dh-notice dh-notice-error" role="alert">{{ mensaje }}</p>
          }

          <button type="submit" [disabled]="enviando()" class="dh-btn dh-btn-block">
            {{ enviando() ? 'Enviando…' : 'Enviar enlace' }}
          </button>
        </form>
      }

      <p class="dh-notice dh-notice-warn">
        <dh-icono nombre="alert" [tam]="18" />
        <span>La recuperación solo funciona si verificaste tu correo desde la aplicación.</span>
      </p>
      <p class="dh-auth-alt"><a routerLink="/login" class="dh-link">Volver a iniciar sesión</a></p>
    </dh-auth-layout>
  `,
})
export class Recuperar {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly cuenta = inject(CuentaService)

  protected readonly mensajeEnviado = MENSAJE_ENVIADO
  protected readonly enviando = signal(false)
  protected readonly enviado = signal(false)
  protected readonly errorGeneral = signal<string | null>(null)

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(EMAIL_MAX)]],
  })

  protected error(): string | null {
    const control = this.form.controls.email
    if (!control.touched && !control.dirty) return null
    if (control.errors?.['required']) return 'El correo es obligatorio'
    if (control.errors?.['email']) return 'El correo no es válido'
    if (control.errors?.['maxlength']) return `El correo no puede superar ${EMAIL_MAX} caracteres`
    return null
  }

  protected async enviar(): Promise<void> {
    this.errorGeneral.set(null)
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    this.enviando.set(true)
    try {
      await firstValueFrom(this.cuenta.solicitarRecuperacion(this.form.getRawValue().email.trim()))
      this.enviado.set(true)
    } catch {
      this.errorGeneral.set('No pudimos procesar la solicitud. Intenta más tarde.')
    } finally {
      this.enviando.set(false)
    }
  }
}
