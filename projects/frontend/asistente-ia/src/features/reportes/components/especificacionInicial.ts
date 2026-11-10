import type { EspecificacionReporte } from '../schemas/especificacionSchema'
import type { FuenteCatalogo } from '../types/reportes.types'

/** Una especificación nueva para esa fuente: sus columnas por defecto y sin filtros. */
export function especificacionInicial(fuente: FuenteCatalogo): EspecificacionReporte {
  return {
    fuente: fuente.clave,
    titulo: fuente.nombre,
    columnas: [...fuente.porDefecto],
    filtros: { desde: null, hasta: null, estado: null, tipo: null },
    orden: null,
    formato: 'pdf',
  }
}
