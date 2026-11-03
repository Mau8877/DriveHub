import { generateKeyPair, SignJWT } from 'jose'
import { beforeAll, describe, expect, it } from 'vitest'

import { VerificadorJwt } from './verificador-jwt.js'

const EMISOR = 'drivehub-identidad'

describe('VerificadorJwt', () => {
  let publica: CryptoKey
  let privada: CryptoKey
  let ajena: CryptoKey
  let verificador: VerificadorJwt

  beforeAll(async () => {
    const par = await generateKeyPair('RS256')
    publica = par.publicKey
    privada = par.privateKey
    ajena = (await generateKeyPair('RS256')).privateKey
    verificador = new VerificadorJwt(async () => publica, EMISOR)
  })

  function firmar(opciones: { emisor?: string; expira?: string; clave?: CryptoKey } = {}) {
    return new SignJWT({ roles: ['ESTUDIANTE'], escuela: 'esc-1' })
      .setProtectedHeader({ alg: 'RS256' })
      .setSubject('usuario-1')
      .setIssuer(opciones.emisor ?? EMISOR)
      .setExpirationTime(opciones.expira ?? '5m')
      .sign(opciones.clave ?? privada)
  }

  it('devuelve la persona de un token válido', async () => {
    const usuario = await verificador.verificar(await firmar())
    expect(usuario).toEqual({ codigo: 'usuario-1', roles: ['ESTUDIANTE'], escuelaCodigo: 'esc-1' })
  })

  it('rechaza un token vencido', async () => {
    expect(await verificador.verificar(await firmar({ expira: '-1m' }))).toBeNull()
  })

  it('rechaza un token de otro emisor', async () => {
    expect(await verificador.verificar(await firmar({ emisor: 'otro' }))).toBeNull()
  })

  it('rechaza un token firmado con otra clave', async () => {
    expect(await verificador.verificar(await firmar({ clave: ajena }))).toBeNull()
  })

  it('rechaza un texto que no es un token', async () => {
    expect(await verificador.verificar('no-es-un-jwt')).toBeNull()
  })
})
