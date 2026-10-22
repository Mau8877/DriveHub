import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core'
import { RouterLink } from '@angular/router'

import { SesionService } from '../core/sesion/sesion.service'

/** Iniciales (hasta dos) de nombres y apellidos, en mayúscula. */
export function iniciales(nombres: string, apellidos: string): string {
  const inicial = (texto: string) => texto.trim().charAt(0).toUpperCase()
  return `${inicial(nombres)}${inicial(apellidos)}`
}

/**
 * Avatar de la cabecera con el menú de la cuenta (CU06): foto o iniciales y la opción
 * "Editar perfil". Sin sesión no se muestra.
 *
 * * Si la foto no carga (por ejemplo la URL prefirmada venció), cae a las iniciales.
 * * El menú se cierra al elegir una opción, con Escape, al hacer clic fuera y al perder el foco;
 * * al cerrarlo con el teclado el foco vuelve al botón.
 */
@Component({
  selector: 'app-avatar-menu',
  imports: [RouterLink],
  host: {
    '(document:click)': 'alHacerClic($event)',
    '(focusout)': 'alPerderFoco($event)',
    '(keydown.escape)': 'cerrar(true)',
  },
  template: `
    @if (usuario(); as u) {
      <div class="relative">
        <button
          #boton
          type="button"
          class="bg-primary text-primary-foreground flex size-9 items-center justify-center overflow-hidden rounded-full text-sm font-semibold"
          aria-haspopup="menu"
          [attr.aria-expanded]="abierto()"
          aria-label="Menú de la cuenta"
          (click)="alternar()"
        >
          @if (u.fotoUrl && u.fotoUrl !== urlRota()) {
            <img
              [src]="u.fotoUrl"
              alt=""
              class="size-full object-cover"
              (error)="urlRota.set(u.fotoUrl)"
            />
          } @else {
            <span aria-hidden="true">{{ iniciales() }}</span>
          }
        </button>

        @if (abierto()) {
          <div
            role="menu"
            class="bg-popover text-popover-foreground border-border absolute right-0 z-10 mt-2 w-56 rounded-md border p-1 shadow-md"
          >
            <p class="border-border truncate border-b px-3 py-2 text-sm font-medium">
              {{ u.nombres }} {{ u.apellidos }}
            </p>
            <a
              role="menuitem"
              routerLink="/perfil"
              class="hover:bg-accent block rounded-md px-3 py-2 text-sm"
              (click)="cerrar(false)"
              >Editar perfil</a
            >
          </div>
        }
      </div>
    }
  `,
})
export class AvatarMenu {
  private readonly sesion = inject(SesionService)
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef)
  private readonly boton = viewChild<ElementRef<HTMLButtonElement>>('boton')

  protected readonly usuario = this.sesion.usuario
  protected readonly abierto = signal(false)
  // * La URL que falló al cargar; si la foto cambia se vuelve a intentar mostrarla
  protected readonly urlRota = signal<string | null>(null)
  protected readonly iniciales = computed(() => {
    const u = this.usuario()
    return u ? iniciales(u.nombres, u.apellidos) : ''
  })

  protected alternar(): void {
    this.abierto.update((v) => !v)
  }

  protected cerrar(devolverFoco: boolean): void {
    if (!this.abierto()) return
    this.abierto.set(false)
    if (devolverFoco) this.boton()?.nativeElement.focus()
  }

  protected alHacerClic(evento: Event): void {
    if (!this.anfitrion.nativeElement.contains(evento.target as Node)) this.cerrar(false)
  }

  protected alPerderFoco(evento: FocusEvent): void {
    const siguiente = evento.relatedTarget as Node | null
    if (siguiente && !this.anfitrion.nativeElement.contains(siguiente)) this.cerrar(false)
  }
}
