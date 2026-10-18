import { createFileRoute } from '@tanstack/react-router'

// * Placeholder mínimo: un layout pathless (_authenticated.tsx) necesita al
// * menos una ruta hija para no colisionar con "/" -- ver el comentario en
// * _authenticated.tsx. Esta pantalla se reemplaza por la primera feature
// * real que viva detrás del login.
export const Route = createFileRoute('/_authenticated/panel')({
  component: PanelPage,
})

function PanelPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background text-foreground">
      <h1 className="text-2xl font-semibold">Panel</h1>
    </div>
  )
}
