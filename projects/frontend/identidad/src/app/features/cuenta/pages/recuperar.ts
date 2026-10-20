import { Component, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { RouterLink } from '@angular/router'
import { firstValueFrom } from 'rxjs'

import { EMAIL_MAX } from '../../registro/registro.reglas'
import { CuentaService } from '../cuenta.service'

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
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-sm rounded-lg border p-6">
        <h1 class="mb-1 text-xl font-semibold">Recuperar contraseña</h1>
        <p class="text-muted-foreground mb-4 text-sm">
          Ingresa tu correo y te enviaremos un enlace para elegir una contraseña nueva.
        </p>

        @if (enviado()) {
          <p
            class="bg-secondary text-secondary-foreground mb-4 rounded-md px-3 py-2 text-sm"
            role="status"
          >
            {{ mensajeEnviado }}
          </p>
        } @else {
          <form
            [formGroup]="form"
            (ngSubmit)="enviar()"
            class="mb-4 flex flex-col gap-4"
            novalidate
          >
            <div class="flex flex-col gap-1">
              <label for="email" class="text-sm font-medium">Correo</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                autocomplete="email"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error(); as mensaje) {
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
              {{ enviando() ? 'Enviando…' : 'Enviar enlace' }}
            </button>
          </form>
        }

        <p class="text-muted-foreground mb-4 text-xs">
          La recuperación solo funciona si verificaste tu correo desde la aplicación.
        </p>
        <a routerLink="/login" class="text-sm font-medium underline">Volver a iniciar sesión</a>
      </div>
    </section>
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
