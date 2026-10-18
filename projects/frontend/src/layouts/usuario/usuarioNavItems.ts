import { HomeIcon, ClipboardListIcon, UserIcon } from 'lucide-react'

import type { NavItem } from '@/components/layout'

export const usuarioNavItems: NavItem[] = [
  { label: 'Inicio', href: '/', icon: HomeIcon },
  { label: 'Pedidos', href: '/pedidos', icon: ClipboardListIcon },
  { label: 'Perfil', href: '/perfil', icon: UserIcon },
]
