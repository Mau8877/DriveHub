import { DhGestion } from './dh-gestion.js'

/**
 * Registra <dh-gestion>. Se registra una sola vez aunque dos remotos incluyan su propia copia
 * del paquete: gana la primera (customElements no admite redefinir un nombre).
 */
export function registrarGestion() {
  if (!customElements.get('dh-gestion')) {
    customElements.define('dh-gestion', DhGestion)
  }
}

registrarGestion()

export { DhGestion }
export { ErrorApi, solicitar } from './cliente-api.js'
export { PATRONES, edadEnAnios, fechaDeHoy, fechaDeManana, slugDesdeTexto, validar } from './reglas.js'
