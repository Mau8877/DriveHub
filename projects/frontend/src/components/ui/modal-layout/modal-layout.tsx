import type { Dialog as DialogPrimitive } from '@base-ui/react/dialog'
import { Dialog, DialogContent } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import { ModalLayoutFooter } from './modal-layout-footer'
import { ModalLayoutHeader } from './modal-layout-header'
import type { ModalLayoutProps, ModalLayoutSize } from './types'

const sizeClassName: Record<ModalLayoutSize, string> = {
  sm: 'sm:max-w-md',
  md: 'sm:max-w-lg',
  lg: 'sm:max-w-2xl',
}

export function ModalLayout({
  open,
  onOpenChange,
  title,
  subtitle,
  icon,
  size = 'md',
  formId,
  isSubmitting = false,
  confirmLabel,
  cancelLabel,
  onCancel,
  children,
}: ModalLayoutProps) {
  const handleOpenChange = (
    nextOpen: boolean,
    eventDetails: DialogPrimitive.Root.ChangeEventDetails,
  ) => {
    // * Con isSubmitting en true se cancela CUALQUIER intento de cierre (X,
    // * click afuera, Escape) -- eventDetails.cancel() cubre las tres vías
    // * por igual, sin tener que distinguir el `reason` una por una.
    if (isSubmitting) {
      eventDetails.cancel()
      return
    }
    onOpenChange(nextOpen)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        // * max-h-[85vh]: no hay token de tamaño del sistema para "un límite
        // * relativo al viewport" -- es el tope que evita que el modal ocupe
        // * la pantalla completa en formularios largos, dejando aire arriba/
        // * abajo. El ancho sí usa tokens (`sizeClassName`).
        // * overflow-hidden: el header (bg-header) tiene esquinas rectas --
        // * sin esto, sus esquinas cuadradas sobresaldrían por encima del
        // * radio ya redondeado que trae DialogContent, dejando el modal con
        // * pinta de "no redondeado" arriba a pesar de que el contenedor sí
        // * lo es.
        className={cn('flex max-h-[85vh] flex-col gap-0 overflow-hidden p-0', sizeClassName[size])}
        // * El botón X propio se dibuja en ModalLayoutHeader -- el default de
        // * DialogContent está pensado para un fondo claro (bg-secondary) y
        // * quedaría ilegible sobre el bg-header del header.
        showCloseButton={false}
      >
        <ModalLayoutHeader title={title} subtitle={subtitle} icon={icon} />
        <div className="flex-1 overflow-y-auto p-6">{children}</div>
        <ModalLayoutFooter
          formId={formId}
          isSubmitting={isSubmitting}
          confirmLabel={confirmLabel}
          cancelLabel={cancelLabel}
          onCancel={onCancel ?? (() => onOpenChange(false))}
        />
      </DialogContent>
    </Dialog>
  )
}
