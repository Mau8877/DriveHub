/** Condición sobre un registro: el campo (con puntos para anidados) igual o distinto de un valor. */
export interface Condicion {
  campo: string
  igual?: unknown
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
  tipo?: 'texto' | 'fecha' | 'insignia'
  /** Para `tipo: 'insignia'`: cómo se muestra cada valor. */
  insignias?: Record<string, Insignia>
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
  fechaNoFutura?: string
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
  tipo?: 'texto' | 'email' | 'password' | 'fecha' | 'telefono' | 'seleccion' | 'ubicacion'
  requerido?: boolean
  /** Largo mínimo y máximo del texto. */
  min?: number
  max?: number
  /** Nombre de un patrón (`slug`) o una expresión regular. */
  patron?: string
  /** Reglas con nombre: `fechaNoFutura`, `edadMinima:18`. */
  validaciones?: string[]
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
   * son nombres de campo de cada elemento (ej. `sigla` y `descripcion`).
   */
  opcionesDe?: { recurso: string; valor: string; etiqueta: string; params?: Record<string, string> }
  /** `seleccion`: valor preseleccionado al crear. */
  porDefecto?: string
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
  tipo?: 'texto' | 'fecha' | 'insignia'
  /** Para `tipo: 'insignia'`: cómo se muestra cada valor. */
  insignias?: Record<string, Insignia>
  /** Con otro campo `mitad` a continuación, los dos comparten fila. */
  mitad?: boolean
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
  opciones?: { params?: Record<string, string | number | null | undefined>; cuerpo?: unknown }
): Promise<{ data: any; meta?: any }>

export declare const PATRONES: Record<string, { regex: RegExp; mensaje: string }>
export declare function slugDesdeTexto(texto: string, largoMaximo?: number): string
export declare function edadEnAnios(fechaNacimiento: string, hoy?: Date): number
export declare function validar(campo: Campo, valor: unknown, hoy?: Date): string | null

declare global {
  interface HTMLElementTagNameMap {
    'dh-gestion': DhGestion
  }
}
