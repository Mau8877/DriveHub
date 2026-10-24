import { LitElement, css, html, nothing } from 'lit'

import { base } from './comunes.js'
import { icono } from './iconos.js'

/**
 * <dh-dialogo>: diálogo de confirmación (¿Quitar tu foto?, ¿Desactivar la escuela?). Lo usa toda
 * pantalla que necesite confirmar una acción: nadie dibuja su propio "¿estás seguro?".
 *
 * Propiedades: abierto, titulo, texto, aceptar (por defecto "Aceptar"), peligro, ocupado.
 * Eventos: `dh-aceptar` y `dh-cancelar` (también con Escape o un clic en el fondo). El componente
 * no se cierra solo: la app baja `abierto` cuando termina.
 */
export class DhDialogo extends LitElement {
  static properties = {
    abierto: { type: Boolean, reflect: true },
    titulo: { type: String },
    texto: { type: String },
    aceptar: { type: String },
    peligro: { type: Boolean },
    ocupado: { type: Boolean },
  }

  static styles = [
    base,
    css`
      :host {
        display: contents;
      }
      .fondo {
        position: fixed;
        inset: 0;
        z-index: 150;
        display: grid;
        place-items: center;
        padding: 1rem;
        background: rgb(15 23 42 / 50%);
        backdrop-filter: blur(5px);
        animation: aparece 0.25s;
      }
      .dialogo {
        width: min(26rem, 100%);
        padding: 1.6rem;
        text-align: center;
        background: var(--card);
        border: 1px solid var(--border);
        border-radius: calc(var(--radius) * 1.4);
        box-shadow: var(--shadow-2);
        animation: entra 0.3s var(--ease, ease);
      }
      .ico-caja {
        display: grid;
        place-items: center;
        width: 3.2rem;
        height: 3.2rem;
        margin: 0 auto 1rem;
        border-radius: 1rem;
        background: color-mix(in srgb, var(--azul-tecnico) 13%, transparent);
        color: var(--info);
      }
      .ico-caja.peligro {
        background: color-mix(in srgb, var(--destructive) 13%, transparent);
        color: var(--destructive);
      }
      h2 {
        margin: 0;
        font-size: 1.15rem;
        font-weight: 700;
        letter-spacing: -0.01em;
      }
      p {
        margin: 0.5rem 0 0;
        color: var(--muted-foreground);
        font-size: 0.875rem;
      }
      .acciones {
        display: flex;
        gap: 0.75rem;
        justify-content: center;
        margin-top: 1.4rem;
      }
      .btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 2.65rem;
        padding: 0 1.15rem;
        border: 1px solid transparent;
        border-radius: var(--radius);
        font-weight: 650;
        font-size: 0.9rem;
        background: var(--primary);
        color: var(--primary-foreground);
        transition: transform 0.2s var(--ease, ease), filter 0.2s, background-color 0.2s;
      }
      .btn:hover:not(:disabled) {
        filter: brightness(1.06);
      }
      .btn:active:not(:disabled) {
        transform: scale(0.97);
      }
      .btn:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }
      .btn.ghost {
        background: transparent;
        color: var(--foreground);
        border-color: var(--border);
      }
      .btn.ghost:hover:not(:disabled) {
        background: var(--accent);
        filter: none;
      }
      .btn.peligro {
        background: var(--destructive);
        color: var(--blanco-puro);
      }
      @keyframes aparece {
        from {
          opacity: 0;
        }
      }
      @keyframes entra {
        from {
          opacity: 0;
          transform: scale(0.94) translateY(8px);
        }
      }
    `,
  ]

  constructor() {
    super()
    this.abierto = false
    this.titulo = ''
    this.texto = ''
    this.aceptar = 'Aceptar'
    this.peligro = false
    this.ocupado = false
    this._tecla = (e) => {
      if (e.key === 'Escape' && this.abierto && !this.ocupado) this._cancelar()
    }
  }

  connectedCallback() {
    super.connectedCallback()
    document.addEventListener('keydown', this._tecla)
  }

  disconnectedCallback() {
    document.removeEventListener('keydown', this._tecla)
    super.disconnectedCallback()
  }

  updated(cambios) {
    // * Al abrir, el foco va al botón de aceptar (como el diálogo del diseño)
    if (cambios.has('abierto') && this.abierto) {
      this.renderRoot.querySelector('.btn:not(.ghost)')?.focus()
    }
  }

  _cancelar() {
    this.dispatchEvent(new CustomEvent('dh-cancelar', { bubbles: true, composed: true }))
  }

  _aceptar() {
    this.dispatchEvent(new CustomEvent('dh-aceptar', { bubbles: true, composed: true }))
  }

  render() {
    if (!this.abierto) return nothing
    return html`
      <div class="fondo" @mousedown=${(e) => e.target === e.currentTarget && !this.ocupado && this._cancelar()}>
        <div class="dialogo" role="alertdialog" aria-modal="true" aria-labelledby="titulo">
          <div class="ico-caja ${this.peligro ? 'peligro' : ''}">${icono(this.peligro ? 'alert' : 'info', 22)}</div>
          <h2 id="titulo">${this.titulo}</h2>
          ${this.texto ? html`<p>${this.texto}</p>` : nothing}
          <div class="acciones">
            <button type="button" class="btn ghost" ?disabled=${this.ocupado} @click=${this._cancelar}>Cancelar</button>
            <button
              type="button"
              class="btn ${this.peligro ? 'peligro' : ''}"
              ?disabled=${this.ocupado}
              @click=${this._aceptar}
            >
              ${this.aceptar}
            </button>
          </div>
        </div>
      </div>
    `
  }
}
