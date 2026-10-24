import { LitElement, css, html, nothing } from 'lit'

import './dh-avatar.js'
import { base } from './comunes.js'
import { icono } from './iconos.js'

/**
 * <dh-header>: cabecera de la web con migas, acciones, avisos y el menú de la cuenta.
 *
 * Propiedades
 *   migas            Miga[]  { etiqueta, ruta? }  la última es la página actual
 *   usuario          { nombres, apellidos, fotoUrl?, rol? }  sin usuario no hay menú de cuenta
 *   accionesUsuario  AccionUsuario[]  { id, etiqueta, icono?, tono?: 'peligro', ruta? }
 *   avisos           muestra la campana (número = avisos sin leer; true = solo la campana)
 *   buscador         muestra el botón de búsqueda
 *   menuMovil        muestra el botón de menú (hamburguesa) en pantallas chicas
 *
 * Eventos (burbujean y atraviesan el Shadow DOM)
 *   dh-menu      pidió abrir el menú lateral (pantallas chicas)
 *   dh-navegar   { ruta }  eligió una miga o una acción del menú de la cuenta que tiene `ruta`
 *   dh-accion    { id }    eligió una acción del menú de la cuenta sin `ruta` (ej. cerrar sesión)
 *   dh-avisos / dh-buscar   pulsó la campana / el buscador
 *
 * Slot `acciones`: botones propios de quien lo usa (ej. "Verificar correo"), antes de la campana.
 * Partes (::part): header, migas, acciones, avisos, cuenta, menu.
 * Variable CSS: --dh-header-alto.
 */
export class DhHeader extends LitElement {
  static properties = {
    migas: { type: Array },
    usuario: { type: Object },
    accionesUsuario: { type: Array },
    avisos: {},
    buscador: { type: Boolean },
    menuMovil: { type: Boolean, attribute: 'menu-movil' },
    _menuAbierto: { state: true },
  }

  static styles = [
    base,
    css`
      :host {
        display: block;
        position: sticky;
        top: 0;
        z-index: 30;
      }
      header {
        display: flex;
        align-items: center;
        gap: 1rem;
        height: var(--dh-header-alto, 4rem);
        padding: 0 var(--dh-pad, clamp(1rem, 2.4vw, 2rem));
        background: color-mix(in srgb, var(--card) 88%, transparent);
        backdrop-filter: blur(14px) saturate(1.4);
        border-bottom: 1px solid var(--border);
      }
      .icono-btn {
        position: relative;
        display: inline-grid;
        place-items: center;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: var(--radius);
        transition: background-color 0.2s, transform 0.2s var(--ease, ease);
      }
      .icono-btn:hover {
        background: var(--accent);
      }
      .icono-btn:active {
        transform: scale(0.92);
      }
      .hamburguesa {
        display: none;
      }
      .migas {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        min-width: 0;
        font-size: 0.8125rem;
        color: var(--muted-foreground);
      }
      .migas a:hover {
        color: var(--foreground);
      }
      .migas [aria-current] {
        color: var(--foreground);
        font-weight: 650;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .derecha {
        margin-left: auto;
        display: flex;
        align-items: center;
        gap: 0.35rem;
      }
      .buscador {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        height: 2.5rem;
        min-width: min(18rem, 28vw);
        padding: 0 0.75rem;
        margin-right: 0.5rem;
        border-radius: var(--radius);
        background: var(--background);
        border: 1px solid var(--border);
        color: var(--muted-foreground);
        font-size: 0.8125rem;
        text-align: left;
      }
      .buscador span {
        flex: 1;
      }
      .punto::after {
        content: '';
        position: absolute;
        top: 0.55rem;
        right: 0.6rem;
        width: 0.55rem;
        height: 0.55rem;
        border-radius: 50%;
        background: var(--primary);
        box-shadow: 0 0 0 2px var(--card);
      }
      .cuenta {
        position: relative;
      }
      .avatar-btn {
        display: grid;
        border-radius: 50%;
        padding: 0.15rem;
        transition: box-shadow 0.2s;
      }
      .avatar-btn:hover {
        box-shadow: 0 0 0 3px color-mix(in srgb, var(--ambar-vial) 35%, transparent);
      }
      .menu {
        position: absolute;
        right: 0;
        top: calc(100% + 0.6rem);
        width: 16.5rem;
        padding: 0.5rem;
        z-index: 60;
        background: var(--popover);
        color: var(--popover-foreground);
        border: 1px solid var(--border);
        border-radius: calc(var(--radius) * 1.4);
        box-shadow: var(--shadow-2, 0 10px 30px -8px rgb(30 41 59 / 22%));
        transform-origin: top right;
        animation: pop 0.22s var(--ease, ease);
      }
      @keyframes pop {
        from {
          opacity: 0;
          transform: scale(0.94) translateY(-6px);
        }
      }
      .usuario {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.6rem;
        margin-bottom: 0.35rem;
        border-bottom: 1px solid var(--border);
      }
      .usuario b {
        display: block;
        font-size: 0.875rem;
      }
      .usuario small {
        color: var(--muted-foreground);
        font-size: 0.78rem;
      }
      .opcion {
        display: flex;
        align-items: center;
        gap: 0.7rem;
        width: 100%;
        padding: 0.6rem;
        border-radius: var(--radius);
        font-size: 0.875rem;
        font-weight: 500;
        text-align: left;
      }
      .opcion:hover {
        background: var(--accent);
      }
      .opcion.peligro {
        color: var(--destructive);
      }
      @media (max-width: 900px) {
        .hamburguesa {
          display: inline-grid;
        }
        .buscador {
          min-width: 0;
          width: 2.5rem;
          padding: 0;
          justify-content: center;
          margin-right: 0;
        }
        .buscador span {
          display: none;
        }
      }
      @media (max-width: 680px) {
        .migas a,
        .migas .ico {
          display: none;
        }
      }
    `,
  ]

  constructor() {
    super()
    this.migas = []
    this.usuario = null
    this.accionesUsuario = []
    this.avisos = false
    this.buscador = false
    this.menuMovil = true
    this._menuAbierto = false
    this._fuera = (e) => {
      if (this._menuAbierto && !e.composedPath().includes(this.renderRoot.querySelector('.cuenta'))) {
        this._menuAbierto = false
      }
    }
    this._tecla = (e) => {
      if (e.key === 'Escape' && this._menuAbierto) this._cerrarMenu(true)
    }
  }

  connectedCallback() {
    super.connectedCallback()
    document.addEventListener('click', this._fuera)
    document.addEventListener('keydown', this._tecla)
  }

  disconnectedCallback() {
    document.removeEventListener('click', this._fuera)
    document.removeEventListener('keydown', this._tecla)
    super.disconnectedCallback()
  }

  _emitir(nombre, detalle) {
    this.dispatchEvent(new CustomEvent(nombre, { detail: detalle, bubbles: true, composed: true }))
  }

  _cerrarMenu(devolverFoco) {
    this._menuAbierto = false
    if (devolverFoco) this.renderRoot.querySelector('.avatar-btn')?.focus()
  }

  _elegirAccion(accion) {
    this._cerrarMenu(false)
    if (accion.ruta) this._emitir('dh-navegar', { ruta: accion.ruta, accion })
    else this._emitir('dh-accion', { id: accion.id, accion })
  }

  _irMiga(evento, miga) {
    if (evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey) return
    evento.preventDefault()
    this._emitir('dh-navegar', { ruta: miga.ruta, miga })
  }

  render() {
    const migas = this.migas ?? []
    const avisos = this.avisos
    return html`
      <header part="header">
        ${this.menuMovil
          ? html`<button type="button" class="icono-btn hamburguesa" aria-label="Abrir menú" @click=${() => this._emitir('dh-menu')}>${icono('menu', 20)}</button>`
          : nothing}
        <nav class="migas" part="migas" aria-label="Ruta">
          ${migas.map((m, i) =>
            i === migas.length - 1 || !m.ruta
              ? html`<span aria-current=${i === migas.length - 1 ? 'page' : nothing}>${m.etiqueta}</span>${i < migas.length - 1 ? icono('chevron-right', 14) : nothing}`
              : html`<a href=${m.ruta} @click=${(e) => this._irMiga(e, m)}>${m.etiqueta}</a>${icono('chevron-right', 14)}`,
          )}
        </nav>
        <div class="derecha" part="acciones">
          ${this.buscador
            ? html`<button type="button" class="buscador" aria-label="Buscar" @click=${() => this._emitir('dh-buscar')}>${icono('search', 16)}<span>Buscar…</span></button>`
            : nothing}
          <slot name="acciones"></slot>
          ${avisos
            ? html`<button type="button" part="avisos" class="icono-btn ${Number(avisos) !== 0 ? 'punto' : ''}" aria-label="Avisos" @click=${() => this._emitir('dh-avisos')}>${icono('bell', 20)}</button>`
            : nothing}
          ${this.usuario ? this._renderCuenta() : nothing}
        </div>
      </header>
    `
  }

  _renderCuenta() {
    const u = this.usuario
    return html`<div class="cuenta" part="cuenta">
      <button type="button" class="avatar-btn" aria-haspopup="menu" aria-expanded=${this._menuAbierto ? 'true' : 'false'} aria-label="Menú de la cuenta" @click=${() => (this._menuAbierto = !this._menuAbierto)}>
        <dh-avatar nombres=${u.nombres} apellidos=${u.apellidos} foto-url=${u.fotoUrl ?? ''}></dh-avatar>
      </button>
      ${this._menuAbierto
        ? html`<div class="menu" part="menu" role="menu">
            <div class="usuario">
              <dh-avatar nombres=${u.nombres} apellidos=${u.apellidos} foto-url=${u.fotoUrl ?? ''} tam="44"></dh-avatar>
              <div><b>${u.nombres} ${u.apellidos}</b>${u.rol ? html`<small>${u.rol}</small>` : nothing}</div>
            </div>
            ${(this.accionesUsuario ?? []).map(
              (a) => html`<button type="button" role="menuitem" class="opcion ${a.tono === 'peligro' ? 'peligro' : ''}" @click=${() => this._elegirAccion(a)}>${icono(a.icono, 18)}${a.etiqueta}</button>`,
            )}
          </div>`
        : nothing}
    </div>`
  }
}
