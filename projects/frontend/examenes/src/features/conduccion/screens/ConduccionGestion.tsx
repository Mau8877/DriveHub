// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_CONDUCCION } from '../schemas/conduccion.esquema'

/**
 * Gestionar los exámenes de conducción (CU22, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado, el formulario y las acciones los dibuja el Web Component a partir del esquema.
 */
export function ConduccionGestion() {
  return <dh-gestion esquema={ESQUEMA_CONDUCCION} />
}
