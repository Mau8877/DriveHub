import { mount } from './mount'

// Solo para correr el remoto suelto (pnpm dev, puerto 4205): simula lo que
// haría el shell. Sin shell no hay sesión, así que no hay token.
mount(document.getElementById('root')!, {
  basePath: '/',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,
  obtenerToken: () => null,
  navegar: (ruta) => window.location.assign(ruta),
})
