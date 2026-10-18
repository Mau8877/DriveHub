import { toast } from "sonner"

// * Envuelve toast.success/error/warning/info de sonner con nuestros cuatro
// * tipos de notificación -- el color de cada uno ya sale de los tokens
// * --success-*/--error-*/--warning-*/--info-* configurados en
// * components/ui/sonner.tsx, así que quien llama a estas funciones no
// * vuelve a decidir estilos, solo el texto.
export function toastSuccess(message: string, description?: string) {
  toast.success(message, { description })
}

export function toastError(message: string, description?: string) {
  toast.error(message, { description })
}

export function toastWarning(message: string, description?: string) {
  toast.warning(message, { description })
}

export function toastInfo(message: string, description?: string) {
  toast.info(message, { description })
}
