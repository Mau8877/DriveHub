import type { ValidationErrors } from '@angular/forms'

import {
  APELLIDOS_MAX,
  CI_MAX,
  CI_MIN,
  EDAD_MINIMA_REGISTRO,
  EMAIL_MAX,
  NOMBRES_MAX,
  PASSWORD_MAX,
  PASSWORD_MIN,
  TELEFONO_MAX,
} from './registro.reglas'

export type CampoRegistro =
  | 'nombres'
  | 'apellidos'
  | 'fechaNacimiento'
  | 'email'
  | 'password'
  | 'telefono'
  | 'ci'
  | 'sexo'
  | 'ubicacion'

const OBLIGATORIO: Record<CampoRegistro, string> = {
  nombres: 'Los nombres son obligatorios',
  apellidos: 'Los apellidos son obligatorios',
  fechaNacimiento: 'La fecha de nacimiento es obligatoria',
  email: 'El correo es obligatorio',
  password: 'La contraseña es obligatoria',
  telefono: 'El teléfono es obligatorio',
  ci: 'El CI es obligatorio',
  sexo: 'Elige tu sexo',
  ubicacion: 'Elige tu ubicación en el mapa',
}

const MAXIMO: Partial<Record<CampoRegistro, string>> = {
  nombres: `Los nombres no pueden superar ${NOMBRES_MAX} caracteres`,
  apellidos: `Los apellidos no pueden superar ${APELLIDOS_MAX} caracteres`,
  email: `El correo no puede superar ${EMAIL_MAX} caracteres`,
  password: `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres`,
  telefono: `El teléfono no puede superar ${TELEFONO_MAX} caracteres`,
}

/** Mensaje en español para el primer error de un campo, o null si es válido. */
export function mensajeDeError(
  campo: CampoRegistro,
  errores: ValidationErrors | null
): string | null {
  if (!errores) return null
  if (errores['required']) return OBLIGATORIO[campo]
  if (errores['email']) return 'El correo no es válido'
  if (errores['ciFormato'])
    return `El CI debe tener entre ${CI_MIN} y ${CI_MAX} caracteres: letras, números y guion`
  if (errores['coordenada']) return 'La ubicación no es válida'
  if (errores['minlength'])
    return `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres`
  if (errores['maxlength']) return MAXIMO[campo] ?? null
  if (errores['fechaFutura']) return 'La fecha de nacimiento debe estar en el pasado'
  if (errores['menorDeEdad']) return `Debes tener al menos ${EDAD_MINIMA_REGISTRO} años`
  return null
}
