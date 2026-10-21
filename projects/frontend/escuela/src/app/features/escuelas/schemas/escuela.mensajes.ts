import type { ValidationErrors } from '@angular/forms'

import {
  APELLIDOS_MAX,
  EDAD_MINIMA_ADMIN,
  EMAIL_MAX,
  NOMBRES_MAX,
  NOMBRE_ESCUELA_MAX,
  NOMBRE_ESCUELA_MIN,
  PASSWORD_MAX,
  PASSWORD_MIN,
  SLUG_MAX,
  SLUG_MIN,
  TELEFONO_MAX,
} from './escuela.reglas'

export type CampoEscuela =
  | 'nombre'
  | 'slug'
  | 'nombres'
  | 'apellidos'
  | 'fechaNacimiento'
  | 'email'
  | 'password'
  | 'telefono'

const OBLIGATORIO: Record<CampoEscuela, string> = {
  nombre: 'El nombre es obligatorio',
  slug: 'El slug es obligatorio',
  nombres: 'Los nombres son obligatorios',
  apellidos: 'Los apellidos son obligatorios',
  fechaNacimiento: 'La fecha de nacimiento es obligatoria',
  email: 'El correo es obligatorio',
  password: 'La contraseña es obligatoria',
  telefono: 'El teléfono es obligatorio',
}

const MAXIMO: Partial<Record<CampoEscuela, string>> = {
  nombre: `El nombre debe tener entre ${NOMBRE_ESCUELA_MIN} y ${NOMBRE_ESCUELA_MAX} caracteres`,
  slug: `El slug debe tener entre ${SLUG_MIN} y ${SLUG_MAX} caracteres`,
  nombres: `Los nombres no pueden superar ${NOMBRES_MAX} caracteres`,
  apellidos: `Los apellidos no pueden superar ${APELLIDOS_MAX} caracteres`,
  email: `El correo no puede superar ${EMAIL_MAX} caracteres`,
  password: `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres`,
  telefono: `El teléfono no puede superar ${TELEFONO_MAX} caracteres`,
}

/** Mensaje en español para el primer error de un campo, o null si es válido. */
export function mensajeDeError(
  campo: CampoEscuela,
  errores: ValidationErrors | null
): string | null {
  if (!errores) return null
  if (errores['required']) return OBLIGATORIO[campo]
  if (errores['email']) return 'El correo no es válido'
  if (errores['minlength'] || errores['maxlength']) return MAXIMO[campo] ?? null
  if (errores['slugInvalido'])
    return 'El slug solo admite minúsculas, números y guiones, sin guion al inicio ni al final'
  if (errores['fechaFutura']) return 'La fecha de nacimiento debe estar en el pasado'
  if (errores['menorDeEdad'])
    return `El administrador debe tener al menos ${EDAD_MINIMA_ADMIN} años`
  return null
}
