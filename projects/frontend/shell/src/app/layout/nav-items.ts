import { ROLES } from '../core/sesion/roles'

/**
 * Menú de primer nivel, distinto por rol (CU05): el Super admin y el Administrador de escuela
 * no comparten ítems.
 *
 * * Ocultar un ítem NO protege la ruta: la protegen el `rolGuard` del shell y la autorización
 * * del backend.
 */
export interface NavItem {
  ruta: string
  etiqueta: string
  descripcion: string
  /** Nombre de un icono de @drivehub/ui (ver `ICONOS`). */
  icono: string
  /** Roles que ven este ítem: basta con tener uno. */
  roles: readonly string[]
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
    ruta: '/identidad',
    etiqueta: 'Identidad',
    descripcion: 'Usuarios, roles y perfil',
    icono: 'users',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/escuela',
    etiqueta: 'Escuela',
    descripcion: 'Autos, alumnos y docentes',
    icono: 'car',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/pagos',
    etiqueta: 'Pagos',
    descripcion: 'Planes y cuotas',
    icono: 'wallet',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/examenes',
    etiqueta: 'Exámenes',
    descripcion: 'Preguntas y simulacros',
    icono: 'clipboard',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/contenido',
    etiqueta: 'Contenido',
    descripcion: 'Manuales de la escuela',
    icono: 'book',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/asistente-ia',
    etiqueta: 'Asistente IA',
    descripcion: 'Agentes n8n e IA local',
    icono: 'bot',
    roles: [ROLES.ADMIN_ESCUELA],
  },
]

/** Los ítems que ve alguien con esos roles. Sin roles reconocidos, ninguno. */
export function navItemsDeRoles(roles: readonly string[]): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.some((rol) => roles.includes(rol)))
}

/** Miga de pan de una ruta: "Inicio > Sección" para los módulos y "Inicio > Mi perfil" para el perfil. */
export function migasDeRuta(
  ruta: string,
  items: readonly NavItem[]
): { etiqueta: string; ruta?: string }[] {
  const actual = ruta.split(/[?#]/)[0]
  if (actual === '/' || actual === '') return [{ etiqueta: 'Inicio' }]
  if (actual === '/perfil') return [{ etiqueta: 'Inicio', ruta: '/' }, { etiqueta: 'Mi perfil' }]
  const item = items.find(
    (i) => i.ruta !== '/' && (actual === i.ruta || actual.startsWith(`${i.ruta}/`))
  )
  return item ? [{ etiqueta: 'Inicio', ruta: '/' }, { etiqueta: item.etiqueta }] : []
}
