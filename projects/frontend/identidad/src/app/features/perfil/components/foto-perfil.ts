import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  OnDestroy,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core'
import '@drivehub/ui'
import { firstValueFrom } from 'rxjs'

import { avisarPerfilActualizado } from '../api/perfil.eventos'
import { PerfilService } from '../api/perfil.service'
import { mensajeDeErrorPerfil } from '../schemas/perfil.errores'
import { validarFoto } from '../schemas/foto.reglas'
import type { Perfil } from '../types/perfil.types'

/**
 * Sección de la foto del perfil (CU06): subir, reemplazar y quitar.
 *
 * * Revisa el archivo antes de enviarlo solo para dar feedback inmediato (tipo, peso y
 * * dimensiones); el backend decide. Un archivo rechazado nunca se envía.
 * * Tras subir o quitar emite `perfil-actualizado` para el avatar del shell.
 */
@Component({
  selector: 'app-foto-perfil',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <div class="dh-foto-bloque">
      <div class="dh-foto">
        @if (imagenMostrada(); as url) {
          <img [src]="url" alt="Tu foto de perfil" (error)="urlRota.set(url)" />
        } @else {
          <span aria-hidden="true">{{ iniciales() }}</span>
        }
      </div>
      <h2>{{ perfil().nombres }} {{ perfil().apellidos }}</h2>
      <p class="dh-muted">{{ rol() }}</p>

      <div class="dh-foto-acciones">
        <input
          #selector
          type="file"
          accept="image/jpeg,image/png,image/webp"
          hidden
          data-testid="selector-foto"
          (change)="alElegir($event)"
        />
        @if (!seleccionada()) {
          <button
            type="button"
            class="dh-btn dh-btn-soft dh-btn-sm"
            [disabled]="ocupado()"
            (click)="selector.click()"
          >
            <dh-icono nombre="camera" [tam]="16" />
            {{ perfil().fotoUrl ? 'Cambiar foto' : 'Subir foto' }}
          </button>
          @if (perfil().fotoUrl && !confirmandoQuitar()) {
            <button
              type="button"
              class="dh-btn dh-btn-ghost dh-btn-sm"
              [disabled]="ocupado()"
              (click)="confirmandoQuitar.set(true)"
            >
              <dh-icono nombre="trash" [tam]="16" />
              Quitar
            </button>
          }
        } @else {
          <button type="button" class="dh-btn dh-btn-sm" [disabled]="ocupado()" (click)="guardar()">
            {{ ocupado() ? 'Subiendo…' : 'Guardar foto' }}
          </button>
          <button
            type="button"
            class="dh-btn dh-btn-ghost dh-btn-sm"
            [disabled]="ocupado()"
            (click)="cancelar()"
          >
            Cancelar
          </button>
        }
      </div>

      @if (error(); as mensaje) {
        <p class="dh-error" role="alert">{{ mensaje }}</p>
      }
      @if (aviso(); as mensaje) {
        <p class="dh-notice dh-notice-ok" role="status">{{ mensaje }}</p>
      }
      <p class="dh-hint">JPG, PNG o WebP · hasta 5 MB · máx. 4000×4000 px</p>
    </div>

    <dh-dialogo
      [abierto]="confirmandoQuitar()"
      titulo="¿Quitar tu foto?"
      texto="Tu cuenta volverá a mostrar tus iniciales."
      aceptar="Quitar"
      [peligro]="true"
      [ocupado]="ocupado()"
      (dh-aceptar)="quitar()"
      (dh-cancelar)="confirmandoQuitar.set(false)"
    />
  `,
})
export class FotoPerfil implements OnDestroy {
  private readonly api = inject(PerfilService)

  readonly perfil = input.required<Perfil>()
  /** Etiqueta legible del rol, que se muestra bajo el nombre. */
  readonly rol = input<string>('')
  readonly perfilCambiado = output<Perfil>()

  protected readonly seleccionada = signal<File | null>(null)
  protected readonly vistaPrevia = signal<string | null>(null)
  protected readonly urlRota = signal<string | null>(null)
  protected readonly ocupado = signal(false)
  protected readonly confirmandoQuitar = signal(false)
  protected readonly error = signal<string | null>(null)
  protected readonly aviso = signal<string | null>(null)

  protected readonly imagenMostrada = computed(() => {
    const url = this.vistaPrevia() ?? this.perfil().fotoUrl
    return url && url !== this.urlRota() ? url : null
  })
  protected readonly iniciales = computed(() => {
    const p = this.perfil()
    return `${p.nombres.trim().charAt(0)}${p.apellidos.trim().charAt(0)}`.toUpperCase()
  })

  ngOnDestroy(): void {
    this.liberarVistaPrevia()
  }

  protected async alElegir(evento: Event): Promise<void> {
    const entrada = evento.target as HTMLInputElement
    const archivo = entrada.files?.[0]
    entrada.value = ''
    this.error.set(null)
    this.aviso.set(null)
    if (!archivo) return

    const problema = await validarFoto(archivo)
    if (problema) {
      this.error.set(problema)
      return
    }
    this.liberarVistaPrevia()
    this.seleccionada.set(archivo)
    this.vistaPrevia.set(URL.createObjectURL(archivo))
  }

  protected cancelar(): void {
    this.liberarVistaPrevia()
    this.seleccionada.set(null)
    this.error.set(null)
  }

  protected async guardar(): Promise<void> {
    const archivo = this.seleccionada()
    if (!archivo) return
    this.ocupado.set(true)
    this.error.set(null)
    try {
      const nuevo = await firstValueFrom(this.api.subirFoto(archivo))
      this.cancelar()
      this.urlRota.set(null)
      this.aviso.set('Foto actualizada')
      this.perfilCambiado.emit(nuevo)
      avisarPerfilActualizado(nuevo)
    } catch (error) {
      // * La foto actual sigue como estaba; se conserva la elegida para reintentar o cancelar
      this.error.set(mensajeDeErrorPerfil(error))
    } finally {
      this.ocupado.set(false)
    }
  }

  protected async quitar(): Promise<void> {
    this.ocupado.set(true)
    this.error.set(null)
    try {
      const nuevo = await firstValueFrom(this.api.quitarFoto())
      this.confirmandoQuitar.set(false)
      this.aviso.set('Foto eliminada')
      this.perfilCambiado.emit(nuevo)
      avisarPerfilActualizado(nuevo)
    } catch (error) {
      this.error.set(mensajeDeErrorPerfil(error))
    } finally {
      this.ocupado.set(false)
    }
  }

  private liberarVistaPrevia(): void {
    const url = this.vistaPrevia()
    if (url) URL.revokeObjectURL(url)
    this.vistaPrevia.set(null)
  }
}
