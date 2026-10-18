import type { CSSProperties } from "react"
import { Toaster as Sonner, type ToasterProps } from "sonner"
import { CircleCheckIcon, InfoIcon, TriangleAlertIcon, OctagonXIcon, Loader2Icon } from "lucide-react"

import { useUiStore } from "@/store"

// * Se usa el `theme` de nuestro useUiStore (no `next-themes`, que la CLI de
// * shadcn instala por default) -- el proyecto ya tiene su propio store de
// * tema persistido, duplicarlo con otra librería sería redundante.
const Toaster = ({ ...props }: ToasterProps) => {
  const theme = useUiStore((s) => s.theme)

  return (
    <Sonner
      theme={theme}
      richColors
      className="toaster group"
      icons={{
        success: (
          <CircleCheckIcon className="size-4" />
        ),
        info: (
          <InfoIcon className="size-4" />
        ),
        warning: (
          <TriangleAlertIcon className="size-4" />
        ),
        error: (
          <OctagonXIcon className="size-4" />
        ),
        loading: (
          <Loader2Icon className="size-4 animate-spin" />
        ),
      }}
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
          "--border-radius": "var(--radius)",
          "--success-bg": "var(--success)",
          "--success-text": "var(--success-foreground)",
          "--success-border": "var(--success)",
          // * El error usa el rojo distintivo de marca (`--accent`), no
          // * `--destructive` -- ese rojo más fuerte es el que se reserva
          // * para el toast de error; `--destructive` sigue usándose en
          // * otros elementos (botones de eliminar, etc.).
          "--error-bg": "var(--accent)",
          "--error-text": "var(--accent-foreground)",
          "--error-border": "var(--accent)",
          "--warning-bg": "var(--warning)",
          "--warning-text": "var(--warning-foreground)",
          "--warning-border": "var(--warning)",
          "--info-bg": "var(--info)",
          "--info-text": "var(--info-foreground)",
          "--info-border": "var(--info)",
        } as CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
