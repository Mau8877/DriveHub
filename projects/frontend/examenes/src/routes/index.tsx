import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: Inicio,
})

// TODO: se reemplaza por las pantallas reales a medida que se implementen
// TODO: los casos de uso de este micro-frontend (CU19, CU20, CU22, CU23).
function Inicio() {
  return (
    <section className="p-6">
      <h1 className="mb-1 text-2xl font-semibold">Exámenes</h1>
      <p className="text-muted-foreground">Banco de preguntas, simulacros y notas.</p>
      <p className="text-muted-foreground mt-4 text-sm">Casos de uso pendientes: CU19, CU20, CU22, CU23.</p>
    </section>
  )
}
