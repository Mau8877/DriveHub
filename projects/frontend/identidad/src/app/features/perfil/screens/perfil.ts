import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import '@drivehub/ui'
import { firstValueFrom } from 'rxjs'

import {
  APELLIDOS_MAX,
  edadMinima,
  fechaNoFutura,
  mensajeDeError,
  NOMBRES_MAX,
  obligatorioConTexto,
  TELEFONO_MAX,
} from '../../registro'
import { PerfilService } from '../api/perfil.service'
import { avisarPerfilActualizado } from '../api/perfil.eventos'
import { CambiarContrasena } from '../components/cambiar-contrasena'
import { CambiarCorreo } from '../components/cambiar-correo'
import { FotoPerfil } from '../components/foto-perfil'
import { codigoDe, mensajeDeErrorPerfil } from '../schemas/perfil.errores'
import type { EditarPerfilRequest, Perfil as PerfilDatos } from '../types/perfil.types'

type Campo = 'nombres' | 'apellidos' | 'telefono' | 'fechaNacimiento'

const ROLES: Record<string, string> = {
  ESTUDIANTE: 'Estudiante',
  INSTRUCTOR: 'Instructor',
  ADMIN_ESCUELA: 'Administrador de escuela',
  SUPER_ADMIN: 'Super admin',
}

function hoyComoTexto(): string {
  const hoy = new Date()
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const dia = String(hoy.getDate()).padStart(2, '0')
  return `${hoy.getFullYear()}-${mes}-${dia}`
}

/**
 * Perfil de la cuenta propia (CU06): datos de solo lectura, edición de los datos personales y las
 * secciones de foto, contraseña y correo. La ven los cuatro roles.
 *
 * * Las reglas de los campos son las del registro (features/registro): mismas en la web y la app.
 * * Solo se envían los campos que cambiaron; un teléfono vaciado se manda como null.
 * * Al guardar nombres o foto se avisa al shell con el evento `perfil-actualizado` para que
 * * actualice el avatar de la cabecera sin recargar.
 */
@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, FotoPerfil, CambiarContrasena, CambiarCorreo],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page">
      <div class="dh-page-head">
        <div>
          <h1>Mi perfil</h1>
          <p>Tus datos, tu foto y la seguridad de tu cuenta.</p>
        </div>
      </div>

      @if (cargando()) {
        <p class="dh-muted dh-texto-sm" role="status">Cargando tu perfil…</p>
      } @else if (perfil(); as p) {
        <div class="dh-perfil-grid">
          <aside class="dh-card dh-perfil-card">
            <app-foto-perfil
              [perfil]="p"
              [rol]="rolesLegibles(p)"
              (perfilCambiado)="reemplazar($event)"
            />
            <dl class="dh-ro">
              <div>
                <dt>Correo</dt>
                <dd>
                  <span data-testid="email">{{ p.email }}</span>
                  @if (p.correoVerificado) {
                    <span class="dh-badge dh-badge-ok">Verificado</span>
                  } @else {
                    <span class="dh-badge">Sin verificar</span>
                  }
                </dd>
              </div>
              <div>
                <dt>Rol</dt>
                <dd data-testid="rol">{{ rolesLegibles(p) }}</dd>
              </div>
              @if (p.escuelaNombre) {
                <div>
                  <dt>Escuela</dt>
                  <dd data-testid="escuela">{{ p.escuelaNombre }}</dd>
                </div>
              }
            </dl>
          </aside>

          <div class="dh-perfil-main">
            <form [formGroup]="form" (ngSubmit)="guardar()" class="dh-card" novalidate>
              <div class="dh-card-head"><h2>Datos personales</h2></div>

              <div class="dh-grid-2">
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
              </div>

              <div class="dh-grid-2">
                <div class="dh-field">
                  <label for="telefono" class="dh-label"
                    >Teléfono <span class="dh-opt">(opcional)</span></label
                  >
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
                  <label for="fechaNacimiento" class="dh-label">Fecha de nacimiento</label>
                  <input
                    id="fechaNacimiento"
                    type="date"
                    formControlName="fechaNacimiento"
                    [max]="hoy"
                    autocomplete="bday"
                    class="dh-input"
                  />
                  @if (error('fechaNacimiento'); as mensaje) {
                    <p class="dh-error" role="alert">{{ mensaje }}</p>
                  }
                </div>
              </div>

              @if (errorGeneral(); as mensaje) {
                <p class="dh-notice dh-notice-error" role="alert">{{ mensaje }}</p>
              }
              @if (aviso(); as mensaje) {
                <p class="dh-notice dh-notice-ok" role="status">
                  <dh-icono nombre="check-circle" [tam]="18" />
                  <span>{{ mensaje }}</span>
                </p>
              }

              <div class="dh-form-foot">
                <button
                  type="submit"
                  [disabled]="guardando() || form.invalid || !hayCambios()"
                  class="dh-btn"
                >
                  {{ guardando() ? 'Guardando…' : 'Guardar cambios' }}
                </button>
              </div>
            </form>

            <app-cambiar-contrasena />
            <app-cambiar-correo [perfil]="p" (perfilCambiado)="reemplazar($event)" />
          </div>
        </div>
      } @else {
        <div class="dh-pila">
          <p class="dh-notice dh-notice-error" role="alert">No pudimos cargar tu perfil.</p>
          <button type="button" class="dh-btn dh-btn-ghost dh-btn-sm" (click)="cargar()">
            Reintentar
          </button>
        </div>
      }
    </section>
  `,
})
export class Perfil implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable
  private readonly api = inject(PerfilService)

  protected readonly hoy = hoyComoTexto()
  protected readonly cargando = signal(true)
  protected readonly perfil = signal<PerfilDatos | null>(null)
  protected readonly guardando = signal(false)
  protected readonly aviso = signal<string | null>(null)
  protected readonly errorGeneral = signal<string | null>(null)
  private readonly errorDeServidor = signal<Partial<Record<Campo, string>>>({})
  private readonly valores = signal({
    nombres: '',
    apellidos: '',
    telefono: '',
    fechaNacimiento: '',
  })

  protected readonly form = this.fb.group({
    nombres: ['', [obligatorioConTexto, Validators.maxLength(NOMBRES_MAX)]],
    apellidos: ['', [obligatorioConTexto, Validators.maxLength(APELLIDOS_MAX)]],
    telefono: ['', [Validators.maxLength(TELEFONO_MAX)]],
    fechaNacimiento: ['', [Validators.required, fechaNoFutura(), edadMinima()]],
  })

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.valores.set(this.form.getRawValue())
      this.errorDeServidor.set({})
    })
  }

  ngOnInit(): void {
    void this.cargar()
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true)
    try {
      this.perfil.set(await firstValueFrom(this.api.obtener()))
      this.llenar(this.perfil()!)
    } catch {
      this.perfil.set(null)
    } finally {
      this.cargando.set(false)
    }
  }

  protected rolesLegibles(p: PerfilDatos): string {
    return p.roles.map((rol) => ROLES[rol] ?? rol).join(', ')
  }

  /** El perfil que devuelve una sección (foto, correo) reemplaza al actual sin tocar lo escrito. */
  protected reemplazar(nuevo: PerfilDatos): void {
    this.perfil.set(nuevo)
  }

  protected error(campo: Campo): string | null {
    const delServidor = this.errorDeServidor()[campo]
    if (delServidor) return delServidor
    const control = this.form.controls[campo]
    if (!control.touched && !control.dirty) return null
    return mensajeDeError(campo, control.errors)
  }

  protected hayCambios(): boolean {
    const p = this.perfil()
    return p !== null && Object.keys(this.cambios(p)).length > 0
  }

  protected async guardar(): Promise<void> {
    const p = this.perfil()
    this.aviso.set(null)
    this.errorGeneral.set(null)
    if (!p || this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }
    const cambios = this.cambios(p)
    if (Object.keys(cambios).length === 0) return

    this.guardando.set(true)
    try {
      const guardado = await firstValueFrom(this.api.editar(cambios))
      this.perfil.set(guardado)
      this.llenar(guardado)
      avisarPerfilActualizado(guardado)
      this.aviso.set('Datos guardados')
    } catch (error) {
      const mensaje = mensajeDeErrorPerfil(error)
      if (codigoDe(error) === 'ERR_IDE_02') {
        this.errorDeServidor.set({ fechaNacimiento: mensaje })
      } else {
        this.errorGeneral.set(mensaje)
      }
    } finally {
      this.guardando.set(false)
    }
  }

  /** Solo los campos que difieren del perfil cargado; un teléfono vaciado va como null. */
  private cambios(p: PerfilDatos): EditarPerfilRequest {
    const v = this.valores()
    const cambios: EditarPerfilRequest = {}
    if (v.nombres.trim() !== p.nombres) cambios.nombres = v.nombres.trim()
    if (v.apellidos.trim() !== p.apellidos) cambios.apellidos = v.apellidos.trim()
    if (v.telefono.trim() !== (p.telefono ?? '')) cambios.telefono = v.telefono.trim() || null
    if (v.fechaNacimiento !== p.fechaNacimiento) cambios.fechaNacimiento = v.fechaNacimiento
    return cambios
  }

  private llenar(p: PerfilDatos): void {
    this.form.reset({
      nombres: p.nombres,
      apellidos: p.apellidos,
      telefono: p.telefono ?? '',
      fechaNacimiento: p.fechaNacimiento,
    })
    this.valores.set(this.form.getRawValue())
  }
}
