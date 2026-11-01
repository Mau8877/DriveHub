// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_SIMULACROS } from '../schemas/simulacros.esquema'

/**
 * Configurar simulacros escritos (CU20, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado, el formulario y las acciones los dibuja el Web Component a partir del esquema.
 */
export function SimulacrosGestion() {
  return <dh-gestion esquema={ESQUEMA_SIMULACROS} />
}
