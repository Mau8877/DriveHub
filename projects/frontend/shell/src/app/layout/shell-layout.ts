import { Component, computed, inject, signal } from '@angular/core'
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'

import { SesionService } from '../core/sesion/sesion.service'
import { NAV_ITEMS } from './nav-items'

/**
 * Layout de toda la web: menú lateral, cabecera (con la campana de avisos)
 * y el área donde se monta cada micro-frontend.
 *
 * TODO: la campana de avisos se conecta con Notificaciones cuando exista
 * TODO: el caso de uso (hoy es solo el lugar reservado en la cabecera).
 */
@Component({
  selector: 'app-shell-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="flex min-h-svh">
      <aside
        class="bg-sidebar text-sidebar-foreground border-sidebar-border w-64 shrink-0 border-r p-4"
      >
        <a routerLink="/" class="mb-6 block text-lg font-semibold">DriveHub</a>
        <nav class="flex flex-col gap-1">
          @for (item of navItems; track item.ruta) {
            <a
              [routerLink]="item.ruta"
              routerLinkActive="bg-sidebar-accent text-sidebar-accent-foreground"
              class="hover:bg-sidebar-accent rounded-md px-3 py-2"
            >
              <span class="block text-sm font-medium">{{ item.etiqueta }}</span>
              <span class="text-muted-foreground block text-xs">{{ item.descripcion }}</span>
            </a>
          }
        </nav>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="border-border flex h-14 items-center justify-end gap-3 border-b px-6">
          <button
            type="button"
            class="hover:bg-accent rounded-md px-3 py-1 text-sm"
            aria-label="Avisos"
          >
            Avisos
          </button>
          @if (mostrarVerificarCorreo()) {
            <button
              type="button"
              class="hover:bg-accent rounded-md px-3 py-1 text-sm disabled:opacity-50"
              [disabled]="verificando()"
              (click)="verificarCorreo()"
            >
              Verificar correo
            </button>
          }
          <button
            type="button"
            class="hover:bg-accent rounded-md px-3 py-1 text-sm disabled:opacity-50"
            [disabled]="cerrando()"
            (click)="cerrarSesion()"
          >
            Cerrar sesión
          </button>
        </header>
        @if (aviso(); as mensaje) {
          <p
            class="border-border border-b px-6 py-2 text-sm"
            [class.text-destructive]="mensaje.tipo === 'error'"
            [attr.role]="mensaje.tipo === 'error' ? 'alert' : 'status'"
          >
            {{ mensaje.texto }}
          </p>
        }
        <main class="flex-1">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class ShellLayout {
  private readonly sesion = inject(SesionService)

  protected readonly navItems = NAV_ITEMS
  protected readonly cerrando = signal(false)
  protected readonly verificando = signal(false)
  protected readonly aviso = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null)
  // * El botón solo se muestra mientras la sesión diga que el correo no está verificado (CU04b)
  protected readonly mostrarVerificarCorreo = computed(
    () => this.sesion.usuario()?.correoVerificado === false
  )

  protected async verificarCorreo(): Promise<void> {
    this.verificando.set(true)
    this.aviso.set(null)
    try {
      await this.sesion.verificarCorreo()
      // * Con ERR_IDE_10 la sesión se renueva y ya figura verificada: no se mandó nada
      const yaVerificado = this.sesion.usuario()?.correoVerificado === true
      this.aviso.set({
        tipo: 'ok',
        texto: yaVerificado
          ? 'Tu correo ya estaba verificado.'
          : 'Te enviamos un correo para verificar tu dirección. Revisa tu bandeja.',
      })
    } catch {
      this.aviso.set({
        tipo: 'error',
        texto: 'No pudimos enviar el correo. Intenta más tarde.',
      })
    } finally {
      this.verificando.set(false)
    }
  }

  protected async cerrarSesion(): Promise<void> {
    this.cerrando.set(true)
    try {
      await this.sesion.cerrarSesion()
    } finally {
      this.cerrando.set(false)
    }
  }
}
