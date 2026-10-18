import { Dialog, DialogContent, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { ModalLayoutHeader } from './modal-layout-header'
import type { ModalDetailLayoutProps, ModalLayoutSize } from './types'

const sizeClassName: Record<ModalLayoutSize, string> = {
  sm: 'sm:max-w-md',
  md: 'sm:max-w-lg',
  lg: 'sm:max-w-2xl',
}

/**
 * Modal de solo lectura (ver detalle de un recurso) -- comparte el header
 * visual con `ModalLayout` (bg-header, ícono, subtítulo) pero no envuelve un
 * `<form>`: sin `formId` ni `isSubmitting`, footer propio con "Cerrar" y,
 * opcionalmente, una acción secundaria (ej. "Editar"). Para crear/editar,
 * usar `ModalLayout` (`@/components/ui/modal-layout`).
 */
export function ModalDetailLayout({
  open,
  onOpenChange,
  title,
  subtitle,
  icon,
  size = 'md',
  closeLabel = 'Cerrar',
  actionLabel,
  onAction,
  children,
}: ModalDetailLayoutProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className={cn('flex max-h-[85vh] flex-col gap-0 overflow-hidden p-0', sizeClassName[size])}
        showCloseButton={false}
      >
        <ModalLayoutHeader title={title} subtitle={subtitle} icon={icon} />
        <div className="flex-1 overflow-y-auto p-6">{children}</div>
        <DialogFooter className="shrink-0 border-t border-border p-6">
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            {closeLabel}
          </Button>
          {actionLabel && onAction && (
            <Button type="button" variant="success" onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
