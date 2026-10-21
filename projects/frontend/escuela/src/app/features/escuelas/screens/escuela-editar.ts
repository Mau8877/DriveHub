import { Component, OnInit, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'

import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import { mensajeDeError } from '../schemas/escuela.mensajes'
import {
  NOMBRE_ESCUELA_MAX,
  NOMBRE_ESCUELA_MIN,
  obligatorioConTexto,
} from '../schemas/escuela.reglas'
import type { EscuelaEstado } from '../types/escuela.types'

/**
 * Editar escuela (CU05): lo único que se cambia es el nombre; el slug se muestra y no se edita
 * (ya viaja en enlaces de registro compartidos). Una escuela inactiva no se edita: hay que
 * reactivarla primero.
 */
@Component({
  selector: 'app-escuela-editar',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-xl p-6">
      <h1 class="mb-4 text-2xl font-semibold">Editar escuela</h1>

      @if (cargando()) {
        <p class="text-muted-foreground text-sm">Cargando escuela…</p>
      } @else if (noEncontrada()) {
        <p class="text-destructive text-sm" role="alert">No se encontró la escuela.</p>
        <a routerLink="/escuelas" class="mt-3 inline-block text-sm underline">Volver al listado</a>
      } @else if (escuela(); as actual) {
        @if (!actual.activa) {
          <p class="text-sm" role="alert">La escuela está inactiva. Reactívala para editarla.</p>
          <a routerLink="/escuelas" class="mt-3 inline-block text-sm underline">
            Volver al listado
          </a>
        } @else {
          <form [formGroup]="form" (ngSubmit)="enviar()" class="flex flex-col gap-4" novalidate>
            <div class="flex flex-col gap-1">
              <label for="slug" class="text-sm font-medium">Slug (no se puede cambiar)</label>
              <input
                id="slug"
                [value]="actual.slug"
                readonly
                class="border-border bg-muted text-muted-foreground rounded-md border px-3 py-2 text-sm"
              />
            </div>

            <div class="flex flex-col gap-1">
              <label for="nombre" class="text-sm font-medium">Nombre</label>
              <input
                id="nombre"
                formControlName="nombre"
                class="border-border bg-background rounded-md border px-3 py-2 text-sm"
              />
              @if (error(); as mensaje) {
                <p class="text-destructive text-xs" role="alert">{{ mensaje }}</p>
              }
            </div>

            @if (errorGeneral(); as mensaje) {
              <p class="text-destructive text-sm" role="alert">{{ mensaje }}</p>
            }

            <div class="flex items-center gap-3">
              <button
                type="submit"
                [disabled]="enviando()"
                class="bg-primary text-primary-foreground rounded-md px-4 py-2 text-sm font-medium disabled:opacity-50"
              >
                {{ enviando() ? 'Guardando…' : 'Guardar' }}
              </button>
              <a routerLink="/escuelas" class="text-sm underline">Cancelar</a>
            </div>
          </form>
        }
      }
    </section>
  `,
})
export class EscuelaEditar implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly servicio = inject(EscuelasService)
  private readonly ruta = inject(ActivatedRoute)
  private readonly router = inject(Router)

  readonly cargando = signal(true)
  readonly noEncontrada = signal(false)
  readonly escuela = signal<EscuelaEstado | null>(null)
  readonly enviando = signal(false)
  readonly errorGeneral = signal<string | null>(null)

  readonly form = this.fb.group({
    nombre: [
      '',
      [
        obligatorioConTexto,
        Validators.minLength(NOMBRE_ESCUELA_MIN),
        Validators.maxLength(NOMBRE_ESCUELA_MAX),
      ],
    ],
  })

  private get codigo(): string {
    return this.ruta.snapshot.paramMap.get('codigo') ?? ''
  }

  ngOnInit(): void {
    this.servicio.obtener(this.codigo).subscribe({
      next: (escuela) => {
        this.escuela.set(escuela)
        this.form.controls.nombre.setValue(escuela.nombre)
        this.cargando.set(false)
      },
      error: () => {
        this.noEncontrada.set(true)
        this.cargando.set(false)
      },
    })
  }

  error(): string | null {
    const control = this.form.controls.nombre
    if (!control.touched && !control.dirty) return null
    return mensajeDeError('nombre', control.errors)
  }

  enviar(): void {
    this.errorGeneral.set(null)
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }

    this.enviando.set(true)
    this.servicio.editar(this.codigo, this.form.getRawValue().nombre.trim()).subscribe({
      next: () => {
        this.enviando.set(false)
        void this.router.navigate(['/escuelas'], {
          state: { aviso: 'Escuela actualizada correctamente.' },
        })
      },
      error: (error: unknown) => {
        this.enviando.set(false)
        const codigo = error instanceof ApiError ? error.code : null
        if (codigo === 'ERR_ESC_06') {
          this.errorGeneral.set('La escuela está inactiva. Reactívala para editarla.')
        } else if (codigo === 'ERR_ESC_02') {
          this.errorGeneral.set('No se encontró la escuela.')
        } else if (codigo === 'ERR_SYS_01') {
          this.errorGeneral.set('Revisa el nombre e inténtalo de nuevo.')
        } else {
          this.errorGeneral.set('No pudimos guardar los cambios. Inténtalo de nuevo más tarde.')
        }
      },
    })
  }
}
