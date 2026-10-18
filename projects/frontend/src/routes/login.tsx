import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/login')({
  component: LoginPage,
})

function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background text-foreground">
      <h1 className="text-2xl font-semibold">Iniciar sesión</h1>
    </div>
  )
}
