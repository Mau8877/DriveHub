// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_PREGUNTAS } from '../schemas/preguntas.esquema'

/**
 * Gestionar el banco de preguntas (CU19, Administrador de escuela). Es solo el anfitrión de
 * <dh-gestion>: el listado, el formulario y las acciones los dibuja el Web Component a partir del
 * esquema. React 19 le pasa el esquema como propiedad (ver GESTION_FRONTEND.md).
 */
export function PreguntasGestion() {
  return <dh-gestion esquema={ESQUEMA_PREGUNTAS} />
}
