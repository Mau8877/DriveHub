import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, inject, signal } from '@angular/core'
import '@drivehub/ui'

import { ApiError } from '../../../core/api/api.types'
import {
  type Aviso,
  AvisosService,
  type TipoAviso,
} from '../../../core/notificaciones/avisos.service'
import { ROLES } from '../../../core/sesion/roles'
import { SesionService } from '../../../core/sesion/sesion.service'

const ICONO_TIPO: Record<TipoAviso, string> = {
  RESERVA: 'calendar',
  PAGO: 'wallet',
  EXAMEN: 'clipboard',
  RECORDATORIO: 'clock',
}

const FECHA = new Intl.DateTimeFormat('es-BO', { dateStyle: 'medium', timeStyle: 'short' })

/**
 * Bandeja de avisos del panel (CU53): los avisos de la escuela para el Administrador.
 *
 * * Abrir un aviso lo marca como leído (y baja la campana); verlo en la lista no.
 * * Un aviso que ya no existe (404 `ERR_NOT_04`) se quita de la lista. Si marcarlo falla, el mensaje se muestra
 * * igual y se vuelve a intentar la próxima vez que se abra.
 */
@Component({
  selector: 'app-avisos',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <section class="dh-page">
      <div class="dh-page-head">
        <div>
          <h1>Avisos</h1>
          <p>{{ descripcion() }}</p>
        </div>
        <button
          type="button"
          class="dh-btn dh-btn-soft"
          [disabled]="avisosService.noLeidas() === 0 || marcandoTodas()"
          (click)="leerTodas()"
        >
          <dh-icono nombre="check" [tam]="16" />
          Marcar todos como leídos
        </button>
      </div>

      <div class="flex gap-2" role="group" aria-label="Filtrar avisos">
        <button
          type="button"
          class="dh-btn dh-btn-sm"
          [class.dh-btn-soft]="!soloNoLeidas()"
          [class.dh-btn-ghost]="soloNoLeidas()"
          [attr.aria-pressed]="!soloNoLeidas()"
          (click)="filtrar(false)"
        >
          Todos
        </button>
        <button
          type="button"
          class="dh-btn dh-btn-sm"
          [class.dh-btn-soft]="soloNoLeidas()"
          [class.dh-btn-ghost]="!soloNoLeidas()"
          [attr.aria-pressed]="soloNoLeidas()"
          (click)="filtrar(true)"
        >
          No leídos
          @if (avisosService.noLeidas() > 0) {
            ({{ avisosService.noLeidas() }})
          }
        </button>
      </div>

      @if (aviso(); as mensaje) {
        <p
          class="dh-notice"
          [class.dh-notice-error]="mensaje.error"
          [class.dh-notice-info]="!mensaje.error"
          role="status"
        >
          {{ mensaje.texto }}
        </p>
      }

      @if (error() && avisos().length === 0) {
        <div class="dh-card dh-state p-8" role="alert">
          <p>No pudimos cargar los avisos.</p>
          <button type="button" class="dh-btn dh-btn-soft" (click)="cargar(1)">
            <dh-icono nombre="refresh" [tam]="16" />
            Reintentar
          </button>
        </div>
      } @else if (cargando() && avisos().length === 0) {
        <div class="dh-card dh-state p-8" role="status">
          <p class="dh-muted">Cargando avisos…</p>
        </div>
      } @else if (avisos().length === 0) {
        <div class="dh-card dh-state p-8">
          <dh-icono nombre="bell" [tam]="28" />
          <p>{{ soloNoLeidas() ? 'No tienes avisos sin leer.' : 'Todavía no hay avisos.' }}</p>
        </div>
      } @else {
        <div class="dh-card">
          @for (item of avisos(); track item.codigoNotificacion) {
            <details
              class="dh-acordeon border-border border-b last:border-b-0"
              (toggle)="abrir($event, item)"
            >
              <summary>
                <span class="dh-acordeon-ico"><dh-icono [nombre]="icono(item)" [tam]="18" /></span>
                <span class="dh-acordeon-texto">
                  <span [class.font-semibold]="!item.leida">{{ item.titulo }}</span>
                  <small>{{ fecha(item) }}</small>
                </span>
                @if (!item.leida) {
                  <span class="dh-badge dh-badge-ok">Nuevo</span>
                }
                <dh-icono nombre="chevron-down" [tam]="18" />
              </summary>
              <div class="dh-acordeon-cuerpo">
                <p class="dh-texto-sm">{{ item.mensaje }}</p>
              </div>
            </details>
          }
        </div>
        @if (error()) {
          <p class="dh-notice dh-notice-error" role="alert">
            No pudimos cargar más avisos.
            <button type="button" class="dh-link" (click)="cargar(pagina() + 1)">Reintentar</button>
          </p>
        } @else if (hayMas()) {
          <button
            type="button"
            class="dh-btn dh-btn-ghost self-center"
            [disabled]="cargando()"
            (click)="cargar(pagina() + 1)"
          >
            {{ cargando() ? 'Cargando…' : 'Ver más avisos' }}
          </button>
        }
      }
    </section>
  `,
})
export class Avisos {
  protected readonly avisosService = inject(AvisosService)
  private readonly sesion = inject(SesionService)

  protected readonly avisos = signal<Aviso[]>([])
  protected readonly pagina = signal(0)
  protected readonly totalPaginas = signal(1)
  protected readonly soloNoLeidas = signal(false)
  protected readonly cargando = signal(false)
  protected readonly error = signal(false)
  protected readonly marcandoTodas = signal(false)
  protected readonly aviso = signal<{ texto: string; error: boolean } | null>(null)

  protected readonly hayMas = computed(() => this.pagina() < this.totalPaginas())
  protected readonly descripcion = computed(() =>
    this.sesion.roles().includes(ROLES.ADMIN_ESCUELA)
      ? 'Compras de horas, reembolsos y clases canceladas de tu escuela.'
      : 'Los avisos que te envió la plataforma.'
  )

  constructor() {
    void this.cargar(1)
    void this.avisosService.refrescarConteo()
  }

  /** Carga una página: la 1 reemplaza la lista, las siguientes se agregan. Lo ya cargado no se pierde si falla. */
  async cargar(page: number): Promise<void> {
    this.cargando.set(true)
    this.error.set(false)
    try {
      const { items, meta } = await this.avisosService.listar(page, this.soloNoLeidas())
      this.avisos.update((actuales) => (page === 1 ? items : [...actuales, ...items]))
      this.pagina.set(meta.page)
      this.totalPaginas.set(meta.totalPages)
    } catch {
      this.error.set(true)
    } finally {
      this.cargando.set(false)
    }
  }

  protected filtrar(soloNoLeidas: boolean): void {
    if (this.soloNoLeidas() === soloNoLeidas) return
    this.soloNoLeidas.set(soloNoLeidas)
    this.aviso.set(null)
    void this.cargar(1)
  }

  /** Al desplegar un aviso sin leer, se marca como leído. */
  async abrir(evento: Event, item: Aviso): Promise<void> {
    if (!(evento.target as HTMLDetailsElement).open || item.leida) return
    try {
      await this.avisosService.leer(item)
      this.avisos.update((lista) =>
        lista.map((a) =>
          a.codigoNotificacion === item.codigoNotificacion ? { ...a, leida: true } : a
        )
      )
    } catch (e) {
      if (e instanceof ApiError && e.code === 'ERR_NOT_04') {
        this.avisos.update((lista) =>
          lista.filter((a) => a.codigoNotificacion !== item.codigoNotificacion)
        )
        this.aviso.set({ texto: 'Ese aviso ya no existe.', error: true })
        return
      }
      this.aviso.set({
        texto: 'No pudimos marcar el aviso como leído. Lo intentaremos de nuevo.',
        error: true,
      })
    }
  }

  protected async leerTodas(): Promise<void> {
    this.marcandoTodas.set(true)
    this.aviso.set(null)
    try {
      await this.avisosService.leerTodas()
      this.aviso.set({ texto: 'Marcamos todos los avisos como leídos.', error: false })
      await this.cargar(1)
    } catch {
      this.aviso.set({ texto: 'No pudimos marcar los avisos. Intenta de nuevo.', error: true })
    } finally {
      this.marcandoTodas.set(false)
    }
  }

  protected icono(item: Aviso): string {
    return ICONO_TIPO[item.tipo] ?? 'bell'
  }

  protected fecha(item: Aviso): string {
    return FECHA.format(new Date(item.creadaEn))
  }
}
