import { createFileRoute } from '@tanstack/react-router'

import { PreguntasGestion } from '@/features/preguntas'

// * Una sola ruta: <dh-gestion> maneja por dentro el listado, crear y editar (GESTION_FRONTEND.md).
// * Los simulacros y el examen de conducción (CU20, CU22) suman sus rutas cuando se implementen.
export const Route = createFileRoute('/')({
  component: PreguntasGestion,
})
