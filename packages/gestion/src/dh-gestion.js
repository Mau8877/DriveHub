import { LitElement, html, nothing } from 'lit'

import { ErrorApi, solicitar } from './cliente-api.js'
import { estilos } from './estilos.js'
import { slugDesdeTexto, validar } from './reglas.js'

const ESPERA_BUSQUEDA_MS = 300
const MAX_IMAGEN_BYTES = 5 * 1024 * 1024
const MAX_ARCHIVO_BYTES_POR_DEFECTO = 20 * 1024 * 1024

/** Valor de un campo tal como se muestra en el formulario: una lista se ve como "a, b, c". */
function paraFormulario(campo, valor) {
  if (campo.tipo === 'lista') return Array.isArray(valor) ? valor.join(', ') : (valor ?? '')
  if (campo.tipo === 'opciones') return filasDe(campo, valor)
  // * Un desplegable maneja texto: un número (ej. la dificultad) se muestra como texto
  if (campo.tipo === 'seleccion') return valor == null ? '' : String(valor)
  return valor ?? ''
}

const FILAS_POR_DEFECTO = 2
const FILAS_MAXIMAS_POR_DEFECTO = 6

/** Las filas de un campo `opciones`: las guardadas, o las vacías que pide `min` si todavía no hay. */
function filasDe(campo, valor) {
  const filas = Array.isArray(valor) ? valor : []
  if (filas.length > 0) return filas.map((f) => ({ texto: f.texto ?? '', correcta: Boolean(f.correcta) }))
  return Array.from({ length: campo.min ?? FILAS_POR_DEFECTO }, () => ({ texto: '', correcta: false }))
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
  const ancho = c.ancho ?? (c.tipo === 'imagen' || c.tipo === 'enlace' ? '6.5rem' : c.tipo === 'insignia' || c.tipo === 'fecha' ? '8rem' : '')
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
  }

  static styles = estilos

  constructor() {
    super()
    this.esquema = null
    this._reiniciar()
    this._temporizador = null
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
  }

  willUpdate(cambios) {
    if (cambios.has('esquema') && this.esquema) {
      this._reiniciar()
      this._cargar()
    }
  }

  disconnectedCallback() {
    clearTimeout(this._temporizador)
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
    if (accion.confirmar) this._confirmando = { id: fila[this._id], accion: accion.id }
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
        valores[campo.campo] = paraFormulario(campo, leer(data, campo.campo))
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
    this._subiendo = {}
    this._errorSubida = {}
    this._bloqueo = null
    this._registro = null
    this._manuales = new Set()
    this._modo = modo
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
    return this._vista === 'lista' ? this._renderLista() : this._renderFormulario()
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
                ${esquema.crear.etiqueta}
              </button>`
            : nothing}
        </div>

        ${this._aviso
          ? html`<p
              class="aviso ${this._aviso.tipo === 'error' ? 'error' : ''}"
              role=${this._aviso.tipo === 'error' ? 'alert' : 'status'}
            >
              ${this._aviso.texto}
            </p>`
          : nothing}
        ${this._renderFiltros()}

        <div class="tarjeta">
          ${this._cargando
            ? html`<p class="estado">Cargando…</p>`
            : this._errorCarga
              ? html`<div class="estado error" role="alert">
                  <p>${this._errorCarga}</p>
                  <button type="button" class="enlace" @click=${() => this._cargar()}>Reintentar</button>
                </div>`
              : this._filas.length === 0
                ? html`<p class="estado">
                    ${this._hayFiltros()
                      ? (esquema.mensajes?.sinResultados ?? 'Sin resultados.')
                      : (esquema.mensajes?.vacio ?? 'Todavía no hay registros.')}
                  </p>`
                : this._renderTabla()}
        </div>
      </section>
    `
  }

  _renderFiltros() {
    const esquema = this.esquema
    if (!esquema.busqueda && !esquema.filtros?.length) return nothing
    return html`
      <div class="tarjeta filtros">
        ${esquema.busqueda
          ? html`<input
              type="search"
              aria-label="Buscar"
              placeholder=${esquema.busqueda.placeholder ?? 'Buscar'}
              .value=${this._busqueda}
              @input=${(e) => this._alBuscar(e.target.value)}
            />`
          : nothing}
        ${(esquema.filtros ?? []).map(
          (filtro) => html`<select
            aria-label=${filtro.titulo}
            .value=${this._filtros[filtro.clave] ?? ''}
            @change=${(e) => this._alFiltrar(filtro.clave, e.target.value)}
          >
            <option value="">${filtro.titulo}: todos</option>
            ${filtro.opciones.map((o) => html`<option value=${o.valor}>${o.etiqueta}</option>`)}
          </select>`
        )}
      </div>
    `
  }

  _renderTabla() {
    const esquema = this.esquema
    return html`
      <div class="contenedor-tabla">
        <table>
          <thead>
            <tr>
              <th scope="col" class="nro">Nro</th>
              ${esquema.columnas.map((c) => html`<th scope="col" style=${anchoColumna(c)}>${c.titulo}</th>`)}
              <th scope="col">Acciones</th>
            </tr>
          </thead>
          <tbody>
            ${this._filas.map(
              (fila, i) => html`<tr>
                <td class="nro">${(this._pagina - 1) * (esquema.porPagina ?? 20) + i + 1}</td>
                ${esquema.columnas.map((c) => html`<td class=${c.secundaria ? 'secundaria' : ''}>${this._celda(c, fila)}</td>`)}
                <td>${this._renderAcciones(fila)}</td>
              </tr>`
            )}
          </tbody>
        </table>
      </div>
      ${this._renderPie()}
    `
  }

  _celda(columna, fila) {
    const valor = leer(fila, columna.campo)
    if (columna.tipo === 'insignia') {
      const insignia = columna.insignias?.[valor]
      return html`<span class="insignia ${insignia?.tono ?? ''}">${insignia?.etiqueta ?? valor}</span>`
    }
    if (columna.tipo === 'enlace') {
      return valor
        ? html`<a class="enlace-archivo" href=${valor} target="_blank" rel="noopener noreferrer">${columna.etiqueta ?? 'Abrir'}</a>`
        : ''
    }
    if (columna.tipo === 'imagen') {
      return valor ? html`<img class="miniatura" src=${miniatura(valor, 96, 64)} alt="" loading="lazy" />` : ''
    }
    if (columna.tipo === 'fecha') {
      return valor ? new Date(valor).toLocaleDateString('es') : ''
    }
    return valor ?? ''
  }

  _renderAcciones(fila) {
    const esquema = this.esquema
    const id = fila[this._id]
    const trabajando = this._trabajando === id
    if (this._confirmando?.id === id) {
      const accion = (esquema.acciones ?? []).find((a) => a.id === this._confirmando.accion)
      return html`<span class="acciones">
        <span>${accion.confirmar}</span>
        <button
          type="button"
          class="enlace peligro"
          ?disabled=${trabajando}
          @click=${() => this._ejecutarAccion(accion, fila)}
        >
          Confirmar
        </button>
        <button type="button" class="enlace" @click=${() => (this._confirmando = null)}>Cancelar</button>
      </span>`
    }
    const puedeEditar = esquema.editar && cumple(esquema.editar.visibleSi, fila)
    const visibles = (esquema.acciones ?? []).filter((a) => cumple(a.visibleSi, fila))
    // * Una fila final (ej. un auto de baja) no tiene acciones: se dice, en vez de dejar la celda vacía
    if (!puedeEditar && visibles.length === 0) {
      return html`<span class="sin-acciones">Sin acciones</span>`
    }
    return html`<span class="acciones">
      ${puedeEditar
        ? html`<button type="button" class="enlace" @click=${() => this._abrirEditar(fila)}>Editar</button>`
        : nothing}
      ${visibles.map(
        (a) => html`<button
          type="button"
          class="enlace ${a.tono === 'peligro' ? 'peligro' : ''}"
          ?disabled=${trabajando}
          @click=${() => this._alPulsarAccion(a, fila)}
        >
          ${a.etiqueta}
        </button>`
      )}
    </span>`
  }

  _renderPie() {
    const meta = this._meta
    if (!meta) return nothing
    return html`<div class="pie">
      <span>Página ${meta.page} de ${meta.totalPages} · ${meta.totalItems} en total</span>
      <span class="botones">
        <button type="button" class="boton secundario" ?disabled=${meta.page <= 1} @click=${() => this._irAPagina(meta.page - 1)}>
          Anterior
        </button>
        <button
          type="button"
          class="boton secundario"
          ?disabled=${meta.page >= meta.totalPages}
          @click=${() => this._irAPagina(meta.page + 1)}
        >
          Siguiente
        </button>
      </span>
    </div>`
  }

  _renderFormulario() {
    const creando = this._vista === 'crear'
    const definicion = creando ? this.esquema.crear : this.esquema.editar
    return html`
      <section class="pagina">
        <div class="cabecera">
          <div>
            <h1>${definicion.titulo ?? (creando ? this.esquema.crear.etiqueta : `Editar ${this.esquema.nombreSingular ?? 'registro'}`)}</h1>
            ${definicion.descripcion ? html`<p class="descripcion">${definicion.descripcion}</p>` : nothing}
          </div>
        </div>
        ${this._cargandoRegistro
          ? html`<p class="estado">Cargando…</p>`
          : this._bloqueo
            ? html`<div class="tarjeta estado" role="alert">
                <p>${this._bloqueo}</p>
                <button type="button" class="enlace" @click=${() => this._volverALista()}>Volver al listado</button>
              </div>`
            : html`<form @submit=${(e) => this._enviar(e)} novalidate>
                ${definicion.secciones.map((seccion) => this._renderSeccion(seccion))}
                ${this._errorGeneral ? html`<p class="error-campo" role="alert">${this._errorGeneral}</p>` : nothing}
                <div class="pie-formulario">
                  <button type="submit" class="boton" ?disabled=${this._enviando}>
                    ${this._enviando ? 'Guardando…' : (definicion.etiquetaEnviar ?? (creando ? 'Crear' : 'Guardar'))}
                  </button>
                  <button type="button" class="enlace" @click=${() => this._volverALista()}>Cancelar</button>
                </div>
              </form>`}
      </section>
    `
  }

  _renderSeccion(seccion) {
    return html`<fieldset class="tarjeta">
      ${seccion.titulo ? html`<legend>${seccion.titulo}</legend>` : nothing}
      ${seccion.campos.map((campo) => this._renderCampo(campo))}
    </fieldset>`
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

  _renderArchivo(campo, id, error) {
    const actual = this._valores[campo.campo]
    const subiendo = Boolean(this._subiendo[campo.campo])
    const falla = this._errorSubida[campo.campo] ?? error
    return html`<div class="campo">
      <label for=${id}>${campo.titulo}${campo.requerido ? '' : ' (opcional)'}</label>
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
      ${falla ? html`<span class="error-campo" role="alert">${falla}</span>` : nothing}
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
  _renderOpciones(campo, id, error) {
    const filas = filasDe(campo, this._valores[campo.campo])
    const minimo = campo.min ?? FILAS_POR_DEFECTO
    const maximo = campo.max ?? FILAS_MAXIMAS_POR_DEFECTO
    return html`<div class="campo" id=${id}>
      <span class="etiqueta">${campo.titulo}${campo.requerido ? '' : ' (opcional)'}</span>
      <ol class="filas-opciones">
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
        class="boton secundario"
        ?disabled=${filas.length >= maximo}
        @click=${() => this._cambiarFilas(campo, (f) => [...f, { texto: '', correcta: false }])}
      >
        Agregar opción
      </button>
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }

  _renderImagen(campo, id, error) {
    const url = this._valores[campo.campo]
    const subiendo = Boolean(this._subiendo[campo.campo])
    const falla = this._errorSubida[campo.campo] ?? error
    return html`<div class="campo">
      <label for=${id}>${campo.titulo}${campo.requerido ? '' : ' (opcional)'}</label>
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
      ${falla ? html`<span class="error-campo" role="alert">${falla}</span>` : nothing}
    </div>`
  }

  _renderCampo(campo) {
    const id = `campo-${campo.campo.replace('.', '-')}`
    const error = this._errorDe(campo)
    if (campo.tipo === 'imagen') return this._renderImagen(campo, id, error)
    if (campo.tipo === 'archivo') return this._renderArchivo(campo, id, error)
    if (campo.tipo === 'opciones') return this._renderOpciones(campo, id, error)
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
            .value=${String(this._valores[campo.campo] ?? '')}
            ?readonly=${campo.soloLectura}
            aria-invalid=${error ? 'true' : 'false'}
            @input=${(e) => this._alEscribir(campo, e.target.value)}
            @blur=${() => this._tocar(campo)}
          ></textarea>`
        : campo.tipo === 'seleccion'
        ? html`<select
            id=${id}
            .value=${String(this._valores[campo.campo] ?? '')}
            ?disabled=${campo.soloLectura}
            aria-invalid=${error ? 'true' : 'false'}
            @change=${(e) => this._alEscribir(campo, e.target.value)}
            @blur=${() => this._tocar(campo)}
          >
            <option value="">Elige una opción</option>
            ${(campo.opciones ?? []).map(
              (o) => html`<option value=${o.valor} ?selected=${o.valor === this._valores[campo.campo]}>${o.etiqueta}</option>`
            )}
          </select>`
        : html`<input
            id=${id}
            type=${tipo}
            autocomplete=${esPassword ? 'new-password' : 'off'}
            .value=${String(this._valores[campo.campo] ?? '')}
            ?readonly=${campo.soloLectura}
            aria-invalid=${error ? 'true' : 'false'}
            @input=${(e) => this._alEscribir(campo, e.target.value)}
            @blur=${() => this._tocar(campo)}
          />`
    return html`<div class="campo">
      <label for=${id}>${campo.titulo}${campo.requerido ? '' : ' (opcional)'}</label>
      ${esPassword
        ? html`<div class="con-boton">
            ${entrada}
            <button
              type="button"
              class="boton secundario"
              @click=${() => (this._verPassword = { ...this._verPassword, [campo.campo]: !this._verPassword[campo.campo] })}
            >
              ${this._verPassword[campo.campo] ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>`
        : entrada}
      ${campo.ayuda ? html`<span class="ayuda">${campo.ayuda}</span>` : nothing}
      ${error ? html`<span class="error-campo" role="alert">${error}</span>` : nothing}
    </div>`
  }
}
