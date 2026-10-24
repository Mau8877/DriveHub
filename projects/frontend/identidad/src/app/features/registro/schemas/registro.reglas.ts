import type { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms'

/**
 * Reglas de validación del registro del estudiante (CU01).
 *
 * * Deben coincidir con las del backend y las de mobile
 * * (projects/mobile/lib/features/auth/validators/registro_validators.dart).
 * * CU08a: CI (5 a 15 caracteres: letras, números y guion) y coordenadas del mapa.
 */
export const EDAD_MINIMA_REGISTRO = 18
export const PASSWORD_MIN = 9
export const PASSWORD_MAX = 64
export const NOMBRES_MAX = 80
export const APELLIDOS_MAX = 80
export const EMAIL_MAX = 120
export const TELEFONO_MAX = 20
export const CI_MIN = 5
export const CI_MAX = 15
const CI_PATRON = /^[A-Za-z0-9-]+$/

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

/**
 * La última fecha de nacimiento que cumple la edad mínima (hoy menos EDAD_MINIMA_REGISTRO años). Es lo
 * que el calendario deja elegir como máximo; el validador `edadMinima` sigue siendo la regla.
 * El 29 de febrero cae el 28 si el año de destino no es bisiesto.
 */
export function fechaMaximaDeNacimiento(hoy: Date = new Date()): string {
  const anio = hoy.getFullYear() - EDAD_MINIMA_REGISTRO
  const ultimoDiaDelMes = new Date(anio, hoy.getMonth() + 1, 0).getDate()
  return fechaDeHoyComoTexto(
    new Date(anio, hoy.getMonth(), Math.min(hoy.getDate(), ultimoDiaDelMes))
  )
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

/** CI de 5 a 15 caracteres, solo letras, números y guion (sin espacios: se recorta antes de validar). */
export const ciValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '').trim()
  if (valor === '') return null
  return valor.length < CI_MIN || valor.length > CI_MAX || !CI_PATRON.test(valor)
    ? { ciFormato: true }
    : null
}

/** Una coordenada obligatoria y dentro de [-limite, limite]. */
export function coordenada(limite: number): ValidatorFn {
  return (control) => {
    const valor = control.value as number | null
    if (valor === null || valor === undefined) return { required: true }
    return !Number.isFinite(valor) || valor < -limite || valor > limite
      ? { coordenada: true }
      : null
  }
}

export const LATITUD_MAX = 90
export const LONGITUD_MAX = 180
