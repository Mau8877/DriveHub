import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, inject, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute } from '@angular/router'
import '@drivehub/ui'

import { ApiError } from '../../../core/api/api.types'
import { DhValor } from '../../../core/forms/dh-valor.directive'
import type { DetalleUbicacion } from '@drivehub/ui'
import type { EscuelaPublica, Sexo } from '../types/registro.types'
import { type CampoRegistro, mensajeDeError } from '../schemas/registro.mensajes'
import {
  APELLIDOS_MAX,
  ciValido,
  coordenada,
  edadMinima,
  EMAIL_MAX,
  fechaMaximaDeNacimiento,
  fechaNoFutura,
  LATITUD_MAX,
  LONGITUD_MAX,
  NOMBRES_MAX,
  obligatorioConTexto,
  PASSWORD_MAX,
  PASSWORD_MIN,
  TELEFONO_MAX,
} from '../schemas/registro.reglas'
import { RegistroService } from '../api/registro.service'

/**
 * Registro del estudiante en el enlace de su escuela (CU01). Pantalla
 * pública: sin sesión y sin auto-login. Al terminar muestra que ingrese
 * desde la app móvil (los estudiantes no usan la web).
 *
 * * La escuela sale de la URL (/register/:escuelaSlug); se consulta para
 * * mostrar su nombre y, si no está disponible, no se muestra el formulario.
 * * CU08a: pide además CI, sexo y la ubicación en un mapa (<dh-mapa-ubicacion>). El mapa solo
 * * elige el punto: la dirección la calcula el servidor al registrar.
 */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, DhValor],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-auth-layout
      titulo="Empieza a conducir tu futuro"
      mensaje="Crea tu cuenta de estudiante y reserva clases en minutos."
    >
      @if (cargandoEscuela()) {
        <p class="dh-sub-auth" role="status">Cargando escuela…</p>
      } @else if (registrado()) {
        <div class="dh-state">
          <div class="dh-icono-grande dh-ok"><dh-icono nombre="check-circle" [tam]="34" /></div>
          <h1 class="dh-titulo-auth">¡Cuenta creada!</h1>
          <p class="dh-sub-auth" role="status">
            Tu cuenta fue creada. Ingresa desde la app móvil de DriveHub.
          </p>
        </div>
      } @else if (escuelaNoDisponible()) {
        <h1 class="dh-titulo-auth">Registro</h1>
        <p class="dh-notice dh-notice-error" role="alert">Esta escuela no está disponible.</p>
      } @else {
        <h1 class="dh-titulo-auth">Registro en {{ escuela()?.nombre }}</h1>
        <p class="dh-sub-auth">Crea tu cuenta de estudiante para empezar.</p>

        <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <div class="dh-field">
            <label for="nombres" class="dh-label">Nombres</label>
            <input
              id="nombres"
              formControlName="nombres"
              autocomplete="given-name"
              class="dh-input"
            />
            @if (error('nombres'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <label for="apellidos" class="dh-label">Apellidos</label>
            <input
              id="apellidos"
              formControlName="apellidos"
              autocomplete="family-name"
              class="dh-input"
            />
            @if (error('apellidos'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <label for="fechaNacimiento" class="dh-label">Fecha de nacimiento</label>
            <dh-fecha
              id="fechaNacimiento"
              formControlName="fechaNacimiento"
              etiqueta="Fecha de nacimiento"
              [max]="maximoNacimiento"
              [invalido]="!!error('fechaNacimiento')"
            ></dh-fecha>
            @if (error('fechaNacimiento'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <label for="sexo" class="dh-label">Sexo</label>
            <dh-select
              id="sexo"
              formControlName="sexo"
              etiqueta="Sexo"
              [opciones]="opcionesSexo"
              [invalido]="!!error('sexo')"
            ></dh-select>
            @if (error('sexo'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <label for="ci" class="dh-label">CI</label>
            <input id="ci" formControlName="ci" autocomplete="off" class="dh-input" />
            @if (error('ci'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <label for="email" class="dh-label">Correo</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="email"
              class="dh-input"
            />
            @if (error('email'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <label for="password" class="dh-label">Contraseña</label>
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
            <label for="telefono" class="dh-label">Teléfono (opcional)</label>
            <input
              id="telefono"
              type="tel"
              formControlName="telefono"
              autocomplete="tel"
              class="dh-input"
            />
            @if (error('telefono'); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          <div class="dh-field">
            <span class="dh-label">Tu ubicación</span>
            <dh-mapa-ubicacion
              [latitud]="form.controls.latitud.value"
              [longitud]="form.controls.longitud.value"
              (dh-ubicacion)="alElegirUbicacion($event)"
            />
            @if (errorUbicacion(); as mensaje) {
              <p class="dh-error" role="alert">{{ mensaje }}</p>
            }
          </div>

          @if (errorGeneral(); as mensaje) {
            <p class="dh-notice dh-notice-error" role="alert">{{ mensaje }}</p>
          }

          <button type="submit" [disabled]="enviando()" class="dh-btn dh-btn-block">
            {{ enviando() ? 'Registrando…' : 'Registrarme' }}
          </button>
        </form>
      }
    </dh-auth-layout>
  `,
})
export class Registro implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly ruta = inject(ActivatedRoute)
  private readonly servicio = inject(RegistroService)

  readonly cargandoEscuela = signal(true)
  readonly escuela = signal<EscuelaPublica | null>(null)
  // * Hasta dónde deja elegir el calendario (la edad mínima); la regla de verdad es `edadMinima`
  protected readonly maximoNacimiento = fechaMaximaDeNacimiento()
  protected readonly opcionesSexo = [
    { valor: 'MASCULINO', etiqueta: 'Masculino' },
    { valor: 'FEMENINO', etiqueta: 'Femenino' },
  ]
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
    sexo: ['' as Sexo | '', [Validators.required]],
    ci: ['', [obligatorioConTexto, ciValido]],
    latitud: [null as number | null, [coordenada(LATITUD_MAX)]],
    longitud: [null as number | null, [coordenada(LONGITUD_MAX)]],
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
  error(campo: Exclude<CampoRegistro, 'ubicacion'>): string | null {
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    return mensajeDeError(campo, control.errors) ?? this.errorDelServidor(campo)
  }

  /** El mapa no es un control: su error sale de las dos coordenadas, una vez tocadas. */
  errorUbicacion(): string | null {
    const { latitud, longitud } = this.form.controls
    if (!latitud.touched && !longitud.touched) return null
    return mensajeDeError('ubicacion', latitud.errors ?? longitud.errors)
  }

  alElegirUbicacion(detalle: Event): void {
    const { latitud, longitud } = (detalle as CustomEvent<DetalleUbicacion>).detail
    this.form.patchValue({ latitud, longitud })
    this.form.controls.latitud.markAsTouched()
    this.form.controls.longitud.markAsTouched()
  }

  private readonly erroresDelServidor = signal<
    Partial<Record<Exclude<CampoRegistro, 'ubicacion'>, string>>
  >({})

  private errorDelServidor(campo: Exclude<CampoRegistro, 'ubicacion'>): string | null {
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
        ci: valores.ci.trim(),
        sexo: valores.sexo as Sexo,
        latitud: valores.latitud as number,
        longitud: valores.longitud as number,
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
      case 'ERR_IDE_19':
        this.marcarCampo('ci', 'Este CI ya está registrado')
        break
      case 'ERR_SYS_01':
        this.errorGeneral.set('Revisa los datos ingresados e inténtalo de nuevo.')
        break
      default:
        this.errorGeneral.set('No pudimos completar el registro. Inténtalo de nuevo más tarde.')
    }
  }

  private marcarCampo(campo: Exclude<CampoRegistro, 'ubicacion'>, mensaje: string): void {
    this.form.controls[campo].markAsTouched()
    this.erroresDelServidor.set({ [campo]: mensaje })
  }
}
