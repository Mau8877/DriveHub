import { montarShellSuelto } from '@drivehub/ui/desarrollo'

import { mount } from './mount'

// Solo para correr el remoto suelto (pnpm dev, puerto 4205): simula lo que haría el shell.
// Lo envuelve en el mismo armazón (<dh-shell>, menú y cabecera de @drivehub/ui) para que se vea
// igual que dentro del shell. Sin shell no hay sesión, así que no hay token.
const { contenido } = montarShellSuelto(document.getElementById('root')!, {
  items: [{ ruta: '/', etiqueta: 'Contenido', descripcion: 'Manuales de la escuela.', icono: 'book' }],
  activo: '/',
  migas: [{ etiqueta: 'Contenido' }],
})

mount(contenido, {
  basePath: '/',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,
  obtenerToken: () => null,
  navegar: (ruta) => window.location.assign(ruta),
  // * Suelto no hay shell que renueve la sesión
  renovarSesion: async () => null,
})
