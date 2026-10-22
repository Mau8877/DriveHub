/**
 * Tipos del perfil de la cuenta propia (CU06). Coinciden con el contrato de
 * /api/v1/usuarios/me del backend.
 */
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
}

/** Solo los campos que cambian; `telefono: null` lo borra. */
export interface EditarPerfilRequest {
  nombres?: string
  apellidos?: string
  telefono?: string | null
  fechaNacimiento?: string
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
