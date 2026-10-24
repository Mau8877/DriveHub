/**
 * Cuerpo de POST /api/v1/usuarios (registro del estudiante, CU01).
 *
 * * No tiene campo de rol a propósito: el backend asigna siempre ESTUDIANTE.
 * * `fechaNacimiento` viaja como yyyy-MM-dd (lo que entrega <input type="date">).
 * * CU08a: suma CI, sexo y la ubicación elegida en el mapa. No hay dirección: la calcula el servidor.
 */
export type Sexo = 'MASCULINO' | 'FEMENINO'

export interface RegistroEstudianteRequest {
  nombres: string
  apellidos: string
  fechaNacimiento: string
  email: string
  password: string
  telefono?: string
  escuelaSlug: string
  ci: string
  sexo: Sexo
  latitud: number
  longitud: number
}

/** Cuenta recién creada (sin contraseña ni sesión). */
export interface UsuarioRegistrado {
  codigo: string
  email: string
  nombres: string
  apellidos: string
}

/** Datos públicos de una escuela (GET /api/v1/escuelas/{slug}). */
export interface EscuelaPublica {
  codigo: string
  slug: string
  nombre: string
}
