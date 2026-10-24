import { FOTO_MAX_BYTES, validarFoto } from '../schemas/foto.reglas'

const archivo = (tipo: string, bytes = 10) => new File([new Uint8Array(bytes)], 'f', { type: tipo })
const dim = (ancho: number, alto: number) => () => Promise.resolve({ ancho, alto })

describe('validarFoto', () => {
  it('acepta JPG, PNG y WebP dentro de los límites', async () => {
    for (const tipo of ['image/jpeg', 'image/png', 'image/webp']) {
      expect(await validarFoto(archivo(tipo), dim(100, 100))).toBeNull()
    }
  })

  it('rechaza otros tipos', async () => {
    expect(await validarFoto(archivo('image/gif'), dim(1, 1))).toBe(
      'Solo se admiten imágenes JPG, PNG o WebP'
    )
    expect(await validarFoto(archivo('application/pdf'), dim(1, 1))).toBe(
      'Solo se admiten imágenes JPG, PNG o WebP'
    )
  })

  it('rechaza más de 5 MB y acepta exactamente 5 MB', async () => {
    expect(await validarFoto(archivo('image/png', FOTO_MAX_BYTES + 1), dim(1, 1))).toBe(
      'La imagen no puede superar 5 MB'
    )
    expect(await validarFoto(archivo('image/png', FOTO_MAX_BYTES), dim(1, 1))).toBeNull()
  })

  it('rechaza más de 4000×4000 píxeles y acepta 4000', async () => {
    const mensaje = 'La imagen no puede superar 4000×4000 píxeles'
    expect(await validarFoto(archivo('image/png'), dim(4001, 10))).toBe(mensaje)
    expect(await validarFoto(archivo('image/png'), dim(10, 4001))).toBe(mensaje)
    expect(await validarFoto(archivo('image/png'), dim(4000, 4000))).toBeNull()
  })

  it('si no se puede leer la imagen, lo informa', async () => {
    const mensaje = await validarFoto(archivo('image/png'), () => Promise.reject(new Error('x')))

    expect(mensaje).toBe('No pudimos leer la imagen')
  })
})
