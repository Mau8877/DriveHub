import { icono } from '@drivehub/ui'
import { LitElement, html, nothing } from 'lit'

import { ErrorApi, solicitar } from './cliente-api.js'
import { estilos } from './estilos.js'
import { slugDesdeTexto, validar } from './reglas.js'

const ESPERA_BUSQUEDA_MS = 300

/** Lee un valor de un objeto por ruta con puntos ('administrador.nombres'). */
function leer(objeto, ruta) {
  return ruta.split('.').reduce((actual, clave) => (actual == null ? undefined : actual[clave]), objeto)
}

/** Escribe un valor en un objeto por ruta con puntos, creando los niveles que falten. */
function escribir(objeto, ruta, valor) {
  const claves = ruta.split('.')
  let actual = objeto
  claves.slice(0, -1).forEach((clave) => {
    actual[clave] ??= {}
    actual = actual[clave]
  })
  actual[claves[claves.length - 1]] = valor
}

function cumple(condicion, fila) {
  if (!condicion) return true
  const valor = leer(fila, condicion.campo)
  if ('igual' in condicion) return valor === condicion.igual
  if ('distinto' in condicion) return valor !== condicion.distinto
  return true
}

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

/** '2026-05-04...' -> '04 may 2026' (formato del diseño; sin depender del idioma del navegador). */
function formatearFecha(valor) {
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return String(valor)
  return `${String(fecha.getUTCDate()).padStart(2, '0')} ${MESES[fecha.getUTCMonth()]} ${fecha.getUTCFullYear()}`
}

function camposDe(secciones) {
  return secciones.flatMap((seccion) => seccion.campos)
}

/**
 * <dh-gestion>: pantalla de gestión completa (listado con búsqueda, filtros y paginación; crear;
 * editar; acciones por fila) configurada por un esquema. Es UNA implementación para Angular y
 * React: se usa igual en los dos (ver GESTION_FRONTEND.md y el README del paquete).
 *
 * * La URL de la API y el token salen del contexto del shell (@drivehub/mf-contract).
 * * Maneja sus vistas por dentro (lista, crear, editar): el framework solo la monta en UNA ruta.
 * * Los colores salen de @drivehub/design-tokens a través del Shadow DOM.
 */
/** Ancho de una columna: el del esquema, o uno corto para insignias y fechas. El resto reparte lo que sobra. */
function anchoColumna(c) {
  const ancho = c.ancho ?? (c.tipo === 'insignia' || c.tipo === 'fecha' ? '8rem' : '')
  return ancho ? `width: ${ancho}` : ''
}

export class DhGestion extends LitElement {
  static properties = {
    /** @type {import('./index.js').EsquemaGestion} */
    esquema: { attribute: false },
    _vista: { state: true },
    _filas: { state: true },
    _meta: { state: true },
    _cargando: { state: true },
    _errorCarga: { state: true },
    _aviso: { state: true },
    _pagina: { state: true },
    _busqueda: { state: true },
    _filtros: { state: true },
    _confirmando: { state: true },
    _trabajando: { state: true },
    _valores: { state: true },
    _tocados: { state: true },
    _erroresServidor: { state: true },
    _errorGeneral: { state: true },
    _enviando: { state: true },
    _verPassword: { state: true },
    _cargandoRegistro: { state: true },
    _bloqueo: { state: true },
  }

  static styles = estilos

  constructor() {
    super()
    this.esquema = null
    this._reiniciar()
    this._temporizador = null
    this._temporizadorAviso = null
    this._tecla = (e) => {
      if (e.key !== 'Escape') return
      if (this._confirmando) this._confirmando = null
      else if (this._vista !== 'lista') this._cerrarDrawer()
    }
    this._idEditando = null
    this._manuales = new Set()
  }

  _reiniciar() {
    this._vista = 'lista'
    this._filas = []
    this._meta = null
    this._cargando = true
    this._errorCarga = null
    this._aviso = null
    this._pagina = 1
    this._busqueda = ''
    this._filtros = {}
    this._confirmando = null
    this._trabajando = null
    this._valores = {}
    this._tocados = new Set()
    this._erroresServidor = {}
    this._errorGeneral = null
    this._enviando = false
    this._verPassword = {}
    this._cargandoRegistro = false
    this._bloqueo = null
  }

  updated(cambios) {
    // * El aviso (toast) se va solo a los pocos segundos
    if (cambios.has('_aviso') && this._aviso) {
      clearTimeout(this._temporizadorAviso)
      this._temporizadorAviso = setTimeout(() => (this._aviso = null), 3600)
    }
  }

  willUpdate(cambios) {
    if (cambios.has('esquema') && this.esquema) {
      this._reiniciar()
      this._cargar()
    }
  }

  connectedCallback() {
    super.connectedCallback()
    document.addEventListener('keydown', this._tecla)
  }

  disconnectedCallback() {
    document.removeEventListener('keydown', this._tecla)
    clearTimeout(this._temporizador)
    clearTimeout(this._temporizadorAviso)
    super.disconnectedCallback()
  }

  get _id() {
    return this.esquema.idCampo ?? 'codigo'
  }

  // ------------------------------------------------------------------ listado

  async _cargar() {
    const esquema = this.esquema
    this._cargando = true
    this._errorCarga = null
    try {
      const { data, meta } = await solicitar('GET', esquema.recurso, {
        params: {
          page: this._pagina,
          pageSize: esquema.porPagina ?? 20,
          search: this._busqueda.trim(),
          ...this._filtros,
        },
      })
      if (esquema !== this.esquema) return
      this._filas = data ?? []
      this._meta = meta ?? null
    } catch (error) {
      if (esquema !== this.esquema) return
      this._errorCarga = error instanceof ErrorApi ? error.message : 'No pudimos cargar el listado.'
    } finally {
      if (esquema === this.esquema) this._cargando = false
    }
  }

  _alBuscar(texto) {
    clearTimeout(this._temporizador)
    this._temporizador = setTimeout(() => {
      if (texto === this._busqueda) return
      this._busqueda = texto
      this._pagina = 1
      this._cargar()
    }, ESPERA_BUSQUEDA_MS)
  }

  _alFiltrar(clave, valor) {
    const filtros = { ...this._filtros }
    if (valor) filtros[clave] = valor
    else delete filtros[clave]
    this._filtros = filtros
    this._pagina = 1
    this._cargar()
  }

  _irAPagina(pagina) {
    this._pagina = pagina
    this._cargar()
  }

  _hayFiltros() {
    return this._busqueda.trim() !== '' || Object.keys(this._filtros).length > 0
  }

  // ------------------------------------------------------------------ acciones por fila

  async _ejecutarAccion(accion, fila) {
    const id = fila[this._id]
    this._trabajando = id
    try {
      await solicitar(accion.metodo, `${this.esquema.recurso}/${accion.ruta.replace('{id}', encodeURIComponent(id))}`)
      this._aviso = { tipo: 'ok', texto: accion.mensajeExito }
    } catch (error) {
      this._aviso = {
        tipo: 'error',
        texto: error instanceof ErrorApi ? error.message : 'No pudimos completar la operación.',
      }
    } finally {
      this._trabajando = null
      this._confirmando = null
    }
    this._cargar()
  }

  _alPulsarAccion(accion, fila) {
    if (accion.confirmar) this._confirmando = { accion, fila }
    else this._ejecutarAccion(accion, fila)
  }

  // ------------------------------------------------------------------ formulario

  _abrirCrear() {
    this._prepararFormulario('crear')
    this._vista = 'crear'
  }

  async _abrirEditar(fila) {
    this._prepararFormulario('editar')
    this._idEditando = fila[this._id]
    this._vista = 'editar'
    const editar = this.esquema.editar
    this._cargandoRegistro = true
    try {
      const ruta = (editar.cargar ?? '{id}').replace('{id}', encodeURIComponent(this._idEditando))
      const { data } = await solicitar('GET', `${this.esquema.recurso}/${ruta}`)
      const valores = {}
      camposDe(editar.secciones).forEach((campo) => {
        valores[campo.campo] = leer(data, campo.campo) ?? ''
      })
      this._valores = valores
      this._registro = data
      if (editar.bloqueo && leer(data, editar.bloqueo.campo) === editar.bloqueo.valor) {
        this._bloqueo = editar.bloqueo.mensaje
      }
    } catch (error) {
      this._bloqueo = error instanceof ErrorApi ? error.message : 'No pudimos cargar el registro.'
    } finally {
      this._cargandoRegistro = false
    }
  }

  _prepararFormulario(modo) {
    this._valores = {}
    this._tocados = new Set()
    this._erroresServidor = {}
    this._errorGeneral = null
    this._enviando = false
    this._verPassword = {}
    this._bloqueo = null
    this._registro = null
    this._manuales = new Set()
    this._modo = modo
  }

  _cerrarDrawer() {
    this._vista = 'lista'
  }

  _volverALista(aviso) {
    this._vista = 'lista'
    this._aviso = aviso ?? null
    this._cargar()
  }

  _secciones() {
    return (this._vista === 'crear' ? this.esquema.crear : this.esquema.editar).secciones
  }

  _alEscribir(campo, texto) {
    const valores = { ...this._valores, [campo.campo]: texto }
    this._manuales.add(campo.campo)
    // * Un campo "sugerido de" otro se rellena solo mientras la persona no lo haya editado a mano
    camposDe(this._secciones()).forEach((otro) => {
      if (otro.sugeridoDe === campo.campo && !this._manuales.has(otro.campo)) {
        valores[otro.campo] = slugDesdeTexto(texto, otro.max)
      }
    })
    this._valores = valores
    const errores = { ...this._erroresServidor }
    delete errores[campo.campo]
    this._erroresServidor = errores
  }

  _tocar(campo) {
    this._tocados = new Set(this._tocados).add(campo.campo)
  }

  _errorDe(campo) {
    if (!this._tocados.has(campo.campo)) return null
    return validar(campo, this._valores[campo.campo]) ?? this._erroresServidor[campo.campo] ?? null
  }

  async _enviar(evento) {
    evento.preventDefault()
    this._errorGeneral = null
    this._erroresServidor = {}
    const secciones = this._secciones()
    const campos = camposDe(secciones).filter((c) => !c.soloLectura)
    const invalidos = campos.filter((c) => validar(c, this._valores[c.campo]))
    if (invalidos.length > 0) {
      this._tocados = new Set(campos.map((c) => c.campo))
      return
    }

    const cuerpo = {}
    campos.forEach((campo) => {
      const texto = String(this._valores[campo.campo] ?? '').trim()
      if (texto === '' && !campo.requerido) return
      // * Las contraseñas no se recortan: los espacios cuentan
      escribir(cuerpo, campo.campo, campo.tipo === 'password' ? this._valores[campo.campo] : texto)
    })

    const creando = this._vista === 'crear'
    const definicion = creando ? this.esquema.crear : this.esquema.editar
    const ruta = creando
      ? this.esquema.recurso
      : `${this.esquema.recurso}/${(definicion.ruta ?? '{id}').replace('{id}', encodeURIComponent(this._idEditando))}`
    this._enviando = true
    try {
      await solicitar(creando ? 'POST' : (definicion.metodo ?? 'PATCH'), ruta, { cuerpo })
      this._enviando = false
      this._volverALista({ tipo: 'ok', texto: definicion.mensajeExito })
    } catch (error) {
      this._enviando = false
      this._mostrarErrorDelServidor(error)
    }
  }

  _mostrarErrorDelServidor(error) {
    const codigo = error instanceof ErrorApi ? error.codigo : null
    const regla = codigo ? this.esquema.erroresDeCampo?.[codigo] : null
    if (regla) {
      this._tocados = new Set(this._tocados).add(regla.campo)
      this._erroresServidor = { [regla.campo]: regla.mensaje }
      return
    }
    this._errorGeneral =
      codigo === 'ERR_SYS_01'
        ? 'Revisa los datos ingresados e inténtalo de nuevo.'
        : (this.esquema.mensajes?.errorGuardar ?? 'No pudimos guardar. Inténtalo de nuevo más tarde.')
  }

  // ------------------------------------------------------------------ render

  render() {
    if (!this.esquema) return nothing
    return html`
      ${this._renderLista()}
      ${this._vista === 'lista' ? nothing : this._renderDrawer()}
      ${this._confirmando ? this._renderDialogo() : nothing}
      ${this._aviso ? this._renderToast() : nothing}
    `
  }

  _renderLista() {
    const esquema = this.esquema
    return html`
      <section class="pagina">
        <div class="cabecera">
          <div>
            <h1>${esquema.titulo}</h1>
            ${esquema.descripcion ? html`<p class="descripcion">${esquema.descripcion}</p>` : nothing}
          </div>
          ${esquema.crear
            ? html`<button type="button" class="boton" @click=${() => this._abrirCrear()}>
                ${icono('plus', 18)}<span>${esquema.crear.etiqueta}</span>
              </button>`
            : nothing}
        </div>

        <div class="tarjeta gestion">
          ${this._renderFiltros()}
          ${this._errorCarga
            ? html`<div class="estado error" role="alert">
                <p>${this._errorCarga}</p>
                <button type="button" class="enlace" @click=${() => this._cargar()}>Reintentar</button>
              </div>`
            : html`${this._renderTabla()}${this._renderPie()}`}
        </div>
      </section>
    `
  }

  _renderFiltros() {
    const esquema = this.esquema
    if (!esquema.busqueda && !esquema.filtros?.length) return nothing
    return html`
      <div class="barra">
        ${esquema.busqueda
          ? html`<label class="buscador">
              ${icono('search', 18)}
              <span class="solo-lectores">Buscar</span>
              <input
                type="search"
                placeholder=${esquema.busqueda.placeholder ?? 'Buscar'}
                autocomplete="off"
                .value=${this._busqueda}
                @input=${(e) => this._alBuscar(e.target.value)}
              />
            </label>`
          : nothing}
        ${(esquema.filtros ?? []).map((filtro) => this._renderFiltro(filtro))}
      </div>
    `
  }

  _renderFiltro(filtro) {
    const actual = this._filtros[filtro.clave] ?? ''
    // * Pocas opciones: control segmentado (como el diseño). Muchas: lista desplegable
    if (filtro.opciones.length > 4) {
      return html`<select
        aria-label=${filtro.titulo}
        .value=${actual}
        @change=${(e) => this._alFiltrar(filtro.clave, e.target.value)}
      >
        <option value="">${filtro.titulo}: todos</option>
        ${filtro.opciones.map((o) => html`<option value=${o.valor}>${o.etiqueta}</option>`)}
      </select>`
    }
    const opciones = [{ valor: '', etiqueta: filtro.todas ?? 'Todas' }, ...filtro.opciones]
    return html`<div class="segmentos" role="group" aria-label=${filtro.titulo}>
      ${opciones.map(
        (o) => html`<button
          type="button"
          aria-pressed=${String(actual === o.valor)}
          @click=${() => this._alFiltrar(filtro.clave, o.valor)}
        >
          ${o.etiqueta}
        </button>`
      )}
    </div>`
  }

  _renderTabla() {
    const esquema = this.esquema
    const porPagina = esquema.porPagina ?? 20
    const columnas = esquema.columnas.length + 2
    let cuerpo
    if (this._cargando) {
      cuerpo = Array.from({ length: 6 }).map(
        () => html`<tr class="esqueleto"><td colspan=${columnas}><span class="brillo"></span></td></tr>`
      )
    } else if (this._filas.length === 0) {
      const filtrado = this._hayFiltros()
      cuerpo = html`<tr>
        <td colspan=${columnas}>
          <div class="vacio">
            ${icono('search', 28)}
            <b>${filtrado ? 'Sin resultados' : 'Todavía no hay registros'}</b>
            <p>
              ${filtrado
                ? (esquema.mensajes?.sinResultados ?? 'Prueba con otro texto o quita los filtros.')
                : (esquema.mensajes?.vacio ?? 'Crea el primero para empezar.')}
            </p>
          </div>
        </td>
      </tr>`
    } else {
      cuerpo = this._filas.map(
        (fila, i) => html`<tr style=${`--i:${i}`}>
          <td class="nro" data-label="Nro">${(this._pagina - 1) * porPagina + i + 1}</td>
          ${esquema.columnas.map(
            (c) => html`<td
              class=${[c.principal ? 'principal' : '', c.secundaria ? 'secundaria' : ''].join(' ').trim()}
              data-label=${c.titulo}
            >
              ${this._celda(c, fila)}
            </td>`
          )}
          <td class="derecha">${this._renderAcciones(fila)}</td>
        </tr>`
      )
    }
    return html`<div class="contenedor-tabla">
      <table>
        <thead>
          <tr>
            <th scope="col" class="nro">Nro</th>
            ${esquema.columnas.map((c) => html`<th scope="col" style=${anchoColumna(c)}>${c.titulo}</th>`)}
            <th scope="col" class="derecha">Acciones</th>
          </tr>
        </thead>
        <tbody>
          ${cuerpo}
        </tbody>
      </table>
    </div>`
  }

  _celda(columna, fila) {
    const valor = leer(fila, columna.campo)
    if (columna.tipo === 'insignia') {
      const insignia = columna.insignias?.[valor]
      return html`<span class="insignia ${insignia?.tono ?? ''}">${insignia?.etiqueta ?? valor}</span>`
    }
    if (columna.tipo === 'fecha') return valor ? formatearFecha(valor) : ''
    if (columna.principal) {
      return html`<span class="principal-celda">
        ${columna.icono ? html`<span class="icono-fila">${icono(columna.icono, 18)}</span>` : nothing}
        <span class="nombre">${valor ?? ''}</span>
      </span>`
    }
    if (columna.codigo) return html`<code>${valor ?? ''}</code>`
    return valor ?? ''
  }

  _renderAcciones(fila) {
    const esquema = this.esquema
    const id = fila[this._id]
    const trabajando = this._trabajando === id
    const puedeEditar = esquema.editar && cumple(esquema.editar.visibleSi, fila)
    return html`<span class="acciones">
      ${puedeEditar
        ? html`<button type="button" class="enlace" @click=${() => this._abrirEditar(fila)}>
            ${icono(esquema.editar.icono ?? 'edit', 16)}<span>Editar</span>
          </button>`
        : nothing}
      ${(esquema.acciones ?? [])
        .filter((a) => cumple(a.visibleSi, fila))
        .map(
          (a) => html`<button
            type="button"
            class="enlace ${a.tono === 'peligro' ? 'peligro' : ''}"
            ?disabled=${trabajando}
            @click=${() => this._alPulsarAccion(a, fila)}
          >
            ${a.icono ? icono(a.icono, 16) : nothing}<span>${a.etiqueta}</span>
          </button>`
        )}
    </span>`
  }

  _renderPie() {
    const meta = this._meta
    if (this._cargando || !meta) return nothing
    const desde = (meta.page - 1) * meta.pageSize + 1
    const hasta = Math.min(meta.page * meta.pageSize, meta.totalItems)
    const botones = []
    for (let p = 1; p <= meta.totalPages; p++) {
      botones.push(html`<button
        type="button"
        class="pagina-btn"
        aria-current=${p === meta.page ? 'page' : nothing}
        @click=${() => this._irAPagina(p)}
      >
        ${p}
      </button>`)
    }
    return html`<div class="pie">
      <span class="secundaria">
        ${meta.totalItems ? `Mostrando ${desde}–${hasta} de ${meta.totalItems}` : '0 resultados'}
      </span>
      <div class="paginas">
        <button
          type="button"
          class="pagina-btn"
          aria-label="Página anterior"
          ?disabled=${meta.page <= 1}
          @click=${() => this._irAPagina(meta.page - 1)}
        >
          ${icono('chevron-left', 16)}
        </button>
        ${botones}
        <button
          type="button"
          class="pagina-btn"
          aria-label="Página siguiente"
          ?disabled=${meta.page >= meta.totalPages}
          @click=${() => this._irAPagina(meta.page + 1)}
        >
          ${icono('chevron-right', 16)}
        </button>
      </div>
    </div>`
  }

  // ------------------------------------------------------------------ drawer, diálogo y aviso

  _renderDrawer() {
    const creando = this._vista === 'crear'
    const definicion = creando ? this.esquema.crear : this.esquema.editar
    const titulo =
      definicion.titulo ?? (creando ? this.esquema.crear.etiqueta : `Editar ${this.esquema.nombreSingular ?? 'registro'}`)
    const numerar = definicion.secciones.length > 1
    return html`
      <div class="fondo" @mousedown=${(e) => e.target === e.currentTarget && this._cerrarDrawer()}>
        <aside class="drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-titulo">
          <header class="drawer-cabeza">
            <div>
              <h2 id="drawer-titulo">${titulo}</h2>
              ${definicion.descripcion ? html`<p class="secundaria">${definicion.descripcion}</p>` : nothing}
            </div>
            <button type="button" class="icono-btn" aria-label="Cerrar" @click=${() => this._cerrarDrawer()}>
              ${icono('x', 20)}
            </button>
          </header>
          ${this._cargandoRegistro
            ? html`<div class="drawer-cuerpo" aria-busy="true">
                <span class="brillo"></span><span class="brillo"></span><span class="brillo"></span>
              </div>`
            : this._bloqueo
              ? html`<div class="drawer-cuerpo">
                  <p class="aviso-caja error" role="alert">${icono('alert', 18)}<span>${this._bloqueo}</span></p>
                  <div class="drawer-pie">
                    <button type="button" class="boton secundario" @click=${() => this._cerrarDrawer()}>Cerrar</button>
                  </div>
                </div>`
              : html`<form class="drawer-cuerpo" @submit=${(e) => this._enviar(e)} novalidate>
                  ${definicion.secciones.map((seccion, i) => this._renderSeccion(seccion, numerar ? i + 1 : null))}
                  ${this._errorGeneral
                    ? html`<p class="aviso-caja error" role="alert">${icono('alert', 18)}<span>${this._errorGeneral}</span></p>`
                    : nothing}
                  <div class="drawer-pie">
                    <button type="button" class="boton secundario" @click=${() => this._cerrarDrawer()}>Cancelar</button>
                    <button type="submit" class="boton" ?disabled=${this._enviando}>
                      ${this._enviando ? 'Guardando…' : (definicion.etiquetaEnviar ?? (creando ? 'Crear' : 'Guardar'))}
                    </button>
                  </div>
                </form>`}
        </aside>
      </div>
    `
  }

  _renderDialogo() {
    const { accion, fila } = this._confirmando
    const peligro = accion.tono === 'peligro'
    return html`
      <div class="fondo centrado" @mousedown=${(e) => e.target === e.currentTarget && (this._confirmando = null)}>
        <div class="dialogo" role="alertdialog" aria-modal="true" aria-labelledby="dialogo-titulo">
          <div class="dialogo-ico ${peligro ? 'peligro' : ''}">${icono(peligro ? 'alert' : 'info', 22)}</div>
          <h2 id="dialogo-titulo">${accion.confirmar}</h2>
          ${accion.confirmarTexto ? html`<p>${accion.confirmarTexto}</p>` : nothing}
          <div class="dialogo-acciones">
            <button type="button" class="boton secundario" @click=${() => (this._confirmando = null)}>Cancelar</button>
            <button
              type="button"
              class="boton ${peligro ? 'peligro' : ''}"
              ?disabled=${this._trabajando !== null}
              @click=${() => this._ejecutarAccion(accion, fila)}
            >
              ${accion.etiqueta}
            </button>
          </div>
        </div>
      </div>
    `
  }

  _renderToast() {
    const error = this._aviso.tipo === 'error'
    return html`<div class="avisos">
      <p class="toast ${error ? 'error' : ''}" role=${error ? 'alert' : 'status'}>
        ${icono(error ? 'alert' : 'check-circle', 18)}<span>${this._aviso.texto}</span>
      </p>
    </div>`
  }

  _renderSeccion(seccion, numero) {
    // * Los campos marcados `mitad` que van seguidos comparten fila (de a dos)
    const bloques = []
    seccion.campos.forEach((campo) => {
      const ultimo = bloques[bloques.length - 1]
      if (campo.mitad && ultimo?.par && ultimo.campos.length < 2) ultimo.campos.push(campo)
      else bloques.push({ par: !!campo.mitad, campos: [campo] })
    })
    return html`<fieldset>
      ${seccion.titulo
        ? html`<legend>${numero ? html`<span class="paso">${numero}</span>` : nothing}${seccion.titulo}</legend>`
        : nothing}
      ${bloques.map((b) =>
        b.par && b.campos.length === 2
          ? html`<div class="grid-2">${b.campos.map((c) => this._renderCampo(c))}</div>`
          : b.campos.map((c) => this._renderCampo(c))
      )}
    </fieldset>`
  }

  _renderCampo(campo) {
    const id = `campo-${campo.campo.replace('.', '-')}`
    const error = this._errorDe(campo)
    const esPassword = campo.tipo === 'password'
    const tipo =
      esPassword && !this._verPassword[campo.campo]
        ? 'password'
        : { email: 'email', fecha: 'date', telefono: 'tel' }[campo.tipo] ?? 'text'
    const entrada = html`<input
      id=${id}
      type=${tipo}
      autocomplete=${esPassword ? 'new-password' : 'off'}
      maxlength=${campo.max ?? nothing}
      .value=${String(this._valores[campo.campo] ?? '')}
      ?readonly=${campo.soloLectura}
      aria-invalid=${error ? 'true' : 'false'}
      @input=${(e) => this._alEscribir(campo, e.target.value)}
      @blur=${() => this._tocar(campo)}
    />`
    return html`<div class="campo ${error ? 'con-error' : ''}">
      <label for=${id}
        >${campo.titulo}${campo.requerido || campo.soloLectura ? '' : html` <span class="opcional">(opcional)</span>`}</label
      >
      ${esPassword
        ? html`<div class="con-ojo">
            ${entrada}
            <button
              type="button"
              class="ojo"
              aria-label=${this._verPassword[campo.campo] ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              @click=${() => (this._verPassword = { ...this._verPassword, [campo.campo]: !this._verPassword[campo.campo] })}
            >
              ${icono(this._verPassword[campo.campo] ? 'eye-off' : 'eye', 18)}
            </button>
          </div>`
        : entrada}
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }
}
