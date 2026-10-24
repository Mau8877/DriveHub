/**
 * Revisión previa de la foto en el navegador (CU06): solo da feedback inmediato; el backend es el
 * que decide (revisa el contenido real del archivo). Debe coincidir con sus límites.
 */
export const FOTO_TIPOS = ['image/jpeg', 'image/png', 'image/webp']
export const FOTO_MAX_BYTES = 5 * 1024 * 1024
export const FOTO_MAX_LADO_PX = 4000

export const MENSAJE_FOTO_TIPO = 'Solo se admiten imágenes JPG, PNG o WebP'
export const MENSAJE_FOTO_PESO = 'La imagen no puede superar 5 MB'
export const MENSAJE_FOTO_DIMENSIONES = 'La imagen no puede superar 4000×4000 píxeles'
export const MENSAJE_FOTO_ILEGIBLE = 'No pudimos leer la imagen'

export interface Dimensiones {
  ancho: number
  alto: number
}

/** Lee el ancho y el alto del archivo sin subirlo. */
export async function leerDimensiones(archivo: File): Promise<Dimensiones> {
  if (typeof createImageBitmap === 'function') {
    const imagen = await createImageBitmap(archivo)
    const dimensiones = { ancho: imagen.width, alto: imagen.height }
    imagen.close()
    return dimensiones
  }
  const url = URL.createObjectURL(archivo)
  try {
    return await new Promise<Dimensiones>((resolver, rechazar) => {
      const imagen = new Image()
      imagen.onload = () => resolver({ ancho: imagen.naturalWidth, alto: imagen.naturalHeight })
      imagen.onerror = () => rechazar(new Error('imagen ilegible'))
      imagen.src = url
    })
  } finally {
    URL.revokeObjectURL(url)
  }
}

/** El mensaje del primer problema del archivo, o null si puede enviarse. */
export async function validarFoto(
  archivo: File,
  dimensiones: (archivo: File) => Promise<Dimensiones> = leerDimensiones
): Promise<string | null> {
  if (!FOTO_TIPOS.includes(archivo.type)) return MENSAJE_FOTO_TIPO
  if (archivo.size > FOTO_MAX_BYTES) return MENSAJE_FOTO_PESO
  try {
    const { ancho, alto } = await dimensiones(archivo)
    return ancho > FOTO_MAX_LADO_PX || alto > FOTO_MAX_LADO_PX ? MENSAJE_FOTO_DIMENSIONES : null
  } catch {
    return MENSAJE_FOTO_ILEGIBLE
  }
}
