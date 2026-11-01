// * Importarlo registra <dh-gestion> (una sola vez aunque otro remoto traiga su copia)
import '@drivehub/gestion'

import { ESQUEMA_RESULTADOS } from '../schemas/resultados.esquema'

/**
 * Resultados y notas de la escuela (CU23, Administrador de escuela). Es solo el anfitrión de <dh-gestion>: el
 * listado y el filtro los dibuja el Web Component a partir del esquema.
 */
export function ResultadosGestion() {
  return <dh-gestion esquema={ESQUEMA_RESULTADOS} />
}
