/** Un enlace del menú lateral. */
export interface ItemNavegacion {
  /** Ruta a la que lleva (ej. `/escuelas`). Es también lo que se compara con `activo`. */
  ruta: string
  etiqueta: string
  descripcion?: string
  /** Nombre de un icono de `ICONOS` (ej. `school`). */
  icono?: string
  /** Si cambia entre items consecutivos, se dibuja un título de grupo. */
  grupo?: string
  insignia?: string | number
}

export interface MarcaShell {
  prefijo?: string
  resaltado?: string
  ruta?: string
}

export interface UsuarioShell {
  nombres: string
  apellidos: string
  fotoUrl?: string | null
  /** Texto bajo el nombre en el menú de la cuenta (ej. "Super admin"). */
  rol?: string
}

/** Una opción del menú de la cuenta. Con `ruta` navega; sin `ruta` emite `dh-accion`. */
export interface AccionUsuario {
  id: string
  etiqueta: string
  icono?: string
  tono?: 'peligro'
  ruta?: string
}

export interface Miga {
  etiqueta: string
  ruta?: string
}

export interface PuntoAuth {
  icono: string
  texto: string
}

export interface DetalleNavegar {
  ruta: string
  item?: ItemNavegacion
  miga?: Miga
  accion?: AccionUsuario
}

export interface DetalleAccion {
  id: string
  accion: AccionUsuario
}

export class DhAvatar extends HTMLElement {
  nombres: string
  apellidos: string
  fotoUrl: string | null
  tam: number
}

export class DhDialogo extends HTMLElement {
  abierto: boolean
  titulo: string
  texto: string
  aceptar: string
  peligro: boolean
  ocupado: boolean
}

export class DhIcono extends HTMLElement {
  nombre: string
  tam: number
}

/** Detalle del evento `dh-ubicacion` de <dh-mapa-ubicacion>: el punto elegido, con 6 decimales. */
export interface DetalleUbicacion {
  latitud: number
  longitud: number
}

/**
 * <dh-mapa-ubicacion>: elegir un punto en un mapa de OpenStreetMap. Emite `dh-ubicacion` con
 * `DetalleUbicacion`. No calcula la dirección: la calcula el servidor al guardar.
 */
export class DhMapaUbicacion extends HTMLElement {
  latitud: number | null
  longitud: number | null
  /** Texto de solo lectura que se muestra bajo el mapa (la dirección ya guardada). */
  direccion: string
  deshabilitado: boolean
  /** `[latitud, longitud]` donde se centra el mapa mientras no hay punto (Santa Cruz de la Sierra por defecto). */
  centro: [number, number]
}

/** Detalle del evento `dh-cambio` de <dh-select>. */
export interface DetalleCambio {
  valor: string
}

/** Detalle del evento `dh-cambio` de <dh-select multiple>: los valores elegidos, en el orden de `opciones`. */
export interface DhCambioMultiple {
  valores: string[]
}

export interface OpcionSelect {
  valor: string
  etiqueta: string
}

/**
 * Lista desplegable propia (reemplaza al `<select>` nativo). Emite `dh-cambio` ({@link DetalleCambio})
 * al elegir y `dh-blur` cuando el foco sale. Con `sinVacio` no ofrece quitar la elección.
 */
export class DhSelect extends HTMLElement {
  valor: string
  opciones: OpcionSelect[]
  placeholder: string
  sinVacio: boolean
  /** Elige varios valores (`valores`) y los muestra como fichas; emite `dh-cambio` con {@link DhCambioMultiple}. */
  multiple: boolean
  /** Valores elegidos en modo `multiple`. */
  valores: string[]
  /** Nombre accesible del control. */
  etiqueta: string
  deshabilitado: boolean
  invalido: boolean
}

/**
 * Selector de fecha propio (reemplaza al `<input type="date">` nativo). `valor`, `min` y `max` son
 * 'yyyy-MM-dd'. Se puede escribir (dd/mm/aaaa) o elegir en el calendario. Emite `dh-cambio`
 * ({@link DetalleCambio}) al elegir, escribir una fecha válida o borrarla (`''`), y `dh-blur` al salir.
 */
export class DhFecha extends HTMLElement {
  valor: string
  min: string
  max: string
  placeholder: string
  /** Nombre accesible del control. */
  etiqueta: string
  deshabilitado: boolean
  invalido: boolean
}

/** 'yyyy-MM-dd' -> 'dd/MM/yyyy' ('' si no es una fecha real). */
export declare function formatearFecha(valor: string): string
/** 'dd/MM/yyyy' -> 'yyyy-MM-dd', o null si no es una fecha real. */
export declare function analizarFecha(texto: string): string | null

export class DhSidebar extends HTMLElement {
  items: ItemNavegacion[]
  activo: string
  marca: MarcaShell | null
  etiquetaGrupo: string
  colapsado: boolean
  colapsable: boolean
  abierto: boolean
  navegacionNativa: boolean
}

export class DhHeader extends HTMLElement {
  migas: Miga[]
  usuario: UsuarioShell | null
  accionesUsuario: AccionUsuario[]
  avisos: boolean | number
  buscador: boolean
  menuMovil: boolean
}

export class DhShell extends HTMLElement {
  items: ItemNavegacion[]
  activo: string
  marca: MarcaShell | null
  etiquetaGrupo: string
  migas: Miga[]
  usuario: UsuarioShell | null
  accionesUsuario: AccionUsuario[]
  avisos: boolean | number
  buscador: boolean
  colapsable: boolean
  recordarColapso: boolean
}

export class DhAuthLayout extends HTMLElement {
  titulo: string
  mensaje: string
  puntos: PuntoAuth[]
  marca: MarcaShell | null
}

/** Trazados SVG de los iconos disponibles, por nombre. */
export const ICONOS: Record<string, string>

/** Iniciales (hasta dos) de nombres y apellidos, en mayúscula. */
export function iniciales(nombres?: string, apellidos?: string): string

/** Registra los componentes (se hace solo al importar el paquete; seguro de llamar varias veces). */
export function registrarUi(): void

declare global {
  interface HTMLElementTagNameMap {
    'dh-avatar': DhAvatar
    'dh-dialogo': DhDialogo
    'dh-icono': DhIcono
    'dh-mapa-ubicacion': DhMapaUbicacion
    'dh-select': DhSelect
    'dh-fecha': DhFecha
    'dh-sidebar': DhSidebar
    'dh-header': DhHeader
    'dh-shell': DhShell
    'dh-auth-layout': DhAuthLayout
  }
}
