/**
 * Destino al que se vuelve tras iniciar sesión.
 *
 * ! Solo se acepta una ruta interna (empieza con una sola "/"): un `redirect`
 * ! externo o con "//" se ignora, para que el login no sirva de redirección abierta.
 */
export function destinoSeguro(redirect: string | null | undefined): string {
  if (!redirect) return '/'
  if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.includes('\\')) return '/'
  return redirect
}
