import { UrlSegment, type Route, type UrlSegmentGroup } from '@angular/router'

import { prefijo } from './prefijo.matcher'

const segmentos = (...paths: string[]) => paths.map((p) => new UrlSegment(p, {}))
const grupo = {} as UrlSegmentGroup
const ruta = {} as Route

describe('prefijo', () => {
  const matcher = prefijo('examenes')

  it('consume todas las subrutas del remoto', () => {
    const url = segmentos('examenes', 'simulacros', '3')

    expect(matcher(url, grupo, ruta)?.consumed).toEqual(url)
  })

  it('acepta la ruta base sola', () => {
    expect(matcher(segmentos('examenes'), grupo, ruta)).not.toBeNull()
  })

  it('no acepta otra ruta ni un prefijo parcial', () => {
    expect(matcher(segmentos('escuela'), grupo, ruta)).toBeNull()
    expect(matcher(segmentos('examenes-viejos'), grupo, ruta)).toBeNull()
    expect(matcher([], grupo, ruta)).toBeNull()
  })
})
