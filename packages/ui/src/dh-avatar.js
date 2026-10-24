import { LitElement, css, html } from 'lit'

/** Iniciales (hasta dos) de nombres y apellidos, en mayúscula. */
export function iniciales(nombres = '', apellidos = '') {
  const primera = (texto) => String(texto).trim().charAt(0).toUpperCase()
  return `${primera(nombres)}${primera(apellidos)}`
}

/**
 * <dh-avatar>: la foto de la persona o, sin foto o si la imagen no carga (por ejemplo la URL
 * prefirmada venció), sus iniciales sobre el color de marca.
 *
 * Propiedades: `nombres`, `apellidos`, `fotoUrl`, `tam` (px, por defecto 40).
 */
export class DhAvatar extends LitElement {
  static properties = {
    nombres: { type: String },
    apellidos: { type: String },
    fotoUrl: { type: String, attribute: 'foto-url' },
    tam: { type: Number },
    _urlRota: { state: true },
  }

  static styles = css`
    :host {
      display: inline-grid;
      flex: none;
      place-items: center;
      overflow: hidden;
      border-radius: 50%;
      background: var(--primary);
      color: var(--primary-foreground);
      font-family: var(--font-sans);
      font-weight: 750;
      line-height: 1;
      width: var(--dh-avatar-tam, 2.5rem);
      height: var(--dh-avatar-tam, 2.5rem);
      font-size: calc(var(--dh-avatar-tam, 2.5rem) * 0.36);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  `

  constructor() {
    super()
    this.nombres = ''
    this.apellidos = ''
    this.fotoUrl = null
    this.tam = 40
    this._urlRota = null
  }

  willUpdate(cambios) {
    if (cambios.has('tam')) this.style.setProperty('--dh-avatar-tam', `${this.tam}px`)
  }

  render() {
    const mostrarFoto = this.fotoUrl && this.fotoUrl !== this._urlRota
    return mostrarFoto
      ? html`<img src=${this.fotoUrl} alt="" @error=${() => (this._urlRota = this.fotoUrl)} />`
      : html`<span aria-hidden="true">${iniciales(this.nombres, this.apellidos)}</span>`
  }
}
