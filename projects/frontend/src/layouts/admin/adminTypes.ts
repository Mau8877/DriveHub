import type { ReactNode } from 'react'

import type { LayoutUser, NotificationsController } from '@/components/layout'

export interface AdminLayoutProps {
  title: string
  user: LayoutUser
  /** Ver AppHeaderProps. */
  notifications?: NotificationsController
  /** Solo para pantallas de prueba: fuerza qué ítem se ve activo. */
  activeHref?: string
  children: ReactNode
}
