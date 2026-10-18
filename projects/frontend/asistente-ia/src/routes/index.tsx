import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: Inicio,
})

// TODO: se reemplaza por las pantallas reales a medida que se implementen
// TODO: los casos de uso de este micro-frontend (CU41, CU43).
function Inicio() {
  return (
    <section className="p-6">
      <h1 className="mb-1 text-2xl font-semibold">Asistente IA</h1>
      <p className="text-muted-foreground">IA local web y agentes n8n.</p>
      <p className="text-muted-foreground mt-4 text-sm">Casos de uso pendientes: CU41, CU43.</p>
    </section>
  )
}
