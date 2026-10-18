import type { LucideIcon } from 'lucide-react'

export interface NavItem {
  label: string
  href: string
  icon: LucideIcon
}

export interface LayoutUser {
  nombre: string
  rol: string
  avatarUrl?: string
}

// Los 4 tipos ya establecidos en el sistema de toasts (ver
// components/ui/toast.ts/sonner.tsx) -- se reusa el mismo vocabulario y el
// mismo ícono/color por tipo (CircleCheckIcon+success, InfoIcon+info,
// TriangleAlertIcon+warning, OctagonXIcon+error) para que una notificación
// "info" se vea igual acá que en cualquier otro lugar de la app que ya
// distingue estos 4 estados, no un set de íconos nuevo inventado solo para
// este componente.
export type NotificationType = 'success' | 'info' | 'warning' | 'error'

// El orden (más nueva primero) es responsabilidad de quien arma el array,
// no de `NotificationsMenu` -- mismo criterio que el resto de este paquete
// de layout: los componentes son "tontos" (reciben todo por props), la
// lógica de negocio vive en la feature/hook que los consume.
export interface NotificationItem {
  id: string
  tipo: NotificationType
  titulo: string
  detalle?: string
  /** ISO-8601 -- `NotificationsMenu` la formatea a relativo (ej. "hace 5 minutos") con `formatRelativeDate`. */
  fecha: string
  /** Al marcar como leída NO desaparece de la lista -- solo cambia el fondo de gris a blanco (ver NotificationsMenu). */
  leida: boolean
}

export interface NotificationsPageMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface NotificationsController {
  /** Página(s) ya cargadas, acumuladas (ver onLoadMore) -- no es "solo la página actual". */
  items: NotificationItem[]
  /** `undefined` mientras no se pidió la primera página (ver onOpenChange). */
  meta: NotificationsPageMeta | undefined
  /** Carga inicial (panel recién abierto, todavía sin `items`). */
  isLoading: boolean
  /** Carga de una página siguiente (ya hay `items`, se están agregando más). */
  isLoadingMore: boolean
  /**
   * Cantidad de no leídas para el "circulito" de la campanita. Llega por
   * una vía distinta al GET paginado de `items` (según quien arme este
   * controller -- ej. un endpoint propio de resumen, o un campo del
   * usuario autenticado), por eso es un campo aparte y no
   * `items.filter(i => !i.leida).length` -- esa cuenta solo reflejaría lo
   * que ya se cargó en el panel, no el total real sin leer.
   */
  unreadCount: number
  /** Se llama al abrir/cerrar el panel -- dispara el GET de la primera página la primera vez que se abre. */
  onOpenChange: (open: boolean) => void
  /** Pide la siguiente página (10 en 10) sin descartar lo ya cargado. */
  onLoadMore: () => void
  /** Marca una notificación como leída -- sigue en la lista (ver NotificationItem.leida). */
  onMarkAsRead: (id: string) => void
  onMarkAllAsRead: () => void
  /** Hard delete de todas las notificaciones (no solo "marcar como leídas"). */
  onDeleteAll: () => void
}
