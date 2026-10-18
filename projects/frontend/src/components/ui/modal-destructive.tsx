import type { AlertDialog as AlertDialogPrimitive } from '@base-ui/react/alert-dialog'
import { Loader2Icon, TriangleAlertIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

export interface ModalDestructiveProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  /** Qué se va a eliminar -- la advertencia de "no se puede deshacer" ya la agrega el componente, no hace falta repetirla acá. */
  description: string
  isSubmitting?: boolean
  confirmLabel?: string
  cancelLabel?: string
  onConfirm: () => void
  onCancel?: () => void
}

/**
 * Confirmación de hard delete -- variante de un solo propósito, no un
 * `ModalLayout` genérico: siempre el mismo ícono de advertencia, siempre la
 * misma leyenda de "esta acción no se puede deshacer", y sin `formId` (no
 * envuelve un form, solo confirma/cancela). Para crear/editar, usar
 * `ModalLayout` (`@/components/ui/modal-layout`).
 */
export function ModalDestructive({
  open,
  onOpenChange,
  title,
  description,
  isSubmitting = false,
  confirmLabel = 'Eliminar',
  cancelLabel = 'Cancelar',
  onConfirm,
  onCancel,
}: ModalDestructiveProps) {
  const handleOpenChange = (
    nextOpen: boolean,
    eventDetails: AlertDialogPrimitive.Root.ChangeEventDetails,
  ) => {
    // * Mismo criterio que ModalLayout: mientras se está eliminando, ni
    // * Escape ni la acción imperativa cierran el modal a mitad de camino.
    // * El click afuera ya viene bloqueado por default en AlertDialog (a
    // * diferencia de Dialog) -- acá solo falta cubrir Escape.
    if (isSubmitting) {
      eventDetails.cancel()
      return
    }
    onOpenChange(nextOpen)
  }

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogContent
        // * overflow-hidden: mismo motivo que en ModalLayout -- el bloque
        // * rojo de arriba tiene esquinas rectas y, sin esto, sobresaldría
        // * por encima del radio ya redondeado que trae AlertDialogContent.
        // * El ancho ya viene fijo en AlertDialogContent, no hace falta
        // * repetirlo acá.
        className="flex flex-col gap-0 overflow-hidden p-0"
      >
        <div className="flex flex-col items-center gap-2 bg-destructive p-6 text-center text-destructive-foreground">
          <TriangleAlertIcon className="size-10 shrink-0" />
          <AlertDialogTitle className="text-destructive-foreground">{title}</AlertDialogTitle>
          <AlertDialogDescription className="text-destructive-foreground/90">
            {description}
          </AlertDialogDescription>
          <p className="text-sm font-semibold text-destructive-foreground">
            Esta acción no se puede deshacer.
          </p>
        </div>
        <div className="flex justify-end gap-2 border-t border-border p-6">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel ?? (() => onOpenChange(false))}
            disabled={isSubmitting}
          >
            {cancelLabel}
          </Button>
          <Button type="button" variant="destructive-solid" onClick={onConfirm} disabled={isSubmitting}>
            {isSubmitting && <Loader2Icon data-icon="inline-start" className="animate-spin" />}
            {confirmLabel}
          </Button>
        </div>
      </AlertDialogContent>
    </AlertDialog>
  )
}
