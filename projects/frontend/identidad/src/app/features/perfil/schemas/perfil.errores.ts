import { ApiError } from '../../../core/api/api.types'

const GENERICO = 'Ocurrió un error. Intenta más tarde.'

/** Mensajes en español de los códigos de negocio del perfil (CU06), sin detalles técnicos. */
export const MENSAJES_PERFIL: Record<string, string> = {
  ERR_IDE_01: 'Ese correo ya está en uso',
  ERR_IDE_02: 'Debes tener al menos 18 años',
  ERR_IDE_11: 'El enlace no es válido, venció o ya se usó',
  ERR_IDE_14: 'La contraseña actual no es correcta',
  ERR_IDE_15: 'La contraseña nueva debe ser distinta de la actual',
  ERR_IDE_16: 'El archivo no es una imagen válida. Usa una foto JPG, PNG o WebP',
  ERR_IDE_17: 'La imagen no puede superar 5 MB',
  ERR_IDE_18: 'Ese ya es tu correo',
  ERR_IDE_19: 'Este CI ya está registrado',
}

/** El mensaje de un error de la API; genérico si no es un ApiError o el código no está en el mapa. */
export function mensajeDeErrorPerfil(error: unknown): string {
  return (error instanceof ApiError && MENSAJES_PERFIL[error.code]) || GENERICO
}

/** El código de negocio de un error, o null si no es un ApiError. */
export function codigoDe(error: unknown): string | null {
  return error instanceof ApiError ? error.code : null
}
