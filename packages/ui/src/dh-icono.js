import { LitElement, css } from 'lit'

import { base } from './comunes.js'
import { icono } from './iconos.js'

/**
 * <dh-icono nombre="mail" tam="20">: un icono de la librería de @drivehub/ui, para usarlo en las
 * plantillas de Angular y React sin depender de una librería de iconos. Toma el color del texto.
 */
export class DhIcono extends LitElement {
  static properties = {
    nombre: { type: String },
    tam: { type: Number },
  }

  static styles = [
    base,
    css`
      :host {
        display: inline-flex;
        flex: none;
        vertical-align: middle;
        line-height: 0;
      }
    `,
  ]

  constructor() {
    super()
    this.nombre = ''
    this.tam = 20
  }

  render() {
    return icono(this.nombre, this.tam)
  }
}
