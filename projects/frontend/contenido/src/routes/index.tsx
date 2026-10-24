import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: Inicio,
})

// TODO: se reemplaza por las pantallas reales a medida que se implementen
// TODO: los casos de uso de este micro-frontend (CU32).
function Inicio() {
  return (
    <section className="dh-page">
      <div className="dh-page-head">
        <div>
          <h1>Contenido</h1>
          <p>Manuales de la escuela.</p>
        </div>
      </div>
      <div className="dh-card p-5">
        <p className="dh-muted m-0 text-sm">Casos de uso pendientes: CU32.</p>
      </div>
    </section>
  )
}
