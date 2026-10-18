import { useState } from 'react'
import {
  BellIcon,
  CircleCheckIcon,
  InfoIcon,
  Loader2Icon,
  OctagonXIcon,
  Trash2Icon,
  TriangleAlertIcon,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { cn } from '@/lib/utils'
import { formatRelativeDate } from '@/utils/formatRelativeDate'
import {
  AlertDialog,
  AlertDialogClose,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button, buttonVariants } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'

import type { NotificationItem, NotificationsController, NotificationType } from './types'

interface NotificationsMenuProps {
  notifications: NotificationsController
  /** className del botón campanita (ej. headerButtonClass de AppHeader, para que combine con el fondo navy del header). */
  triggerClassName?: string
}

// Mismo vocabulario visual que ya usan los toasts (ver
// components/ui/sonner.tsx) -- una notificación "info" se ve igual acá que
// en cualquier otro lugar de la app que distingue estos 4 estados.
const TYPE_CONFIG: Record<NotificationType, { icon: LucideIcon; className: string }> = {
  success: { icon: CircleCheckIcon, className: 'bg-success/15 text-success' },
  info: { icon: InfoIcon, className: 'bg-info/15 text-info' },
  warning: { icon: TriangleAlertIcon, className: 'bg-warning/15 text-warning' },
  error: { icon: OctagonXIcon, className: 'bg-accent/15 text-accent' },
}

// * Altura de ~5 filas y media -- a partir de ahí aparece scroll adentro de
// * la lista (el botón "Mostrar más" queda dentro de esa zona con scroll,
// * no en el footer fijo).
const LIST_MAX_HEIGHT = 'max-h-[380px]'

function NotificationRow({ item, onMarkAsRead }: { item: NotificationItem; onMarkAsRead: (id: string) => void }) {
  const { icon: Icon, className: iconClassName } = TYPE_CONFIG[item.tipo]

  return (
    <li>
      <button
        type="button"
        disabled={item.leida}
        onClick={() => onMarkAsRead(item.id)}
        className={cn(
          'flex w-full items-start gap-2.5 px-4 py-3 text-left transition-colors',
          item.leida ? 'bg-popover hover:bg-muted/40' : 'bg-muted hover:bg-muted/70',
        )}
      >
        <span className={cn('mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full', iconClassName)}>
          <Icon className="size-4" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-sm font-medium text-foreground">{item.titulo}</span>
          {item.detalle && (
            <span className="mt-0.5 block text-xs text-muted-foreground">{item.detalle}</span>
          )}
          <span className="mt-1 block text-xs text-muted-foreground/70">{formatRelativeDate(item.fecha)}</span>
        </span>
      </button>
    </li>
  )
}

export function NotificationsMenu({ notifications, triggerClassName }: NotificationsMenuProps) {
  const {
    items,
    meta,
    isLoading,
    isLoadingMore,
    unreadCount,
    onOpenChange,
    onLoadMore,
    onMarkAsRead,
    onMarkAllAsRead,
    onDeleteAll,
  } = notifications
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false)
  const hasItems = items.length > 0
  const hasMore = !!meta && meta.page < meta.totalPages

  function handleConfirmDelete() {
    onDeleteAll()
    setConfirmDeleteOpen(false)
  }

  return (
    <>
      <Popover onOpenChange={onOpenChange}>
        <PopoverTrigger
          className={cn(buttonVariants({ variant: 'ghost', size: 'icon-sm' }), 'relative', triggerClassName)}
        >
          <BellIcon />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[10px] leading-none font-medium text-accent-foreground ring-2 ring-header">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
          <span className="sr-only">Notificaciones</span>
        </PopoverTrigger>

        <PopoverContent align="end" className="w-80 p-0">
          <div className="flex items-center justify-between px-4 py-3">
            <span className="font-heading text-sm font-medium text-foreground">Notificaciones</span>
            {unreadCount > 0 && (
              <span className="text-xs text-muted-foreground">{unreadCount} sin leer</span>
            )}
          </div>
          <Separator />

          {isLoading ? (
            <div className="flex flex-col gap-2 divide-y divide-border/60 p-2">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="flex items-start gap-2.5 px-2 py-2.5">
                  <Skeleton className="size-8 shrink-0 rounded-full" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-3.5 w-3/4" />
                    <Skeleton className="h-3 w-1/2" />
                  </div>
                </div>
              ))}
            </div>
          ) : hasItems ? (
            <ul className={cn('divide-y divide-border/60 overflow-y-auto', LIST_MAX_HEIGHT)}>
              {items.map((item) => (
                <NotificationRow key={item.id} item={item} onMarkAsRead={onMarkAsRead} />
              ))}
              {hasMore && (
                <li className="p-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="w-full"
                    disabled={isLoadingMore}
                    onClick={onLoadMore}
                  >
                    {isLoadingMore ? (
                      <>
                        <Loader2Icon data-icon="inline-start" className="animate-spin" />
                        Cargando...
                      </>
                    ) : (
                      'Mostrar más'
                    )}
                  </Button>
                </li>
              )}
            </ul>
          ) : (
            <p className="px-4 py-8 text-center text-sm text-muted-foreground">
              No tenés notificaciones.
            </p>
          )}

          <Separator />
          <div className="flex items-center gap-2 p-2">
            <Button
              variant="ghost"
              size="sm"
              className="flex-1"
              disabled={!hasItems || unreadCount === 0}
              onClick={onMarkAllAsRead}
            >
              Leer todo
            </Button>
            <Button
              variant="destructive"
              size="sm"
              className="flex-1"
              disabled={!hasItems}
              onClick={() => setConfirmDeleteOpen(true)}
            >
              <Trash2Icon data-icon="inline-start" />
              Eliminar notificaciones
            </Button>
          </div>
        </PopoverContent>
      </Popover>

      <AlertDialog open={confirmDeleteOpen} onOpenChange={setConfirmDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar todas las notificaciones?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta acción no se puede deshacer -- se eliminan permanentemente, no solo se marcan
              como leídas.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogClose>Cancelar</AlertDialogClose>
            <Button variant="destructive" onClick={handleConfirmDelete}>
              <Trash2Icon data-icon="inline-start" />
              Eliminar notificaciones
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
