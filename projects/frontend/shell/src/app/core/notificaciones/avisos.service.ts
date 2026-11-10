import { Injectable, inject, signal } from '@angular/core'

import { GraphqlClient } from '../api/graphql.client'

export type TipoAviso = 'RESERVA' | 'PAGO' | 'EXAMEN' | 'RECORDATORIO'

export interface Aviso {
  codigoNotificacion: string
  tipo: TipoAviso
  titulo: string
  mensaje: string
  leida: boolean
  /** ISO-8601 en UTC. */
  creadaEn: string
}

export interface PaginaAvisos {
  items: Aviso[]
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number }
}

// ! Tienen que ser válidos contra el esquema de `notificaciones` (features/notificaciones/graphql/)
const CONSULTA_AVISOS = `query Avisos($page: Int!, $pageSize: Int!, $leida: Boolean) {
  notificaciones(page: $page, pageSize: $pageSize, leida: $leida) {
    items { codigoNotificacion tipo titulo mensaje leida creadaEn }
    meta { page pageSize totalItems totalPages }
  }
}`
const CONSULTA_CONTEO = `query { conteoNoLeidas }`
const MUTACION_LEER = `mutation Leer($codigo: ID!) { marcarLeida(codigo: $codigo) { codigoNotificacion leida } }`
const MUTACION_LEER_TODAS = `mutation { marcarTodasLeidas }`

export const AVISOS_POR_PAGINA = 20

/**
 * Bandeja de avisos del panel (CU53): la de la escuela para el Administrador; el backend decide cuál por el token.
 *
 * * Va por GraphQL (CU45), como el push del navegador: la misma entrada que ya usa el shell.
 * * `noLeidas` alimenta la campana de la cabecera. Se refresca al navegar y después de marcar avisos; no hay
 * * actualización en vivo.
 */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  private readonly graphql = inject(GraphqlClient)
  private readonly _noLeidas = signal(0)

  readonly noLeidas = this._noLeidas.asReadonly()

  /** Vuelve a pedir el contador. Un fallo no rompe la cabecera: queda el último valor conocido. */
  async refrescarConteo(): Promise<void> {
    try {
      const { conteoNoLeidas } = await this.graphql.ejecutar<{ conteoNoLeidas: number }>(
        CONSULTA_CONTEO
      )
      this._noLeidas.set(conteoNoLeidas)
    } catch {
      // * La campana es secundaria: sin conteo se sigue mostrando la anterior
    }
  }

  async listar(page: number, soloNoLeidas: boolean): Promise<PaginaAvisos> {
    const { notificaciones } = await this.graphql.ejecutar<{ notificaciones: PaginaAvisos }>(
      CONSULTA_AVISOS,
      { page, pageSize: AVISOS_POR_PAGINA, leida: soloNoLeidas ? false : null }
    )
    return notificaciones
  }

  /** Marca un aviso como leído (idempotente) y baja el contador si estaba sin leer. */
  async leer(aviso: Aviso): Promise<void> {
    await this.graphql.ejecutar(MUTACION_LEER, { codigo: aviso.codigoNotificacion })
    if (!aviso.leida) this._noLeidas.update((n) => Math.max(0, n - 1))
  }

  async leerTodas(): Promise<void> {
    await this.graphql.ejecutar(MUTACION_LEER_TODAS)
    this._noLeidas.set(0)
  }
}
