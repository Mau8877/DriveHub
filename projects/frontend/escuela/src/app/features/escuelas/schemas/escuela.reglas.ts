import type { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms'

/**
 * Reglas de validación de los formularios de escuelas (CU05).
 *
 * * Las del Administrador (contraseña, edad, longitudes) son las del registro del estudiante de
 * * `identidad` y se DUPLICAN a propósito: ninguna app importa código de otra. Tienen que seguir
 * * iguales a esas, a las del backend y a las de mobile.
 */
export const NOMBRE_ESCUELA_MIN = 3
export const NOMBRE_ESCUELA_MAX = 120
export const SLUG_MIN = 3
export const SLUG_MAX = 60
/** Minúsculas, números y guiones; sin guion al inicio, al final ni doble. */
export const SLUG_PATRON = /^[a-z0-9]+(-[a-z0-9]+)*$/

export const EDAD_MINIMA_ADMIN = 18
export const PASSWORD_MIN = 9
export const PASSWORD_MAX = 64
export const NOMBRES_MAX = 80
export const APELLIDOS_MAX = 80
export const EMAIL_MAX = 120
export const TELEFONO_MAX = 20

/** Propone un slug a partir del nombre: minúsculas, sin tildes y con guiones. */
export function slugDesdeNombre(nombre: string): string {
  return nombre
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, SLUG_MAX)
    .replace(/-+$/g, '')
}

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

/** El slug solo admite minúsculas, números y guiones. Vacío lo cubre `required`. */
export const slugValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '')
  if (valor === '') return null
  return SLUG_PATRON.test(valor) ? null : { slugInvalido: true }
}

/** La fecha de nacimiento debe estar en el pasado. */
export function fechaNoFutura(hoy: () => Date = () => new Date()): ValidatorFn {
  return (control) => {
    const valor = control.value as string
    if (!valor) return null
    return valor >= fechaDeHoyComoTexto(hoy()) ? { fechaFutura: true } : null
  }
}

/** Al menos EDAD_MINIMA_ADMIN años cumplidos; el día en que cumple 18 es válido. */
export function edadMinima(hoy: () => Date = () => new Date()): ValidatorFn {
  return (control) => {
    const valor = control.value as string
    if (!valor) return null
    return edadEnAnios(valor, hoy()) < EDAD_MINIMA_ADMIN ? { menorDeEdad: true } : null
  }
}
