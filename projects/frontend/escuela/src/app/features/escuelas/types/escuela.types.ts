/** Estado de una escuela: derivado de su borrado lógico (activa si no está eliminada). */
export type EstadoEscuela = 'ACTIVA' | 'INACTIVA'

/** Una escuela en el listado del Super admin (CU05). */
export interface EscuelaListada {
  codigo: string
  slug: string
  nombre: string
  estado: EstadoEscuela
  creadoEn: string
}

/** La escuela en cualquier estado, tal como la devuelve la consulta por código. */
export interface EscuelaEstado {
  codigo: string
  slug: string
  nombre: string
  activa: boolean
}

export interface AdministradorRequest {
  nombres: string
  apellidos: string
  fechaNacimiento: string
  email: string
  password: string
  telefono?: string
}

export interface CrearEscuelaRequest {
  nombre: string
  slug: string
  administrador: AdministradorRequest
}

export interface EscuelaCreada {
  codigo: string
  slug: string
  nombre: string
  estado: EstadoEscuela
  creadoEn: string
  administrador: { codigo: string; email: string; nombres: string; apellidos: string }
}

export interface FiltroEscuelas {
  page: number
  pageSize: number
  search: string
  /** Vacío trae activas e inactivas. */
  estado: EstadoEscuela | ''
}

export interface MetaPagina {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

/** Un listado paginado, como lo entrega el `apiInterceptor` (`data` y `meta`). */
export interface PaginaDeEscuelas {
  data: EscuelaListada[]
  meta: MetaPagina
}
