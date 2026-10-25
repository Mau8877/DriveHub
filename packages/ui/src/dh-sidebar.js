import { LitElement, css, html, nothing } from 'lit'

import { base, marcaVisual } from './comunes.js'
import { icono } from './iconos.js'

/**
 * <dh-sidebar>: menú lateral de la web. No conoce rutas ni roles: dibuja los `items` que recibe y
 * avisa con eventos qué eligió la persona. Cada micro-frontend (o el shell) decide qué items hay.
 *
 * Propiedades
 *   items         ItemNavegacion[]  { ruta, etiqueta, descripcion?, icono?, grupo?, insignia?, hijos? }
 *                 Un item con `hijos` es un botón padre que despliega sus opciones (sin `ruta` propia):
 *                 se abre solo si una de ellas es la ruta actual, y varios pueden estar abiertos a la vez.
 *                 Cada opción puede llevar su `icono`.
 *                 Con el menú contraído (solo iconos), pulsar un padre expande el menú y abre ese grupo
 *   activo        ruta actual; un item está activo si es esa ruta o una subruta suya, salvo que otro
 *                 item coincida con una ruta más específica (gana el más específico)
 *   marca         { prefijo, resaltado, ruta }  (por defecto "DriveHub")
 *   etiquetaGrupo texto sobre el menú (ej. el nombre de la escuela)
 *   colapsado     menú de solo iconos (se refleja como atributo)
 *   colapsable    muestra el botón "Contraer"
 *   abierto       cajón abierto en pantallas chicas (lo controla quien lo usa; <dh-shell> lo hace)
 *   navegacion-nativa  los enlaces navegan solos en vez de emitir `dh-navegar`
 *
 * Eventos (burbujean y atraviesan el Shadow DOM)
 *   dh-navegar   { ruta, item }   eligió un item (clic normal; con Ctrl/Cmd abre en otra pestaña)
 *   dh-colapsar  { colapsado }
 *   dh-cerrar                      pidió cerrar el cajón (eligió un item)
 *
 * Slots: `marca` (reemplaza logo y nombre) y `pie` (arriba del botón de contraer).
 * Partes (::part): sidebar, marca, grupo, nav, item, item-activo, pie, contraer.
 * Variables CSS: --dh-sidebar-ancho, --dh-sidebar-ancho-colapsado.
 */
export class DhSidebar extends LitElement {
  static properties = {
    items: { type: Array },
    activo: { type: String },
    marca: { type: Object },
    etiquetaGrupo: { type: String, attribute: 'etiqueta-grupo' },
    colapsado: { type: Boolean, reflect: true },
    colapsable: { type: Boolean },
    abierto: { type: Boolean, reflect: true },
    navegacionNativa: { type: Boolean, attribute: 'navegacion-nativa' },
    _abiertos: { state: true },
  }

  static styles = [
    base,
    css`
      :host {
        display: block;
        width: var(--dh-sidebar-ancho, 17.5rem);
        transition: width 0.35s var(--ease, cubic-bezier(0.22, 1, 0.36, 1));
        color: var(--sidebar-foreground);
      }
      aside {
        position: sticky;
        top: 0;
        height: 100svh;
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        padding: 1rem 0.85rem;
        overflow: hidden auto;
        background: linear-gradient(180deg, var(--sidebar) 0%, color-mix(in srgb, var(--sidebar) 88%, #000) 100%);
        border-right: 1px solid var(--sidebar-border);
        color: var(--sidebar-foreground);
      }
      .marca {
        display: flex;
        align-items: center;
        gap: 0.7rem;
        padding: 0.35rem 0.5rem 1.25rem;
        margin-bottom: 0.25rem;
        border-bottom: 1px solid var(--sidebar-border);
        font-size: 1.2rem;
        font-weight: 800;
        letter-spacing: -0.02em;
        color: var(--blanco-puro);
      }
      .marca b {
        color: var(--ambar-vial);
        font-weight: 800;
      }
      .marca-nombre {
        white-space: nowrap;
      }
      .grupo {
        padding: 0.75rem 0.75rem 0.35rem;
        font-size: 0.68rem;
        font-weight: 700;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        opacity: 0.5;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      nav {
        display: flex;
        flex-direction: column;
        gap: 0.2rem;
      }
      .item {
        position: relative;
        display: flex;
        align-items: center;
        gap: 0.8rem;
        padding: 0.6rem 0.75rem;
        border-radius: var(--radius);
        color: var(--sidebar-foreground);
        transition: background-color 0.2s, transform 0.2s var(--ease, ease);
      }
      .item:hover {
        background: var(--sidebar-accent);
        transform: translateX(2px);
      }
      .item:hover:not(.activo) .icono {
        color: var(--ambar-vial);
      }
      .item.activo {
        color: var(--sidebar-accent-foreground);
        background: linear-gradient(90deg, color-mix(in srgb, var(--ambar-vial) 22%, transparent), transparent 85%);
      }
      .item.activo::before {
        content: '';
        position: absolute;
        left: -0.85rem;
        top: 22%;
        bottom: 22%;
        width: 4px;
        border-radius: 0 4px 4px 0;
        background: var(--ambar-vial);
        box-shadow: 0 0 14px var(--ambar-vial);
        animation: barra 0.4s var(--ease, ease);
      }
      @keyframes barra {
        from {
          transform: scaleY(0);
        }
      }
      .icono {
        display: grid;
        place-items: center;
        width: 1.5rem;
        flex: none;
        opacity: 0.9;
      }
      .item.activo .icono {
        color: var(--sidebar-primary);
        opacity: 1;
      }
      .texto {
        display: flex;
        flex-direction: column;
        min-width: 0;
        line-height: 1.25;
      }
      .texto strong {
        font-size: 0.875rem;
        font-weight: 600;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .texto small {
        font-size: 0.7rem;
        opacity: 0.55;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .insignia {
        margin-left: auto;
        padding: 0 0.45rem;
        border-radius: 99px;
        font-size: 0.7rem;
        font-weight: 700;
        background: var(--ambar-vial);
        color: var(--gris-asfalto);
      }
      /* Grupos desplegables: un botón padre y sus opciones */
      button.item {
        width: 100%;
        text-align: left;
        font: inherit;
        background: none;
        border: 0;
        cursor: pointer;
      }
      .padre.con-activo > .item .icono {
        color: var(--sidebar-primary);
        opacity: 1;
      }
      .flecha {
        display: grid;
        place-items: center;
        margin-left: auto;
        flex: none;
        opacity: 0.6;
        transition: transform 0.3s var(--ease, ease);
      }
      .padre.abierto > .item .flecha {
        transform: rotate(180deg);
      }
      .hijos {
        display: grid;
        grid-template-rows: 0fr;
        transition: grid-template-rows 0.3s var(--ease, ease);
      }
      .padre.abierto > .hijos {
        grid-template-rows: 1fr;
      }
      .hijos-interior {
        display: flex;
        flex-direction: column;
        gap: 0.15rem;
        min-height: 0;
        overflow: hidden;
        margin-left: 1.45rem;
        padding-left: 0.5rem;
        border-left: 1px solid var(--sidebar-border);
      }
      .padre.abierto > .hijos > .hijos-interior {
        margin-block: 0.15rem 0.3rem;
      }
      .item.hijo {
        gap: 0.6rem;
        padding: 0.5rem 0.7rem;
        font-size: 0.84rem;
      }
      .item.hijo .icono {
        width: 1.25rem;
      }
      .item.hijo:hover {
        transform: translateX(2px);
      }
      .item.hijo.activo::before {
        display: none;
      }
      .item.hijo.activo {
        font-weight: 650;
        background: linear-gradient(90deg, color-mix(in srgb, var(--ambar-vial) 22%, transparent), transparent 90%);
      }
      .pie {
        margin-top: auto;
        display: flex;
        flex-direction: column;
        gap: 0.6rem;
        padding-top: 1rem;
      }
      .contraer {
        display: flex;
        align-items: center;
        gap: 0.7rem;
        padding: 0.55rem 0.75rem;
        border-radius: var(--radius);
        font-size: 0.8125rem;
        opacity: 0.7;
        transition: background-color 0.2s, opacity 0.2s;
      }
      .contraer:hover {
        background: var(--sidebar-accent);
        opacity: 1;
      }
      .contraer .ico {
        transition: transform 0.35s var(--ease, ease);
      }
      :host([colapsado]) .contraer .ico {
        transform: rotate(180deg);
      }
      /* El menú contraído (solo iconos) solo existe en escritorio: en pantallas chicas es un cajón */
      @media (min-width: 901px) {
        :host([colapsado]) {
          width: var(--dh-sidebar-ancho-colapsado, 4.75rem);
        }
        :host([colapsado]) .texto,
        :host([colapsado]) .marca-nombre,
        :host([colapsado]) .grupo,
        :host([colapsado]) .insignia,
        :host([colapsado]) .flecha,
        :host([colapsado]) .hijos,
        :host([colapsado]) .contraer span {
          display: none;
        }
        :host([colapsado]) .item,
        :host([colapsado]) .contraer,
        :host([colapsado]) .marca {
          justify-content: center;
          padding-inline: 0;
        }
      }
      @media (max-width: 900px) {
        .contraer {
          display: none;
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
    this.colapsado = false
    this.colapsable = true
    this.abierto = false
    this.navegacionNativa = false
    this._abiertos = new Set()
  }

  willUpdate(cambios) {
    // * Un grupo con la ruta actual adentro se abre solo; los demás quedan como estaban
    if (cambios.has('activo') || cambios.has('items')) {
      const padres = (this.items ?? []).filter((i) => i.hijos?.some((h) => this._esActivo(h)))
      if (padres.some((p) => !this._abiertos.has(p.etiqueta))) {
        this._abiertos = new Set([...this._abiertos, ...padres.map((p) => p.etiqueta)])
      }
    }
  }

  /** Todos los items con ruta, también los hijos de un grupo (para comparar cuál es el más específico). */
  _planos() {
    return (this.items ?? []).flatMap((item) => (item.hijos?.length ? item.hijos : [item]))
  }

  /**
   * ¿Es la ruta actual (o una subruta) la del item? La raíz `/` solo coincide consigo misma.
   *
   * * Si otro item también coincide con una ruta MÁS específica (`/escuela/estudiantes` frente a
   * * `/escuela`), gana el más específico: nunca quedan dos items activos a la vez.
   */
  _esActivo(item) {
    const actual = String(this.activo ?? '').split(/[?#]/)[0]
    const ruta = String(item.ruta ?? '').split(/[?#]/)[0]
    if (!ruta) return false
    if (!this._coincide(actual, ruta)) return false
    return !this._planos().some((otro) => {
      const otraRuta = String(otro.ruta ?? '').split(/[?#]/)[0]
      return otraRuta.length > ruta.length && this._coincide(actual, otraRuta)
    })
  }

  _coincide(actual, ruta) {
    if (!ruta) return false
    if (ruta === '/') return actual === '/'
    return actual === ruta || actual.startsWith(`${ruta}/`)
  }

  _emitir(nombre, detalle) {
    this.dispatchEvent(new CustomEvent(nombre, { detail: detalle, bubbles: true, composed: true }))
  }

  _ir(evento, item) {
    // * Ctrl/Cmd/Shift/clic del medio: se deja al navegador (abrir en otra pestaña)
    if (this.navegacionNativa || evento.defaultPrevented || evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
      return
    }
    evento.preventDefault()
    this._emitir('dh-navegar', { ruta: item.ruta, item })
    this._emitir('dh-cerrar')
  }

  _alternarPadre(item) {
    const abiertos = new Set(this._abiertos)
    // * Con el menú contraído no hay dónde desplegar: se expande el menú y se abre ese grupo
    if (this.colapsado) {
      abiertos.add(item.etiqueta)
      this.colapsado = false
      this._emitir('dh-colapsar', { colapsado: false })
    } else if (abiertos.has(item.etiqueta)) {
      abiertos.delete(item.etiqueta)
    } else {
      abiertos.add(item.etiqueta)
    }
    this._abiertos = abiertos
  }

  _alternarColapso() {
    this.colapsado = !this.colapsado
    this._emitir('dh-colapsar', { colapsado: this.colapsado })
  }

  _grupos() {
    const salida = []
    let anterior
    for (const item of this.items ?? []) {
      const grupo = item.grupo ?? ''
      if (grupo && grupo !== anterior) salida.push({ grupo })
      salida.push({ item })
      anterior = grupo
    }
    return salida
  }

  render() {
    const m = this.marca ?? {}
    return html`
      <aside part="sidebar" aria-label="Menú principal">
        <a class="marca" part="marca" href=${m.ruta ?? '/'} @click=${(e) => this._ir(e, { ruta: m.ruta ?? '/' })}>
          <slot name="marca">${marcaVisual(this.marca)}</slot>
        </a>
        ${this.etiquetaGrupo ? html`<div class="grupo" part="grupo">${this.etiquetaGrupo}</div>` : nothing}
        <nav part="nav">
          ${this._grupos().map((fila) =>
            fila.grupo
              ? html`<div class="grupo" part="grupo">${fila.grupo}</div>`
              : this._renderItem(fila.item),
          )}
        </nav>
        <div class="pie" part="pie">
          <slot name="pie"></slot>
          ${this.colapsable
            ? html`<button type="button" class="contraer" part="contraer" @click=${this._alternarColapso} aria-label=${this.colapsado ? 'Expandir menú' : 'Contraer menú'}>
                ${icono('chevrons-left', 18)}<span>Contraer</span>
              </button>`
            : nothing}
        </div>
      </aside>
    `
  }

  _renderPadre(item) {
    const abierto = this._abiertos.has(item.etiqueta)
    const conActivo = item.hijos.some((h) => this._esActivo(h))
    const id = `grupo-${String(item.etiqueta).toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
    return html`<div class="padre ${abierto ? 'abierto' : ''} ${conActivo ? 'con-activo' : ''}">
      <button
        type="button"
        class="item"
        part=${conActivo ? 'item item-padre item-activo' : 'item item-padre'}
        title=${item.etiqueta}
        aria-expanded=${String(abierto)}
        aria-controls=${id}
        @click=${() => this._alternarPadre(item)}
      >
        <span class="icono">${icono(item.icono, 20)}</span>
        <span class="texto">
          <strong>${item.etiqueta}</strong>
          ${item.descripcion ? html`<small>${item.descripcion}</small>` : nothing}
        </span>
        <span class="flecha">${icono('chevron-down', 16)}</span>
      </button>
      <div class="hijos" id=${id} role="group" aria-label=${item.etiqueta} ?inert=${!abierto}>
        <div class="hijos-interior">${item.hijos.map((hijo) => this._renderItem(hijo, true))}</div>
      </div>
    </div>`
  }

  _renderItem(item, esHijo = false) {
    if (item.hijos?.length) return this._renderPadre(item)
    const activo = this._esActivo(item)
    return html`<a
      class="item ${esHijo ? 'hijo' : ''} ${activo ? 'activo' : ''}"
      part=${activo ? 'item item-activo' : 'item'}
      href=${item.ruta}
      title=${item.etiqueta}
      aria-current=${activo ? 'page' : nothing}
      @click=${(e) => this._ir(e, item)}
    >
      ${esHijo && !item.icono ? nothing : html`<span class="icono">${icono(item.icono, esHijo ? 17 : 20)}</span>`}
      <span class="texto">
        <strong>${item.etiqueta}</strong>
        ${item.descripcion ? html`<small>${item.descripcion}</small>` : nothing}
      </span>
      ${item.insignia != null && item.insignia !== '' ? html`<span class="insignia">${item.insignia}</span>` : nothing}
    </a>`
  }
}
