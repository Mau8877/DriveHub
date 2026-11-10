/** Una opción de un filtro (estado o tipo) del catálogo de reportes (CU52). */
export interface OpcionCatalogo {
  valor: string
  etiqueta: string
}

export interface ColumnaCatalogo {
  clave: string
  etiqueta: string
  tipo: string
}

/** Una fuente de datos del catálogo: lo único que se puede pedir en un reporte. */
export interface FuenteCatalogo {
  clave: string
  nombre: string
  descripcion: string
  columnas: ColumnaCatalogo[]
  porDefecto: string[]
  estados: OpcionCatalogo[]
  tipos: OpcionCatalogo[]
  filtraPorFecha: boolean
}

/** Lo que devuelve `POST /reportes/interpretar`: la especificación propuesta y lo que se descartó. */
export interface Interpretacion<E> {
  especificacion: E
  descartado: string[]
}

/** El archivo generado, listo para descargar. */
export interface ArchivoGenerado {
  contenido: Blob
  nombre: string
  filas: number
}
