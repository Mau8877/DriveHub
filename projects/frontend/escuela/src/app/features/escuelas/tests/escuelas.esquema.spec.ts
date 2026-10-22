import { validar } from '@drivehub/gestion'

import { ESQUEMA_ESCUELAS } from '../schemas/escuelas.esquema'

const camposDeCrear = () => ESQUEMA_ESCUELAS.crear!.secciones.flatMap((s) => s.campos)
const campo = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!

describe('esquema de escuelas', () => {
  it('apunta al recurso escuelas y consulta por código para editar (cualquier estado)', () => {
    expect(ESQUEMA_ESCUELAS.recurso).toBe('escuelas')
    expect(ESQUEMA_ESCUELAS.editar?.cargar).toBe('por-codigo/{id}')
  })

  it('el filtro de estado usa la clave filter[estado] del backend', () => {
    expect(ESQUEMA_ESCUELAS.filtros?.[0].clave).toBe('filter[estado]')
    expect(ESQUEMA_ESCUELAS.filtros?.[0].opciones.map((o) => o.valor)).toEqual([
      'ACTIVA',
      'INACTIVA',
    ])
  })

  it('las acciones coinciden con los endpoints: DELETE {id} confirma y POST {id}/reactivar no', () => {
    const [desactivar, reactivar] = ESQUEMA_ESCUELAS.acciones!

    expect(desactivar).toMatchObject({
      metodo: 'DELETE',
      ruta: '{id}',
      confirmar: '¿Desactivar la escuela?',
      icono: 'power',
    })
    expect(reactivar).toMatchObject({ metodo: 'POST', ruta: '{id}/reactivar', icono: 'refresh' })
    expect(reactivar.confirmar).toBeUndefined()
  })

  it('solo se edita el nombre: el slug es de solo lectura', () => {
    const campos = ESQUEMA_ESCUELAS.editar!.secciones.flatMap((s) => s.campos)

    expect(campos.find((c) => c.campo === 'slug')?.soloLectura).toBe(true)
    expect(campos.find((c) => c.campo === 'nombre')?.soloLectura).toBeUndefined()
  })

  it('los errores del backend caen en su campo', () => {
    expect(ESQUEMA_ESCUELAS.erroresDeCampo).toMatchObject({
      ERR_ESC_03: { campo: 'slug' },
      ERR_IDE_01: { campo: 'administrador.email' },
      ERR_IDE_02: { campo: 'administrador.fechaNacimiento' },
    })
  })

  it('el slug exige minúsculas, números y guiones; el nombre de 3 a 120', () => {
    expect(validar(campo('slug'), 'autos-sur')).toBeNull()
    expect(validar(campo('slug'), 'Autos Sur')).not.toBeNull()
    expect(validar(campo('slug'), 'ab')).not.toBeNull()
    expect(validar(campo('nombre'), 'ab')).not.toBeNull()
    expect(validar(campo('nombre'), 'Autos del Sur')).toBeNull()
  })

  it('las reglas del administrador son las del registro: contraseña de 9 a 64 y 18 años', () => {
    expect(validar(campo('administrador.password'), '12345678')).not.toBeNull()
    expect(validar(campo('administrador.password'), '123456789')).toBeNull()
    expect(validar(campo('administrador.fechaNacimiento'), '2999-01-01')).not.toBeNull()
    expect(validar(campo('administrador.fechaNacimiento'), '1990-01-01')).toBeNull()
  })

  it('el teléfono es opcional', () => {
    expect(campo('administrador.telefono').requerido).toBeUndefined()
    expect(validar(campo('administrador.telefono'), '')).toBeNull()
  })
})
