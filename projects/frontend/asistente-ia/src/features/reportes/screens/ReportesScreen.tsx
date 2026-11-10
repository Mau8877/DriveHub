import { useState } from 'react'

import { ApiError } from '@/store'

import {
  descargar,
  useCatalogoReportes,
  useGenerarReporte,
  useInterpretarReporte,
} from '../api/useReportes'
import { EditorReporte } from '../components/EditorReporte'
import { especificacionInicial } from '../components/especificacionInicial'
import { useDictado } from '../components/useDictado'
import { especificacionSchema, pedidoSchema, type EspecificacionReporte } from '../schemas/especificacionSchema'

const EJEMPLO = 'Por ejemplo: «Excel con las clases canceladas de octubre por instructor, con fecha y estudiante»'

function mensajeDe(error: unknown, porDefecto: string): string {
  return error instanceof ApiError ? error.message : porDefecto
}

/**
 * Reportes generativos del Administrador (CU52). La persona pide el reporte escrito o dictado, revisa
 * lo que entendió el asistente (fuente, columnas, filtros, formato), lo corrige si hace falta y lo
 * descarga en PDF o Excel. Los números siempre salen de los datos reales: el modelo solo elige del
 * catálogo.
 */
export function ReportesScreen() {
  const catalogo = useCatalogoReportes()
  const interpretar = useInterpretarReporte()
  const generar = useGenerarReporte()

  const [pedido, setPedido] = useState('')
  const [errorPedido, setErrorPedido] = useState<string | null>(null)
  const [especificacion, setEspecificacion] = useState<EspecificacionReporte | null>(null)
  const [descartado, setDescartado] = useState<string[]>([])
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [listo, setListo] = useState<string | null>(null)

  const dictado = useDictado((texto) => setPedido((actual) => (actual ? `${actual} ${texto}` : texto)))

  const entender = () => {
    const validado = pedidoSchema.safeParse(pedido)
    if (!validado.success) {
      setErrorPedido(validado.error.issues[0].message)
      return
    }
    setErrorPedido(null)
    setListo(null)
    interpretar.mutate(validado.data, {
      onSuccess: (resultado) => {
        setEspecificacion(resultado.especificacion)
        setDescartado(resultado.descartado)
        setErrores({})
      },
    })
  }

  const armarAMano = () => {
    const primera = catalogo.data?.[0]
    if (!primera) return
    setEspecificacion(especificacionInicial(primera))
    setDescartado([])
    setErrores({})
    setListo(null)
  }

  const descargarReporte = () => {
    if (!especificacion) return
    const validado = especificacionSchema.safeParse(especificacion)
    if (!validado.success) {
      setErrores(Object.fromEntries(validado.error.issues.map((i) => [i.path.join('.'), i.message])))
      return
    }
    setErrores({})
    setListo(null)
    generar.mutate(validado.data, {
      onSuccess: (archivo) => {
        descargar(archivo)
        setListo(`Listo: ${archivo.filas} registros en ${archivo.nombre}.`)
      },
    })
  }

  return (
    <section className="dh-page">
      <div className="dh-page-head">
        <div>
          <h1>Reportes</h1>
          <p>Pide un reporte con tus palabras, escrito o dictado, y descárgalo en PDF o Excel.</p>
        </div>
      </div>

      <div className="dh-card p-5 flex flex-col gap-3">
        <label className="dh-label" htmlFor="reporte-pedido">
          ¿Qué reporte necesitas?
        </label>
        <textarea
          id="reporte-pedido"
          className="dh-input"
          rows={3}
          maxLength={500}
          placeholder={EJEMPLO}
          value={pedido}
          onChange={(e) => setPedido(e.target.value)}
        />
        {errorPedido && <span className="dh-error">{errorPedido}</span>}
        {dictado.error && <span className="dh-error">{dictado.error}</span>}
        {interpretar.isError && (
          <div className="dh-notice dh-notice-error" role="alert">
            {mensajeDe(interpretar.error, 'No pudimos entender el pedido. Inténtalo de nuevo.')}
          </div>
        )}
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            className="dh-btn"
            onClick={entender}
            disabled={interpretar.isPending || !catalogo.data}
          >
            {interpretar.isPending ? 'Entendiendo…' : 'Entender pedido'}
          </button>
          {dictado.soportado && (
            <button
              type="button"
              className="dh-btn dh-btn-soft"
              aria-pressed={dictado.escuchando}
              onClick={dictado.escuchando ? dictado.detener : dictado.iniciar}
            >
              {dictado.escuchando ? 'Escuchando… (toca para terminar)' : 'Dictar'}
            </button>
          )}
          <button
            type="button"
            className="dh-btn dh-btn-ghost"
            onClick={armarAMano}
            disabled={!catalogo.data}
          >
            Armar a mano
          </button>
        </div>
        {catalogo.isError && (
          <div className="dh-notice dh-notice-error" role="alert">
            {mensajeDe(catalogo.error, 'No pudimos cargar los reportes disponibles.')}
          </div>
        )}
      </div>

      {especificacion && catalogo.data && (
        <div className="dh-card p-5 mt-4 flex flex-col gap-4">
          <div className="dh-card-head">
            <h2>Revisa el reporte</h2>
            <p className="dh-muted">Corrige lo que haga falta antes de descargarlo.</p>
          </div>
          {descartado.length > 0 && (
            <div className="dh-notice dh-notice-warn">
              No incluí {descartado.join(', ')}: no está entre los datos disponibles.
            </div>
          )}
          <EditorReporte
            catalogo={catalogo.data}
            valor={especificacion}
            errores={errores}
            onCambio={setEspecificacion}
          />
          {generar.isError && (
            <div className="dh-notice dh-notice-error" role="alert">
              {mensajeDe(generar.error, 'No pudimos generar el reporte. Inténtalo de nuevo.')}
            </div>
          )}
          {listo && <div className="dh-notice dh-notice-ok">{listo}</div>}
          <div className="dh-form-foot">
            <button type="button" className="dh-btn" onClick={descargarReporte} disabled={generar.isPending}>
              {generar.isPending ? 'Generando…' : `Descargar ${especificacion.formato === 'xlsx' ? 'Excel' : 'PDF'}`}
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
