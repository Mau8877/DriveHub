import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'

// Layout "pathless" (prefijo `_`) que envuelve todas las rutas privadas.
// Ninguna ruta nueva que necesite sesión se valida a mano por su cuenta:
// se agrega como hija de este archivo (carpeta `_authenticated/`) y hereda
// este guard. Ver .claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md.
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: () => {
    // TODO: reemplazar por la validación real de sesión una vez que exista
    // TODO: el store/hook de auth (ver SEGURIDAD_AUTH_BACKEND.md para el
    // TODO: flujo de JWT + refresh token del lado del backend). Por ahora
    // TODO: no hay ningún token que chequear, así que no hay redirect real.
    const isLoggedIn = true

    if (!isLoggedIn) {
      throw redirect({ to: '/login' })
    }
  },
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  return <Outlet />
}
