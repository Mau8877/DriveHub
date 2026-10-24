import { LitElement, css, html, nothing } from 'lit'

import { base } from './comunes.js'
import { icono } from './iconos.js'

const ESPERA_TECLAS_MS = 600
const ALTO_MAXIMO_REM = 16
const MARGEN_PX = 8
const MAX_FICHAS = 3

/** Para buscar al escribir: minúsculas y sin tildes ("Árbol" se encuentra con "ar"). */
function normalizar(texto) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
}

/**
 * <dh-select>: lista desplegable propia de DriveHub. Reemplaza al `<select>` nativo, cuyo menú
 * lo dibuja el navegador y no sigue el diseño Asfalto.
 *
 * Propiedades
 *   valor          valor elegido ('' = nada)
 *   opciones       { valor, etiqueta }[]
 *   placeholder    texto cuando no hay nada elegido (por defecto "Elige una opción"); también es la
 *                  primera opción de la lista, con la que se quita la elección
 *   sinVacio       (atributo `sin-vacio`) no ofrece la opción de quitar la elección
 *   etiqueta       nombre accesible del control (lo que diría su <label>)
 *   multiple       elige varios valores (`valores`): el menú queda abierto al elegir, cada opción se
 *                  alterna y lo elegido se ve como fichas con una `×`. Ignora `valor` y `sinVacio`
 *   valores        valores elegidos en modo `multiple`
 *   deshabilitado  no se abre ni se elige
 *   invalido       borde de error
 *
 * Eventos (burbujean y atraviesan el Shadow DOM)
 *   dh-cambio   { valor }   eligió una opción (con `multiple`: { valores }, en el orden de `opciones`)
 *   dh-blur                 el foco salió del control (para marcar el campo como tocado)
 *
 * Teclado: Enter, Espacio o flechas abren; flechas, Inicio y Fin se mueven; Enter elige; Escape
 * cierra (sin llegar a quien escuche Escape más arriba, como el panel que contiene el campo);
 * escribir salta a la opción que empieza con esas letras. Con `multiple`, Enter y Espacio alternan la
 * opción activa y Backspace (con el menú cerrado) quita la última ficha.
 *
 * * La lista se posiciona con `position: fixed` según el control: así no la recorta el
 * * `overflow` de un panel con scroll, y se abre hacia arriba si abajo no entra.
 * * Se cierra al hacer clic fuera, al cambiar el tamaño de la ventana o al hacer scroll fuera de ella.
 * ! No usar `<select>` nativo en la web: si falta algo, se agrega acá para todos.
 */
export class DhSelect extends LitElement {
  static properties = {
    valor: { type: String },
    opciones: { type: Array },
    placeholder: { type: String },
    sinVacio: { type: Boolean, attribute: 'sin-vacio' },
    multiple: { type: Boolean },
    valores: { type: Array },
    etiqueta: { type: String },
    deshabilitado: { type: Boolean, reflect: true },
    invalido: { type: Boolean, reflect: true },
    _abierto: { state: true },
    _activo: { state: true },
    _posicion: { state: true },
  }

  static styles = [
    base,
    css`
      :host {
        display: block;
        min-width: 0;
      }
      .disparador {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.5rem;
        width: 100%;
        height: var(--dh-select-alto, 2.75rem);
        padding: 0 0.8rem 0 0.9rem;
        text-align: left;
        background: var(--card);
        color: var(--foreground);
        border: 1px solid var(--input);
        border-radius: var(--radius);
        transition: border-color 0.2s, box-shadow 0.2s;
      }
      .disparador:hover:not(:disabled) {
        border-color: color-mix(in srgb, var(--gris-medio) 55%, transparent);
      }
      .disparador:focus-visible,
      .disparador[aria-expanded='true'] {
        outline: none;
        border-color: var(--ambar-vial);
        box-shadow: 0 0 0 4px color-mix(in srgb, var(--ambar-vial) 22%, transparent);
      }
      :host([invalido]) .disparador {
        border-color: var(--destructive);
        box-shadow: 0 0 0 4px color-mix(in srgb, var(--destructive) 14%, transparent);
      }
      .disparador:disabled {
        cursor: not-allowed;
        background: var(--muted);
        color: var(--muted-foreground);
      }
      .texto {
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .texto.vacio {
        color: var(--muted-foreground);
      }
      div.disparador {
        cursor: pointer;
      }
      div.disparador[aria-disabled='true'] {
        cursor: not-allowed;
        background: var(--muted);
        color: var(--muted-foreground);
      }
      div.disparador:focus-visible {
        outline: none;
      }
      .fichas {
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        gap: 0.3rem;
        min-width: 0;
        overflow: hidden;
      }
      .ficha {
        display: inline-flex;
        align-items: center;
        gap: 0.2rem;
        flex: none;
        padding: 0.12rem 0.3rem 0.12rem 0.55rem;
        font-size: 0.8rem;
        font-weight: 600;
        background: var(--accent);
        color: var(--accent-foreground);
        border-radius: 999px;
      }
      .ficha.mas {
        padding: 0.12rem 0.55rem;
        background: var(--muted);
        color: var(--muted-foreground);
      }
      .ficha button {
        display: grid;
        place-items: center;
        width: 1.1rem;
        height: 1.1rem;
        padding: 0;
        color: inherit;
        background: transparent;
        border: 0;
        border-radius: 999px;
        cursor: pointer;
      }
      .ficha button:hover {
        background: color-mix(in srgb, var(--foreground) 12%, transparent);
      }
      .casilla {
        display: grid;
        place-items: center;
        flex: none;
        width: 1.1rem;
        height: 1.1rem;
        color: var(--primary-foreground);
        border: 1px solid var(--input);
        border-radius: 0.3rem;
      }
      .casilla.marcada {
        background: var(--primary);
        border-color: var(--primary);
      }
      .flecha {
        display: grid;
        place-items: center;
        color: var(--muted-foreground);
        transition: transform 0.2s var(--ease);
      }
      .disparador[aria-expanded='true'] .flecha {
        transform: rotate(180deg);
      }
      .lista {
        position: fixed;
        z-index: 300;
        margin: 0;
        padding: 0.3rem;
        list-style: none;
        overflow-y: auto;
        background: var(--popover);
        color: var(--popover-foreground);
        border: 1px solid var(--border);
        border-radius: var(--radius);
        box-shadow: var(--shadow-2);
        animation: abrir 0.16s var(--ease);
      }
      .lista.arriba {
        transform-origin: bottom;
      }
      .opcion {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.5rem;
        padding: 0.55rem 0.65rem;
        border-radius: calc(var(--radius) * 0.7);
        cursor: pointer;
        user-select: none;
      }
      .opcion.activa {
        background: var(--accent);
        color: var(--accent-foreground);
      }
      .opcion.elegida {
        font-weight: 650;
      }
      .opcion.placeholder {
        color: var(--muted-foreground);
      }
      .opcion .marca {
        display: grid;
        color: var(--primary);
      }
      .sin-opciones {
        padding: 0.55rem 0.65rem;
        color: var(--muted-foreground);
      }
      @keyframes abrir {
        from {
          opacity: 0;
          transform: translateY(-4px) scale(0.98);
        }
      }
    `,
  ]

  constructor() {
    super()
    this.valor = ''
    this.opciones = []
    this.placeholder = 'Elige una opción'
    this.sinVacio = false
    this.multiple = false
    this.valores = []
    this.etiqueta = ''
    this.deshabilitado = false
    this.invalido = false
    this._abierto = false
    this._activo = -1
    this._posicion = null
    this._buscado = ''
    this._temporizador = null
    this._fuera = (e) => {
      if (!e.composedPath().includes(this)) this._cerrar()
    }
    this._alMoverse = (e) => {
      // * El scroll de la propia lista no cierra nada
      if (e.type === 'scroll' && e.composedPath().includes(this)) return
      this._cerrar()
    }
  }

  disconnectedCallback() {
    this._escuchar(false)
    clearTimeout(this._temporizador)
    super.disconnectedCallback()
  }

  /** La primera opción es el placeholder (quita la elección), salvo con `sin-vacio`. */
  get _items() {
    const reales = this.opciones ?? []
    return this.sinVacio || this.multiple ? reales : [{ valor: '', etiqueta: this.placeholder, vacio: true }, ...reales]
  }

  get _elegida() {
    return (this.opciones ?? []).find((o) => String(o.valor) === String(this.valor ?? ''))
  }

  /** Las opciones elegidas en modo `multiple`, en el orden de `opciones` (lo desconocido se ignora). */
  get _marcadas() {
    const elegidos = new Set((this.valores ?? []).map(String))
    return (this.opciones ?? []).filter((o) => elegidos.has(String(o.valor)))
  }

  _esElegida(item) {
    return this.multiple
      ? this._marcadas.some((o) => String(o.valor) === String(item.valor))
      : String(item.valor) === String(this.valor ?? '')
  }

  _escuchar(activar) {
    const accion = activar ? 'addEventListener' : 'removeEventListener'
    document[accion]('pointerdown', this._fuera, true)
    window[accion]('resize', this._alMoverse)
    window[accion]('scroll', this._alMoverse, true)
  }

  // ------------------------------------------------------------------ abrir, cerrar, elegir

  _abrir() {
    if (this.deshabilitado || this._abierto) return
    const disparador = this.renderRoot.querySelector('.disparador')
    const caja = disparador.getBoundingClientRect()
    const altoMaximo = ALTO_MAXIMO_REM * (parseFloat(getComputedStyle(document.documentElement).fontSize) || 16)
    const abajo = window.innerHeight - caja.bottom - MARGEN_PX
    const arriba = caja.top - MARGEN_PX
    // * Se abre hacia arriba solo si abajo no entra y arriba hay más lugar
    const haciaArriba = abajo < Math.min(altoMaximo, this._items.length * 40 + 12) && arriba > abajo
    const espacio = Math.max(80, haciaArriba ? arriba : abajo)
    this._posicion = {
      arriba: haciaArriba,
      izquierda: caja.left,
      ancho: caja.width,
      ...(haciaArriba ? { abajo: window.innerHeight - caja.top + 4 } : { superior: caja.bottom + 4 }),
      alto: Math.min(altoMaximo, espacio),
    }
    const elegida = this._items.findIndex((o) => this._esElegida(o))
    this._activo = elegida >= 0 ? elegida : 0
    this._abierto = true
    this._escuchar(true)
  }

  _cerrar() {
    if (!this._abierto) return
    this._abierto = false
    this._escuchar(false)
  }

  _elegir(indice) {
    const item = this._items[indice]
    if (!item) return
    if (this.multiple) {
      this._alternar(String(item.valor))
      return
    }
    this._cerrar()
    const valor = String(item.valor ?? '')
    if (valor !== String(this.valor ?? '')) {
      this.valor = valor
      this.dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor }, bubbles: true, composed: true }))
    }
    this.renderRoot.querySelector('.disparador')?.focus()
  }

  /** Con `multiple`: marca o desmarca un valor y avisa con todos los valores, en el orden de `opciones`. */
  _alternar(valor) {
    const actuales = this._marcadas.map((o) => String(o.valor))
    const nuevos = actuales.includes(valor) ? actuales.filter((v) => v !== valor) : [...actuales, valor]
    this.valores = (this.opciones ?? []).map((o) => String(o.valor)).filter((v) => nuevos.includes(v))
    this.dispatchEvent(new CustomEvent('dh-cambio', { detail: { valores: this.valores }, bubbles: true, composed: true }))
  }

  _mover(delta) {
    const total = this._items.length
    if (total === 0) return
    this._activo = Math.min(total - 1, Math.max(0, this._activo + delta))
  }

  updated(cambios) {
    // * La opción activa siempre a la vista al moverse con el teclado
    if (this._abierto && cambios.has('_activo')) {
      this.renderRoot.querySelector('.opcion.activa')?.scrollIntoView?.({ block: 'nearest' })
    }
  }

  // ------------------------------------------------------------------ teclado

  _alTeclear(e) {
    if (this.deshabilitado) return
    const abierto = this._abierto
    switch (e.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        e.preventDefault()
        if (!abierto) this._abrir()
        else this._mover(e.key === 'ArrowDown' ? 1 : -1)
        return
      case 'Home':
      case 'End':
        if (!abierto) return
        e.preventDefault()
        this._activo = e.key === 'Home' ? 0 : this._items.length - 1
        return
      case 'Enter':
      case ' ':
        e.preventDefault()
        if (!abierto) this._abrir()
        else this._elegir(this._activo)
        return
      case 'Escape':
        if (!abierto) return
        // * Solo cierra la lista: un panel que escuche Escape más arriba no debe cerrarse también
        e.preventDefault()
        e.stopPropagation()
        this._cerrar()
        return
      case 'Tab':
        this._cerrar()
        return
      case 'Backspace': {
        if (!this.multiple || abierto) return
        const ultima = this._marcadas.at(-1)
        if (ultima) {
          e.preventDefault()
          this._alternar(String(ultima.valor))
        }
        return
      }
      default:
        if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) this._buscar(e.key)
    }
  }

  /** Escribir salta a la opción que empieza con lo escrito (se olvida a los 600 ms). */
  _buscar(letra) {
    clearTimeout(this._temporizador)
    this._buscado += normalizar(letra)
    this._temporizador = setTimeout(() => (this._buscado = ''), ESPERA_TECLAS_MS)
    const items = this._items
    const desde = this._abierto ? this._activo : items.findIndex((o) => String(o.valor) === String(this.valor ?? ''))
    // * Una sola letra repetida recorre las que empiezan igual; varias afinan la búsqueda desde la actual
    const orden = [...items.keys()].map((i) => (i + Math.max(desde, 0) + (this._buscado.length === 1 ? 1 : 0)) % items.length)
    const encontrado = orden.find((i) => normalizar(items[i].etiqueta).startsWith(this._buscado))
    if (encontrado === undefined) return
    if (this._abierto) this._activo = encontrado
    else if (!this.multiple) this._elegir(encontrado)
  }

  _alSalir(e) {
    // * Si el foco pasó a algo de este mismo componente no se avisa
    if (e.relatedTarget && this.renderRoot.contains(e.relatedTarget)) return
    this._cerrar()
    this.dispatchEvent(new CustomEvent('dh-blur', { bubbles: true, composed: true }))
  }

  // ------------------------------------------------------------------ render

  render() {
    if (this.multiple) return this._renderMultiple()
    const elegida = this._elegida
    return html`
      <button
        type="button"
        class="disparador"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded=${String(this._abierto)}
        aria-controls="lista"
        aria-label=${this.etiqueta || nothing}
        aria-invalid=${this.invalido ? 'true' : nothing}
        ?disabled=${this.deshabilitado}
        @click=${() => (this._abierto ? this._cerrar() : this._abrir())}
        @keydown=${(e) => this._alTeclear(e)}
        @focusout=${(e) => this._alSalir(e)}
      >
        <span class="texto ${elegida ? '' : 'vacio'}">${elegida ? elegida.etiqueta : this.placeholder}</span>
        <span class="flecha">${icono('chevron-down', 18)}</span>
      </button>
      ${this._abierto ? this._renderLista() : nothing}
    `
  }

  _renderMultiple() {
    const marcadas = this._marcadas
    const visibles = marcadas.slice(0, MAX_FICHAS)
    const resto = marcadas.length - visibles.length
    return html`
      <div
        class="disparador"
        role="combobox"
        tabindex=${this.deshabilitado ? '-1' : '0'}
        aria-haspopup="listbox"
        aria-expanded=${String(this._abierto)}
        aria-controls="lista"
        aria-label=${this.etiqueta || nothing}
        aria-invalid=${this.invalido ? 'true' : nothing}
        aria-disabled=${this.deshabilitado ? 'true' : 'false'}
        @click=${() => (this._abierto ? this._cerrar() : this._abrir())}
        @keydown=${(e) => this._alTeclear(e)}
        @focusout=${(e) => this._alSalir(e)}
      >
        ${marcadas.length === 0
          ? html`<span class="texto vacio">${this.placeholder}</span>`
          : html`<span class="fichas">
              ${visibles.map(
                (o) => html`<span class="ficha">
                  ${o.etiqueta}
                  <button
                    type="button"
                    tabindex="-1"
                    aria-label=${`Quitar ${o.etiqueta}`}
                    ?disabled=${this.deshabilitado}
                    @click=${(e) => {
                      e.stopPropagation()
                      this._alternar(String(o.valor))
                    }}
                  >
                    ${icono('x', 12)}
                  </button>
                </span>`,
              )}
              ${resto > 0 ? html`<span class="ficha mas">+${resto}</span>` : nothing}
            </span>`}
        <span class="flecha">${icono('chevron-down', 18)}</span>
      </div>
      ${this._abierto ? this._renderLista() : nothing}
    `
  }

  _renderLista() {
    const p = this._posicion
    const estilo = [
      `left: ${p.izquierda}px`,
      `width: ${p.ancho}px`,
      `max-height: ${p.alto}px`,
      p.arriba ? `bottom: ${p.abajo}px` : `top: ${p.superior}px`,
    ].join('; ')
    const items = this._items
    return html`<ul id="lista" class="lista ${p.arriba ? 'arriba' : ''}" role="listbox" aria-multiselectable=${this.multiple ? 'true' : nothing} style=${estilo}>
      ${items.length === 0
        ? html`<li class="sin-opciones" role="presentation">Sin opciones</li>`
        : items.map((o, i) => {
            const elegida = this._esElegida(o)
            return html`<li
              class="opcion ${i === this._activo ? 'activa' : ''} ${elegida ? 'elegida' : ''} ${o.vacio ? 'placeholder' : ''}"
              role="option"
              aria-selected=${String(elegida)}
              @mousedown=${(e) => e.preventDefault()}
              @mousemove=${() => (this._activo = i)}
              @click=${() => this._elegir(i)}
            >
              <span>${o.etiqueta}</span>
              ${this.multiple
                ? html`<span class="casilla ${elegida ? 'marcada' : ''}">${elegida ? icono('check', 12) : nothing}</span>`
                : elegida
                  ? html`<span class="marca">${icono('check', 16)}</span>`
                  : nothing}
            </li>`
          })}
    </ul>`
  }
}
