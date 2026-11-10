import { useMutation, useQuery } from '@tanstack/react-query'

import { apiClient } from '@/store'

import type { EspecificacionReporte } from '../schemas/especificacionSchema'
import type { ArchivoGenerado, FuenteCatalogo, Interpretacion } from '../types/reportes.types'
import { reportesKeys } from './keys'

/** Las fuentes, columnas y filtros que se pueden pedir. Cambia solo con un despliegue: no se vuelve a pedir. */
export function useCatalogoReportes() {
  return useQuery({
    queryKey: reportesKeys.catalogo(),
    queryFn: async () => (await apiClient.get<FuenteCatalogo[]>('/reportes/catalogo')).data,
    staleTime: Infinity,
  })
}

/** Traduce el pedido (texto o voz) a una especificación del catálogo, con el modelo de OpenRouter. */
export function useInterpretarReporte() {
  return useMutation({
    mutationFn: async (texto: string) =>
      (await apiClient.post<Interpretacion<EspecificacionReporte>>('/reportes/interpretar', { texto }))
        .data,
  })
}

/** El nombre que manda el backend en `Content-Disposition` (`filename*=UTF-8''...`), o uno por defecto. */
export function nombreDeArchivo(cabecera: string | undefined, formato: string): string {
  const coincidencia = cabecera?.match(/filename\*=UTF-8''([^;]+)/i)
  if (coincidencia) {
    try {
      return decodeURIComponent(coincidencia[1])
    } catch {
      // * Nombre mal codificado: se usa el de por defecto
    }
  }
  return `reporte.${formato}`
}

/** Genera el archivo con los datos reales de la escuela. */
export function useGenerarReporte() {
  return useMutation({
    mutationFn: async (especificacion: EspecificacionReporte): Promise<ArchivoGenerado> => {
      const respuesta = await apiClient.post<Blob>('/reportes/generar', especificacion, {
        responseType: 'blob',
      })
      return {
        contenido: respuesta.data,
        nombre: nombreDeArchivo(respuesta.headers['content-disposition'], especificacion.formato),
        filas: Number(respuesta.headers['x-reporte-filas'] ?? 0),
      }
    },
  })
}

/** Descarga el archivo en el navegador (enlace temporal que se libera enseguida). */
export function descargar(archivo: ArchivoGenerado) {
  const url = URL.createObjectURL(archivo.contenido)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = archivo.nombre
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}
