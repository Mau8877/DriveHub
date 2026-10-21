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
  /** Roles que ven este ítem: basta con tener uno. */
  roles: readonly string[]
}

export const NAV_ITEMS: NavItem[] = [
  {
    ruta: '/escuelas',
    etiqueta: 'Gestionar escuelas',
    descripcion: 'Altas, nombres y estado de las escuelas',
    roles: [ROLES.SUPER_ADMIN],
  },
  {
    ruta: '/identidad',
    etiqueta: 'Identidad',
    descripcion: 'Usuarios, roles y perfil',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/escuela',
    etiqueta: 'Escuela',
    descripcion: 'Autos, alumnos y docentes',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/pagos',
    etiqueta: 'Pagos',
    descripcion: 'Planes y cuotas',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/examenes',
    etiqueta: 'Exámenes',
    descripcion: 'Preguntas y simulacros',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/contenido',
    etiqueta: 'Contenido',
    descripcion: 'Manuales de la escuela',
    roles: [ROLES.ADMIN_ESCUELA],
  },
  {
    ruta: '/asistente-ia',
    etiqueta: 'Asistente IA',
    descripcion: 'Agentes n8n e IA local',
    roles: [ROLES.ADMIN_ESCUELA],
  },
]

/** Los ítems que ve alguien con esos roles. Sin roles reconocidos, ninguno. */
export function navItemsDeRoles(roles: readonly string[]): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.some((rol) => roles.includes(rol)))
}
