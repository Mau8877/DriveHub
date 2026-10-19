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
}

/**
 * En la web el refresh token NO viene aquí: viaja en una cookie HttpOnly que
 * el código de la página no puede leer.
 */
export interface SesionRespuesta {
  accessToken: string
  expiraEnSegundos: number
  usuario: UsuarioSesion
}
