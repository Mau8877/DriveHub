import type { ReactNode } from 'react'
import { buttonVariants } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'

export interface RowActionButtonProps {
  /** Ícono a mostrar (ej. `<PencilIcon />` de lucide-react). El tooltip es lo que explica qué hace -- el ícono solo no alcanza para accesibilidad. */
  icon: ReactNode
  /** Texto del tooltip al hover, y también `aria-label` del botón (ej. "Editar", "Eliminar", "Ver detalles"). */
  label: string
  onClick?: () => void
  /** `destructive` para acciones como eliminar -- mismo color que el resto del sistema de diseño. */
  variant?: 'default' | 'destructive'
  disabled?: boolean
  className?: string
}

/**
 * Botón de ícono con tooltip para una fila de `DataTable`/`DataCards`
 * (ver detalles, editar, eliminar, etc.) -- pensado para usarse dentro de
 * `renderActions`, varios juntos por fila. El tooltip explica el ícono al
 * hover, así no hace falta texto visible en cada botón.
 */
export function RowActionButton({
  icon,
  label,
  onClick,
  variant = 'default',
  disabled,
  className,
}: RowActionButtonProps) {
  return (
    <Tooltip>
      <TooltipTrigger
        aria-label={label}
        disabled={disabled}
        onClick={onClick}
        className={cn(
          buttonVariants({ variant: 'ghost', size: 'icon-sm' }),
          variant === 'destructive' && 'text-destructive hover:bg-destructive/10',
          className,
        )}
      >
        {icon}
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  )
}
