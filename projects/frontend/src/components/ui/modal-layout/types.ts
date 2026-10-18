import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

export type ModalLayoutSize = 'sm' | 'md' | 'lg'

export interface ModalLayoutProps {
  /** Controla la visibilidad del modal (mismo patrón que `Dialog`). */
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  subtitle?: string
  /** Ícono de lucide-react al inicio del título. Sin este prop, no se muestra ninguno. */
  icon?: LucideIcon
  /** Ancho del modal -- set fijo de tokens, nunca un valor libre. Default `"md"`. */
  size?: ModalLayoutSize
  /**
   * `id` del `<form>` que vive en `children`. El botón de confirmar del
   * footer usa el atributo HTML `form` para disparar el submit nativo de
   * ese form sin que el footer conozca su lógica de `handleSubmit`.
   */
  formId: string
  /**
   * Mientras es `true`: bloquea el cierre del modal (X, click afuera,
   * Escape) y deshabilita ambos botones del footer.
   */
  isSubmitting?: boolean
  confirmLabel?: string
  cancelLabel?: string
  /** Default: cierra el modal (`onOpenChange(false)`). */
  onCancel?: () => void
  children: ReactNode
}

export interface ModalDetailLayoutProps {
  /** Controla la visibilidad del modal (mismo patrón que `Dialog`). */
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  subtitle?: string
  /** Ícono de lucide-react al inicio del título. Sin este prop, no se muestra ninguno. */
  icon?: LucideIcon
  /** Ancho del modal -- set fijo de tokens, nunca un valor libre. Default `"md"`. */
  size?: ModalLayoutSize
  /** Default: `"Cerrar"`. */
  closeLabel?: string
  /**
   * Acción secundaria opcional del footer (ej. "Editar"). Solo se muestra
   * cuando se pasan `actionLabel` y `onAction` juntos.
   */
  actionLabel?: string
  onAction?: () => void
  children: ReactNode
}
