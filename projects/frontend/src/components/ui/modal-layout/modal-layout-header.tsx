import { type LucideIcon, XIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DialogClose, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

export interface ModalLayoutHeaderProps {
  title: string
  subtitle?: string
  icon?: LucideIcon
  className?: string
}

export function ModalLayoutHeader({ title, subtitle, icon: Icon, className }: ModalLayoutHeaderProps) {
  return (
    // * bg-header/text-header-foreground (no bg-primary) a propósito -- es
    // * el mismo token fijo (no varía entre claro/oscuro) que usa AppHeader
    // * para la barra superior del layout, así el header del modal combina
    // * con el resto de la app en ambos temas en vez de aclararse en oscuro
    // * como haría bg-primary (ver TAILWIND_STYLES_FRONTEND.md).
    <DialogHeader className={cn('shrink-0 border-b border-border bg-header p-6 text-header-foreground', className)}>
      <div className="flex items-start justify-between gap-4">
        <DialogTitle className="flex items-center gap-2 text-header-foreground">
          {Icon && <Icon className="size-5 shrink-0" />}
          {title}
        </DialogTitle>
        {/* * Reemplaza el botón X default de DialogContent (bg-secondary, pensado
         * para un header claro) -- ModalLayout lo desactiva vía
         * showCloseButton={false} y renderiza este acá para que quede legible
         * sobre el fondo bg-header. */}
        <DialogClose
          render={
            <Button
              variant="ghost"
              size="icon-sm"
              className="-mt-1 -mr-1 shrink-0 text-header-foreground hover:bg-header-foreground/10 hover:text-header-foreground"
            />
          }
        >
          <XIcon />
          <span className="sr-only">Cerrar</span>
        </DialogClose>
      </div>
      {subtitle && <DialogDescription className="text-header-foreground/80">{subtitle}</DialogDescription>}
    </DialogHeader>
  )
}
