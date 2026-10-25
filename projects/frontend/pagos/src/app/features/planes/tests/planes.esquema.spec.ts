import { validar } from '@drivehub/gestion'

import { ESQUEMA_PLANES } from '../schemas/planes.esquema'

const camposDeCrear = () => ESQUEMA_PLANES.crear!.secciones.flatMap((s) => s.campos)
const camposDeEditar = () => ESQUEMA_PLANES.editar!.secciones.flatMap((s) => s.campos)
const crear = (nombre: string) => camposDeCrear().find((c) => c.campo === nombre)!
const accion = (id: string) => ESQUEMA_PLANES.acciones!.find((a) => a.id === id)!

describe('esquema de planes de horas', () => {
  it('apunta al recurso planes-pago y se llama Planes de horas', () => {
    expect(ESQUEMA_PLANES.recurso).toBe('planes-pago')
    expect(ESQUEMA_PLANES.titulo).toBe('Planes de horas')
    expect(ESQUEMA_PLANES.nombreSingular).toBe('plan')
    expect(ESQUEMA_PLANES.crear?.etiqueta).toBe('Nuevo plan')
  })

  it('las columnas son nombre, horas, costo en Bs, costo en USD y estado', () => {
    expect(ESQUEMA_PLANES.columnas.map((c) => c.campo)).toEqual([
      'nombre',
      'horas',
      'costoBs',
      'costoUsd',
      'activo',
    ])
    const [nombre, horas, bs, usd, estado] = ESQUEMA_PLANES.columnas
    expect(nombre).toMatchObject({ principal: true, titulo: 'Nombre' })
    expect(horas).toMatchObject({ titulo: 'Horas', ancho: '6rem' })
    expect(bs).toMatchObject({ tipo: 'moneda', moneda: 'BOB', titulo: 'Costo (Bs)' })
    expect(usd).toMatchObject({ tipo: 'moneda', moneda: 'USD', titulo: 'Costo (USD)' })
    expect(estado.tipo).toBe('insignia')
    expect(estado.insignias).toEqual({
      true: { etiqueta: 'Activo', tono: 'exito' },
      false: { etiqueta: 'Desactivado' },
    })
  })

  it('busca por nombre y filtra por estado con filter[activo]', () => {
    expect(ESQUEMA_PLANES.busqueda?.placeholder).toBe('Buscar por nombre')
    expect(ESQUEMA_PLANES.filtros).toHaveLength(1)
    expect(ESQUEMA_PLANES.filtros![0]).toMatchObject({
      clave: 'filter[activo]',
      titulo: 'Estado',
      todas: 'Todos',
    })
    expect(ESQUEMA_PLANES.filtros![0].opciones).toEqual([
      { valor: 'true', etiqueta: 'Activos' },
      { valor: 'false', etiqueta: 'Desactivados' },
    ])
  })

  it('crear y editar usan los mismos tres campos, y editar es un PATCH', () => {
    expect(camposDeCrear().map((c) => c.campo)).toEqual(['nombre', 'horas', 'costoBs'])
    expect(camposDeEditar()).toEqual(camposDeCrear())
    expect(ESQUEMA_PLANES.editar!.metodo).toBe('PATCH')
    expect(ESQUEMA_PLANES.crear!.secciones).toHaveLength(1)
  })

  it('el dólar no es un campo del formulario: se calcula solo', () => {
    expect(camposDeCrear().some((c) => c.campo === 'costoUsd')).toBe(false)
    expect(camposDeEditar().some((c) => c.campo === 'costoUsd')).toBe(false)
    expect(crear('costoBs').ayuda).toBe('El equivalente en dólares se calcula solo.')
  })

  it('no hay detalle de solo lectura ni acción de borrar', () => {
    expect(ESQUEMA_PLANES.detalle).toBeUndefined()
    expect(ESQUEMA_PLANES.acciones!.map((a) => a.id)).toEqual(['desactivar', 'activar'])
  })

  it('desactivar se ve solo en un plan activo, pide confirmación y es de peligro', () => {
    const desactivar = accion('desactivar')

    expect(desactivar).toMatchObject({
      metodo: 'POST',
      ruta: '{id}/desactivar',
      tono: 'peligro',
      confirmar: '¿Desactivar el plan?',
      visibleSi: { campo: 'activo', igual: true },
    })
    expect(desactivar.confirmarTexto).toContain('conserva su historia')
  })

  it('reactivar se ve solo en un plan desactivado y no pide confirmación', () => {
    const reactivar = accion('activar')

    expect(reactivar).toMatchObject({
      metodo: 'POST',
      ruta: '{id}/activar',
      visibleSi: { campo: 'activo', igual: false },
    })
    expect(reactivar.confirmar).toBeUndefined()
  })

  it('el nombre repetido (ERR_PAG_01) se muestra bajo el campo nombre', () => {
    expect(ESQUEMA_PLANES.erroresDeCampo).toEqual({
      ERR_PAG_01: { campo: 'nombre', mensaje: 'Ya existe un plan con ese nombre en tu escuela' },
    })
  })
})

describe('reglas de validación de los planes', () => {
  it('el nombre es obligatorio y de hasta 80 caracteres', () => {
    const nombre = crear('nombre')

    expect(validar(nombre, '')).toBe('El nombre es obligatorio')
    expect(validar(nombre, '   ')).toBe('El nombre es obligatorio')
    expect(validar(nombre, 'x'.repeat(81))).toContain('80')
    expect(validar(nombre, 'x'.repeat(80))).toBeNull()
    expect(validar(nombre, 'Plan 10 horas')).toBeNull()
  })

  it('las horas son un entero de 1 a 500', () => {
    const horas = crear('horas')
    const mensaje = 'Las horas deben ser un número entero de 1 a 500'

    expect(validar(horas, '')).toBe('Las horas son obligatorias')
    expect(validar(horas, '0')).toBe(mensaje)
    expect(validar(horas, '501')).toBe(mensaje)
    expect(validar(horas, '10.5')).toBe(mensaje)
    expect(validar(horas, 'diez')).toBe(mensaje)
    expect(validar(horas, '-3')).toBe(mensaje)
    expect(validar(horas, '1')).toBeNull()
    expect(validar(horas, '500')).toBeNull()
  })

  it('el costo es mayor a 0, con hasta dos decimales, con punto o coma, y hasta 99999999,99', () => {
    const costo = crear('costoBs')

    expect(validar(costo, '')).toBe('El costo es obligatorio')
    expect(validar(costo, '0')).toBe('El costo debe ser mayor a 0')
    expect(validar(costo, '0,00')).toBe('El costo debe ser mayor a 0')
    expect(validar(costo, '-5')).toBe('El costo debe ser un número con hasta 2 decimales')
    expect(validar(costo, '10.123')).toBe('El costo debe ser un número con hasta 2 decimales')
    expect(validar(costo, 'cien')).toBe('El costo debe ser un número con hasta 2 decimales')
    expect(validar(costo, '100000000')).toBe('El costo no puede superar 99999999,99')
    expect(validar(costo, '450')).toBeNull()
    expect(validar(costo, '450.5')).toBeNull()
    expect(validar(costo, '450,55')).toBeNull()
    expect(validar(costo, '0.01')).toBeNull()
    expect(validar(costo, '99999999.99')).toBeNull()
  })
})
