import { LitElement, css, html, nothing } from 'lit'

import { base } from './comunes.js'
import { icono } from './iconos.js'

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']
const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
// * La semana empieza en lunes
const DIAS_SEMANA = ['L', 'M', 'X', 'J', 'V', 'S', 'D']
const ANIOS_POR_PAGINA = 12
const ALTO_ESTIMADO_PX = 360
const MARGEN_PX = 8

const dos = (n) => String(n).padStart(2, '0')

/** 'yyyy-MM-dd' de un año, mes (0-11) y día. */
const clave = (anio, mes, dia) => `${String(anio).padStart(4, '0')}-${dos(mes + 1)}-${dos(dia)}`

/** Descompone 'yyyy-MM-dd' en { anio, mes (0-11), dia } o null si no es una fecha real del calendario. */
function partes(valor) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(valor ?? ''))
  if (!m) return null
  const anio = Number(m[1])
  const mes = Number(m[2]) - 1
  const dia = Number(m[3])
  const fecha = new Date(Date.UTC(anio, mes, dia))
  // * Rechaza el 31 de febrero, el mes 13, etc.: el Date las "corrige" y ya no coinciden
  if (fecha.getUTCFullYear() !== anio || fecha.getUTCMonth() !== mes || fecha.getUTCDate() !== dia) return null
  return { anio, mes, dia }
}

const diasDelMes = (anio, mes) => new Date(Date.UTC(anio, mes + 1, 0)).getUTCDate()

/** Día de la semana con lunes = 0 y domingo = 6. */
const diaDeSemana = (anio, mes, dia) => (new Date(Date.UTC(anio, mes, dia)).getUTCDay() + 6) % 7

/** Suma días a 'yyyy-MM-dd' (negativos restan). */
function sumarDias(valor, dias) {
  const p = partes(valor)
  const f = new Date(Date.UTC(p.anio, p.mes, p.dia + dias))
  return clave(f.getUTCFullYear(), f.getUTCMonth(), f.getUTCDate())
}

/** Suma meses sin salirse del mes destino (el 31 de enero + 1 mes = 28/29 de febrero). */
function sumarMeses(valor, meses) {
  const p = partes(valor)
  const total = p.anio * 12 + p.mes + meses
  const anio = Math.floor(total / 12)
  const mes = ((total % 12) + 12) % 12
  return clave(anio, mes, Math.min(p.dia, diasDelMes(anio, mes)))
}

/** 'yyyy-MM-dd' -> 'dd/MM/yyyy' ('' si no es una fecha). */
export function formatearFecha(valor) {
  const p = partes(valor)
  return p ? `${dos(p.dia)}/${dos(p.mes + 1)}/${String(p.anio).padStart(4, '0')}` : ''
}

/** 'dd/MM/yyyy' -> 'yyyy-MM-dd', o null si no es una fecha real. */
export function analizarFecha(texto) {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(String(texto ?? '').trim())
  if (!m) return null
  const valor = `${m[3]}-${m[2]}-${m[1]}`
  return partes(valor) ? valor : null
}

/** Solo dígitos, con las barras puestas solas: '04051' -> '04/05/1'. */
function enmascarar(texto) {
  const d = String(texto ?? '').replace(/\D/g, '').slice(0, 8)
  if (d.length <= 2) return d
  if (d.length <= 4) return `${d.slice(0, 2)}/${d.slice(2)}`
  return `${d.slice(0, 2)}/${d.slice(2, 4)}/${d.slice(4)}`
}

function hoy() {
  const f = new Date()
  return clave(f.getFullYear(), f.getMonth(), f.getDate())
}

/**
 * <dh-fecha>: selector de fecha propio de DriveHub. Reemplaza al `<input type="date">` nativo, cuyo
 * calendario lo dibuja el navegador y no sigue el diseño Asfalto.
 *
 * Propiedades
 *   valor          'yyyy-MM-dd' ('' = sin fecha)
 *   min, max       límites 'yyyy-MM-dd' (inclusive); los días fuera de rango no se pueden elegir
 *   placeholder    por defecto "dd/mm/aaaa"
 *   etiqueta       nombre accesible del control
 *   deshabilitado  no se escribe ni se abre
 *   invalido       borde de error
 *
 * Eventos (burbujean y atraviesan el Shadow DOM)
 *   dh-cambio   { valor }   eligió o escribió una fecha válida, o la borró ('')
 *   dh-blur                 el foco salió del control (para marcar el campo como tocado)
 *
 * Se puede ESCRIBIR la fecha (las barras se ponen solas) o elegirla en el calendario. Lo escrito no
 * se limita a `min`/`max` (eso solo apaga días del calendario): validar el rango es del formulario. El calendario
 * tiene tres vistas: días, meses (clic en el mes) y años (clic en el año): así una fecha de
 * nacimiento se elige en tres clics. Si no hay fecha, abre cerca de hoy o, si hoy queda fuera de
 * `min`/`max`, en el límite más cercano.
 *
 * Teclado (con el calendario abierto): flechas ±1 día y ±7 días, Inicio y Fin (lunes y domingo),
 * RePág y AvPág ±1 mes (con Mayús, ±1 año), Enter elige, Escape cierra (sin llegar a quien escuche
 * Escape más arriba, como el panel que contiene el campo).
 *
 * * El calendario usa `position: fixed` según el control: no lo recorta el `overflow` de un panel
 * * con scroll y se abre hacia arriba si abajo no entra.
 * ! No usar `<input type="date">` en la web: si falta algo, se agrega acá para todos.
 */
export class DhFecha extends LitElement {
  static properties = {
    valor: { type: String },
    min: { type: String },
    max: { type: String },
    placeholder: { type: String },
    etiqueta: { type: String },
    deshabilitado: { type: Boolean, reflect: true },
    invalido: { type: Boolean, reflect: true },
    _texto: { state: true },
    _abierto: { state: true },
    _vista: { state: true },
    _foco: { state: true },
    _paginaAnios: { state: true },
    _posicion: { state: true },
  }

  static styles = [
    base,
    css`
      :host {
        display: block;
        min-width: 0;
      }
      .campo {
        position: relative;
        display: flex;
        align-items: center;
      }
      input {
        font: inherit;
        width: 100%;
        height: var(--dh-fecha-alto, 2.75rem);
        padding: 0 2.9rem 0 0.9rem;
        color: var(--foreground);
        background: var(--card);
        border: 1px solid var(--input);
        border-radius: var(--radius);
        transition: border-color 0.2s, box-shadow 0.2s;
      }
      input::placeholder {
        color: color-mix(in srgb, var(--gris-medio) 70%, transparent);
      }
      input:hover:not(:disabled) {
        border-color: color-mix(in srgb, var(--gris-medio) 55%, transparent);
      }
      input:focus,
      input[aria-expanded='true'] {
        outline: none;
        border-color: var(--ambar-vial);
        box-shadow: 0 0 0 4px color-mix(in srgb, var(--ambar-vial) 22%, transparent);
      }
      :host([invalido]) input {
        border-color: var(--destructive);
        box-shadow: 0 0 0 4px color-mix(in srgb, var(--destructive) 14%, transparent);
      }
      input:disabled {
        cursor: not-allowed;
        background: var(--muted);
        color: var(--muted-foreground);
      }
      .abrir {
        position: absolute;
        right: 0.35rem;
        width: 2.2rem;
        height: 2.2rem;
        display: grid;
        place-items: center;
        border-radius: calc(var(--radius) * 0.7);
        color: var(--muted-foreground);
      }
      .abrir:hover:not(:disabled) {
        background: var(--accent);
        color: var(--foreground);
      }
      .abrir:disabled {
        cursor: not-allowed;
      }
      .calendario {
        position: fixed;
        z-index: 300;
        width: 18.5rem;
        padding: 0.75rem;
        background: var(--popover);
        color: var(--popover-foreground);
        border: 1px solid var(--border);
        border-radius: calc(var(--radius) * 1.2);
        box-shadow: var(--shadow-2);
        animation: abrir 0.16s var(--ease);
      }
      .calendario.arriba {
        transform-origin: bottom;
      }
      .cabeza {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.25rem;
        margin-bottom: 0.5rem;
      }
      .titulo {
        flex: 1;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.25rem;
        height: 2.2rem;
        padding: 0 0.6rem;
        font-weight: 700;
        text-transform: capitalize;
        border-radius: calc(var(--radius) * 0.7);
      }
      .titulo:hover:not(:disabled) {
        background: var(--accent);
      }
      .nav {
        width: 2.2rem;
        height: 2.2rem;
        display: grid;
        place-items: center;
        border-radius: calc(var(--radius) * 0.7);
        color: var(--muted-foreground);
      }
      .nav:hover:not(:disabled) {
        background: var(--accent);
        color: var(--foreground);
      }
      .nav:disabled {
        opacity: 0.35;
        cursor: not-allowed;
      }
      .semana,
      .dias {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 2px;
      }
      .semana span {
        display: grid;
        place-items: center;
        height: 1.8rem;
        font-size: 0.72rem;
        font-weight: 700;
        color: var(--muted-foreground);
      }
      .dia,
      .celda {
        display: grid;
        place-items: center;
        border-radius: calc(var(--radius) * 0.7);
        font-size: 0.875rem;
      }
      .dia {
        height: 2.2rem;
      }
      .celdas {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 4px;
      }
      .celda {
        height: 2.8rem;
        text-transform: capitalize;
      }
      .dia:hover:not(:disabled),
      .celda:hover:not(:disabled),
      .dia.foco,
      .celda.foco {
        background: var(--accent);
        color: var(--accent-foreground);
      }
      .dia.hoy,
      .celda.hoy {
        box-shadow: inset 0 0 0 1.5px var(--ambar-vial);
      }
      .dia.elegido,
      .celda.elegido {
        background: var(--primary);
        color: var(--primary-foreground);
        font-weight: 700;
      }
      .dia.otro-mes {
        color: var(--muted-foreground);
      }
      .dia:disabled,
      .celda:disabled {
        opacity: 0.3;
        cursor: not-allowed;
      }
      .pie {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
        margin-top: 0.5rem;
        padding-top: 0.5rem;
        border-top: 1px solid var(--border);
      }
      .enlace {
        height: 2rem;
        padding: 0 0.6rem;
        font-size: 0.8125rem;
        font-weight: 650;
        color: var(--info);
        border-radius: calc(var(--radius) * 0.7);
      }
      .enlace:hover:not(:disabled) {
        background: var(--accent);
      }
      .enlace:disabled {
        opacity: 0.35;
        cursor: not-allowed;
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
    this.min = ''
    this.max = ''
    this.placeholder = 'dd/mm/aaaa'
    this.etiqueta = ''
    this.deshabilitado = false
    this.invalido = false
    this._texto = ''
    this._abierto = false
    this._vista = 'dias'
    this._foco = ''
    this._paginaAnios = 0
    this._posicion = null
    this._fuera = (e) => {
      if (!e.composedPath().includes(this)) this._cerrar()
    }
    this._alMoverse = (e) => {
      if (e.type === 'scroll' && e.composedPath().includes(this)) return
      this._cerrar()
    }
  }

  willUpdate(cambios) {
    // * El valor puede cambiar desde afuera (un formulario que carga un registro)
    if (cambios.has('valor')) this._texto = formatearFecha(this.valor)
  }

  disconnectedCallback() {
    this._escuchar(false)
    super.disconnectedCallback()
  }

  _escuchar(activar) {
    const accion = activar ? 'addEventListener' : 'removeEventListener'
    document[accion]('pointerdown', this._fuera, true)
    window[accion]('resize', this._alMoverse)
    window[accion]('scroll', this._alMoverse, true)
  }

  // ------------------------------------------------------------------ rango

  _fueraDeRango(valor) {
    return (!!this.min && valor < this.min) || (!!this.max && valor > this.max)
  }

  /** Lleva una fecha al rango permitido. */
  _acotar(valor) {
    if (this.min && valor < this.min) return this.min
    if (this.max && valor > this.max) return this.max
    return valor
  }

  /** ¿Algún día del mes (o del año si `mes` es null) entra en el rango? */
  _hayDiasPermitidos(anio, mes) {
    const desde = mes === null ? clave(anio, 0, 1) : clave(anio, mes, 1)
    const hasta = mes === null ? clave(anio, 11, 31) : clave(anio, mes, diasDelMes(anio, mes))
    return !((this.min && hasta < this.min) || (this.max && desde > this.max))
  }

  // ------------------------------------------------------------------ abrir, cerrar, elegir

  _abrir() {
    if (this.deshabilitado || this._abierto) return
    const campo = this.renderRoot.querySelector('.campo')
    const caja = campo.getBoundingClientRect()
    const abajo = window.innerHeight - caja.bottom - MARGEN_PX
    const arriba = caja.top - MARGEN_PX
    const haciaArriba = abajo < ALTO_ESTIMADO_PX && arriba > abajo
    const ancho = 18.5 * (parseFloat(getComputedStyle(document.documentElement).fontSize) || 16)
    this._posicion = {
      arriba: haciaArriba,
      // * Que no se salga por la derecha de la pantalla
      izquierda: Math.max(MARGEN_PX, Math.min(caja.left, window.innerWidth - ancho - MARGEN_PX)),
      ...(haciaArriba ? { abajo: window.innerHeight - caja.top + 4 } : { superior: caja.bottom + 4 }),
    }
    this._foco = this._acotar(partes(this.valor) ? this.valor : hoy())
    this._vista = 'dias'
    this._abierto = true
    this._escuchar(true)
  }

  _cerrar() {
    if (!this._abierto) return
    this._abierto = false
    this._escuchar(false)
  }

  _cambiarA(valor) {
    const nuevo = valor ?? ''
    if (nuevo === (this.valor ?? '')) return
    this.valor = nuevo
    this.dispatchEvent(new CustomEvent('dh-cambio', { detail: { valor: nuevo }, bubbles: true, composed: true }))
  }

  _elegir(valor) {
    if (this._fueraDeRango(valor)) return
    this._cerrar()
    this._texto = formatearFecha(valor)
    this._cambiarA(valor)
    this.renderRoot.querySelector('input')?.focus()
  }

  _limpiar() {
    this._cerrar()
    this._texto = ''
    this._cambiarA('')
    this.renderRoot.querySelector('input')?.focus()
  }

  // ------------------------------------------------------------------ escribir

  _alEscribir(e) {
    const texto = enmascarar(e.target.value)
    this._texto = texto
    // * El Lit no vuelve a pintar el input si el texto enmascarado ya era el de antes
    e.target.value = texto
    // * Una fecha real escrita a mano se acepta aunque caiga fuera de min/max: el formulario le dice a la
    // * persona qué regla no cumple (un mensaje es más útil que volver a la fecha anterior sin explicar).
    const valor = analizarFecha(texto)
    if (valor) {
      this._cambiarA(valor)
      if (this._abierto) this._foco = valor
    }
  }

  /** Al salir: un texto vacío borra la fecha; uno incompleto o fuera de rango vuelve a la última fecha buena. */
  _alSalir(e) {
    if (e.relatedTarget && this.renderRoot.contains(e.relatedTarget)) return
    this._cerrar()
    if (this._texto === '') this._cambiarA('')
    else if (this._texto !== formatearFecha(this.valor)) this._texto = formatearFecha(this.valor)
    this.dispatchEvent(new CustomEvent('dh-blur', { bubbles: true, composed: true }))
  }

  // ------------------------------------------------------------------ teclado

  _alTeclear(e) {
    if (this.deshabilitado) return
    if (e.key === 'Escape') {
      if (!this._abierto) return
      e.preventDefault()
      e.stopPropagation()
      this._cerrar()
      return
    }
    if (e.key === 'Tab') {
      this._cerrar()
      return
    }
    if (!this._abierto) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault()
        this._abrir()
      }
      return
    }
    if (this._vista !== 'dias') return this._teclearVistaLarga(e)

    const salto = {
      ArrowLeft: () => sumarDias(this._foco, -1),
      ArrowRight: () => sumarDias(this._foco, 1),
      ArrowUp: () => sumarDias(this._foco, -7),
      ArrowDown: () => sumarDias(this._foco, 7),
      PageUp: () => sumarMeses(this._foco, e.shiftKey ? -12 : -1),
      PageDown: () => sumarMeses(this._foco, e.shiftKey ? 12 : 1),
      Home: () => {
        const p = partes(this._foco)
        return sumarDias(this._foco, -diaDeSemana(p.anio, p.mes, p.dia))
      },
      End: () => {
        const p = partes(this._foco)
        return sumarDias(this._foco, 6 - diaDeSemana(p.anio, p.mes, p.dia))
      },
    }[e.key]
    if (salto) {
      e.preventDefault()
      this._foco = this._acotar(salto())
      return
    }
    if (e.key === 'Enter') {
      e.preventDefault()
      this._elegir(this._foco)
    }
  }

  /** En las vistas de meses y años las flechas mueven de a uno (y de a 3 en vertical) y Enter baja una vista. */
  _teclearVistaLarga(e) {
    const p = partes(this._foco)
    const paso = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -3, ArrowDown: 3 }[e.key]
    if (paso !== undefined) {
      e.preventDefault()
      this._foco =
        this._vista === 'meses' ? sumarMeses(this._foco, paso) : clave(p.anio + paso, p.mes, Math.min(p.dia, diasDelMes(p.anio + paso, p.mes)))
      this._paginaAnios = Math.floor(partes(this._foco).anio / ANIOS_POR_PAGINA)
      return
    }
    if (e.key === 'Enter') {
      e.preventDefault()
      this._bajarVista(this._vista === 'meses' ? partes(this._foco).mes : partes(this._foco).anio)
    }
  }

  // ------------------------------------------------------------------ navegación del calendario

  _subirVista() {
    if (this._vista === 'dias') this._vista = 'meses'
    else if (this._vista === 'meses') {
      this._paginaAnios = Math.floor(partes(this._foco).anio / ANIOS_POR_PAGINA)
      this._vista = 'anios'
    }
  }

  /** Elegir un mes (o un año) vuelve a la vista de abajo, con el día más cercano permitido. */
  _bajarVista(elegido) {
    const p = partes(this._foco)
    if (this._vista === 'anios') {
      this._foco = this._acotar(clave(elegido, p.mes, Math.min(p.dia, diasDelMes(elegido, p.mes))))
      this._vista = 'meses'
    } else {
      this._foco = this._acotar(clave(p.anio, elegido, Math.min(p.dia, diasDelMes(p.anio, elegido))))
      this._vista = 'dias'
    }
  }

  _anterior() {
    this._mover(-1)
  }

  _siguiente() {
    this._mover(1)
  }

  _mover(sentido) {
    if (this._vista === 'dias') this._foco = sumarMeses(this._foco, sentido)
    else if (this._vista === 'meses') this._foco = sumarMeses(this._foco, 12 * sentido)
    else this._paginaAnios += sentido
  }

  /** ¿Se puede ir en ese sentido? (hay algún día permitido en el mes, año o página de años de destino). */
  _puedeMover(sentido) {
    const p = partes(this._foco)
    if (this._vista === 'dias') {
      const destino = partes(sumarMeses(this._foco, sentido))
      return this._hayDiasPermitidos(destino.anio, destino.mes)
    }
    if (this._vista === 'meses') return this._hayDiasPermitidos(p.anio + sentido, null)
    const primero = (this._paginaAnios + sentido) * ANIOS_POR_PAGINA
    return Array.from({ length: ANIOS_POR_PAGINA }, (_, i) => primero + i).some((a) => this._hayDiasPermitidos(a, null))
  }

  // ------------------------------------------------------------------ render

  render() {
    return html`
      <div class="campo">
        <input
          type="text"
          inputmode="numeric"
          autocomplete="off"
          role="combobox"
          aria-haspopup="dialog"
          aria-expanded=${String(this._abierto)}
          aria-controls="calendario"
          aria-label=${this.etiqueta || nothing}
          aria-invalid=${this.invalido ? 'true' : nothing}
          placeholder=${this.placeholder}
          maxlength="10"
          .value=${this._texto}
          ?disabled=${this.deshabilitado}
          @input=${(e) => this._alEscribir(e)}
          @keydown=${(e) => this._alTeclear(e)}
          @focusout=${(e) => this._alSalir(e)}
        />
        <button
          type="button"
          class="abrir"
          tabindex="-1"
          aria-label="Abrir calendario"
          ?disabled=${this.deshabilitado}
          @mousedown=${(e) => e.preventDefault()}
          @click=${() => {
            if (this._abierto) this._cerrar()
            else {
              this.renderRoot.querySelector('input')?.focus()
              this._abrir()
            }
          }}
        >
          ${icono('calendar', 18)}
        </button>
      </div>
      ${this._abierto ? this._renderCalendario() : nothing}
    `
  }

  _renderCalendario() {
    const p = this._posicion
    const estilo = [`left: ${p.izquierda}px`, p.arriba ? `bottom: ${p.abajo}px` : `top: ${p.superior}px`].join('; ')
    const foco = partes(this._foco)
    const titulo =
      this._vista === 'dias'
        ? `${MESES[foco.mes]} ${foco.anio}`
        : this._vista === 'meses'
          ? String(foco.anio)
          : `${this._paginaAnios * ANIOS_POR_PAGINA} – ${this._paginaAnios * ANIOS_POR_PAGINA + ANIOS_POR_PAGINA - 1}`
    return html`<div
      id="calendario"
      class="calendario ${p.arriba ? 'arriba' : ''}"
      role="dialog"
      aria-label="Calendario"
      style=${estilo}
      @mousedown=${(e) => e.preventDefault()}
    >
      <div class="cabeza">
        <button type="button" class="nav" aria-label="Anterior" ?disabled=${!this._puedeMover(-1)} @click=${() => this._anterior()}>
          ${icono('chevron-left', 18)}
        </button>
        <button
          type="button"
          class="titulo"
          aria-live="polite"
          ?disabled=${this._vista === 'anios'}
          @click=${() => this._subirVista()}
        >
          ${titulo}${this._vista === 'anios' ? nothing : icono('chevron-down', 16)}
        </button>
        <button type="button" class="nav" aria-label="Siguiente" ?disabled=${!this._puedeMover(1)} @click=${() => this._siguiente()}>
          ${icono('chevron-right', 18)}
        </button>
      </div>
      ${this._vista === 'dias' ? this._renderDias(foco) : this._vista === 'meses' ? this._renderMeses(foco) : this._renderAnios(foco)}
      <div class="pie">
        <button type="button" class="enlace" ?disabled=${this._fueraDeRango(hoy())} @click=${() => this._elegir(hoy())}>Hoy</button>
        <button type="button" class="enlace" ?disabled=${!this.valor} @click=${() => this._limpiar()}>Borrar</button>
      </div>
    </div>`
  }

  _renderDias(foco) {
    const primero = diaDeSemana(foco.anio, foco.mes, 1)
    const total = diasDelMes(foco.anio, foco.mes)
    const celdas = []
    // * Los días de los meses vecinos rellenan la primera y la última semana, y también se pueden elegir
    for (let i = primero; i > 0; i--) celdas.push({ valor: sumarDias(clave(foco.anio, foco.mes, 1), -i), otro: true })
    for (let d = 1; d <= total; d++) celdas.push({ valor: clave(foco.anio, foco.mes, d), otro: false })
    const resto = (7 - (celdas.length % 7)) % 7
    for (let i = 1; i <= resto; i++) celdas.push({ valor: sumarDias(clave(foco.anio, foco.mes, total), i), otro: true })
    const hoyClave = hoy()
    return html`
      <div class="semana" aria-hidden="true">${DIAS_SEMANA.map((d) => html`<span>${d}</span>`)}</div>
      <div class="dias" role="grid">
        ${celdas.map((c) => {
          const numero = partes(c.valor).dia
          return html`<button
            type="button"
            role="gridcell"
            class="dia ${c.otro ? 'otro-mes' : ''} ${c.valor === this.valor ? 'elegido' : ''} ${c.valor === hoyClave ? 'hoy' : ''} ${c.valor === this._foco ? 'foco' : ''}"
            aria-label=${formatearFecha(c.valor)}
            aria-selected=${String(c.valor === this.valor)}
            ?disabled=${this._fueraDeRango(c.valor)}
            @click=${() => this._elegir(c.valor)}
          >
            ${numero}
          </button>`
        })}
      </div>
    `
  }

  _renderMeses(foco) {
    const hoyP = partes(hoy())
    const actual = partes(this.valor)
    return html`<div class="celdas">
      ${MESES_CORTOS.map(
        (nombre, mes) => html`<button
          type="button"
          class="celda ${actual && actual.anio === foco.anio && actual.mes === mes ? 'elegido' : ''} ${hoyP.anio === foco.anio && hoyP.mes === mes ? 'hoy' : ''} ${mes === foco.mes ? 'foco' : ''}"
          ?disabled=${!this._hayDiasPermitidos(foco.anio, mes)}
          @click=${() => this._bajarVista(mes)}
        >
          ${nombre}
        </button>`,
      )}
    </div>`
  }

  _renderAnios(foco) {
    const hoyP = partes(hoy())
    const actual = partes(this.valor)
    const primero = this._paginaAnios * ANIOS_POR_PAGINA
    return html`<div class="celdas">
      ${Array.from({ length: ANIOS_POR_PAGINA }, (_, i) => primero + i).map(
        (anio) => html`<button
          type="button"
          class="celda ${actual && actual.anio === anio ? 'elegido' : ''} ${hoyP.anio === anio ? 'hoy' : ''} ${anio === foco.anio ? 'foco' : ''}"
          ?disabled=${!this._hayDiasPermitidos(anio, null)}
          @click=${() => this._bajarVista(anio)}
        >
          ${anio}
        </button>`,
      )}
    </div>`
  }
}
