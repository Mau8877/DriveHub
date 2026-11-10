import { CUSTOM_ELEMENTS_SCHEMA, Component, computed, effect, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router, RouterOutlet } from '@angular/router'
import type { AccionUsuario, DetalleAccion, DetalleNavegar, UsuarioShell } from '@drivehub/ui'
import '@drivehub/ui'
import { filter, map } from 'rxjs'

import { AvisosService } from '../core/notificaciones/avisos.service'
import { PushWebService } from '../core/notificaciones/push-web.service'
import { ROLES } from '../core/sesion/roles'
import { SesionService } from '../core/sesion/sesion.service'
import { migasDeRuta, navItemsDeRoles } from './nav-items'

const ROLES_CON_BANDEJA: readonly string[] = [
  ROLES.ADMIN_ESCUELA,
  ROLES.INSTRUCTOR,
  ROLES.ESTUDIANTE,
]

const ROLES_LEGIBLES: Record<string, string> = {
  [ROLES.SUPER_ADMIN]: 'Super admin',
  [ROLES.ADMIN_ESCUELA]: 'Administrador de escuela',
  [ROLES.INSTRUCTOR]: 'Instructor',
  [ROLES.ESTUDIANTE]: 'Estudiante',
}

/** Opciones del menú de la cuenta: "Editar perfil" navega y "Cerrar sesión" emite `dh-accion`. */
const ACCIONES_USUARIO: AccionUsuario[] = [
  { id: 'perfil', etiqueta: 'Editar perfil', icono: 'user', ruta: '/perfil' },
  { id: 'cerrar-sesion', etiqueta: 'Cerrar sesión', icono: 'logout', tono: 'peligro' },
]

/**
 * Layout de toda la web: el menú, la cabecera y el área donde se monta cada micro-frontend.
 *
 * * El aspecto vive en @drivehub/ui (`<dh-shell>`): acá solo se decide QUÉ mostrar (el menú del rol,
 * * las migas, el usuario) y se traducen sus eventos a la navegación del router y a la sesión.
 * * Lo propio del shell entra por los slots: "Verificar correo" en la cabecera y la banda de aviso.
 * * La campana muestra los avisos sin leer de la bandeja (CU53) y lleva a /avisos. El Super admin no tiene
 * * bandeja: no la ve. El push del navegador se registra acá (CU36).
 */
@Component({
  selector: 'app-shell-layout',
  imports: [RouterOutlet],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <dh-shell
      [items]="items()"
      [activo]="ruta()"
      [migas]="migas()"
      [etiquetaGrupo]="grupo()"
      [usuario]="usuarioShell()"
      [accionesUsuario]="acciones"
      [avisos]="campana()"
      (dh-navegar)="navegar($event)"
      (dh-avisos)="abrirAvisos()"
      (dh-accion)="accion($event)"
    >
      @if (mostrarVerificarCorreo()) {
        <button
          slot="header-acciones"
          type="button"
          class="dh-btn dh-btn-soft dh-btn-sm"
          [disabled]="verificando()"
          (click)="verificarCorreo()"
        >
          <dh-icono nombre="mail" [tam]="16" />
          Verificar correo
        </button>
      }
      @if (aviso(); as mensaje) {
        <p
          slot="aviso"
          class="border-border border-b px-6 py-2 text-sm"
          [class.text-destructive]="mensaje.tipo === 'error'"
          [attr.role]="mensaje.tipo === 'error' ? 'alert' : 'status'"
        >
          {{ mensaje.texto }}
        </p>
      }
      <router-outlet />
    </dh-shell>
  `,
})
export class ShellLayout {
  private readonly sesion = inject(SesionService)
  private readonly router = inject(Router)
  private readonly pushWeb = inject(PushWebService)
  private readonly avisos = inject(AvisosService)

  protected readonly acciones = ACCIONES_USUARIO
  protected readonly verificando = signal(false)
  protected readonly aviso = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null)

  protected readonly ruta = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  )

  // * Cada rol ve solo su menú: el Super admin y el Administrador de escuela no se mezclan (CU05)
  protected readonly items = computed(() => navItemsDeRoles(this.sesion.usuario()?.roles ?? []))
  protected readonly migas = computed(() => migasDeRuta(this.ruta(), this.items()))
  protected readonly grupo = computed(() =>
    this.sesion.usuario()?.roles.includes(ROLES.SUPER_ADMIN) ? 'Plataforma' : 'Tu escuela'
  )
  protected readonly usuarioShell = computed<UsuarioShell | null>(() => {
    const u = this.sesion.usuario()
    if (!u) return null
    const rol = u.roles.map((r) => ROLES_LEGIBLES[r] ?? r)[0]
    return { nombres: u.nombres, apellidos: u.apellidos, fotoUrl: u.fotoUrl, rol }
  })
  // * Tienen bandeja el Administrador de escuela (la de su escuela), el Instructor y el Estudiante (la suya)
  protected readonly tieneBandeja = computed(() =>
    (this.sesion.usuario()?.roles ?? []).some((r) => ROLES_CON_BANDEJA.includes(r))
  )
  protected readonly campana = computed(() =>
    this.tieneBandeja() ? this.avisos.noLeidas() : false
  )
  // * El botón solo se muestra mientras la sesión diga que el correo no está verificado (CU04b)
  protected readonly mostrarVerificarCorreo = computed(
    () => this.sesion.usuario()?.correoVerificado === false
  )

  constructor() {
    // * Con sesión, el navegador se registra para recibir los avisos (CU36). No espera el resultado
    // * y es idempotente: se vuelve a llamar cuando cambia el perfil sin registrar dos veces.
    effect(() => {
      if (this.sesion.usuario()) void this.pushWeb.activar()
    })
    // * El contador de la campana se refresca al entrar y cada vez que se navega por el panel (CU53)
    effect(() => {
      this.ruta()
      if (this.tieneBandeja()) void this.avisos.refrescarConteo()
    })
  }

  protected abrirAvisos(): void {
    void this.router.navigateByUrl('/avisos')
  }

  protected navegar(evento: Event): void {
    const { ruta } = (evento as CustomEvent<DetalleNavegar>).detail
    void this.router.navigateByUrl(ruta)
  }

  protected async accion(evento: Event): Promise<void> {
    const { id } = (evento as CustomEvent<DetalleAccion>).detail
    if (id === 'cerrar-sesion') await this.sesion.cerrarSesion()
  }

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
}
