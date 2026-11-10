import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient, configurarApi } from '@/store'
import MockAdapter from '@/store/test-utils/mockAdapter'

import { ReportesScreen } from '../screens/ReportesScreen'

const backend = new MockAdapter(apiClient)

const sobre = (data: unknown) => ({ status: 'success', data, message: 'ok', timestamp: 'x' })

const CATALOGO = [
  {
    clave: 'pagos',
    nombre: 'Pagos y horas vendidas',
    descripcion: 'Compras de planes de horas',
    columnas: [
      { clave: 'fecha', etiqueta: 'Fecha', tipo: 'fecha_hora' },
      { clave: 'montoBs', etiqueta: 'Monto (Bs)', tipo: 'moneda_bs' },
    ],
    porDefecto: ['fecha', 'montoBs'],
    estados: [{ valor: 'CONFIRMADO', etiqueta: 'Confirmado' }],
    tipos: [],
    filtraPorFecha: true,
  },
]

const INTERPRETACION = {
  especificacion: {
    fuente: 'pagos',
    titulo: 'Ventas de octubre',
    columnas: ['fecha', 'montoBs'],
    filtros: { desde: '2026-10-01', hasta: '2026-10-31', estado: null, tipo: null },
    orden: null,
    formato: 'xlsx',
  },
  descartado: ['columna «sueldo»'],
}

function pantalla() {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={cliente}>
      <ReportesScreen />
    </QueryClientProvider>,
  )
}

describe('ReportesScreen', () => {
  beforeEach(() => {
    backend.reset()
    configurarApi({ apiBaseUrl: 'http://api.test/api/v1', obtenerToken: () => 'token' })
    backend.responder('/reportes/catalogo', 200, sobre(CATALOGO))
    URL.createObjectURL = vi.fn(() => 'blob:reporte')
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('no manda un pedido vacío al asistente', async () => {
    pantalla()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Entender pedido' })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: 'Entender pedido' }))

    expect(screen.getByText('Escribe o dicta qué reporte quieres')).toBeInTheDocument()
    expect(backend.peticiones.some((p) => p.url === '/reportes/interpretar')).toBe(false)
  })

  it('entiende el pedido, muestra lo descartado y descarga el archivo con lo revisado', async () => {
    backend.responder('/reportes/interpretar', 200, sobre(INTERPRETACION))
    backend.responder('/reportes/generar', 200, new Blob(['PK'], { type: 'application/zip' }))
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    pantalla()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Entender pedido' })).toBeEnabled())

    await userEvent.type(screen.getByLabelText('¿Qué reporte necesitas?'), 'ventas de octubre en excel')
    await userEvent.click(screen.getByRole('button', { name: 'Entender pedido' }))

    expect(await screen.findByText('Revisa el reporte')).toBeInTheDocument()
    expect(screen.getByText(/No incluí columna «sueldo»/)).toBeInTheDocument()
    expect(screen.getByLabelText('Título')).toHaveValue('Ventas de octubre')

    await userEvent.click(screen.getByRole('button', { name: 'Descargar Excel' }))

    await waitFor(() => expect(clic).toHaveBeenCalled())
    const pedido = backend.peticiones.find((p) => p.url === '/reportes/generar')!
    expect(JSON.parse(pedido.data as string)).toEqual(INTERPRETACION.especificacion)
    expect(pedido.responseType).toBe('blob')
    expect(await screen.findByText(/Listo:/)).toBeInTheDocument()
  })

  it('muestra el mensaje del backend si el asistente no responde', async () => {
    backend.responder('/reportes/interpretar', 503, {
      status: 'failed',
      data: null,
      message: 'El asistente de reportes no responde ahora.',
      timestamp: 'x',
      error: 'ERR_IA_08',
    })
    pantalla()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Entender pedido' })).toBeEnabled())

    await userEvent.type(screen.getByLabelText('¿Qué reporte necesitas?'), 'ventas')
    await userEvent.click(screen.getByRole('button', { name: 'Entender pedido' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El asistente de reportes no responde ahora.')
  })

  it('se puede armar a mano sin el asistente', async () => {
    pantalla()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Armar a mano' })).toBeEnabled())

    await userEvent.click(screen.getByRole('button', { name: 'Armar a mano' }))

    expect(screen.getByText('Revisa el reporte')).toBeInTheDocument()
    expect(screen.getByLabelText('Título')).toHaveValue('Pagos y horas vendidas')
    expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeInTheDocument()
  })
})
