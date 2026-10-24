import { ROLES } from '../core/sesion/roles'
import { NAV_ITEMS, migasDeRuta, navItemsDeRoles } from './nav-items'

describe('navItemsDeRoles', () => {
  const rutas = (roles: string[]) => navItemsDeRoles(roles).map((i) => i.ruta)

  it('el Super admin ve "Inicio" y "Gestionar escuelas"', () => {
    expect(navItemsDeRoles([ROLES.SUPER_ADMIN]).map((i) => i.etiqueta)).toEqual([
      'Inicio',
      'Gestionar escuelas',
    ])
    expect(rutas([ROLES.SUPER_ADMIN])).toEqual(['/', '/escuelas'])
  })

  it('el Administrador de escuela ve Inicio, "Estudiantes", "Autos" y los seis módulos de siempre, y no "Gestionar escuelas"', () => {
    expect(rutas([ROLES.ADMIN_ESCUELA])).toEqual([
      '/',
      '/identidad',
      '/escuela/estudiantes',
      '/escuela/autos',
      '/escuela',
      '/pagos',
      '/examenes',
      '/contenido',
      '/asistente-ia',
    ])
  })

  it('los menús de los dos roles solo comparten Inicio', () => {
    const superAdmin = rutas([ROLES.SUPER_ADMIN])
    const admin = rutas([ROLES.ADMIN_ESCUELA])

    expect(superAdmin.filter((r) => admin.includes(r))).toEqual(['/'])
  })

  it('"Estudiantes" lleva a /escuela/estudiantes y solo lo ve el Administrador de escuela', () => {
    const estudiantes = NAV_ITEMS.find((i) => i.etiqueta === 'Estudiantes')

    expect(estudiantes).toMatchObject({
      ruta: '/escuela/estudiantes',
      descripcion: 'Alumnos de tu escuela',
      icono: 'users',
    })
    expect(rutas([ROLES.ADMIN_ESCUELA])).toContain('/escuela/estudiantes')
    expect(rutas([ROLES.SUPER_ADMIN])).not.toContain('/escuela/estudiantes')
    expect(rutas([ROLES.INSTRUCTOR])).not.toContain('/escuela/estudiantes')
    expect(rutas([ROLES.ESTUDIANTE])).not.toContain('/escuela/estudiantes')
  })

  it('"Autos" lleva a /escuela/autos, solo lo ve el Administrador de escuela y va antes que "Escuela"', () => {
    const autos = NAV_ITEMS.find((i) => i.etiqueta === 'Autos')
    const etiquetas = NAV_ITEMS.map((i) => i.etiqueta)

    expect(autos).toMatchObject({
      ruta: '/escuela/autos',
      descripcion: 'Flota de la escuela',
      icono: 'car',
    })
    expect(rutas([ROLES.ADMIN_ESCUELA])).toContain('/escuela/autos')
    expect(rutas([ROLES.SUPER_ADMIN])).not.toContain('/escuela/autos')
    expect(rutas([ROLES.INSTRUCTOR])).not.toContain('/escuela/autos')
    expect(etiquetas.indexOf('Autos')).toBeLessThan(etiquetas.indexOf('Escuela'))
  })

  it('"Estudiantes" va antes que "Escuela" (las migas toman el primer ítem que coincide por prefijo)', () => {
    const etiquetas = NAV_ITEMS.map((i) => i.etiqueta)

    expect(etiquetas.indexOf('Estudiantes')).toBeLessThan(etiquetas.indexOf('Escuela'))
  })

  it('todos los ítems tienen un icono', () => {
    expect(NAV_ITEMS.every((i) => i.icono.length > 0)).toBe(true)
  })

  it('un rol sin ítems (o sin roles) no ve nada', () => {
    expect(rutas([ROLES.ESTUDIANTE])).toEqual([])
    expect(rutas([])).toEqual([])
  })

  it('con varios roles ve la unión de sus ítems', () => {
    expect(rutas([ROLES.SUPER_ADMIN, ROLES.ADMIN_ESCUELA])).toHaveLength(NAV_ITEMS.length)
  })
})

describe('migasDeRuta', () => {
  const items = navItemsDeRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN_ESCUELA])

  it('en el inicio solo está "Inicio"', () => {
    expect(migasDeRuta('/', items)).toEqual([{ etiqueta: 'Inicio' }])
  })

  it('un módulo cuelga de Inicio, también en sus subrutas y con query', () => {
    const esperado = [{ etiqueta: 'Inicio', ruta: '/' }, { etiqueta: 'Gestionar escuelas' }]
    expect(migasDeRuta('/escuelas', items)).toEqual(esperado)
    expect(migasDeRuta('/escuelas/nueva?x=1', items)).toEqual(esperado)
  })

  it('/escuela/estudiantes cuelga de Inicio como "Estudiantes" y /escuela sigue siendo "Escuela"', () => {
    expect(migasDeRuta('/escuela/estudiantes', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Estudiantes' },
    ])
    expect(migasDeRuta('/escuela/estudiantes?search=ana', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Estudiantes' },
    ])
    expect(migasDeRuta('/escuela', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Escuela' },
    ])
  })

  it('el perfil cuelga de Inicio como "Mi perfil"', () => {
    expect(migasDeRuta('/perfil', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Mi perfil' },
    ])
  })

  it('una ruta desconocida no tiene migas', () => {
    expect(migasDeRuta('/nada', items)).toEqual([])
  })
})
