/** Condición sobre un registro: el campo (con puntos para anidados) igual o distinto de un valor. */
export interface Condicion {
  campo: string
  /** Con `null`, se cumple si el campo no tiene valor (nulo o ausente). */
  igual?: unknown
  /** Con `null`, se cumple si el campo tiene algún valor. */
  distinto?: unknown
}

export interface Insignia {
  etiqueta: string
  /** Color de la insignia, de los tokens: `exito` (verde), `info` (azul técnico), `ambar` o neutro. */
  tono?: 'exito' | 'info' | 'ambar'
}

export interface Columna {
  /** Campo del registro (admite puntos). */
  campo: string
  titulo: string
  tipo?: 'texto' | 'fecha' | 'insignia' | 'foto' | 'etiquetas' | 'imagen' | 'enlace' | 'moneda'
  /**
   * `tipo: 'moneda'`: el valor numérico se dibuja con el prefijo de esta moneda, miles con punto y dos decimales
   * con coma (`BOB` -> `Bs 1.250,00`, `USD` -> `US$ 179,60`). Vacío o no numérico se ve vacío.
   */
  moneda?: 'BOB' | 'USD'
  /** `tipo: 'foto'`: campo con el nombre completo, para las iniciales cuando la persona no tiene foto. */
  nombre?: string
  /** Para `tipo: 'insignia'`: cómo se muestra cada valor. */
  insignias?: Record<string, Insignia>
  /**
   * `tipo: 'etiquetas'`: el valor es un arreglo (de textos o de objetos) que se dibuja como insignias
   * pequeñas; con objetos, `etiqueta` es la propiedad que se muestra (ej. `sigla`). Vacío se ve "—".
   * `tipo: 'enlace'`: el texto del enlace (el valor del campo es la URL).
   */
  etiqueta?: string
  /** Texto atenuado (datos de apoyo como el slug). */
  secundaria?: boolean
  /** Columna principal: el texto va en negrita y, si hay `icono`, con su ficha de icono a la izquierda. */
  principal?: boolean
  /** Nombre de un icono de `@drivehub/ui` (ej. `school`); solo con `principal`. */
  icono?: string
  /** Muestra el valor como código (ej. el slug). */
  codigo?: boolean
  /** Ancho CSS de la columna (ej. '10rem'). Por defecto, corto para insignias y fechas; el resto reparte lo que sobra. */
  ancho?: string
  /** Texto que se muestra cuando el valor está vacío (ej. "Sin asignar"). Sin él, la celda queda vacía. */
  vacioTexto?: string
}

export interface Filtro {
  /** Parámetro de la API, tal cual (ej. `filter[estado]`). */
  clave: string
  titulo: string
  /** Texto de la opción que quita el filtro (por defecto `Todas`). */
  todas?: string
  opciones: { valor: string; etiqueta: string }[]
}

export interface MensajesCampo {
  requerido?: string
  min?: string
  max?: string
  email?: string
  patron?: string
  /** `archivo`: mensaje cuando el tipo de archivo no es el aceptado. */
  tipo?: string
  fechaNoFutura?: string
  fechaFutura?: string
  hastaHoy?: string
  edadMinima?: string
  /** `seleccion`: el valor no está entre las opciones. */
  opcion?: string
  /** `ubicacion`: las coordenadas están incompletas o fuera de rango. */
  ubicacion?: string
}

export interface Campo {
  /** Campo del cuerpo (admite puntos: `administrador.nombres` arma un objeto anidado). */
  campo: string
  titulo: string
  tipo?:
    | 'texto'
    | 'email'
    | 'password'
    | 'fecha'
    | 'telefono'
    | 'numero'
    | 'decimal'
    | 'seleccion'
    | 'ubicacion'
    | 'imagen'
    | 'archivo'
    | 'lista'
    | 'textarea'
    | 'opciones'
  requerido?: boolean
  /**
   * `decimal`: cuántos decimales admite como máximo (2 por defecto; 0 = solo enteros). Se escribe con punto o
   * con coma y viaja como número (`1250,5` -> `1250.5`); `min` y `max` son el rango del valor.
   */
  decimales?: number
  /**
   * Largo mínimo y máximo del texto. En `tipo: 'numero'` y `'decimal'` son el rango del valor y en
   * `tipo: 'opciones'` la cantidad de filas (2 y 6 por defecto).
   */
  min?: number
  max?: number
  /** Nombre de un patrón (`slug`) o una expresión regular. */
  patron?: string
  /**
   * Reglas con nombre: `fechaNoFutura` (anterior a hoy), `hastaHoy` (hoy o antes), `fechaFutura`
   * (posterior a hoy), `edadMinima:18`.
   */
  validaciones?: string[]
  /**
   * Al EDITAR, las `validaciones` solo se aplican si el valor cambió respecto del registro (ej. un
   * vencimiento ya pasado no impide guardar otros datos). En el alta se aplican siempre.
   */
  soloSiCambia?: boolean
  mensajes?: MensajesCampo
  /** Se rellena con un slug de otro campo mientras la persona no lo edite a mano. */
  sugeridoDe?: string
  /** Se muestra pero no se edita ni se envía. */
  soloLectura?: boolean
  ayuda?: string
  /** Con otro campo `mitad` a continuación, los dos comparten fila. */
  mitad?: boolean
  /** `seleccion`: opciones fijas del esquema. */
  opciones?: { valor: string; etiqueta: string }[]
  /**
   * `seleccion`: opciones traídas de la API (`GET /{recurso}` con `pageSize` 100). `valor` y `etiqueta`
   * son nombres de campo de cada elemento (ej. `sigla` y `descripcion`). `excluir` lista valores del
   * catálogo que no se ofrecen (ej. `['SIN']`).
   */
  opcionesDe?: { recurso: string; valor: string; etiqueta: string; params?: Record<string, string>; excluir?: string[] }
  /** `seleccion`: elige una o más opciones (un `<dh-select multiple>`); el valor es un arreglo y `requerido` pide al menos una. */
  multiple?: boolean
  /**
   * `seleccion` múltiple: al editar, propiedad de cada elemento del registro que se guarda cuando llegan
   * objetos (ej. `categorias: [{ sigla, descripcion }]` con `valorDe: 'sigla'`). Por defecto, la de `opcionesDe.valor`.
   */
  valorDe?: string
  /** `seleccion`: valor preseleccionado al crear. `fecha`: `'hoy'` precarga la fecha de hoy al crear. */
  porDefecto?: string
  /** `seleccion`: el valor elegido viaja como número (ej. una dificultad de 1 a 3). */
  numerico?: boolean
  /** `opciones`: largo máximo del texto de cada fila (200). El valor viaja como arreglo de `{ texto, correcta }`. */
  maxTexto?: number
  /**
   * `imagen`: ruta (relativa al API) que firma la subida a Cloudinary y devuelve
   * `{ cloudName, apiKey, timestamp, folder, signature }`. El valor del campo es la URL resultante.
   */
  firma?: string
  /**
   * `archivo`: ruta (relativa al API) que recibe el archivo (multipart, campo `archivo`) y devuelve su
   * descriptor. El descriptor viaja en el cuerpo, bajo este campo, solo si es nuevo.
   */
  subida?: string
  /** `archivo`: tipos MIME aceptados, separados por coma (ej. `application/pdf`). */
  accept?: string
  /** `archivo`: tamaño máximo en bytes (por defecto 20 MB). */
  maxBytes?: number
  /**
   * `ubicacion`: campo del registro cargado cuyo valor se muestra bajo el mapa como dirección (solo
   * lectura). El campo escribe `latitud` y `longitud` en el cuerpo con el prefijo de su ruta
   * (`administrador.ubicacion` escribe `administrador.latitud` y `administrador.longitud`).
   */
  direccionDe?: string
}

export interface Seccion {
  titulo?: string
  campos: Campo[]
}

export interface Accion {
  id: string
  etiqueta: string
  metodo: 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  /** Relativa al recurso; `{id}` se reemplaza por el identificador (ej. `{id}/reactivar`). */
  ruta: string
  /** Si existe, pide confirmación en un diálogo con ese título (ej. "¿Desactivar la escuela?"). */
  confirmar?: string
  /** Texto de apoyo del diálogo de confirmación. */
  confirmarTexto?: string
  /** Nombre de un icono de `@drivehub/ui` que se muestra junto a la etiqueta. */
  icono?: string
  mensajeExito: string
  visibleSi?: Condicion
  tono?: 'peligro'
  /**
   * Si existe, la acción pide datos antes de enviarse: abre el mismo panel lateral de crear y editar con estas
   * secciones y, al enviar, llama a `ruta` con `metodo` y el cuerpo del formulario (como crear). Gana sobre `confirmar`.
   * Un error de negocio del backend sin campo en `erroresDeCampo` se muestra con su mensaje en el panel.
   */
  formulario?: {
    titulo?: string
    descripcion?: string
    /** Texto del botón de enviar (por defecto, la etiqueta de la acción). */
    etiquetaEnviar?: string
    secciones: Seccion[]
  }
}

export interface Crear {
  /** Texto del botón que abre el formulario. */
  etiqueta: string
  titulo?: string
  descripcion?: string
  etiquetaEnviar?: string
  secciones: Seccion[]
  mensajeExito: string
}

export interface Editar {
  titulo?: string
  descripcion?: string
  etiquetaEnviar?: string
  /** Ruta (relativa al recurso, con `{id}`) para cargar el registro; por defecto `{id}`. */
  cargar?: string
  /** Ruta para guardar (con `{id}`); por defecto `{id}`. */
  ruta?: string
  metodo?: 'PATCH' | 'PUT'
  secciones: Seccion[]
  mensajeExito: string
  /** Icono del botón "Editar" (por defecto `edit`). */
  icono?: string
  /** El botón "Editar" solo aparece en las filas que cumplen esto. */
  visibleSi?: Condicion
  /** Si el registro cargado cumple esto, no se muestra el formulario sino el mensaje. */
  bloqueo?: { campo: string; valor: unknown; mensaje: string }
}

/** Un dato del panel de detalle: mismo significado que en `Columna` (`tipo`, `insignias`). */
export interface CampoDetalle {
  /** Campo del registro (admite puntos). */
  campo: string
  titulo: string
  tipo?: 'texto' | 'fecha' | 'insignia' | 'foto' | 'etiquetas'
  /** `tipo: 'foto'`: campo con el nombre completo, para las iniciales cuando la persona no tiene foto. */
  nombre?: string
  /** Para `tipo: 'insignia'`: cómo se muestra cada valor. */
  insignias?: Record<string, Insignia>
  /** `tipo: 'etiquetas'`: propiedad que se muestra de cada objeto del arreglo (ej. `sigla`). */
  etiqueta?: string
  /** Con otro campo `mitad` a continuación, los dos comparten fila. */
  mitad?: boolean
  /** Texto que se muestra cuando el valor está vacío (por defecto "—", ej. "Vigente" para una fecha de fin). */
  vacioTexto?: string
}

export interface SeccionDetalle {
  titulo?: string
  campos: CampoDetalle[]
}

/** Panel lateral de solo lectura con los datos de un registro (sin formulario ni "Guardar"). */
export interface Detalle {
  titulo?: string
  descripcion?: string
  /** Ruta (relativa al recurso, con `{id}`) para cargar el registro; por defecto `{id}`. */
  cargar?: string
  /** Icono del botón (por defecto `eye`). */
  icono?: string
  /** Texto del botón (por defecto "Ver detalles"). */
  etiqueta?: string
  /** El botón solo aparece en las filas que cumplen esto. */
  visibleSi?: Condicion
  secciones: SeccionDetalle[]
  /**
   * Detalle en forma de lista (ej. el historial de un auto): la respuesta de `cargar` es un **arreglo** y el panel
   * dibuja una tarjeta por fila con estos datos, en lugar de las `secciones` (que entonces va como `[]`). Sin filas se muestra `vacio`.
   */
  lista?: { columnas: CampoDetalle[]; vacio?: string }
}

export interface EsquemaGestion {
  /** Recurso de la API: `escuelas` es `{apiBaseUrl}/escuelas`. */
  recurso: string
  titulo: string
  descripcion?: string
  /** Campo identificador del registro (por defecto `codigo`). */
  idCampo?: string
  nombreSingular?: string
  columnas: Columna[]
  /** Si existe, muestra el buscador (parámetro `search`). */
  busqueda?: { placeholder?: string }
  filtros?: Filtro[]
  porPagina?: number
  crear?: Crear
  editar?: Editar
  /** Si existe, cada fila tiene un botón que abre el registro en un panel de solo lectura. */
  detalle?: Detalle
  acciones?: Accion[]
  /** Errores de negocio del backend que se muestran bajo un campo del formulario. */
  erroresDeCampo?: Record<string, { campo: string; mensaje: string }>
  mensajes?: { vacio?: string; sinResultados?: string; errorGuardar?: string }
}

/** El elemento <dh-gestion>: se le asigna `esquema` como PROPIEDAD, no como atributo. */
export declare class DhGestion extends HTMLElement {
  esquema: EsquemaGestion | null
}

export declare function registrarGestion(): void

export declare class ErrorApi extends Error {
  codigo: string
  estado: number
  constructor(codigo: string, mensaje: string, estado?: number)
}

export declare function solicitar(
  metodo: string,
  ruta: string,
  opciones?: {
    params?: Record<string, string | number | null | undefined>
    cuerpo?: unknown
    formulario?: FormData
  }
): Promise<{ data: any; meta?: any }>

export declare const PATRONES: Record<string, { regex: RegExp; mensaje: string }>
export declare function slugDesdeTexto(texto: string, largoMaximo?: number): string
export declare function edadEnAnios(fechaNacimiento: string, hoy?: Date): number
export declare function fechaDeHoy(hoy?: Date): string
export declare function fechaDeManana(hoy?: Date): string
export declare function validar(
  campo: Campo,
  valor: unknown,
  hoy?: Date,
  contexto?: { valorInicial?: unknown }
): string | null

declare global {
  interface HTMLElementTagNameMap {
    'dh-gestion': DhGestion
  }
}
