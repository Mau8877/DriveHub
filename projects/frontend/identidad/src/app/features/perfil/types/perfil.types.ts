/**
 * Tipos del perfil de la cuenta propia (CU06, CU08a). Coinciden con el contrato de
 * /api/v1/usuarios/me del backend.
 */
export type Sexo = 'MASCULINO' | 'FEMENINO'

export interface Perfil {
  codigo: string
  nombres: string
  apellidos: string
  /** yyyy-MM-dd */
  fechaNacimiento: string
  telefono: string | null
  email: string
  correoVerificado: boolean
  roles: string[]
  /** null en el Super admin */
  escuelaCodigo: string | null
  escuelaNombre: string | null
  /** URL prefirmada de lectura (vale 60 minutos), o null si no tiene foto */
  fotoUrl: string | null
  ci: string
  sexo: Sexo
  /** La calcula el servidor desde el punto del mapa; null si no se pudo (o en el Super admin) */
  direccion: string | null
  latitud: number | null
  longitud: number | null
  /** Solo el estudiante la tiene y solo se lee: la asigna la escuela */
  categoriaSigla: string | null
}

/** Solo los campos que cambian; `telefono: null` lo borra. */
export interface EditarPerfilRequest {
  nombres?: string
  apellidos?: string
  telefono?: string | null
  fechaNacimiento?: string
  ci?: string
  sexo?: Sexo
  /** Van juntas; el servidor recalcula la dirección. No hay `direccion` ni `categoriaSigla`. */
  latitud?: number
  longitud?: number
}

export interface CambiarContrasenaRequest {
  passwordActual: string
  passwordNueva: string
  dispositivo: 'WEB'
}

export interface CambiarCorreoRequest {
  passwordActual: string
  correoNuevo: string
}
