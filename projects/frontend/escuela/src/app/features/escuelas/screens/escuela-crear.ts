import { Component, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { Router, RouterLink } from '@angular/router'

import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import { type CampoEscuela, mensajeDeError } from '../schemas/escuela.mensajes'
import {
  APELLIDOS_MAX,
  EMAIL_MAX,
  NOMBRES_MAX,
  NOMBRE_ESCUELA_MAX,
  NOMBRE_ESCUELA_MIN,
  PASSWORD_MAX,
  PASSWORD_MIN,
  SLUG_MAX,
  SLUG_MIN,
  TELEFONO_MAX,
  edadMinima,
  fechaNoFutura,
  obligatorioConTexto,
  slugDesdeNombre,
  slugValido,
} from '../schemas/escuela.reglas'

/**
 * Nueva escuela (CU05): el formulario de la escuela y de su Administrador. La contraseña inicial
 * la escribe el Super admin.
 *
 * * El slug se sugiere desde el nombre mientras no se haya editado a mano.
 * * Los errores del servidor se muestran en su campo: ERR_ESC_03 (slug), ERR_IDE_01 (correo) y
 * * ERR_IDE_02 (edad). Nada se pierde del formulario al fallar.
 */
@Component({
  selector: 'app-escuela-crear',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-2xl p-6">
      <h1 class="mb-1 text-2xl font-semibold">Nueva escuela</h1>
      <p class="text-muted-foreground mb-4 text-sm">
        Se crea la escuela junto con la cuenta de su Administrador.
      </p>

      <form [formGroup]="form" (ngSubmit)="enviar()" class="flex flex-col gap-6" novalidate>
        <fieldset class="flex flex-col gap-4">
          <legend class="mb-1 text-lg font-medium">Escuela</legend>

          <div class="flex flex-col gap-1">
            <label for="nombre" class="text-sm font-medium">Nombre</label>
            <input
              id="nombre"
              formControlName="nombre"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('nombre'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="flex flex-col gap-1">
            <label for="slug" class="text-sm font-medium">Slug (enlace de registro)</label>
            <input
              id="slug"
              formControlName="slug"
              (input)="slugEditadoAMano = true"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('slug'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>
        </fieldset>

        <fieldset class="flex flex-col gap-4" formGroupName="administrador">
          <legend class="mb-1 text-lg font-medium">Administrador</legend>

          <div class="flex flex-col gap-1">
            <label for="nombres" class="text-sm font-medium">Nombres</label>
            <input
              id="nombres"
              formControlName="nombres"
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
              autocomplete="off"
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('email'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="flex flex-col gap-1">
            <label for="password" class="text-sm font-medium">Contraseña inicial</label>
            <div class="flex gap-2">
              <input
                id="password"
                [type]="mostrarPassword() ? 'text' : 'password'"
                formControlName="password"
                autocomplete="new-password"
                class="border-border bg-background flex-1 rounded-md border px-3 py-2 text-sm"
              />
              <button
                type="button"
                class="border-border rounded-md border px-3 text-sm"
                (click)="mostrarPassword.set(!mostrarPassword())"
              >
                {{ mostrarPassword() ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
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
              class="border-border bg-background rounded-md border px-3 py-2 text-sm"
            />
            @if (error('telefono'); as mensaje) {
              <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
            }
          </div>
        </fieldset>

        @if (errorGeneral(); as mensaje) {
          <p class="text-destructive text-sm" role="alert">{{ mensaje }}</p>
        }

        <div class="flex items-center gap-3">
          <button
            type="submit"
            [disabled]="enviando()"
            class="bg-primary text-primary-foreground rounded-md px-4 py-2 text-sm font-medium disabled:opacity-50"
          >
            {{ enviando() ? 'Creando…' : 'Crear escuela' }}
          </button>
          <a routerLink="/escuelas" class="text-sm underline">Cancelar</a>
        </div>
      </form>
    </section>
  `,
})
export class EscuelaCrear {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly servicio = inject(EscuelasService)
  private readonly router = inject(Router)

  readonly enviando = signal(false)
  readonly mostrarPassword = signal(false)
  readonly errorGeneral = signal<string | null>(null)
  private readonly erroresDelServidor = signal<Partial<Record<CampoEscuela, string>>>({})
  /** Mientras sea false, el slug se sugiere desde el nombre. */
  slugEditadoAMano = false

  readonly form = this.fb.group({
    nombre: [
      '',
      [
        obligatorioConTexto,
        Validators.minLength(NOMBRE_ESCUELA_MIN),
        Validators.maxLength(NOMBRE_ESCUELA_MAX),
      ],
    ],
    slug: [
      '',
      [
        Validators.required,
        Validators.minLength(SLUG_MIN),
        Validators.maxLength(SLUG_MAX),
        slugValido,
      ],
    ],
    administrador: this.fb.group({
      nombres: ['', [obligatorioConTexto, Validators.maxLength(NOMBRES_MAX)]],
      apellidos: ['', [obligatorioConTexto, Validators.maxLength(APELLIDOS_MAX)]],
      fechaNacimiento: ['', [Validators.required, fechaNoFutura(), edadMinima()]],
      email: ['', [obligatorioConTexto, Validators.email, Validators.maxLength(EMAIL_MAX)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN),
          Validators.maxLength(PASSWORD_MAX),
        ],
      ],
      telefono: ['', [Validators.maxLength(TELEFONO_MAX)]],
    }),
  })

  constructor() {
    this.form.controls.nombre.valueChanges.subscribe((nombre) => {
      if (!this.slugEditadoAMano) {
        this.form.controls.slug.setValue(slugDesdeNombre(nombre), { emitEvent: false })
      }
    })
  }

  /** Mensaje del campo si ya fue tocado y es inválido (o si el servidor lo marcó). */
  error(campo: CampoEscuela): string | null {
    const control = this.controlDe(campo)
    if (!control.touched && !control.dirty) return null
    return mensajeDeError(campo, control.errors) ?? this.erroresDelServidor()[campo] ?? null
  }

  enviar(): void {
    this.errorGeneral.set(null)
    this.erroresDelServidor.set({})
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    const valores = this.form.getRawValue()
    const telefono = valores.administrador.telefono.trim()
    this.enviando.set(true)
    this.servicio
      .crear({
        nombre: valores.nombre.trim(),
        slug: valores.slug,
        administrador: {
          nombres: valores.administrador.nombres.trim(),
          apellidos: valores.administrador.apellidos.trim(),
          fechaNacimiento: valores.administrador.fechaNacimiento,
          email: valores.administrador.email.trim(),
          password: valores.administrador.password,
          ...(telefono ? { telefono } : {}),
        },
      })
      .subscribe({
        next: () => {
          this.enviando.set(false)
          void this.router.navigate(['/escuelas'], {
            state: { aviso: 'Escuela creada correctamente.' },
          })
        },
        error: (error: unknown) => {
          this.enviando.set(false)
          this.mostrarErrorDelServidor(error)
        },
      })
  }

  private controlDe(campo: CampoEscuela) {
    return campo === 'nombre' || campo === 'slug'
      ? this.form.controls[campo]
      : this.form.controls.administrador.controls[campo]
  }

  private mostrarErrorDelServidor(error: unknown): void {
    const codigo = error instanceof ApiError ? error.code : null
    switch (codigo) {
      case 'ERR_ESC_03':
        this.marcarCampo('slug', 'Ese slug ya está en uso')
        break
      case 'ERR_IDE_01':
        this.marcarCampo('email', 'Este correo ya está registrado')
        break
      case 'ERR_IDE_02':
        this.marcarCampo('fechaNacimiento', 'El administrador debe tener al menos 18 años')
        break
      case 'ERR_SYS_01':
        this.errorGeneral.set('Revisa los datos ingresados e inténtalo de nuevo.')
        break
      default:
        this.errorGeneral.set('No pudimos crear la escuela. Inténtalo de nuevo más tarde.')
    }
  }

  private marcarCampo(campo: CampoEscuela, mensaje: string): void {
    this.controlDe(campo).markAsTouched()
    this.erroresDelServidor.set({ [campo]: mensaje })
  }
}
