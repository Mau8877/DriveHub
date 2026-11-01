import { createFileRoute } from '@tanstack/react-router'

import { SimulacrosGestion } from '@/features/simulacros'

// * Una sola ruta: <dh-gestion> maneja por dentro el listado, crear y editar (GESTION_FRONTEND.md).
export const Route = createFileRoute('/simulacros')({
  component: SimulacrosGestion,
})
