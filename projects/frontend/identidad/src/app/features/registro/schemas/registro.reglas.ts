import type { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms'

/**
 * Reglas de validación del registro del estudiante (CU01).
 *
 * * Deben coincidir con las del backend y las de mobile
 * * (projects/mobile/lib/features/auth/validators/registro_validators.dart).
 */
export const EDAD_MINIMA_REGISTRO = 18
export const PASSWORD_MIN = 9
export const PASSWORD_MAX = 64
export const NOMBRES_MAX = 80
export const APELLIDOS_MAX = 80
export const EMAIL_MAX = 120
export const TELEFONO_MAX = 20

/** Años cumplidos a la fecha `hoy` (fecha de nacimiento como yyyy-MM-dd). */
export function edadEnAnios(fechaNacimiento: string, hoy: Date = new Date()): number {
  const [anio, mes, dia] = fechaNacimiento.split('-').map(Number)
  let edad = hoy.getFullYear() - anio
  const cumpleEsteAnio =
    hoy.getMonth() + 1 > mes || (hoy.getMonth() + 1 === mes && hoy.getDate() >= dia)
  if (!cumpleEsteAnio) edad -= 1
  return edad
}

function fechaDeHoyComoTexto(hoy: Date): string {
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const dia = String(hoy.getDate()).padStart(2, '0')
  return `${hoy.getFullYear()}-${mes}-${dia}`
}

/** Obligatorio y que no sea solo espacios. */
export const obligatorioConTexto: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null =>
  String(control.value ?? '').trim() === '' ? { required: true } : null

/** La fecha de nacimiento debe estar en el pasado. */
export function fechaNoFutura(hoy: () => Date = () => new Date()): ValidatorFn {
  return (control) => {
    const valor = control.value as string
    if (!valor) return null
    return valor >= fechaDeHoyComoTexto(hoy()) ? { fechaFutura: true } : null
  }
}

/** Al menos EDAD_MINIMA_REGISTRO años cumplidos; el día en que cumple 18 es válido. */
export function edadMinima(hoy: () => Date = () => new Date()): ValidatorFn {
  return (control) => {
    const valor = control.value as string
    if (!valor) return null
    return edadEnAnios(valor, hoy()) < EDAD_MINIMA_REGISTRO ? { menorDeEdad: true } : null
  }
}
