import { Component, OnInit, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute } from '@angular/router'

import { ApiError } from '../../../core/api/api.types'
import type { EscuelaPublica } from '../models/registro.models'
import { type CampoRegistro, mensajeDeError } from '../registro.mensajes'
import {
  APELLIDOS_MAX,
  edadMinima,
  EMAIL_MAX,
  fechaNoFutura,
  NOMBRES_MAX,
  obligatorioConTexto,
  PASSWORD_MAX,
  PASSWORD_MIN,
  TELEFONO_MAX,
} from '../registro.reglas'
import { RegistroService } from '../registro.service'

/**
 * Registro del estudiante en el enlace de su escuela (CU01). Pantalla
 * pública: sin sesión y sin auto-login. Al terminar muestra que ingrese
 * desde la app móvil (los estudiantes no usan la web).
 *
 * * La escuela sale de la URL (/register/:escuelaSlug); se consulta para
 * * mostrar su nombre y, si no está disponible, no se muestra el formulario.
 */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule],
  template: `
    <section class="flex min-h-svh items-center justify-center p-6">
      <div class="bg-card border-border w-full max-w-md rounded-lg border p-6">
        @if (cargandoEscuela()) {
          <p class="text-muted-foreground text-sm">Cargando escuela…</p>
        } @else if (registrado()) {
          <h1 class="mb-1 text-xl font-semibold">¡Cuenta creada!</h1>
          <p class="text-muted-foreground text-sm" role="status">
            Tu cuenta fue creada. Ingresa desde la app móvil de DriveHub.
          </p>
        } @else if (escuelaNoDisponible()) {
          <h1 class="mb-1 text-xl font-semibold">Registro</h1>
          <p class="text-destructive text-sm" role="alert">Esta escuela no está disponible.</p>
        } @else {
          <h1 class="mb-1 text-xl font-semibold">Registro en {{ escuela()?.nombre }}</h1>
          <p class="text-muted-foreground mb-4 text-sm">
            Crea tu cuenta de estudiante para empezar.
          </p>

          <form [formGroup]="form" (ngSubmit)="enviar()" class="flex flex-col gap-4" novalidate>
            <div class="flex flex-col gap-1">
              <label for="nombres" class="text-sm font-medium">Nombres</label>
              <input
                id="nombres"
                formControlName="nombres"
                autocomplete="given-name"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error('nombres'); as mensaje) {
                <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
              }
            </div>

            <div class="flex flex-col gap-1">
              <label for="apellidos" class="text-sm font-medium">Apellidos</label>
              <input
                id="apellidos"
                formControlName="apellidos"
                autocomplete="family-name"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error('apellidos'); as mensaje) {
                <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
              }
            </div>

            <div class="flex flex-col gap-1">
              <label for="fechaNacimiento" class="text-sm font-medium">Fecha de nacimiento</label>
              <input
                id="fechaNacimiento"
                type="date"
                formControlName="fechaNacimiento"
                autocomplete="bday"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error('fechaNacimiento'); as mensaje) {
                <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
              }
            </div>

            <div class="flex flex-col gap-1">
              <label for="email" class="text-sm font-medium">Correo</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                autocomplete="email"
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
                autocomplete="new-password"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error('password'); as mensaje) {
                <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
              }
            </div>

            <div class="flex flex-col gap-1">
              <label for="telefono" class="text-sm font-medium">Teléfono (opcional)</label>
              <input
                id="telefono"
                type="tel"
                formControlName="telefono"
                autocomplete="tel"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error('telefono'); as mensaje) {
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
              {{ enviando() ? 'Registrando…' : 'Registrarme' }}
            </button>
          </form>
        }
      </div>
    </section>
  `,
})
export class Registro implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly ruta = inject(ActivatedRoute)
  private readonly servicio = inject(RegistroService)

  readonly cargandoEscuela = signal(true)
  readonly escuela = signal<EscuelaPublica | null>(null)
  readonly escuelaNoDisponible = signal(false)
  readonly enviando = signal(false)
  readonly registrado = signal(false)
  readonly errorGeneral = signal<string | null>(null)

  readonly form = this.fb.group({
    nombres: ['', [obligatorioConTexto, Validators.maxLength(NOMBRES_MAX)]],
    apellidos: ['', [obligatorioConTexto, Validators.maxLength(APELLIDOS_MAX)]],
    fechaNacimiento: ['', [Validators.required, fechaNoFutura(), edadMinima()]],
    email: ['', [obligatorioConTexto, Validators.email, Validators.maxLength(EMAIL_MAX)]],
    password: [
      '',
      [Validators.required, Validators.minLength(PASSWORD_MIN), Validators.maxLength(PASSWORD_MAX)],
    ],
    telefono: ['', [Validators.maxLength(TELEFONO_MAX)]],
  })

  private get escuelaSlug(): string {
    return this.ruta.snapshot.paramMap.get('escuelaSlug') ?? ''
  }

  ngOnInit(): void {
    this.servicio.obtenerEscuela(this.escuelaSlug).subscribe({
      next: (escuela) => {
        this.escuela.set(escuela)
        this.cargandoEscuela.set(false)
      },
      error: () => {
        this.escuelaNoDisponible.set(true)
        this.cargandoEscuela.set(false)
      },
    })
  }

  /** Mensaje del campo si ya fue tocado y es inválido. */
  error(campo: CampoRegistro): string | null {
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    return mensajeDeError(campo, control.errors) ?? this.errorDelServidor(campo)
  }

  private readonly erroresDelServidor = signal<Partial<Record<CampoRegistro, string>>>({})

  private errorDelServidor(campo: CampoRegistro): string | null {
    return this.erroresDelServidor()[campo] ?? null
  }

  enviar(): void {
    this.errorGeneral.set(null)
    this.erroresDelServidor.set({})
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    const valores = this.form.getRawValue()
    const telefono = valores.telefono.trim()
    this.enviando.set(true)
    this.servicio
      .registrar({
        nombres: valores.nombres.trim(),
        apellidos: valores.apellidos.trim(),
        fechaNacimiento: valores.fechaNacimiento,
        email: valores.email.trim(),
        password: valores.password,
        ...(telefono ? { telefono } : {}),
        escuelaSlug: this.escuelaSlug,
      })
      .subscribe({
        next: () => {
          this.enviando.set(false)
          this.registrado.set(true)
        },
        error: (error: unknown) => {
          this.enviando.set(false)
          this.mostrarErrorDelServidor(error)
        },
      })
  }

  private mostrarErrorDelServidor(error: unknown): void {
    const codigo = error instanceof ApiError ? error.code : null
    switch (codigo) {
      case 'ERR_IDE_01':
        this.marcarCampo('email', 'Este correo ya está registrado')
        break
      case 'ERR_IDE_02':
        this.marcarCampo('fechaNacimiento', 'Debes tener al menos 18 años')
        break
      case 'ERR_IDE_03':
        this.escuelaNoDisponible.set(true)
        break
      case 'ERR_SYS_01':
        this.errorGeneral.set('Revisa los datos ingresados e inténtalo de nuevo.')
        break
      default:
        this.errorGeneral.set('No pudimos completar el registro. Inténtalo de nuevo más tarde.')
    }
  }

  private marcarCampo(campo: CampoRegistro, mensaje: string): void {
    this.form.controls[campo].markAsTouched()
    this.erroresDelServidor.set({ [campo]: mensaje })
  }
}
