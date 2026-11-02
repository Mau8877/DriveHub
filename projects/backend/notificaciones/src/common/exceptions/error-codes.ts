/**
 * Catálogo de códigos de error del servicio (ERR_NOT_NN). Ver
 * docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md.
 *
 * * Un número no se reutiliza aunque el error desaparezca.
 */
export const ErrorCodes = {
  CREDENCIAL_INVALIDA: 'ERR_NOT_01',
  DISPOSITIVO_NO_ENCONTRADO: 'ERR_NOT_02',
  // Transversal (SYS): lo usan todos los servicios que validan el JWT
  NO_AUTENTICADO: 'ERR_SYS_04',
} as const
