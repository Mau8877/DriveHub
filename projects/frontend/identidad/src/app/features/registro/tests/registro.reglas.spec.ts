import { FormControl, Validators } from '@angular/forms'

import { mensajeDeError } from '../schemas/registro.mensajes'
import {
  ciValido,
  coordenada,
  edadEnAnios,
  edadMinima,
  fechaNoFutura,
  obligatorioConTexto,
  PASSWORD_MAX,
  PASSWORD_MIN,
} from '../schemas/registro.reglas'

const HOY = new Date(2026, 9, 19) // 19 de octubre de 2026

describe('registro.reglas', () => {
  describe('edadEnAnios', () => {
    it('cuenta los años cumplidos a la fecha de hoy', () => {
      expect(edadEnAnios('2000-10-19', HOY)).toBe(26)
      expect(edadEnAnios('2000-10-20', HOY)).toBe(25)
    })
  })

  describe('edadMinima', () => {
    const validar = (fecha: string) => edadMinima(() => HOY)(new FormControl(fecha))

    it('rechaza a quien cumple 18 mañana', () => {
      expect(validar('2008-10-20')).toEqual({ menorDeEdad: true })
    })

    it('acepta a quien cumple 18 hoy', () => {
      expect(validar('2008-10-19')).toBeNull()
    })

    it('acepta a un mayor de edad', () => {
      expect(validar('1990-01-01')).toBeNull()
    })

    it('no opina si la fecha está vacía (eso lo valida required)', () => {
      expect(validar('')).toBeNull()
    })
  })

  describe('fechaNoFutura', () => {
    const validar = (fecha: string) => fechaNoFutura(() => HOY)(new FormControl(fecha))

    it('rechaza hoy y fechas futuras', () => {
      expect(validar('2026-10-19')).toEqual({ fechaFutura: true })
      expect(validar('2027-01-01')).toEqual({ fechaFutura: true })
    })

    it('acepta una fecha pasada', () => {
      expect(validar('2026-10-18')).toBeNull()
    })
  })

  describe('contraseña', () => {
    const errores = (valor: string) =>
      Validators.compose([Validators.minLength(PASSWORD_MIN), Validators.maxLength(PASSWORD_MAX)])!(
        new FormControl(valor)
      )

    it('rechaza 8 caracteres, acepta 9 y rechaza 65', () => {
      expect(errores('a'.repeat(8))).not.toBeNull()
      expect(errores('a'.repeat(9))).toBeNull()
      expect(errores('a'.repeat(65))).not.toBeNull()
    })
  })

  describe('obligatorioConTexto', () => {
    it('rechaza vacío y solo espacios', () => {
      expect(obligatorioConTexto(new FormControl(''))).toEqual({ required: true })
      expect(obligatorioConTexto(new FormControl('   '))).toEqual({ required: true })
      expect(obligatorioConTexto(new FormControl('Ana'))).toBeNull()
    })
  })

  describe('mensajeDeError', () => {
    it('devuelve el mensaje en español de cada error', () => {
      expect(mensajeDeError('email', { required: true })).toBe('El correo es obligatorio')
      expect(mensajeDeError('email', { email: true })).toBe('El correo no es válido')
      expect(mensajeDeError('fechaNacimiento', { menorDeEdad: true })).toBe(
        'Debes tener al menos 18 años'
      )
      expect(
        mensajeDeError('password', { minlength: { requiredLength: 9, actualLength: 8 } })
      ).toBe('La contraseña debe tener entre 9 y 64 caracteres')
    })

    it('devuelve null si no hay errores', () => {
      expect(mensajeDeError('email', null)).toBeNull()
    })
  })

  describe('ciValido', () => {
    const validar = (ci: string) => ciValido(new FormControl(ci))

    it('acepta de 5 a 15 caracteres con letras, números y guion', () => {
      expect(validar('12345')).toBeNull()
      expect(validar('AB-12345')).toBeNull()
      expect(validar('1'.repeat(15))).toBeNull()
    })

    it('rechaza menos de 5, más de 15 y los símbolos', () => {
      expect(validar('1234')).toEqual({ ciFormato: true })
      expect(validar('1'.repeat(16))).toEqual({ ciFormato: true })
      expect(validar('AB 1234')).toEqual({ ciFormato: true })
      expect(validar('AB#1234')).toEqual({ ciFormato: true })
    })

    it('no opina si está vacío (eso lo valida obligatorio)', () => {
      expect(validar('')).toBeNull()
    })
  })

  describe('coordenada', () => {
    it('exige valor y lo acepta dentro de los límites, incluidos ellos', () => {
      expect(coordenada(90)(new FormControl(null))).toEqual({ required: true })
      expect(coordenada(90)(new FormControl(-90))).toBeNull()
      expect(coordenada(180)(new FormControl(180))).toBeNull()
    })

    it('rechaza lo que está fuera de rango o no es un número', () => {
      expect(coordenada(90)(new FormControl(90.1))).toEqual({ coordenada: true })
      expect(coordenada(180)(new FormControl(-181))).toEqual({ coordenada: true })
      expect(coordenada(90)(new FormControl(Number.NaN))).toEqual({ coordenada: true })
    })
  })

  describe('mensajes del CI y la ubicación', () => {
    it('cada error tiene su mensaje en español', () => {
      expect(mensajeDeError('ci', { required: true })).toBe('El CI es obligatorio')
      expect(mensajeDeError('ci', { ciFormato: true })).toContain('entre 5 y 15 caracteres')
      expect(mensajeDeError('sexo', { required: true })).toBe('Elige tu sexo')
      expect(mensajeDeError('ubicacion', { required: true })).toBe('Elige tu ubicación en el mapa')
      expect(mensajeDeError('ubicacion', { coordenada: true })).toBe('La ubicación no es válida')
    })
  })
})
