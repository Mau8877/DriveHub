import { validar } from '@drivehub/gestion'

import { ESQUEMA_AUTOS } from '../schemas/autos.esquema'

const camposDeCrear = () => ESQUEMA_AUTOS.crear!.secciones.flatMap((s) => s.campos)
const campo = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!

describe('esquema de autos', () => {
  it('apunta al recurso vehiculos y carga por código para editar', () => {
    expect(ESQUEMA_AUTOS.recurso).toBe('vehiculos')
    expect(ESQUEMA_AUTOS.editar?.cargar).toBeUndefined()
  })

  it('el filtro de estado usa la clave filter[estado] con los tres estados', () => {
    expect(ESQUEMA_AUTOS.filtros?.[0].clave).toBe('filter[estado]')
    expect(ESQUEMA_AUTOS.filtros?.[0].opciones.map((o) => o.valor)).toEqual([
      'DISPONIBLE',
      'MANTENIMIENTO',
      'BAJA',
    ])
  })

  it('las acciones coinciden con los endpoints y solo dar de baja pide confirmación', () => {
    const [mantenimiento, habilitar, baja] = ESQUEMA_AUTOS.acciones!

    expect(mantenimiento).toMatchObject({ metodo: 'POST', ruta: '{id}/enviar-a-mantenimiento' })
    expect(mantenimiento.visibleSi).toEqual({ campo: 'estado', igual: 'DISPONIBLE' })
    expect(habilitar).toMatchObject({ metodo: 'POST', ruta: '{id}/habilitar' })
    expect(habilitar.visibleSi).toEqual({ campo: 'estado', igual: 'MANTENIMIENTO' })
    expect(baja).toMatchObject({
      metodo: 'POST',
      ruta: '{id}/dar-de-baja',
      confirmar: '¿Dar de baja?',
    })
    expect(baja.visibleSi).toEqual({ campo: 'estado', distinto: 'BAJA' })
    expect(mantenimiento.confirmar).toBeUndefined()
    expect(habilitar.confirmar).toBeUndefined()
  })

  it('al editar, la placa y la transmisión son de solo lectura y un auto de baja no se edita', () => {
    const campos = ESQUEMA_AUTOS.editar!.secciones.flatMap((s) => s.campos)

    expect(campos.find((c) => c.campo === 'placa')?.soloLectura).toBe(true)
    expect(campos.find((c) => c.campo === 'transmision')?.soloLectura).toBe(true)
    expect(campos.find((c) => c.campo === 'marca')?.soloLectura).toBeUndefined()
    expect(ESQUEMA_AUTOS.editar?.visibleSi).toEqual({ campo: 'estado', distinto: 'BAJA' })
    expect(ESQUEMA_AUTOS.editar?.bloqueo).toMatchObject({ campo: 'estado', valor: 'BAJA' })
  })

  it('la placa en uso cae bajo el campo placa', () => {
    expect(ESQUEMA_AUTOS.erroresDeCampo).toMatchObject({ ERR_ESC_07: { campo: 'placa' } })
  })

  it('la placa admite de 5 a 10 letras, números y guiones', () => {
    expect(validar(campo('placa'), 'ABC-123')).toBeNull()
    expect(validar(campo('placa'), 'abc123')).toBeNull()
    expect(validar(campo('placa'), 'AB1')).not.toBeNull()
    expect(validar(campo('placa'), 'ABC 123')).not.toBeNull()
    expect(validar(campo('placa'), 'A'.repeat(11))).not.toBeNull()
    expect(validar(campo('placa'), '')).toBe('La placa es obligatoria')
  })

  it('el año va de 1980 al próximo y el kilometraje no es negativo', () => {
    const proximo = new Date().getFullYear() + 1

    expect(validar(campo('anio'), '2020')).toBeNull()
    expect(validar(campo('anio'), '1979')).toContain('1980')
    expect(validar(campo('anio'), String(proximo + 1))).toContain(String(proximo))
    expect(validar(campo('anio'), '')).toBeNull()
    expect(validar(campo('kilometraje'), '0')).toBeNull()
    expect(validar(campo('kilometraje'), '-1')).toBe('El kilometraje no puede ser negativo')
  })

  it('la transmisión es obligatoria y solo acepta manual o automática', () => {
    expect(validar(campo('transmision'), 'MANUAL')).toBeNull()
    expect(validar(campo('transmision'), 'AUTOMATICO')).toBeNull()
    expect(validar(campo('transmision'), 'CVT')).not.toBeNull()
    expect(validar(campo('transmision'), '')).toBe('La transmisión es obligatoria')
  })
})
