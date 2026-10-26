import { ICONOS } from '@drivehub/ui'

import { ROLES } from '../core/sesion/roles'
import { NAV_ITEMS, migasDeRuta, navItemsDeRoles } from './nav-items'

/** Las rutas de todas las opciones con pantalla que ve ese rol, con los hijos de los grupos aplanados. */
const rutas = (roles: string[]) =>
  navItemsDeRoles(roles).flatMap((i) => (i.hijos ? i.hijos.map((h) => h.ruta) : [i.ruta]))
const grupo = (roles: string[], etiqueta: string) =>
  navItemsDeRoles(roles).find((i) => i.etiqueta === etiqueta)

describe('navItemsDeRoles', () => {
  it('el Super admin ve "Inicio" y "Gestionar escuelas", sin grupos', () => {
    expect(navItemsDeRoles([ROLES.SUPER_ADMIN]).map((i) => i.etiqueta)).toEqual([
      'Inicio',
      'Gestionar escuelas',
    ])
    expect(rutas([ROLES.SUPER_ADMIN])).toEqual(['/', '/escuelas'])
    expect(navItemsDeRoles([ROLES.SUPER_ADMIN]).some((i) => i.hijos)).toBe(false)
  })

  it('el Administrador de escuela ve Inicio y cinco grupos de gestión', () => {
    expect(navItemsDeRoles([ROLES.ADMIN_ESCUELA]).map((i) => i.etiqueta)).toEqual([
      'Inicio',
      'Gestionar usuarios',
      'Gestionar escuela',
      'Gestionar pagos',
      'Gestionar exámenes',
      'Gestionar contenido',
    ])
  })

  it('cada grupo del Administrador reúne solo opciones que ya tienen pantalla', () => {
    const opciones = (etiqueta: string) =>
      grupo([ROLES.ADMIN_ESCUELA], etiqueta)?.hijos?.map((h) => [h.etiqueta, h.ruta])

    expect(opciones('Gestionar usuarios')).toEqual([
      ['Estudiantes', '/escuela/estudiantes'],
      ['Instructores', '/escuela/instructores'],
    ])
    expect(opciones('Gestionar escuela')).toEqual([
      ['Autos', '/escuela/autos'],
      ['Asignación de autos', '/escuela/asignacion-autos'],
    ])
    expect(opciones('Gestionar pagos')).toEqual([['Planes de horas', '/pagos/planes']])
    expect(opciones('Gestionar exámenes')).toEqual([['Banco de preguntas', '/examenes']])
    expect(opciones('Gestionar contenido')).toEqual([['Manuales', '/contenido']])
  })

  it('los grupos no llevan ruta propia: son botones que despliegan', () => {
    const grupos = navItemsDeRoles([ROLES.ADMIN_ESCUELA]).filter((i) => i.hijos)

    expect(grupos.length).toBe(5)
    expect(grupos.every((g) => g.ruta === undefined)).toBe(true)
  })

  it('las opciones que todavía no tienen pantalla no están en el menú', () => {
    const todas = rutas([ROLES.ADMIN_ESCUELA])

    for (const ruta of ['/identidad', '/escuela', '/asistente-ia']) {
      expect(todas).not.toContain(ruta)
    }
  })

  it('los menús de los dos roles solo comparten Inicio', () => {
    const superAdmin = rutas([ROLES.SUPER_ADMIN])
    const admin = rutas([ROLES.ADMIN_ESCUELA])

    expect(superAdmin.filter((r) => admin.includes(r))).toEqual(['/'])
  })

  it('Estudiantes, Instructores, Autos y Asignación de autos solo los ve el Administrador de escuela', () => {
    for (const ruta of [
      '/escuela/estudiantes',
      '/escuela/instructores',
      '/escuela/autos',
      '/escuela/asignacion-autos',
    ]) {
      expect(rutas([ROLES.ADMIN_ESCUELA])).toContain(ruta)
      expect(rutas([ROLES.SUPER_ADMIN])).not.toContain(ruta)
      expect(rutas([ROLES.INSTRUCTOR])).not.toContain(ruta)
      expect(rutas([ROLES.ESTUDIANTE])).not.toContain(ruta)
    }
  })

  it('un grupo se oculta si a ese rol no le queda ninguna opción, y solo muestra las que le quedan', () => {
    NAV_ITEMS.push({
      etiqueta: 'Grupo de prueba',
      icono: 'users',
      roles: [ROLES.ADMIN_ESCUELA, ROLES.INSTRUCTOR],
      hijos: [
        { ruta: '/solo-instructor', etiqueta: 'Solo instructor', roles: [ROLES.INSTRUCTOR] },
        { ruta: '/solo-admin', etiqueta: 'Solo admin', roles: [ROLES.ADMIN_ESCUELA] },
      ],
    })
    try {
      expect(
        grupo([ROLES.ADMIN_ESCUELA], 'Grupo de prueba')?.hijos?.map((h) => h.etiqueta)
      ).toEqual(['Solo admin'])
      expect(grupo([ROLES.INSTRUCTOR], 'Grupo de prueba')?.hijos?.map((h) => h.etiqueta)).toEqual([
        'Solo instructor',
      ])
      // * El Super admin no ve el grupo (no está en sus roles) y un grupo sin opciones visibles tampoco aparece
      expect(grupo([ROLES.SUPER_ADMIN], 'Grupo de prueba')).toBeUndefined()
      NAV_ITEMS[NAV_ITEMS.length - 1].hijos = [
        { ruta: '/solo-admin', etiqueta: 'Solo admin', roles: [ROLES.ADMIN_ESCUELA] },
      ]
      expect(grupo([ROLES.INSTRUCTOR], 'Grupo de prueba')).toBeUndefined()
    } finally {
      NAV_ITEMS.pop()
    }
  })

  it('todos los ítems, los grupos y cada una de sus opciones tienen un icono que existe en @drivehub/ui', () => {
    const todos = NAV_ITEMS.flatMap((i) => [i, ...(i.hijos ?? [])])

    expect(todos.every((i) => !!i.icono && i.icono in ICONOS)).toBe(true)
  })

  it('las opciones de un grupo no repiten el icono de su grupo', () => {
    for (const grupo of NAV_ITEMS.filter((i) => i.hijos)) {
      expect(grupo.hijos!.every((h) => h.icono !== grupo.icono)).toBe(true)
    }
  })

  it('un rol sin ítems (o sin roles) no ve nada', () => {
    expect(navItemsDeRoles([ROLES.ESTUDIANTE])).toEqual([])
    expect(navItemsDeRoles([])).toEqual([])
  })

  it('con varios roles ve la unión de sus ítems', () => {
    expect(navItemsDeRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN_ESCUELA])).toHaveLength(NAV_ITEMS.length)
  })
})

describe('migasDeRuta', () => {
  const items = navItemsDeRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN_ESCUELA])

  it('en el inicio solo está "Inicio"', () => {
    expect(migasDeRuta('/', items)).toEqual([{ etiqueta: 'Inicio' }])
  })

  it('una opción suelta cuelga de Inicio, también en sus subrutas y con query', () => {
    const esperado = [{ etiqueta: 'Inicio', ruta: '/' }, { etiqueta: 'Gestionar escuelas' }]
    expect(migasDeRuta('/escuelas', items)).toEqual(esperado)
    expect(migasDeRuta('/escuelas/nueva?x=1', items)).toEqual(esperado)
  })

  it('una opción de un grupo cuelga de Inicio > su grupo (que no es un enlace)', () => {
    expect(migasDeRuta('/escuela/estudiantes', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Gestionar usuarios' },
      { etiqueta: 'Estudiantes' },
    ])
    expect(migasDeRuta('/escuela/instructores?search=ana', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Gestionar usuarios' },
      { etiqueta: 'Instructores' },
    ])
    expect(migasDeRuta('/escuela/autos', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Gestionar escuela' },
      { etiqueta: 'Autos' },
    ])
    expect(migasDeRuta('/examenes', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Gestionar exámenes' },
      { etiqueta: 'Banco de preguntas' },
    ])
    expect(migasDeRuta('/pagos/planes', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Gestionar pagos' },
      { etiqueta: 'Planes de horas' },
    ])
    expect(migasDeRuta('/pagos/planes?search=plan', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Gestionar pagos' },
      { etiqueta: 'Planes de horas' },
    ])
  })

  it('el perfil cuelga de Inicio como "Mi perfil"', () => {
    expect(migasDeRuta('/perfil', items)).toEqual([
      { etiqueta: 'Inicio', ruta: '/' },
      { etiqueta: 'Mi perfil' },
    ])
  })

  it('una ruta desconocida, o sin opción en el menú, no tiene migas', () => {
    expect(migasDeRuta('/nada', items)).toEqual([])
    expect(migasDeRuta('/escuela', items)).toEqual([])
  })
})
