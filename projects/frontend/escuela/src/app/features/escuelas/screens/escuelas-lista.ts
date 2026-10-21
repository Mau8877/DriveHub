import { DatePipe } from '@angular/common'
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { RouterLink } from '@angular/router'
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import type { EscuelaListada, EstadoEscuela, MetaPagina } from '../types/escuela.types'

const ESPERA_BUSQUEDA_MS = 300
const POR_PAGINA = 20

/**
 * Gestionar escuelas (CU05, Super admin): listado con búsqueda, filtro de estado, paginación y
 * las acciones editar, desactivar y reactivar.
 *
 * * Desactivar se confirma en la misma fila; reactivar no, porque es seguro y reversible.
 * * Las pantallas de crear y editar avisan por el estado del Router ("aviso"); acá se muestra
 * * una sola vez.
 */
@Component({
  selector: 'app-escuelas-lista',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="p-6">
      <div class="mb-4 flex items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-semibold">Gestionar escuelas</h1>
          <p class="text-muted-foreground text-sm">
            Escuelas activas e inactivas de la plataforma.
          </p>
        </div>
        <a
          routerLink="nueva"
          class="bg-primary text-primary-foreground rounded-md px-4 py-2 text-sm font-medium"
        >
          Nueva escuela
        </a>
      </div>

      @if (aviso(); as mensaje) {
        <p
          class="border-border mb-4 rounded-md border px-3 py-2 text-sm"
          [class.text-destructive]="mensaje.tipo === 'error'"
          [attr.role]="mensaje.tipo === 'error' ? 'alert' : 'status'"
        >
          {{ mensaje.texto }}
        </p>
      }

      <div class="mb-4 flex flex-wrap gap-3">
        <input
          type="search"
          aria-label="Buscar escuelas"
          placeholder="Buscar por nombre o slug"
          [value]="busqueda()"
          (input)="buscar($any($event.target).value)"
          class="border-border bg-background min-w-64 rounded-md border px-3 py-2 text-sm"
        />
        <select
          aria-label="Filtrar por estado"
          [value]="estado()"
          (change)="filtrarPorEstado($any($event.target).value)"
          class="border-border bg-background rounded-md border px-3 py-2 text-sm"
        >
          <option value="">Todas</option>
          <option value="ACTIVA">Activas</option>
          <option value="INACTIVA">Inactivas</option>
        </select>
      </div>

      @if (cargando()) {
        <p class="text-muted-foreground text-sm">Cargando escuelas…</p>
      } @else if (error(); as mensaje) {
        <p class="text-destructive text-sm" role="alert">{{ mensaje }}</p>
        <button type="button" class="mt-2 text-sm underline" (click)="cargar()">Reintentar</button>
      } @else if (escuelas().length === 0) {
        <p class="text-muted-foreground text-sm">
          {{ hayFiltros() ? 'Sin resultados.' : 'Todavía no hay escuelas.' }}
        </p>
      } @else {
        <table class="w-full text-left text-sm">
          <thead class="text-muted-foreground border-border border-b">
            <tr>
              <th class="py-2 pr-4 font-medium">Nombre</th>
              <th class="py-2 pr-4 font-medium">Slug</th>
              <th class="py-2 pr-4 font-medium">Estado</th>
              <th class="py-2 pr-4 font-medium">Alta</th>
              <th class="py-2 font-medium">Acciones</th>
            </tr>
          </thead>
          <tbody>
            @for (escuela of escuelas(); track escuela.codigo) {
              <tr class="border-border border-b">
                <td class="py-2 pr-4">{{ escuela.nombre }}</td>
                <td class="text-muted-foreground py-2 pr-4">{{ escuela.slug }}</td>
                <td class="py-2 pr-4">
                  <span
                    class="rounded-full px-2 py-0.5 text-xs"
                    [class.bg-secondary]="escuela.estado === 'ACTIVA'"
                    [class.bg-muted]="escuela.estado === 'INACTIVA'"
                    [class.text-muted-foreground]="escuela.estado === 'INACTIVA'"
                  >
                    {{ escuela.estado === 'ACTIVA' ? 'Activa' : 'Inactiva' }}
                  </span>
                </td>
                <td class="py-2 pr-4">{{ escuela.creadoEn | date: 'dd/MM/yyyy' }}</td>
                <td class="py-2">
                  @if (confirmando() === escuela.codigo) {
                    <span class="mr-2 text-sm">¿Desactivar?</span>
                    <button
                      type="button"
                      class="text-destructive mr-2 text-sm underline"
                      [disabled]="trabajando() === escuela.codigo"
                      (click)="desactivar(escuela)"
                    >
                      Confirmar
                    </button>
                    <button type="button" class="text-sm underline" (click)="cancelar()">
                      Cancelar
                    </button>
                  } @else if (escuela.estado === 'ACTIVA') {
                    <a [routerLink]="[escuela.codigo, 'editar']" class="mr-3 text-sm underline">
                      Editar
                    </a>
                    <button
                      type="button"
                      class="text-sm underline"
                      (click)="pedirConfirmacion(escuela.codigo)"
                    >
                      Desactivar
                    </button>
                  } @else {
                    <button
                      type="button"
                      class="text-sm underline"
                      [disabled]="trabajando() === escuela.codigo"
                      (click)="reactivar(escuela)"
                    >
                      Reactivar
                    </button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>

        @if (meta(); as paginacion) {
          <div class="mt-4 flex items-center justify-between text-sm">
            <span class="text-muted-foreground">
              Página {{ paginacion.page }} de {{ paginacion.totalPages }} ·
              {{ paginacion.totalItems }} escuelas
            </span>
            <span class="flex gap-2">
              <button
                type="button"
                class="border-border rounded-md border px-3 py-1 disabled:opacity-50"
                [disabled]="paginacion.page <= 1"
                (click)="irAPagina(paginacion.page - 1)"
              >
                Anterior
              </button>
              <button
                type="button"
                class="border-border rounded-md border px-3 py-1 disabled:opacity-50"
                [disabled]="paginacion.page >= paginacion.totalPages"
                (click)="irAPagina(paginacion.page + 1)"
              >
                Siguiente
              </button>
            </span>
          </div>
        }
      }
    </section>
  `,
})
export class EscuelasLista implements OnInit {
  private readonly servicio = inject(EscuelasService)
  private readonly destroyRef = inject(DestroyRef)
  private readonly escritura$ = new Subject<string>()

  readonly busqueda = signal('')
  readonly estado = signal<EstadoEscuela | ''>('')
  readonly pagina = signal(1)
  readonly escuelas = signal<EscuelaListada[]>([])
  readonly meta = signal<MetaPagina | null>(null)
  readonly cargando = signal(true)
  readonly error = signal<string | null>(null)
  readonly aviso = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null)
  /** Código de la escuela que está pidiendo confirmación para desactivarse. */
  readonly confirmando = signal<string | null>(null)
  /** Código de la escuela con una acción en curso. */
  readonly trabajando = signal<string | null>(null)

  constructor() {
    // * Espera a que la persona termine de escribir antes de consultar
    this.escritura$
      .pipe(
        debounceTime(ESPERA_BUSQUEDA_MS),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((texto) => {
        this.busqueda.set(texto)
        this.pagina.set(1)
        this.cargar()
      })
  }

  ngOnInit(): void {
    this.mostrarAvisoRecibido()
    this.cargar()
  }

  hayFiltros(): boolean {
    return this.busqueda().trim() !== '' || this.estado() !== ''
  }

  buscar(texto: string): void {
    this.escritura$.next(texto)
  }

  filtrarPorEstado(valor: string): void {
    this.estado.set(valor as EstadoEscuela | '')
    this.pagina.set(1)
    this.cargar()
  }

  irAPagina(pagina: number): void {
    this.pagina.set(pagina)
    this.cargar()
  }

  cargar(): void {
    this.cargando.set(true)
    this.error.set(null)
    this.servicio
      .listar({
        page: this.pagina(),
        pageSize: POR_PAGINA,
        search: this.busqueda(),
        estado: this.estado(),
      })
      .subscribe({
        next: (resultado) => {
          this.escuelas.set(resultado.data)
          this.meta.set(resultado.meta)
          this.cargando.set(false)
        },
        error: (error: unknown) => {
          this.error.set(this.mensajeDe(error, 'No pudimos cargar las escuelas.'))
          this.cargando.set(false)
        },
      })
  }

  pedirConfirmacion(codigo: string): void {
    this.confirmando.set(codigo)
  }

  cancelar(): void {
    this.confirmando.set(null)
  }

  desactivar(escuela: EscuelaListada): void {
    this.trabajando.set(escuela.codigo)
    this.servicio.desactivar(escuela.codigo).subscribe({
      next: () => this.terminarAccion('Escuela desactivada correctamente.'),
      error: (error: unknown) => this.fallarAccion(error, 'No pudimos desactivar la escuela.'),
    })
  }

  reactivar(escuela: EscuelaListada): void {
    this.trabajando.set(escuela.codigo)
    this.servicio.reactivar(escuela.codigo).subscribe({
      next: () => this.terminarAccion('Escuela reactivada correctamente.'),
      error: (error: unknown) => this.fallarAccion(error, 'No pudimos reactivar la escuela.'),
    })
  }

  private terminarAccion(texto: string): void {
    this.trabajando.set(null)
    this.confirmando.set(null)
    this.aviso.set({ tipo: 'ok', texto })
    this.cargar()
  }

  private fallarAccion(error: unknown, porDefecto: string): void {
    this.trabajando.set(null)
    this.confirmando.set(null)
    this.aviso.set({ tipo: 'error', texto: this.mensajeDe(error, porDefecto) })
    this.cargar()
  }

  private mensajeDe(error: unknown, porDefecto: string): string {
    return error instanceof ApiError ? error.message : porDefecto
  }

  /** Las pantallas de crear y editar dejan el aviso en el estado del Router. Se lee una sola vez. */
  private mostrarAvisoRecibido(): void {
    const texto = (history.state as { aviso?: string } | null)?.aviso
    if (!texto) return
    this.aviso.set({ tipo: 'ok', texto })
    history.replaceState({ ...history.state, aviso: undefined }, '')
  }
}
