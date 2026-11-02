export interface RespuestaPush {
  // Dispositivos a los que Firebase aceptó el mensaje
  aceptados: number
  // Tokens que Firebase rechazó por inválidos o dados de baja: hay que desactivarlos
  tokensInvalidos: string[]
}

/** Lo que el servicio necesita para mandar un push; los tests ponen un doble. */
export interface ProveedorPush {
  enviar(
    tokens: string[],
    titulo: string,
    mensaje: string,
    datos: Record<string, unknown>
  ): Promise<RespuestaPush>
}

export const PROVEEDOR_PUSH = Symbol('PROVEEDOR_PUSH')
