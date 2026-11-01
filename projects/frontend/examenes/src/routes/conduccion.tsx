import { createFileRoute } from '@tanstack/react-router'

import { ConduccionGestion } from '@/features/conduccion'

// * Una sola ruta: <dh-gestion> maneja por dentro el listado, programar, reprogramar y las acciones.
export const Route = createFileRoute('/conduccion')({
  component: ConduccionGestion,
})
