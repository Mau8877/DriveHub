import { ROLES } from '../core/sesion/roles'
import { NAV_ITEMS, navItemsDeRoles } from './nav-items'

describe('navItemsDeRoles', () => {
  const rutas = (roles: string[]) => navItemsDeRoles(roles).map((i) => i.ruta)

  it('el Super admin ve solo "Gestionar escuelas"', () => {
    expect(navItemsDeRoles([ROLES.SUPER_ADMIN]).map((i) => i.etiqueta)).toEqual([
      'Gestionar escuelas',
    ])
    expect(rutas([ROLES.SUPER_ADMIN])).toEqual(['/escuelas'])
  })

  it('el Administrador de escuela ve los seis ítems de siempre y no "Gestionar escuelas"', () => {
    expect(rutas([ROLES.ADMIN_ESCUELA])).toEqual([
      '/identidad',
      '/escuela',
      '/pagos',
      '/examenes',
      '/contenido',
      '/asistente-ia',
    ])
  })

  it('los menús de los dos roles no comparten ningún ítem', () => {
    const superAdmin = rutas([ROLES.SUPER_ADMIN])
    const admin = rutas([ROLES.ADMIN_ESCUELA])

    expect(superAdmin.filter((r) => admin.includes(r))).toEqual([])
  })

  it('un rol sin ítems (o sin roles) no ve nada', () => {
    expect(rutas([ROLES.ESTUDIANTE])).toEqual([])
    expect(rutas([])).toEqual([])
  })

  it('con varios roles ve la unión de sus ítems', () => {
    expect(rutas([ROLES.SUPER_ADMIN, ROLES.ADMIN_ESCUELA])).toHaveLength(NAV_ITEMS.length)
  })
})
