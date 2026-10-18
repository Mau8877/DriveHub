import {
  MenuIcon,
  LayoutDashboardIcon,
  SunIcon,
  MoonIcon,
  LogOutIcon,
  UserIcon,
  KeyRoundIcon,
  SettingsIcon,
} from 'lucide-react'

import { cn } from '@/lib/utils'
import { runThemeTransition } from '@/utils/runThemeTransition'
import { useUiStore } from '@/store'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import { Breadcrumbs } from './Breadcrumbs'
import { HeaderClock } from './HeaderClock'
import { NotificationsMenu } from './NotificationsMenu'
import type { LayoutUser, NotificationsController } from './types'

function iniciales(nombre: string) {
  return nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join('')
}

interface AppHeaderProps {
  appName: string
  user: LayoutUser
  /** Sin esto, la campanita no muestra "circulito" y el panel queda vacío. */
  notifications?: NotificationsController
}

const EMPTY_NOTIFICATIONS: NotificationsController = {
  items: [],
  meta: undefined,
  isLoading: false,
  isLoadingMore: false,
  unreadCount: 0,
  onOpenChange: () => {},
  onLoadMore: () => {},
  onMarkAsRead: () => {},
  onMarkAllAsRead: () => {},
  onDeleteAll: () => {},
}

// * Un solo botón de menú controla el sidebar en ambos tamaños de pantalla
// * (desktop: colapsa/expande el ancho; mobile: abre/cierra el drawer) --
// * se decide en runtime con matchMedia, igual que el breakpoint `md` de
// * Tailwind, para que el control visible sea uno solo como en la
// * referencia de diseño, en vez de dos botones distintos.
function toggleSidebar() {
  const isDesktop = window.matchMedia('(min-width: 768px)').matches
  const { toggleSidebarCollapsed, mobileSidebarOpen, setMobileSidebarOpen } = useUiStore.getState()

  if (isDesktop) {
    toggleSidebarCollapsed()
  } else {
    setMobileSidebarOpen(!mobileSidebarOpen)
  }
}

// * Clase compartida por los botones del header: sobre fondo navy sólido
// * (bg-header), el hover/foco por default de Button (pensado para fondos
// * claros) queda invisible -- se pisa acá con blanco translúcido.
const headerButtonClass =
  'text-header-foreground hover:bg-header-foreground/15 hover:text-header-foreground focus-visible:ring-header-foreground/40'

export function AppHeader({ appName, user, notifications = EMPTY_NOTIFICATIONS }: AppHeaderProps) {
  const theme = useUiStore((s) => s.theme)
  const toggleTheme = useUiStore((s) => s.toggleTheme)

  return (
    <header className="flex h-16 shrink-0 items-center gap-3 bg-header px-4 text-header-foreground">
      <Button variant="ghost" size="icon-sm" className={headerButtonClass} onClick={toggleSidebar}>
        <MenuIcon />
        <span className="sr-only">Alternar sidebar</span>
      </Button>

      <div className="flex items-center gap-2.5">
        <span className="flex size-9 items-center justify-center rounded-xl bg-accent">
          <LayoutDashboardIcon className="size-5 text-accent-foreground" />
        </span>
        <span className="font-heading text-lg font-semibold">{appName}</span>
      </div>

      <Breadcrumbs className="ml-2 hidden lg:flex" />

      <div className="ml-auto flex items-center gap-3">
        <HeaderClock className="hidden text-xs text-header-foreground/60 xl:block" />
        <NotificationsMenu notifications={notifications} triggerClassName={headerButtonClass} />

        <DropdownMenu>
          <DropdownMenuTrigger
            className={cn(
              'ml-1 flex items-center gap-2.5 rounded-full px-2.5 py-1.5 outline-none transition-colors hover:bg-header-foreground/15 focus-visible:ring-3 focus-visible:ring-header-foreground/40'
            )}
          >
            <Avatar>
              <AvatarFallback className="bg-accent text-accent-foreground">
                {iniciales(user.nombre)}
              </AvatarFallback>
            </Avatar>
            <span className="hidden text-left text-sm sm:block">
              <span className="block leading-tight font-medium">{user.nombre}</span>
              <span className="block text-xs leading-tight text-header-foreground/70">
                {user.rol}
              </span>
            </span>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuGroup>
              <DropdownMenuLabel>{user.nombre}</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem>
                <UserIcon />
                Mi perfil
              </DropdownMenuItem>
              <DropdownMenuItem>
                <KeyRoundIcon />
                Cambiar contraseña
              </DropdownMenuItem>
              <DropdownMenuItem
                closeOnClick={false}
                onClick={(event) => runThemeTransition(event.clientX, event.clientY, toggleTheme)}
              >
                {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
                Cambiar tema
              </DropdownMenuItem>
              <DropdownMenuItem>
                <SettingsIcon />
                Configuración
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive">
                <LogOutIcon />
                Cerrar sesión
              </DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}
