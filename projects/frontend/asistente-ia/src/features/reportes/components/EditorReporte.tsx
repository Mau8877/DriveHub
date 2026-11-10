import type { EspecificacionReporte } from '../schemas/especificacionSchema'
import type { FuenteCatalogo } from '../types/reportes.types'
import { FechaDh, SelectDh } from './ControlesDh'
import { especificacionInicial } from './especificacionInicial'

interface Props {
  catalogo: FuenteCatalogo[]
  valor: EspecificacionReporte
  errores: Record<string, string>
  onCambio: (especificacion: EspecificacionReporte) => void
}

/**
 * Lo que entendió el asistente, como fichas que la persona puede corregir antes de generar (CU52).
 * Solo ofrece lo que el catálogo permite para la fuente elegida.
 */
export function EditorReporte({ catalogo, valor, errores, onCambio }: Props) {
  const fuente = catalogo.find((f) => f.clave === valor.fuente)
  const cambiar = (parcial: Partial<EspecificacionReporte>) => onCambio({ ...valor, ...parcial })
  const filtrar = (parcial: Partial<EspecificacionReporte['filtros']>) =>
    cambiar({ filtros: { ...valor.filtros, ...parcial } })

  const elegirFuente = (clave: string) => {
    const nueva = catalogo.find((f) => f.clave === clave)
    if (nueva && nueva.clave !== valor.fuente) onCambio({ ...especificacionInicial(nueva), formato: valor.formato })
  }

  const columnasElegidas = valor.columnas.filter((c) => fuente?.columnas.some((col) => col.clave === c))

  return (
    <div className="flex flex-col gap-4">
      <div className="dh-grid-2">
        <div className="dh-field">
          <span className="dh-label">Datos</span>
          <SelectDh
            etiqueta="Datos del reporte"
            sinVacio
            opciones={catalogo.map((f) => ({ valor: f.clave, etiqueta: f.nombre }))}
            valor={valor.fuente}
            onCambio={(v) => elegirFuente(v as string)}
          />
          {fuente && <span className="dh-hint">{fuente.descripcion}</span>}
        </div>
        <div className="dh-field">
          <label className="dh-label" htmlFor="reporte-titulo">
            Título
          </label>
          <input
            id="reporte-titulo"
            className="dh-input"
            maxLength={120}
            value={valor.titulo}
            onChange={(e) => cambiar({ titulo: e.target.value })}
          />
          {errores.titulo && <span className="dh-error">{errores.titulo}</span>}
        </div>
      </div>

      {fuente && (
        <div className="dh-field">
          <span className="dh-label">Columnas</span>
          <SelectDh
            etiqueta="Columnas del reporte"
            multiple
            opciones={fuente.columnas.map((c) => ({ valor: c.clave, etiqueta: c.etiqueta }))}
            valores={columnasElegidas}
            onCambio={(v) => {
              const columnas = v as string[]
              const orden = valor.orden && columnas.includes(valor.orden.columna) ? valor.orden : null
              cambiar({ columnas, orden })
            }}
          />
          {errores.columnas && <span className="dh-error">{errores.columnas}</span>}
        </div>
      )}

      {fuente?.filtraPorFecha && (
        <div className="dh-grid-2">
          <div className="dh-field">
            <span className="dh-label">Desde</span>
            <FechaDh etiqueta="Desde" valor={valor.filtros.desde} onCambio={(v) => filtrar({ desde: v || null })} />
          </div>
          <div className="dh-field">
            <span className="dh-label">Hasta</span>
            <FechaDh
              etiqueta="Hasta"
              valor={valor.filtros.hasta}
              min={valor.filtros.desde ?? undefined}
              invalido={!!errores['filtros.hasta']}
              onCambio={(v) => filtrar({ hasta: v || null })}
            />
            {errores['filtros.hasta'] && <span className="dh-error">{errores['filtros.hasta']}</span>}
          </div>
        </div>
      )}

      {fuente && (fuente.estados.length > 0 || fuente.tipos.length > 0) && (
        <div className="dh-grid-2">
          {fuente.estados.length > 0 && (
            <div className="dh-field">
              <span className="dh-label">Estado</span>
              <SelectDh
                etiqueta="Estado"
                placeholder="Todos"
                opciones={fuente.estados}
                valor={valor.filtros.estado}
                onCambio={(v) => filtrar({ estado: (v as string) || null })}
              />
            </div>
          )}
          {fuente.tipos.length > 0 && (
            <div className="dh-field">
              <span className="dh-label">Tipo</span>
              <SelectDh
                etiqueta="Tipo"
                placeholder="Todos"
                opciones={fuente.tipos}
                valor={valor.filtros.tipo}
                onCambio={(v) => filtrar({ tipo: (v as string) || null })}
              />
            </div>
          )}
        </div>
      )}

      {fuente && (
        <div className="dh-grid-2">
          <div className="dh-field">
            <span className="dh-label">Ordenar por</span>
            <SelectDh
              etiqueta="Ordenar por"
              placeholder="Como vienen"
              opciones={fuente.columnas
                .filter((c) => columnasElegidas.includes(c.clave))
                .map((c) => ({ valor: c.clave, etiqueta: c.etiqueta }))}
              valor={valor.orden?.columna}
              onCambio={(v) =>
                cambiar({ orden: v ? { columna: v as string, descendente: valor.orden?.descendente ?? false } : null })
              }
            />
            {valor.orden && (
              <button
                type="button"
                className="dh-btn dh-btn-ghost dh-btn-sm self-start"
                onClick={() => cambiar({ orden: { ...valor.orden!, descendente: !valor.orden!.descendente } })}
              >
                {valor.orden.descendente ? 'De mayor a menor' : 'De menor a mayor'}
              </button>
            )}
          </div>
          <div className="dh-field">
            <span className="dh-label" id="reporte-formato">
              Formato
            </span>
            <div className="flex gap-2" role="radiogroup" aria-labelledby="reporte-formato">
              {(
                [
                  ['pdf', 'PDF'],
                  ['xlsx', 'Excel'],
                ] as const
              ).map(([formato, etiqueta]) => (
                <button
                  key={formato}
                  type="button"
                  role="radio"
                  aria-checked={valor.formato === formato}
                  className={`dh-btn dh-btn-sm ${valor.formato === formato ? '' : 'dh-btn-ghost'}`}
                  onClick={() => cambiar({ formato })}
                >
                  {etiqueta}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
