import { LitElement, css, html } from 'lit'

import './dh-header.js'
import './dh-sidebar.js'
import { base } from './comunes.js'

const CLAVE_COLAPSADO = 'dh-menu-colapsado'

/**
 * <dh-shell>: el armazón completo de la web (menú lateral + cabecera + contenido). Es lo que usa
 * el shell de Angular y también cualquier micro-frontend que corra suelto: todos se ven igual.
 *
 * Es una composición de <dh-sidebar> y <dh-header>: recibe las mismas propiedades (ver cada uno) y
 * deja pasar sus eventos (`dh-navegar`, `dh-accion`, `dh-avisos`, `dh-buscar`, `dh-colapsar`).
 *
 * Propiedades: items, activo, marca, etiquetaGrupo, migas, usuario, accionesUsuario, avisos,
 *              buscador, colapsable (por defecto sí) y `recordar-colapso` (guarda el estado del menú
 *              en localStorage; por defecto sí).
 *
 * Slots
 *   (por defecto)     el contenido de la pantalla
 *   header-acciones   botones propios en la cabecera (ej. "Verificar correo")
 *   sidebar-pie       bloque propio al pie del menú
 *   aviso             banda de aviso entre la cabecera y el contenido
 *
 * Partes (::part): sidebar, header, contenido. Variables CSS: --dh-sidebar-ancho,
 * --dh-sidebar-ancho-colapsado, --dh-header-alto, --dh-pad, --dh-contenido-max.
 */
export class DhShell extends LitElement {
  static properties = {
    items: { type: Array },
    activo: { type: String },
    marca: { type: Object },
    etiquetaGrupo: { type: String, attribute: 'etiqueta-grupo' },
    migas: { type: Array },
    usuario: { type: Object },
    accionesUsuario: { type: Array },
    avisos: {},
    buscador: { type: Boolean },
    colapsable: { type: Boolean },
    recordarColapso: { type: Boolean, attribute: 'recordar-colapso' },
    _colapsado: { state: true },
    _menuAbierto: { state: true },
  }

  static styles = [
    base,
    css`
      :host {
        display: block;
        min-height: 100svh;
        background: var(--background);
      }
      .app {
        display: flex;
        min-height: 100svh;
      }
      dh-sidebar {
        flex: none;
        z-index: 40;
      }
      .principal {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
      }
      main {
        flex: 1;
        width: 100%;
        max-width: var(--dh-contenido-max, none);
        margin-inline: auto;
        outline: none;
      }
      .scrim {
        display: none;
      }
      @media (max-width: 900px) {
        dh-sidebar {
          position: fixed;
          inset: 0 auto 0 0;
          width: min(18rem, 86vw) !important;
          transform: translateX(-102%);
          transition: transform 0.4s var(--ease, ease);
          box-shadow: var(--shadow-2, 0 10px 30px -8px rgb(30 41 59 / 30%));
        }
        :host([menu-abierto]) dh-sidebar {
          transform: none;
        }
        .scrim {
          display: block;
          position: fixed;
          inset: 0;
          z-index: 35;
          background: rgb(15 23 42 / 50%);
          backdrop-filter: blur(3px);
          opacity: 0;
          pointer-events: none;
          transition: opacity 0.3s;
        }
        :host([menu-abierto]) .scrim {
          opacity: 1;
          pointer-events: auto;
        }
      }
    `,
  ]

  constructor() {
    super()
    this.items = []
    this.activo = ''
    this.marca = null
    this.etiquetaGrupo = ''
    this.migas = []
    this.usuario = null
    this.accionesUsuario = []
    this.avisos = false
    this.buscador = false
    this.colapsable = true
    this.recordarColapso = true
    this._colapsado = false
    this._menuAbierto = false
    this._leido = false
    this._tecla = (e) => {
      if (e.key === 'Escape' && this._menuAbierto) this._abrirMenu(false)
    }
  }

  connectedCallback() {
    super.connectedCallback()
    document.addEventListener('keydown', this._tecla)
    if (!this._leido && this.recordarColapso) {
      this._leido = true
      try {
        this._colapsado = localStorage.getItem(CLAVE_COLAPSADO) === '1'
      } catch {
        /* sin almacenamiento: el menú arranca expandido */
      }
    }
  }

  disconnectedCallback() {
    document.removeEventListener('keydown', this._tecla)
    super.disconnectedCallback()
  }

  _abrirMenu(valor) {
    this._menuAbierto = valor
    this.toggleAttribute('menu-abierto', valor)
  }

  _alColapsar(evento) {
    this._colapsado = evento.detail.colapsado
    if (!this.recordarColapso) return
    try {
      localStorage.setItem(CLAVE_COLAPSADO, this._colapsado ? '1' : '0')
    } catch {
      /* sin almacenamiento */
    }
  }

  render() {
    return html`
      <div class="app">
        <dh-sidebar
          part="sidebar"
          .items=${this.items}
          .activo=${this.activo}
          .marca=${this.marca}
          .etiquetaGrupo=${this.etiquetaGrupo}
          .colapsable=${this.colapsable}
          .colapsado=${this._colapsado}
          .abierto=${this._menuAbierto}
          @dh-colapsar=${this._alColapsar}
          @dh-cerrar=${() => this._abrirMenu(false)}
        >
          <slot name="sidebar-pie" slot="pie"></slot>
        </dh-sidebar>
        <div class="principal">
          <dh-header
            part="header"
            .migas=${this.migas}
            .usuario=${this.usuario}
            .accionesUsuario=${this.accionesUsuario}
            .avisos=${this.avisos}
            .buscador=${this.buscador}
            @dh-menu=${() => this._abrirMenu(true)}
          >
            <slot name="header-acciones" slot="acciones"></slot>
          </dh-header>
          <slot name="aviso"></slot>
          <main id="contenido" part="contenido" tabindex="-1"><slot></slot></main>
        </div>
        <div class="scrim" @click=${() => this._abrirMenu(false)}></div>
      </div>
    `
  }
}
