import { Loader2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

export interface ModalLayoutFooterProps {
  formId: string
  isSubmitting?: boolean
  confirmLabel?: string
  cancelLabel?: string
  onCancel: () => void
  className?: string
}

export function ModalLayoutFooter({
  formId,
  isSubmitting = false,
  confirmLabel = 'Guardar',
  cancelLabel = 'Cancelar',
  onCancel,
  className,
}: ModalLayoutFooterProps) {
  return (
    <DialogFooter className={cn('shrink-0 border-t border-border p-6', className)}>
      <Button type="button" variant="destructive-solid" onClick={onCancel} disabled={isSubmitting}>
        {cancelLabel}
      </Button>
      <Button type="submit" variant="success" form={formId} disabled={isSubmitting}>
        {isSubmitting && <Loader2Icon data-icon="inline-start" className="animate-spin" />}
        {confirmLabel}
      </Button>
    </DialogFooter>
  )
}
