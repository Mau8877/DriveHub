import { ROLES } from '../core/sesion/roles'

/**
 * Menú de primer nivel, distinto por rol (CU05): el Super admin y el Administrador de escuela
 * no comparten ítems.
 *
 * * Un ítem con `hijos` es un grupo desplegable (un botón padre sin ruta propia, ver `<dh-sidebar>`).
 * * Solo hay opciones que ya tienen pantalla: una opción nueva se agrega con el caso de uso que la
 * * crea (y no antes), dentro del grupo que le corresponda.
 * * Ocultar un ítem NO protege la ruta: la protegen el `rolGuard` del shell y la autorización
 * * del backend.
 */
export interface NavItem {
  /** Un grupo no lleva ruta: solo despliega sus `hijos`. */
  ruta?: string
  etiqueta: string
  descripcion?: string
  /** Nombre de un icono de @drivehub/ui (ver `ICONOS`). */
  icono?: string
  /** Roles que ven este ítem: basta con tener uno. */
  roles: readonly string[]
  /** Las opciones de un grupo (un solo nivel: un grupo no se anida dentro de otro). Cada una lleva su icono. */
  hijos?: NavItem[]
}

export const NAV_ITEMS: NavItem[] = [
  {
    ruta: '/',
    etiqueta: 'Inicio',
    descripcion: 'Resumen de la plataforma',
    icono: 'home',
    roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/escuelas',
    etiqueta: 'Gestionar escuelas',
    descripcion: 'Altas, nombres y estado de las escuelas',
    icono: 'school',
    roles: [ROLES.SUPER_ADMIN],
  },
  {
    etiqueta: 'Gestionar usuarios',
    icono: 'users',
    roles: [ROLES.ADMIN_ESCUELA],
    hijos: [
      {
        ruta: '/escuela/estudiantes',
        etiqueta: 'Estudiantes',
        icono: 'user',
        roles: [ROLES.ADMIN_ESCUELA],
      },
      {
        ruta: '/escuela/instructores',
        etiqueta: 'Instructores',
        icono: 'road',
        roles: [ROLES.ADMIN_ESCUELA],
      },
    ],
  },
  {
    etiqueta: 'Gestionar escuela',
    icono: 'school',
    roles: [ROLES.ADMIN_ESCUELA],
    hijos: [
      { ruta: '/escuela/autos', etiqueta: 'Autos', icono: 'car', roles: [ROLES.ADMIN_ESCUELA] },
    ],
  },
  {
    etiqueta: 'Gestionar pagos',
    icono: 'wallet',
    roles: [ROLES.ADMIN_ESCUELA],
    hijos: [
      {
        ruta: '/pagos/planes',
        etiqueta: 'Planes de horas',
        icono: 'receipt',
        roles: [ROLES.ADMIN_ESCUELA],
      },
    ],
  },
  {
    etiqueta: 'Gestionar exámenes',
    icono: 'clipboard',
    roles: [ROLES.ADMIN_ESCUELA],
    hijos: [
      {
        ruta: '/examenes',
        etiqueta: 'Banco de preguntas',
        icono: 'help-circle',
        roles: [ROLES.ADMIN_ESCUELA],
      },
      {
        ruta: '/examenes/simulacros',
        etiqueta: 'Simulacros',
        icono: 'clock',
        roles: [ROLES.ADMIN_ESCUELA],
      },
      {
        ruta: '/examenes/conduccion',
        etiqueta: 'Examen de conducción',
        icono: 'car',
        roles: [ROLES.ADMIN_ESCUELA],
      },
    ],
  },
  {
    etiqueta: 'Gestionar contenido',
    icono: 'book',
    roles: [ROLES.ADMIN_ESCUELA],
    hijos: [
      {
        ruta: '/contenido',
        etiqueta: 'Manuales',
        icono: 'file-text',
        roles: [ROLES.ADMIN_ESCUELA],
      },
    ],
  },
]

const verPor = (item: NavItem, roles: readonly string[]) =>
  item.roles.some((rol) => roles.includes(rol))

/**
 * Los ítems que ve alguien con esos roles. Sin roles reconocidos, ninguno. Un grupo solo aparece si
 * le queda alguna opción visible, y con las que le quedan.
 */
export function navItemsDeRoles(roles: readonly string[]): NavItem[] {
  return NAV_ITEMS.flatMap((item) => {
    if (!verPor(item, roles)) return []
    if (!item.hijos) return [item]
    const hijos = item.hijos.filter((hijo) => verPor(hijo, roles))
    return hijos.length > 0 ? [{ ...item, hijos }] : []
  })
}

const coincide = (actual: string, ruta?: string) =>
  !!ruta && ruta !== '/' && (actual === ruta || actual.startsWith(`${ruta}/`))

/**
 * Miga de pan de una ruta: "Inicio > Sección" para una opción suelta, "Inicio > Grupo > Opción" para
 * una opción de un grupo (el grupo no es una página: no es un enlace) y "Inicio > Mi perfil" para el perfil.
 */
export function migasDeRuta(
  ruta: string,
  items: readonly NavItem[]
): { etiqueta: string; ruta?: string }[] {
  const actual = ruta.split(/[?#]/)[0]
  if (actual === '/' || actual === '') return [{ etiqueta: 'Inicio' }]
  if (actual === '/perfil') return [{ etiqueta: 'Inicio', ruta: '/' }, { etiqueta: 'Mi perfil' }]
  const inicio = { etiqueta: 'Inicio', ruta: '/' }
  for (const item of items) {
    if (coincide(actual, item.ruta)) return [inicio, { etiqueta: item.etiqueta }]
    // * Con dos opciones que coinciden (`/examenes` y `/examenes/simulacros`) gana la de la ruta más específica
    const hijo = item.hijos
      ?.filter((h) => coincide(actual, h.ruta))
      .sort((a, b) => (b.ruta?.length ?? 0) - (a.ruta?.length ?? 0))[0]
    if (hijo) return [inicio, { etiqueta: item.etiqueta }, { etiqueta: hijo.etiqueta }]
  }
  return []
}
