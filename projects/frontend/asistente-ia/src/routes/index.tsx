import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: Inicio,
})

// TODO: se reemplaza por las pantallas reales a medida que se implementen
// TODO: los casos de uso de este micro-frontend (CU41, CU43).
function Inicio() {
  return (
    <section className="dh-page">
      <div className="dh-page-head">
        <div>
          <h1>Asistente IA</h1>
          <p>IA local web y agentes n8n.</p>
        </div>
      </div>
      <div className="dh-card p-5">
        <p className="dh-muted m-0 text-sm">Casos de uso pendientes: CU41, CU43.</p>
      </div>
    </section>
  )
}
