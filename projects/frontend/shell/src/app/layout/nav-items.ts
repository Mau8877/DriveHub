/**
 * Menú de primer nivel: un ítem por micro-frontend web.
 *
 * TODO: filtrar por los roles del usuario (claim del JWT) cuando exista el
 * TODO: login (CU02). Hoy se muestran todos.
 */
export interface NavItem {
  ruta: string
  etiqueta: string
  descripcion: string
}

export const NAV_ITEMS: NavItem[] = [
  { ruta: '/identidad', etiqueta: 'Identidad', descripcion: 'Usuarios, roles y perfil' },
  { ruta: '/escuela', etiqueta: 'Escuela', descripcion: 'Autos, alumnos y docentes' },
  { ruta: '/pagos', etiqueta: 'Pagos', descripcion: 'Planes y cuotas' },
  { ruta: '/examenes', etiqueta: 'Exámenes', descripcion: 'Preguntas y simulacros' },
  { ruta: '/contenido', etiqueta: 'Contenido', descripcion: 'Manuales de la escuela' },
  { ruta: '/asistente-ia', etiqueta: 'Asistente IA', descripcion: 'Agentes n8n e IA local' },
]
