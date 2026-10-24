import { icono } from '@drivehub/ui'
import { LitElement, html, nothing } from 'lit'

import { ErrorApi, solicitar } from './cliente-api.js'
import { estilos } from './estilos.js'
import { slugDesdeTexto, validar } from './reglas.js'

const ESPERA_BUSQUEDA_MS = 300
const MAX_IMAGEN_BYTES = 5 * 1024 * 1024
const MAX_ARCHIVO_BYTES_POR_DEFECTO = 20 * 1024 * 1024
const FILAS_POR_DEFECTO = 2
const FILAS_MAXIMAS_POR_DEFECTO = 6

/** Las filas de un campo `opciones`: las guardadas, o las vacías que pide `min` si todavía no hay. */
function filasDe(campo, valor) {
  const filas = Array.isArray(valor) ? valor : []
  if (filas.length > 0) return filas.map((f) => ({ texto: f.texto ?? '', correcta: Boolean(f.correcta) }))
  return Array.from({ length: campo.min ?? FILAS_POR_DEFECTO }, () => ({ texto: '', correcta: false }))
}

/** Valor de un campo tal como se muestra en el formulario: una lista se ve como "a, b, c". */
function paraFormulario(campo, valor) {
  if (campo.tipo === 'lista') return Array.isArray(valor) ? valor.join(', ') : (valor ?? '')
  if (campo.tipo === 'opciones') return filasDe(campo, valor)
  // * Una lista desplegable maneja texto: un número (ej. la dificultad) se muestra como texto
  if (campo.tipo === 'seleccion') return valor == null ? '' : String(valor)
  return valor ?? ''
}

/** Una lista escrita como "a, b, a" viaja como array sin vacíos ni repetidos. */
function aLista(texto) {
  return [...new Set(String(texto).split(',').map((t) => t.trim()).filter(Boolean))]
}

/**
 * Versión recortada de una imagen de Cloudinary (`c_fill,w_,h_`), para no bajar la foto completa
 * en un listado. Una URL que no sea de Cloudinary se devuelve igual.
 */
function miniatura(url, ancho, alto) {
  return String(url).replace('/image/upload/', `/image/upload/c_fill,w_${ancho},h_${alto},f_auto,q_auto/`)
}

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
 * Prefijo del cuerpo donde un campo `ubicacion` escribe `latitud` y `longitud`: la ruta del campo
 * sin su último tramo ('administrador.ubicacion' -> 'administrador.'; 'ubicacion' -> '').
 */
function prefijoDe(campo) {
  const i = campo.campo.lastIndexOf('.')
  return i < 0 ? '' : campo.campo.slice(0, i + 1)
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
  const ancho =
    c.ancho ?? (c.tipo === 'imagen' || c.tipo === 'enlace' ? '6.5rem' : c.tipo === 'insignia' || c.tipo === 'fecha' ? '8rem' : '')
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
    _subiendo: { state: true },
    _errorSubida: { state: true },
    _cargandoRegistro: { state: true },
    _bloqueo: { state: true },
    _opciones: { state: true },
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
    this._subiendo = {}
    this._errorSubida = {}
    this._cargandoRegistro = false
    this._bloqueo = null
    this._opciones = {}
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
    const valores = {}
    camposDe(this.esquema.crear.secciones).forEach((campo) => {
      if (campo.porDefecto !== undefined) valores[campo.campo] = campo.porDefecto
    })
    this._valores = valores
    this._vista = 'crear'
    this._cargarOpciones(camposDe(this.esquema.crear.secciones))
  }

  /** Trae de la API las opciones de los campos `seleccion` que las piden con `opcionesDe`. */
  async _cargarOpciones(campos) {
    const esquema = this.esquema
    const remotos = campos.filter((c) => c.tipo === 'seleccion' && c.opcionesDe)
    if (remotos.length === 0) return
    this._opciones = {
      ...this._opciones,
      ...Object.fromEntries(remotos.map((c) => [c.campo, { estado: 'cargando', items: [] }])),
    }
    await Promise.all(
      remotos.map(async (campo) => {
        const origen = campo.opcionesDe
        let resultado
        try {
          const { data } = await solicitar('GET', origen.recurso, {
            params: { page: 1, pageSize: 100, ...(origen.params ?? {}) },
          })
          resultado = {
            estado: 'ok',
            items: (data ?? []).map((item) => ({
              valor: String(leer(item, origen.valor) ?? ''),
              etiqueta: String(leer(item, origen.etiqueta) ?? leer(item, origen.valor) ?? ''),
            })),
          }
        } catch {
          resultado = { estado: 'error', items: [] }
        }
        if (esquema === this.esquema) this._opciones = { ...this._opciones, [campo.campo]: resultado }
      })
    )
  }

  async _abrirEditar(fila) {
    this._prepararFormulario('editar')
    this._idEditando = fila[this._id]
    this._vista = 'editar'
    const editar = this.esquema.editar
    this._cargandoRegistro = true
    this._cargarOpciones(camposDe(editar.secciones))
    try {
      const ruta = (editar.cargar ?? '{id}').replace('{id}', encodeURIComponent(this._idEditando))
      const { data } = await solicitar('GET', `${this.esquema.recurso}/${ruta}`)
      const valores = {}
      camposDe(editar.secciones).forEach((campo) => {
        valores[campo.campo] =
          campo.tipo === 'ubicacion'
            ? { latitud: leer(data, `${prefijoDe(campo)}latitud`) ?? null, longitud: leer(data, `${prefijoDe(campo)}longitud`) ?? null }
            : paraFormulario(campo, leer(data, campo.campo))
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

  /** Abre el panel de solo lectura: carga el registro y lo muestra sin formulario. */
  async _abrirDetalle(fila) {
    const esquema = this.esquema
    const id = fila[this._id]
    this._idDetalle = id
    this._registro = null
    this._vista = 'detalle'
    this._cargandoRegistro = true
    try {
      const ruta = (esquema.detalle.cargar ?? '{id}').replace('{id}', encodeURIComponent(id))
      const { data } = await solicitar('GET', `${esquema.recurso}/${ruta}`)
      // * Si cerró el panel o abrió otro mientras cargaba, esta respuesta ya no corresponde
      if (esquema !== this.esquema || this._idDetalle !== id || this._vista !== 'detalle') return
      this._registro = data
    } catch (error) {
      if (esquema !== this.esquema || this._idDetalle !== id || this._vista !== 'detalle') return
      this._aviso = {
        tipo: 'error',
        texto: error instanceof ErrorApi ? error.message : 'No pudimos cargar el registro.',
      }
      this._vista = 'lista'
    } finally {
      if (this._idDetalle === id) this._cargandoRegistro = false
    }
  }

  _prepararFormulario(modo) {
    this._valores = {}
    this._tocados = new Set()
    this._erroresServidor = {}
    this._errorGeneral = null
    this._enviando = false
    this._verPassword = {}
    this._subiendo = {}
    this._errorSubida = {}
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

  _alUbicar(campo, detalle) {
    this._valores = { ...this._valores, [campo.campo]: { latitud: detalle.latitud, longitud: detalle.longitud } }
    this._tocados = new Set(this._tocados).add(campo.campo)
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
      if (campo.tipo === 'ubicacion') {
        const punto = this._valores[campo.campo]
        if (punto?.latitud !== undefined && punto?.latitud !== null && punto?.longitud !== undefined && punto?.longitud !== null) {
          escribir(cuerpo, `${prefijoDe(campo)}latitud`, Number(punto.latitud))
          escribir(cuerpo, `${prefijoDe(campo)}longitud`, Number(punto.longitud))
        }
        return
      }
      // * El archivo ya se subió al elegirlo: al enviar solo viaja su descriptor, y solo si es nuevo
      if (campo.tipo === 'archivo') {
        const { nuevo, ...descriptor } = this._valores[campo.campo] ?? {}
        if (nuevo) escribir(cuerpo, campo.campo, descriptor)
        return
      }
      if (campo.tipo === 'opciones') {
        const filas = filasDe(campo, this._valores[campo.campo])
        escribir(cuerpo, campo.campo, filas.map((f) => ({ texto: f.texto.trim(), correcta: f.correcta })))
        return
      }
      const texto = String(this._valores[campo.campo] ?? '').trim()
      if (campo.tipo === 'lista') {
        if (texto !== '' || campo.requerido) escribir(cuerpo, campo.campo, aLista(texto))
        return
      }
      if (texto === '' && !campo.requerido) return
      // * Las contraseñas no se recortan (los espacios cuentan) y los números viajan como número
      const numerico = campo.tipo === 'numero' || (campo.tipo === 'seleccion' && campo.numerico)
      const valor = campo.tipo === 'password' ? this._valores[campo.campo] : numerico ? Number(texto) : texto
      escribir(cuerpo, campo.campo, valor)
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
      ${this._vista === 'lista' ? nothing : this._vista === 'detalle' ? this._renderDetalle() : this._renderDrawer()}
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
      return html`<dh-select
        class="filtro-lista"
        .etiqueta=${filtro.titulo}
        .valor=${actual}
        .placeholder=${`${filtro.titulo}: todos`}
        .opciones=${filtro.opciones}
        @dh-cambio=${(e) => this._alFiltrar(filtro.clave, e.detail.valor)}
      ></dh-select>`
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
    if (columna.tipo === 'enlace') {
      return valor
        ? html`<a class="enlace-archivo" href=${valor} target="_blank" rel="noopener noreferrer"
            >${columna.etiqueta ?? 'Abrir'}</a
          >`
        : ''
    }
    if (columna.tipo === 'imagen') {
      return valor ? html`<img class="miniatura" src=${miniatura(valor, 96, 64)} alt="" loading="lazy" />` : ''
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
    const puedeVer = esquema.detalle && cumple(esquema.detalle.visibleSi, fila)
    // * Una fila final (ej. un auto de baja sin salida) no tiene acciones: se dice, en vez de dejar la celda vacía
    if (!puedeVer && !puedeEditar && !(esquema.acciones ?? []).some((a) => cumple(a.visibleSi, fila))) {
      return html`<span class="sin-acciones">Sin acciones</span>`
    }
    return html`<span class="acciones">
      ${puedeVer
        ? html`<button type="button" class="enlace" @click=${() => this._abrirDetalle(fila)}>
            ${icono(esquema.detalle.icono ?? 'eye', 16)}<span>${esquema.detalle.etiqueta ?? 'Ver detalles'}</span>
          </button>`
        : nothing}
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

  /** Panel de solo lectura (`detalle` del esquema): mismo drawer, sin formulario ni "Guardar". */
  _renderDetalle() {
    const detalle = this.esquema.detalle
    const titulo = detalle.titulo ?? `Detalle de ${this.esquema.nombreSingular ?? 'registro'}`
    return html`
      <div class="fondo" @mousedown=${(e) => e.target === e.currentTarget && this._cerrarDrawer()}>
        <aside class="drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-titulo">
          <header class="drawer-cabeza">
            <div>
              <h2 id="drawer-titulo">${titulo}</h2>
              ${detalle.descripcion ? html`<p class="secundaria">${detalle.descripcion}</p>` : nothing}
            </div>
            <button type="button" class="icono-btn" aria-label="Cerrar" @click=${() => this._cerrarDrawer()}>
              ${icono('x', 20)}
            </button>
          </header>
          <div class="drawer-cuerpo" aria-busy=${String(this._cargandoRegistro)}>
            ${this._cargandoRegistro || !this._registro
              ? html`<span class="brillo"></span><span class="brillo"></span><span class="brillo"></span>`
              : detalle.secciones.map((seccion) => this._renderSeccionDetalle(seccion))}
            <div class="drawer-pie">
              <button type="button" class="boton secundario" @click=${() => this._cerrarDrawer()}>Cerrar</button>
            </div>
          </div>
        </aside>
      </div>
    `
  }

  _renderSeccionDetalle(seccion) {
    // * Igual que en los formularios: los campos `mitad` que van seguidos comparten fila (de a dos)
    const bloques = []
    seccion.campos.forEach((campo) => {
      const ultimo = bloques[bloques.length - 1]
      if (campo.mitad && ultimo?.par && ultimo.campos.length < 2) ultimo.campos.push(campo)
      else bloques.push({ par: !!campo.mitad, campos: [campo] })
    })
    return html`<fieldset>
      ${seccion.titulo ? html`<legend>${seccion.titulo}</legend>` : nothing}
      ${bloques.map((b) =>
        b.par && b.campos.length === 2
          ? html`<div class="grid-2">${b.campos.map((c) => this._renderDato(c))}</div>`
          : b.campos.map((c) => this._renderDato(c))
      )}
    </fieldset>`
  }

  _renderDato(campo) {
    const valor = leer(this._registro, campo.campo)
    const vacio = valor === undefined || valor === null || valor === ''
    let contenido = '—'
    if (!vacio) {
      if (campo.tipo === 'insignia') {
        const insignia = campo.insignias?.[valor]
        contenido = html`<span class="insignia ${insignia?.tono ?? ''}">${insignia?.etiqueta ?? valor}</span>`
      } else if (campo.tipo === 'fecha') {
        contenido = formatearFecha(valor)
      } else {
        contenido = String(valor)
      }
    }
    return html`<div class="dato">
      <span class="dato-titulo">${campo.titulo}</span>
      <span class="dato-valor ${vacio ? 'sin-valor' : ''}">${contenido}</span>
    </div>`
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
    if (campo.tipo === 'seleccion') return this._renderSeleccion(campo)
    if (campo.tipo === 'ubicacion') return this._renderUbicacion(campo)
    if (campo.tipo === 'imagen') return this._renderImagen(campo)
    if (campo.tipo === 'archivo') return this._renderArchivo(campo)
    if (campo.tipo === 'opciones') return this._renderOpciones(campo)
    const id = `campo-${campo.campo.replace('.', '-')}`
    const error = this._errorDe(campo)
    const esPassword = campo.tipo === 'password'
    const tipo =
      esPassword && !this._verPassword[campo.campo]
        ? 'password'
        : { email: 'email', fecha: 'date', telefono: 'tel', numero: 'number' }[campo.tipo] ?? 'text'
    const entrada =
      campo.tipo === 'textarea'
        ? html`<textarea
            id=${id}
            rows="4"
            maxlength=${campo.max ?? nothing}
            .value=${String(this._valores[campo.campo] ?? '')}
            ?readonly=${campo.soloLectura}
            aria-invalid=${error ? 'true' : 'false'}
            @input=${(e) => this._alEscribir(campo, e.target.value)}
            @blur=${() => this._tocar(campo)}
          ></textarea>`
        : html`<input
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

  /**
   * Sube la foto directo a Cloudinary con una firma que pide el backend (el API secret no sale de
   * él) y deja la URL resultante como valor del campo.
   */
  async _subirImagen(campo, evento) {
    // * Se guarda antes del primer await: después el evento ya no apunta al elemento
    const entrada = evento.target
    const archivo = entrada.files?.[0]
    if (!archivo) return
    const fallar = (mensaje) => (this._errorSubida = { ...this._errorSubida, [campo.campo]: mensaje })
    if (!archivo.type.startsWith('image/')) return fallar('El archivo debe ser una imagen')
    if (archivo.size > MAX_IMAGEN_BYTES) return fallar('La imagen no puede pesar más de 5 MB')

    this._errorSubida = { ...this._errorSubida, [campo.campo]: null }
    this._subiendo = { ...this._subiendo, [campo.campo]: true }
    try {
      const { data: firma } = await solicitar('POST', campo.firma)
      const cuerpo = new FormData()
      cuerpo.set('file', archivo)
      cuerpo.set('api_key', firma.apiKey)
      cuerpo.set('timestamp', String(firma.timestamp))
      cuerpo.set('folder', firma.folder)
      cuerpo.set('signature', firma.signature)
      const respuesta = await fetch(`https://api.cloudinary.com/v1_1/${firma.cloudName}/image/upload`, {
        method: 'POST',
        body: cuerpo,
      })
      const resultado = await respuesta.json().catch(() => null)
      if (!respuesta.ok || !resultado?.secure_url) throw new Error('subida rechazada')
      this._alEscribir(campo, resultado.secure_url)
    } catch (error) {
      fallar(error instanceof ErrorApi ? error.message : 'No pudimos subir la imagen. Inténtalo de nuevo.')
    } finally {
      this._subiendo = { ...this._subiendo, [campo.campo]: false }
      entrada.value = ''
    }
  }

  /**
   * Sube el archivo al backend (multipart, a `campo.subida`) apenas se elige y deja su descriptor
   * como valor del campo, marcado como nuevo. Al enviar el formulario solo viaja ese descriptor.
   */
  async _subirArchivo(campo, evento) {
    const entrada = evento.target
    const archivo = entrada.files?.[0]
    if (!archivo) return
    const fallar = (mensaje) => (this._errorSubida = { ...this._errorSubida, [campo.campo]: mensaje })
    if (campo.accept && !campo.accept.split(',').includes(archivo.type)) {
      return fallar(campo.mensajes?.tipo ?? 'El tipo de archivo no es válido')
    }
    const maximo = campo.maxBytes ?? MAX_ARCHIVO_BYTES_POR_DEFECTO
    if (archivo.size > maximo) {
      return fallar(campo.mensajes?.max ?? `El archivo no puede pesar más de ${Math.round(maximo / 1024 / 1024)} MB`)
    }

    this._errorSubida = { ...this._errorSubida, [campo.campo]: null }
    this._subiendo = { ...this._subiendo, [campo.campo]: true }
    try {
      const cuerpo = new FormData()
      cuerpo.set('archivo', archivo)
      const { data } = await solicitar('POST', campo.subida, { formulario: cuerpo })
      this._alEscribir(campo, { ...data, nuevo: true })
    } catch (error) {
      fallar(error instanceof ErrorApi ? error.message : 'No pudimos subir el archivo. Inténtalo de nuevo.')
    } finally {
      this._subiendo = { ...this._subiendo, [campo.campo]: false }
      entrada.value = ''
    }
  }

  _renderImagen(campo) {
    const id = `campo-${campo.campo.replace('.', '-')}`
    const url = this._valores[campo.campo]
    const subiendo = Boolean(this._subiendo[campo.campo])
    const error = this._errorSubida[campo.campo] ?? this._errorDe(campo)
    return html`<div class="campo ${error ? 'con-error' : ''}">
      <label for=${id}
        >${campo.titulo}${campo.requerido || campo.soloLectura ? '' : html` <span class="opcional">(opcional)</span>`}</label
      >
      ${url ? html`<img class="vista-previa" src=${miniatura(url, 480, 300)} alt="Foto actual" />` : nothing}
      <input
        id=${id}
        type="file"
        accept="image/*"
        ?disabled=${subiendo || campo.soloLectura}
        @change=${(e) => this._subirImagen(campo, e)}
      />
      ${subiendo ? html`<span class="ayuda" role="status">Subiendo imagen…</span>` : nothing}
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }

  _renderArchivo(campo) {
    const id = `campo-${campo.campo.replace('.', '-')}`
    const actual = this._valores[campo.campo]
    const subiendo = Boolean(this._subiendo[campo.campo])
    const error = this._errorSubida[campo.campo] ?? this._errorDe(campo)
    return html`<div class="campo ${error ? 'con-error' : ''}">
      <label for=${id}
        >${campo.titulo}${campo.requerido || campo.soloLectura ? '' : html` <span class="opcional">(opcional)</span>`}</label
      >
      ${actual?.url
        ? html`<span class="archivo-actual">
            <a class="enlace-archivo" href=${actual.url} target="_blank" rel="noopener noreferrer"
              >${actual.nombre ?? 'Ver archivo'}</a
            >
            ${actual.nuevo ? html`<span class="ayuda">(nuevo, se guarda al enviar)</span>` : nothing}
          </span>`
        : nothing}
      <input
        id=${id}
        type="file"
        accept=${campo.accept ?? ''}
        ?disabled=${subiendo || campo.soloLectura}
        @change=${(e) => this._subirArchivo(campo, e)}
      />
      ${subiendo ? html`<span class="ayuda" role="status">Subiendo archivo…</span>` : nothing}
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }

  /** Cambia una fila del campo `opciones` (o la quita/agrega) y deja el nuevo arreglo como valor. */
  _cambiarFilas(campo, cambio) {
    this._alEscribir(campo, cambio(filasDe(campo, this._valores[campo.campo])))
  }

  /**
   * Lista de respuestas repetible: cada fila lleva su texto y una casilla "correcta". `min` y `max`
   * son la cantidad de filas (2 y 6 por defecto) y `maxTexto` el largo de cada texto.
   */
  _renderOpciones(campo) {
    const id = `campo-${campo.campo.replace('.', '-')}`
    const error = this._errorDe(campo)
    const filas = filasDe(campo, this._valores[campo.campo])
    const minimo = campo.min ?? FILAS_POR_DEFECTO
    const maximo = campo.max ?? FILAS_MAXIMAS_POR_DEFECTO
    return html`<div class="campo ${error ? 'con-error' : ''}" id=${id}>
      <span class="etiqueta-campo"
        >${campo.titulo}${campo.requerido || campo.soloLectura ? '' : html` <span class="opcional">(opcional)</span>`}</span
      >
      <ol class="filas-opcion">
        ${filas.map(
          (fila, i) => html`<li class="fila-opcion">
            <input
              type="text"
              aria-label=${`Opción ${i + 1}`}
              maxlength=${campo.maxTexto ?? 200}
              autocomplete="off"
              .value=${fila.texto}
              @input=${(e) =>
                this._cambiarFilas(campo, (f) => f.map((x, j) => (j === i ? { ...x, texto: e.target.value } : x)))}
              @blur=${() => this._tocar(campo)}
            />
            <label class="correcta">
              <input
                type="checkbox"
                .checked=${fila.correcta}
                @change=${(e) =>
                  this._cambiarFilas(campo, (f) => f.map((x, j) => (j === i ? { ...x, correcta: e.target.checked } : x)))}
              />
              Correcta
            </label>
            <button
              type="button"
              class="enlace peligro"
              aria-label=${`Quitar opción ${i + 1}`}
              ?disabled=${filas.length <= minimo}
              @click=${() => this._cambiarFilas(campo, (f) => f.filter((_, j) => j !== i))}
            >
              Quitar
            </button>
          </li>`
        )}
      </ol>
      <button
        type="button"
        class="boton secundario agregar-fila"
        ?disabled=${filas.length >= maximo}
        @click=${() => this._cambiarFilas(campo, (f) => [...f, { texto: '', correcta: false }])}
      >
        Agregar opción
      </button>
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }

  /** Lista de opciones de un campo `seleccion`: fijas del esquema o traídas de la API. */
  _opcionesDe(campo) {
    if (campo.opciones) return { estado: 'ok', items: campo.opciones }
    return this._opciones[campo.campo] ?? { estado: 'ok', items: [] }
  }

  _renderSeleccion(campo) {
    const id = `campo-${campo.campo.replace('.', '-')}`
    const error = this._errorDe(campo)
    const { estado, items } = this._opcionesDe(campo)
    const actual = String(this._valores[campo.campo] ?? '')
    // * Lista propia (<dh-select>), no el <select> nativo: ver UI_FRONTEND.md
    return html`<div class="campo ${error ? 'con-error' : ''}">
      <span class="etiqueta-campo"
        >${campo.titulo}${campo.requerido || campo.soloLectura ? '' : html` <span class="opcional">(opcional)</span>`}</span
      >
      <dh-select
        id=${id}
        .etiqueta=${campo.titulo}
        .valor=${actual}
        .opciones=${items}
        .placeholder=${estado === 'cargando' ? 'Cargando…' : 'Elige una opción'}
        ?deshabilitado=${campo.soloLectura || estado === 'cargando'}
        ?invalido=${!!error}
        @dh-cambio=${(e) => {
          this._alEscribir(campo, e.detail.valor)
          this._tocar(campo)
        }}
        @dh-blur=${() => this._tocar(campo)}
      ></dh-select>
      ${estado === 'error'
        ? html`<span class="error-campo" role="alert">
            No pudimos cargar las opciones.
            <button type="button" class="enlace" @click=${() => this._cargarOpciones([campo])}>Reintentar</button>
          </span>`
        : nothing}
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }

  _renderUbicacion(campo) {
    const error = this._errorDe(campo)
    const punto = this._valores[campo.campo] ?? {}
    const direccion = campo.direccionDe && this._registro ? leer(this._registro, campo.direccionDe) : ''
    return html`<div class="campo ${error ? 'con-error' : ''}">
      <span class="etiqueta-campo"
        >${campo.titulo}${campo.requerido || campo.soloLectura ? '' : html` <span class="opcional">(opcional)</span>`}</span
      >
      <dh-mapa-ubicacion
        .latitud=${punto.latitud ?? null}
        .longitud=${punto.longitud ?? null}
        .direccion=${direccion ?? ''}
        ?deshabilitado=${campo.soloLectura}
        @dh-ubicacion=${(e) => this._alUbicar(campo, e.detail)}
      ></dh-mapa-ubicacion>
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }
}
