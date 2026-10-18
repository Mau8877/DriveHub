import type { UrlMatcher, UrlSegment } from '@angular/router'

/**
 * Matcher que acepta cualquier URL que empiece con `prefijo` y consume
 * todos los segmentos: /examenes, /examenes/simulacros/3, etc.
 *
 * * Lo necesitan las rutas de los remotos React: sus subrutas las resuelve
 * * el router del propio remoto, el shell solo tiene que mantener montado el
 * * RemoteHost mientras la URL siga dentro del prefijo.
 */
export function prefijo(prefijo: string): UrlMatcher {
  return (segmentos: UrlSegment[]) =>
    segmentos.length > 0 && segmentos[0].path === prefijo ? { consumed: segmentos } : null
}
