import { createFileRoute } from '@tanstack/react-router'

import { ManualesGestion } from '@/features/manuales'

// * Una sola ruta: <dh-gestion> maneja por dentro el listado, crear y editar (GESTION_FRONTEND.md)
export const Route = createFileRoute('/')({
  component: ManualesGestion,
})
