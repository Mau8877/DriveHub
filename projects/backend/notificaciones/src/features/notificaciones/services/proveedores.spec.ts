import type { ConfigService } from '@nestjs/config'
import { describe, expect, it, vi } from 'vitest'

import {
  datosComoTexto,
  FirebaseProveedorPush,
  type MensajeriaFirebase,
} from './firebase-proveedor-push.js'
import { construirHtmlCorreo } from './proveedor-correo.js'

const config = (valores: Record<string, string> = {}) =>
  ({ get: (clave: string) => valores[clave] }) as unknown as ConfigService

describe('construirHtmlCorreo', () => {
  it('escapa el título y el mensaje', () => {
    const html = construirHtmlCorreo('<script>alert(1)</script>', 'a & b "c"\nlínea 2')
    expect(html).not.toContain('<script>')
    expect(html).toContain('&lt;script&gt;')
    expect(html).toContain('a &amp; b &quot;c&quot;<br>línea 2')
  })
})

describe('FirebaseProveedorPush', () => {
  function proveedor(responses: { success: boolean; error?: { code: string } }[]) {
    const sendEachForMulticast = vi.fn().mockResolvedValue({ responses })
    const mensajeria: MensajeriaFirebase = { sendEachForMulticast }
    return { sendEachForMulticast, push: new FirebaseProveedorPush(config(), () => mensajeria) }
  }

  it('cuenta los aceptados y separa los tokens inválidos', async () => {
    const { push } = proveedor([
      { success: true },
      { success: false, error: { code: 'messaging/registration-token-not-registered' } },
      { success: false, error: { code: 'messaging/internal-error' } },
    ])
    const r = await push.enviar(['a', 'b', 'c'], 'T', 'M', {})
    expect(r).toEqual({ aceptados: 1, tokensInvalidos: ['b'] })
  })

  it('manda los datos como texto', async () => {
    const { sendEachForMulticast, push } = proveedor([{ success: true }])
    await push.enviar(['a'], 'T', 'M', { reservaCodigo: 'r1', horas: 2, anidado: { x: 1 } })
    expect(sendEachForMulticast).toHaveBeenCalledWith(
      expect.objectContaining({ data: { reservaCodigo: 'r1', horas: '2', anidado: '{"x":1}' } })
    )
  })

  it('falla con un error claro si no hay credenciales de Firebase', async () => {
    const push = new FirebaseProveedorPush(config())
    await expect(push.enviar(['a'], 'T', 'M', {})).rejects.toThrow('FIREBASE_CREDENCIALES')
  })

  it('propaga el error general de Firebase', async () => {
    const mensajeria = { sendEachForMulticast: vi.fn().mockRejectedValue(new Error('caído')) }
    const push = new FirebaseProveedorPush(config(), () => mensajeria)
    await expect(push.enviar(['a'], 'T', 'M', {})).rejects.toThrow('caído')
  })
})

describe('datosComoTexto', () => {
  it('deja intactos los textos y serializa el resto', () => {
    expect(datosComoTexto({ a: 'x', b: true, c: null })).toEqual({ a: 'x', b: 'true', c: 'null' })
  })
})
