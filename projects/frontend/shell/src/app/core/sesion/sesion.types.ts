/**
 * Tipos de la sesión web (CU02). Coinciden con el contrato de
 * POST /api/v1/auth/login y /auth/refresh.
 */
export interface LoginRequest {
  email: string
  password: string
  dispositivo: 'WEB'
}

export interface UsuarioSesion {
  codigo: string
  nombres: string
  apellidos: string
  roles: string[]
  escuelaCodigo: string | null
  /** El correo se verificó con el enlace enviado (CU04b); no bloquea nada, solo la recuperación. */
  correoVerificado: boolean
  /** URL prefirmada de la foto de perfil (CU06), o null si no tiene; se renueva con la sesión. */
  fotoUrl: string | null
}

/** Lo que manda el backend: omite `fotoUrl` cuando la cuenta no tiene foto. */
export type UsuarioSesionRespuesta = Omit<UsuarioSesion, 'fotoUrl'> & { fotoUrl?: string | null }

/**
 * En la web el refresh token NO viene aquí: viaja en una cookie HttpOnly que
 * el código de la página no puede leer.
 */
export interface SesionRespuesta {
  accessToken: string
  expiraEnSegundos: number
  usuario: UsuarioSesionRespuesta
}
