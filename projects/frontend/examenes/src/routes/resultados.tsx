import { createFileRoute } from '@tanstack/react-router'

import { ResultadosGestion } from '@/features/resultados'

// * Una sola ruta, solo lectura: <dh-gestion> dibuja el listado con su filtro (GESTION_FRONTEND.md).
export const Route = createFileRoute('/resultados')({
  component: ResultadosGestion,
})
