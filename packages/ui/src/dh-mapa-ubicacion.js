import { LitElement, css, html, nothing, unsafeCSS } from 'lit'
import * as Leaflet from 'leaflet/dist/leaflet-src.esm.js'

import { base } from './comunes.js'
import { icono } from './iconos.js'
import { LEAFLET_CSS } from './leaflet-css.js'

// * Se importa la build ESM de Leaflet: la "main" del paquete es UMD y Angular avisa que no es ESM
const L = Leaflet

const LA_PAZ = [-16.4897, -68.1193]
const ZOOM_SIN_PUNTO = 12
const ZOOM_CON_PUNTO = 16
const PASO_TECLADO = 0.00005
const TESELAS = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATRIBUCION = '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>'

/** Redondea a 6 decimales (lo que guarda la base: unos 10 cm). */
function redondear(valor) {
  return Math.round(valor * 1e6) / 1e6
}

/**
 * <dh-mapa-ubicacion>: elegir un punto en un mapa de OpenStreetMap (Leaflet).
 *
 * Propiedades: `latitud` y `longitud` (números o `null`), `direccion` (texto de solo lectura que
 * se muestra bajo el mapa), `deshabilitado` y `centro` (`[lat, lng]`, La Paz por defecto).
 *
 * Evento: `dh-ubicacion` con `detail: { latitud, longitud }` (6 decimales) cuando la persona toca
 * el mapa, arrastra el marcador, usa "Mi ubicación" o ajusta con Alt + flechas.
 *
 * * Solo elige el punto. NO calcula la dirección: la calcula el servidor al guardar.
 * * El marcador es un `divIcon` dibujado con CSS: no hace falta ninguna imagen.
 * * Reglas de OpenStreetMap: la atribución siempre está a la vista y no se precargan mosaicos.
 */
export class DhMapaUbicacion extends LitElement {
  static properties = {
    latitud: { type: Number },
    longitud: { type: Number },
    direccion: { type: String },
    deshabilitado: { type: Boolean, reflect: true },
    centro: { attribute: false },
    _aviso: { state: true },
  }

  static styles = [
    base,
    unsafeCSS(LEAFLET_CSS),
    css`
      :host {
        display: block;
        font-family: var(--font-sans);
      }
      .mapa {
        height: var(--dh-mapa-alto, 16rem);
        width: 100%;
        border: 1px solid var(--border);
        border-radius: var(--radius-lg, 0.75rem);
        background: var(--muted);
        overflow: hidden;
        position: relative;
        z-index: 0;
      }
      :host([deshabilitado]) .mapa {
        opacity: 0.7;
      }
      .barra {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.5rem;
      }
      .boton {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        padding: 0.45rem 0.8rem;
        border: 1px solid var(--border);
        border-radius: 0.6rem;
        background: var(--card);
        color: var(--foreground);
        font-size: 0.85rem;
        font-weight: 600;
      }
      .boton:hover:not([disabled]) {
        background: var(--muted);
      }
      .boton[disabled] {
        cursor: not-allowed;
        opacity: 0.6;
      }
      .ayuda {
        color: var(--muted-foreground);
        font-size: 0.8rem;
      }
      .aviso {
        color: var(--destructive);
        font-size: 0.8rem;
        margin-top: 0.4rem;
      }
      .direccion {
        margin-top: 0.5rem;
        font-size: 0.85rem;
        color: var(--foreground);
      }
      .direccion small {
        display: block;
        color: var(--muted-foreground);
      }
      .dh-pin {
        width: 1.6rem;
        height: 1.6rem;
        border-radius: 50% 50% 50% 0;
        background: var(--primary);
        border: 2px solid var(--primary-foreground);
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
        transform: rotate(-45deg);
      }
      .leaflet-control-zoom a {
        display: block;
        width: 1.9rem;
        height: 1.9rem;
        line-height: 1.9rem;
        text-align: center;
        background: var(--card);
        color: var(--foreground);
        border-bottom: 1px solid var(--border);
        font-weight: 700;
        text-decoration: none;
      }
      .leaflet-control-zoom {
        border: 1px solid var(--border);
        border-radius: 0.5rem;
        overflow: hidden;
      }
      .leaflet-control-attribution {
        background: var(--card);
        color: var(--muted-foreground);
        font-size: 0.65rem;
        padding: 0 0.35rem;
      }
      .leaflet-control-attribution a {
        color: var(--foreground);
      }
    `,
  ]

  constructor() {
    super()
    this.latitud = null
    this.longitud = null
    this.direccion = ''
    this.deshabilitado = false
    this.centro = LA_PAZ
    this._aviso = ''
    this._mapa = null
    this._marcador = null
  }

  disconnectedCallback() {
    super.disconnectedCallback()
    this._mapa?.remove()
    this._mapa = null
    this._marcador = null
  }

  firstUpdated() {
    const contenedor = this.renderRoot.querySelector('.mapa')
    const tienePunto = this._tienePunto()
    this._mapa = L.map(contenedor, {
      center: tienePunto ? [this.latitud, this.longitud] : this.centro ?? LA_PAZ,
      zoom: tienePunto ? ZOOM_CON_PUNTO : ZOOM_SIN_PUNTO,
      minZoom: 3,
      maxZoom: 19,
      keyboard: false,
    })
    L.tileLayer(TESELAS, { maxZoom: 19, attribution: ATRIBUCION }).addTo(this._mapa)
    this._mapa.on('click', (e) => this._elegir(e.latlng.lat, e.latlng.lng))
    if (tienePunto) this._colocarMarcador(this.latitud, this.longitud)
    // * El contenedor puede estar en un panel que todavía se está abriendo
    setTimeout(() => this._mapa?.invalidateSize(), 0)
  }

  updated(cambios) {
    if (!this._mapa) return
    if (cambios.has('latitud') || cambios.has('longitud')) {
      if (this._tienePunto()) {
        this._colocarMarcador(this.latitud, this.longitud)
        this._mapa.setView([this.latitud, this.longitud], Math.max(this._mapa.getZoom(), ZOOM_CON_PUNTO))
      } else if (this._marcador) {
        this._marcador.remove()
        this._marcador = null
      }
    }
    if (cambios.has('deshabilitado') && this._marcador) {
      this._habilitarMarcador()
    }
  }

  _tienePunto() {
    return Number.isFinite(this.latitud) && Number.isFinite(this.longitud)
  }

  _colocarMarcador(latitud, longitud) {
    if (this._marcador) {
      this._marcador.setLatLng([latitud, longitud])
      return
    }
    this._marcador = L.marker([latitud, longitud], {
      draggable: !this.deshabilitado,
      keyboard: false,
      icon: L.divIcon({ className: '', html: '<div class="dh-pin"></div>', iconSize: [26, 26], iconAnchor: [13, 26] }),
    }).addTo(this._mapa)
    this._marcador.on('dragend', () => {
      const { lat, lng } = this._marcador.getLatLng()
      this._elegir(lat, lng)
    })
  }

  _habilitarMarcador() {
    if (this.deshabilitado) this._marcador.dragging.disable()
    else this._marcador.dragging.enable()
  }

  _elegir(latitud, longitud) {
    if (this.deshabilitado) return
    this._aviso = ''
    this.latitud = redondear(latitud)
    this.longitud = redondear(longitud)
    this._colocarMarcador(this.latitud, this.longitud)
    this.dispatchEvent(
      new CustomEvent('dh-ubicacion', {
        detail: { latitud: this.latitud, longitud: this.longitud },
        bubbles: true,
        composed: true,
      })
    )
  }

  _miUbicacion() {
    this._aviso = ''
    if (!navigator.geolocation) {
      this._aviso = 'Tu dispositivo no permite obtener la ubicación. Toca el mapa para elegirla.'
      return
    }
    navigator.geolocation.getCurrentPosition(
      (posicion) => {
        this._elegir(posicion.coords.latitude, posicion.coords.longitude)
        this._mapa?.setView([this.latitud, this.longitud], ZOOM_CON_PUNTO)
      },
      () => {
        this._aviso = 'No pudimos obtener tu ubicación. Toca el mapa para elegirla.'
      },
      { enableHighAccuracy: true, timeout: 10000 }
    )
  }

  _teclado(e) {
    if (this.deshabilitado || !e.altKey || !this._tienePunto()) return
    const movimientos = {
      ArrowUp: [PASO_TECLADO, 0],
      ArrowDown: [-PASO_TECLADO, 0],
      ArrowLeft: [0, -PASO_TECLADO],
      ArrowRight: [0, PASO_TECLADO],
    }
    const mover = movimientos[e.key]
    if (!mover) return
    e.preventDefault()
    this._elegir(this.latitud + mover[0], this.longitud + mover[1])
  }

  render() {
    const hayPunto = this._tienePunto()
    return html`
      <div class="barra">
        <button
          class="boton"
          type="button"
          ?disabled=${this.deshabilitado}
          @click=${this._miUbicacion}
        >
          ${icono('pin', 16)}<span>Mi ubicación</span>
        </button>
        <span class="ayuda">
          ${hayPunto ? 'Toca otro lugar o arrastra el marcador para cambiarlo.' : 'Toca el mapa para marcar tu ubicación.'}
        </span>
      </div>
      <div
        class="mapa"
        tabindex="0"
        role="application"
        aria-label="Mapa para elegir la ubicación. Con Alt y las flechas mueves el marcador."
        @keydown=${this._teclado}
      ></div>
      ${this._aviso ? html`<div class="aviso" role="alert">${this._aviso}</div>` : nothing}
      ${this.direccion
        ? html`<div class="direccion"><small>Dirección</small>${this.direccion}</div>`
        : nothing}
    `
  }
}
