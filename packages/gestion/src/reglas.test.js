import { describe, expect, it } from 'vitest'

import { edadEnAnios, fechaDeHoy, fechaDeManana, normalizarDecimal, slugDesdeTexto, validar } from './reglas.js'

describe('slugDesdeTexto', () => {
  it('pasa a minúsculas, quita tildes y usa guiones', () => {
    expect(slugDesdeTexto('Escuela Ñandú Águila')).toBe('escuela-nandu-aguila')
    expect(slugDesdeTexto('  ¡Autos & Motos --- 2026!  ')).toBe('autos-motos-2026')
  })

  it('recorta al máximo sin terminar en guion', () => {
    const slug = slugDesdeTexto('a'.repeat(59) + ' bbbb', 60)

    expect(slug.length).toBeLessThanOrEqual(60)
    expect(slug.endsWith('-')).toBe(false)
  })

  it('un texto vacío da un slug vacío', () => {
    expect(slugDesdeTexto('   ')).toBe('')
    expect(slugDesdeTexto(undefined)).toBe('')
  })
})

describe('validar', () => {
  it('un campo requerido vacío o con solo espacios falla con su mensaje', () => {
    const campo = { campo: 'nombre', titulo: 'Nombre', requerido: true, mensajes: { requerido: 'El nombre es obligatorio' } }

    expect(validar(campo, '')).toBe('El nombre es obligatorio')
    expect(validar(campo, '   ')).toBe('El nombre es obligatorio')
    expect(validar({ ...campo, mensajes: undefined }, '')).toBe('Este campo es obligatorio')
  })

  it('un campo vacío y no requerido siempre es válido', () => {
    expect(validar({ campo: 'tel', titulo: 'Teléfono', max: 5 }, '')).toBeNull()
  })

  it('valida el largo mínimo y máximo', () => {
    const campo = { campo: 'nombre', titulo: 'Nombre', min: 3, max: 5 }

    expect(validar(campo, 'ab')).toBe('Debe tener entre 3 y 5 caracteres')
    expect(validar(campo, 'abcdef')).toBe('Debe tener entre 3 y 5 caracteres')
    expect(validar(campo, 'abcd')).toBeNull()
    expect(validar({ campo: 'x', titulo: 'x', max: 3 }, 'abcd')).toBe('No puede superar 3 caracteres')
  })

  it('valida el correo', () => {
    const campo = { campo: 'email', titulo: 'Correo', tipo: 'email' }

    expect(validar(campo, 'no-es-correo')).toBe('El correo no es válido')
    expect(validar(campo, 'ana@gmail.com')).toBeNull()
  })

  it('valida el patrón con nombre (slug) y uno propio', () => {
    const slug = { campo: 'slug', titulo: 'Slug', patron: 'slug' }

    for (const bueno of ['autos', 'autos-sur', 'e2e-2026']) expect(validar(slug, bueno)).toBeNull()
    for (const malo of ['Autos', 'con espacio', '-autos', 'autos-', 'a--b', 'a_b']) {
      expect(validar(slug, malo)).toContain('minúsculas')
    }
    expect(validar({ campo: 'c', titulo: 'c', patron: '^\\d+$', mensajes: { patron: 'Solo números' } }, 'a1')).toBe('Solo números')
  })

  it('valida fechaNoFutura y edadMinima con la fecha de hoy inyectada', () => {
    const hoy = new Date(2026, 9, 21)
    const campo = { campo: 'f', titulo: 'Fecha', validaciones: ['fechaNoFutura', 'edadMinima:18'] }

    expect(validar(campo, '2026-10-21', hoy)).toBe('La fecha debe estar en el pasado')
    expect(validar(campo, '2008-10-22', hoy)).toBe('Debe tener al menos 18 años')
    expect(validar(campo, '2008-10-21', hoy)).toBeNull()
  })
})

describe('validar números y selecciones', () => {
  const numero = { campo: 'km', titulo: 'Kilometraje', tipo: 'numero', min: 0, max: 100 }
  const seleccion = {
    campo: 'transmision',
    titulo: 'Transmisión',
    tipo: 'seleccion',
    requerido: true,
    opciones: [{ valor: 'MANUAL', etiqueta: 'Manual' }],
  }

  it('un número entero se valida por su valor, no por el largo del texto', () => {
    expect(validar(numero, '0')).toBeNull()
    expect(validar(numero, '100')).toBeNull()
    expect(validar(numero, '-1')).toBe('Debe ser al menos 0')
    expect(validar(numero, '101')).toBe('No puede superar 100')
    expect(validar(numero, '1.5')).toBe('Debe ser un número entero')
    expect(validar(numero, 'abc')).toBe('Debe ser un número entero')
    expect(validar(numero, '')).toBeNull()
  })

  it('una selección solo acepta una de sus opciones', () => {
    expect(validar(seleccion, 'MANUAL')).toBeNull()
    expect(validar(seleccion, 'CVT')).toBe('Elige una opción de la lista')
    expect(validar(seleccion, '')).toBe('Este campo es obligatorio')
  })
})

describe('edadEnAnios', () => {
  it('cuenta años cumplidos', () => {
    expect(edadEnAnios('2000-10-22', new Date(2026, 9, 21))).toBe(25)
    expect(edadEnAnios('2000-10-21', new Date(2026, 9, 21))).toBe(26)
  })

  describe('campo seleccion', () => {
    const sexo = {
      campo: 'sexo',
      titulo: 'Sexo',
      tipo: 'seleccion',
      requerido: true,
      opciones: [
        { valor: 'MASCULINO', etiqueta: 'Masculino' },
        { valor: 'FEMENINO', etiqueta: 'Femenino' },
      ],
    }

    it('requerido sin valor falla con el mensaje del esquema o el de siempre', () => {
      expect(validar(sexo, '')).toBe('Este campo es obligatorio')
      expect(validar({ ...sexo, mensajes: { requerido: 'Elige tu sexo' } }, '')).toBe('Elige tu sexo')
    })

    it('un valor que no está entre las opciones fijas falla', () => {
      expect(validar(sexo, 'OTRO')).toBe('Elige una opción de la lista')
      expect(validar(sexo, 'FEMENINO')).toBeNull()
    })

    it('con opciones traídas de la API (sin lista fija) acepta cualquier valor no vacío', () => {
      const categoria = { campo: 'categoriaSigla', titulo: 'Categoría', tipo: 'seleccion', opcionesDe: { recurso: 'x', valor: 'a', etiqueta: 'b' } }

      expect(validar(categoria, 'B')).toBeNull()
    })
  })

  describe('campo ubicacion', () => {
    const ubicacion = { campo: 'ubicacion', titulo: 'Ubicación', tipo: 'ubicacion', requerido: true }

    it('requerido sin punto falla', () => {
      expect(validar(ubicacion, undefined)).toBe('Elige la ubicación en el mapa')
      expect(validar(ubicacion, { latitud: null, longitud: null })).toBe('Elige la ubicación en el mapa')
      expect(validar({ ...ubicacion, mensajes: { requerido: 'Elige tu ubicación en el mapa' } }, null)).toBe(
        'Elige tu ubicación en el mapa'
      )
    })

    it('no requerido y sin punto es válido', () => {
      expect(validar({ ...ubicacion, requerido: false }, null)).toBeNull()
    })

    it('un punto completo y en rango es válido, incluidos los límites', () => {
      expect(validar(ubicacion, { latitud: -16.4897, longitud: -68.1193 })).toBeNull()
      expect(validar(ubicacion, { latitud: -90, longitud: 180 })).toBeNull()
    })

    it('una sola coordenada o una fuera de rango falla', () => {
      expect(validar(ubicacion, { latitud: -16.5, longitud: null })).toBe('La ubicación no es válida')
      expect(validar(ubicacion, { latitud: 90.1, longitud: 0 })).toBe('La ubicación no es válida')
      expect(validar(ubicacion, { latitud: 0, longitud: -180.5 })).toBe('La ubicación no es válida')
      expect(validar(ubicacion, { latitud: 'abc', longitud: 1 })).toBe('La ubicación no es válida')
    })
  })
})

describe('validar: seleccion multiple', () => {
  const categorias = { campo: 'categorias', titulo: 'Categorías', tipo: 'seleccion', multiple: true, requerido: true }

  it('requerido pide al menos un elemento', () => {
    expect(validar(categorias, [])).toBe('Elige al menos una opción')
    expect(validar(categorias, undefined)).toBe('Elige al menos una opción')
    expect(validar(categorias, ['', '  '])).toBe('Elige al menos una opción')
    expect(validar({ ...categorias, mensajes: { requerido: 'Elige una categoría' } }, [])).toBe('Elige una categoría')
    expect(validar(categorias, ['B', 'C'])).toBeNull()
  })

  it('sin requerido, vacío es válido', () => {
    expect(validar({ ...categorias, requerido: false }, [])).toBeNull()
  })

  it('con opciones fijas rechaza un valor que no está entre ellas', () => {
    const fijas = { ...categorias, opciones: [{ valor: 'B', etiqueta: 'B' }] }

    expect(validar(fijas, ['B', 'Z'])).toBe('Elige una opción de la lista')
    expect(validar(fijas, ['B'])).toBeNull()
  })
})

describe('validar: fechaFutura', () => {
  const hoy = new Date(2026, 9, 24)
  const vence = { campo: 'licenciaVence', titulo: 'Vencimiento', tipo: 'fecha', validaciones: ['fechaFutura'] }

  it('rechaza hoy y ayer y acepta mañana', () => {
    expect(validar(vence, '2026-10-24', hoy)).toBe('La fecha debe ser posterior a hoy')
    expect(validar(vence, '2026-10-23', hoy)).toBe('La fecha debe ser posterior a hoy')
    expect(validar(vence, '2026-10-25', hoy)).toBeNull()
  })

  it('el mensaje se puede cambiar', () => {
    const campo = { ...vence, mensajes: { fechaFutura: 'La licencia ya venció' } }

    expect(validar(campo, '2026-10-01', hoy)).toBe('La licencia ya venció')
  })

  it('fechaDeHoy y fechaDeManana dan yyyy-MM-dd en la zona local', () => {
    expect(fechaDeHoy(new Date(2026, 0, 5))).toBe('2026-01-05')
    expect(fechaDeManana(new Date(2026, 11, 31))).toBe('2027-01-01')
  })
})

describe('validar: soloSiCambia', () => {
  const hoy = new Date(2026, 9, 24)
  const vence = { campo: 'licenciaVence', titulo: 'Vencimiento', tipo: 'fecha', validaciones: ['fechaFutura'], soloSiCambia: true }

  it('al editar no valida un valor que no cambió', () => {
    expect(validar(vence, '2026-01-01', hoy, { valorInicial: '2026-01-01' })).toBeNull()
  })

  it('al editar valida el valor si cambió', () => {
    expect(validar(vence, '2026-02-01', hoy, { valorInicial: '2026-01-01' })).toBe('La fecha debe ser posterior a hoy')
    expect(validar(vence, '2026-12-01', hoy, { valorInicial: '2026-01-01' })).toBeNull()
  })

  it('en el alta (sin valor inicial) se aplica siempre', () => {
    expect(validar(vence, '2026-01-01', hoy)).toBe('La fecha debe ser posterior a hoy')
    expect(validar(vence, '2026-01-01', hoy, {})).toBe('La fecha debe ser posterior a hoy')
  })

  it('sin soloSiCambia, editar tampoco deja pasar el valor sin cambios', () => {
    const sin = { ...vence, soloSiCambia: false }

    expect(validar(sin, '2026-01-01', hoy, { valorInicial: '2026-01-01' })).toBe('La fecha debe ser posterior a hoy')
  })

  it('un campo requerido sigue pidiendo valor aunque no haya cambiado', () => {
    expect(validar({ ...vence, requerido: true }, '', hoy, { valorInicial: '' })).toBe('Este campo es obligatorio')
  })
})

describe('validar: hastaHoy', () => {
  const hoy = new Date(2026, 9, 24)
  const contratacion = { campo: 'fechaContratacion', titulo: 'Contratación', tipo: 'fecha', validaciones: ['hastaHoy'] }

  it('acepta hoy y el pasado, rechaza mañana', () => {
    expect(validar(contratacion, '2026-10-24', hoy)).toBeNull()
    expect(validar(contratacion, '2020-01-01', hoy)).toBeNull()
    expect(validar(contratacion, '2026-10-25', hoy)).toBe('La fecha no puede ser futura')
  })
})

describe('validar opciones de respuesta', () => {
  const campo = { campo: 'opciones', titulo: 'Opciones', tipo: 'opciones', requerido: true }
  const fila = (texto, correcta = false) => ({ texto, correcta })

  it('pide entre 2 y 6 filas, todas con texto, y al menos una correcta', () => {
    expect(validar(campo, [fila('Uno', true), fila('Dos')])).toBeNull()
    expect(validar(campo, [])).toBe('Agrega al menos 2 opciones')
    expect(validar(campo, undefined)).toBe('Agrega al menos 2 opciones')
    expect(validar(campo, [fila('Uno', true)])).toBe('Agrega al menos 2 opciones')
    expect(validar(campo, Array.from({ length: 7 }, (_, i) => fila(`Op ${i}`, i === 0)))).toBe(
      'Puedes agregar hasta 6 opciones'
    )
    expect(validar(campo, [fila('Uno', true), fila('  ')])).toBe('Todas las opciones necesitan texto')
    expect(validar(campo, [fila('Uno'), fila('Dos')])).toBe('Marca al menos una opción correcta')
    expect(validar(campo, [fila('x'.repeat(201), true), fila('Dos')])).toBe(
      'Cada opción puede tener hasta 200 caracteres'
    )
  })

  it('min, max y los mensajes salen del esquema', () => {
    const personalizado = {
      ...campo,
      min: 3,
      max: 4,
      mensajes: { min: 'Pocas', requerido: 'Falta la correcta' },
    }

    expect(validar(personalizado, [fila('a', true), fila('b')])).toBe('Pocas')
    expect(validar(personalizado, [fila('a'), fila('b'), fila('c')])).toBe('Falta la correcta')
    expect(validar(personalizado, [fila('a', true), fila('b'), fila('c')])).toBeNull()
  })

  it('un texto largo (textarea) se valida por su largo como cualquier texto', () => {
    const texto = { campo: 'e', titulo: 'Enunciado', tipo: 'textarea', requerido: true, min: 5, max: 10 }

    expect(validar(texto, 'abc')).not.toBeNull()
    expect(validar(texto, 'abcdef')).toBeNull()
    expect(validar(texto, 'a'.repeat(11))).not.toBeNull()
  })
})

describe('validar: decimal', () => {
  const costo = { campo: 'costoBs', titulo: 'Costo', tipo: 'decimal', requerido: true, min: 0.01, max: 99999999.99 }

  it('acepta un número con punto o con coma, con hasta dos decimales', () => {
    for (const bueno of ['450', '450.5', '450,5', '450.55', '450,55', '0.01', '0,01', '99999999.99', ' 1250,50 ']) {
      expect(validar(costo, bueno)).toBeNull()
    }
  })

  it('rechaza más decimales de los permitidos, texto, signos, miles y más de un separador', () => {
    for (const malo of ['450.555', '450,123', 'abc', '12a', '-5', '+5', '1.250,50', '1,250.50', '1.2.3', '.5', '5.', '1e3']) {
      expect(validar(costo, malo)).toBe('Debe ser un número con hasta 2 decimales')
    }
  })

  it('vacío: obligatorio si es requerido, válido si no', () => {
    expect(validar(costo, '')).toBe('Este campo es obligatorio')
    expect(validar({ ...costo, mensajes: { requerido: 'El costo es obligatorio' } }, '  ')).toBe('El costo es obligatorio')
    expect(validar({ ...costo, requerido: false }, '')).toBeNull()
  })

  it('min y max son el rango del valor, con su mensaje del esquema o uno por defecto con coma decimal', () => {
    expect(validar(costo, '0')).toBe('Debe ser al menos 0,01')
    expect(validar(costo, '0.00')).toBe('Debe ser al menos 0,01')
    expect(validar(costo, '100000000')).toBe('No puede superar 99999999,99')
    expect(validar({ ...costo, mensajes: { min: 'El costo debe ser mayor a 0', max: 'Demasiado alto' } }, '0')).toBe(
      'El costo debe ser mayor a 0'
    )
    expect(validar({ ...costo, mensajes: { max: 'Demasiado alto' } }, '100000000')).toBe('Demasiado alto')
  })

  it('decimales cambia cuántos admite: uno, o cero (solo enteros)', () => {
    expect(validar({ ...costo, decimales: 1 }, '5.5')).toBeNull()
    expect(validar({ ...costo, decimales: 1 }, '5.55')).toBe('Debe ser un número con hasta 1 decimal')
    expect(validar({ ...costo, decimales: 0 }, '5')).toBeNull()
    expect(validar({ ...costo, decimales: 0 }, '5.5')).toBe('Debe ser un número entero')
  })

  it('el mensaje de formato se puede cambiar', () => {
    expect(validar({ ...costo, mensajes: { patron: 'Escribe un monto' } }, 'abc')).toBe('Escribe un monto')
  })
})

describe('normalizarDecimal', () => {
  it('quita espacios y cambia la coma por punto', () => {
    expect(normalizarDecimal(' 1250,5 ')).toBe('1250.5')
    expect(normalizarDecimal('1 250,50')).toBe('1250.50')
    expect(normalizarDecimal('450.00')).toBe('450.00')
    expect(normalizarDecimal(undefined)).toBe('')
  })
})

describe('validar reglas por tema', () => {
  const campo = { campo: 'reglas', titulo: 'Preguntas por tema', tipo: 'reglas', requerido: true }
  const fila = (tema, cantidad) => ({ tema, cantidad })

  it('pide al menos un tema, con nombre, sin repetir y con una cantidad entera de 1 a 100', () => {
    expect(validar(campo, [fila('Señales', '5'), fila('Prioridad', '3')])).toBeNull()
    expect(validar(campo, [])).toBe('Agrega al menos 1 tema')
    expect(validar(campo, undefined)).toBe('Agrega al menos 1 tema')
    expect(validar(campo, [fila('  ', '5')])).toBe('Todos los temas necesitan nombre')
    expect(validar(campo, [fila('Señales', '5'), fila('señales', '2')])).toBe('Un tema no puede repetirse')
    expect(validar(campo, [fila('Señales', '')])).toBe('Cada cantidad es un número entero de 1 a 100')
    expect(validar(campo, [fila('Señales', '0')])).toBe('Cada cantidad es un número entero de 1 a 100')
    expect(validar(campo, [fila('Señales', '2.5')])).toBe('Cada cantidad es un número entero de 1 a 100')
    expect(validar(campo, [fila('Señales', '101')])).toBe('Cada cantidad es un número entero de 1 a 100')
    expect(validar(campo, [fila('x'.repeat(81), '1')])).toBe('Cada tema puede tener hasta 80 caracteres')
  })

  it('max de filas y mensajes salen del esquema', () => {
    const personalizado = { ...campo, max: 1, mensajes: { max: 'Demasiados' } }

    expect(validar(personalizado, [fila('a', '1'), fila('b', '1')])).toBe('Demasiados')
  })
})
