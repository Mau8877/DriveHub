import type { ColumnDef, RowData, SortingState, Table } from '@tanstack/react-table'

// Extiende `ColumnMeta` de TanStack Table con la metadata que este paquete
// usa para derivar la vista de Cards a partir de las mismas columnas que
// usa la tabla -- una sola fuente de verdad, sin repetir el mapeo campo→UI
// por feature. Vive acá (no en cada feature) porque TypeScript aplica una
// module augmentation de forma global apenas el módulo se importa una vez
// en el árbol, y `components/ui/` es lo que todo el árbol termina
// importando.
declare module '@tanstack/react-table' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- TValue es parte de la firma que el declaration merging con la interfaz original de la librería debe respetar (mismo número de type params), aunque este proyecto no lo use en el cuerpo (TData sí se usa, en `cardImage`/`cardImageAlt`).
  interface ColumnMeta<TData extends RowData, TValue> {
    /** Marca esta columna como el título principal de la card derivada. */
    cardTitle?: boolean
    /** Label mostrado en la card antes del valor (default: `header` de la columna, si es string). */
    cardLabel?: string
    /** Oculta esta columna en DataCards (sigue visible en DataTable). */
    hideInCard?: boolean
    /** Orden explícito del campo dentro de la card (default 0, estable por índice de columna). */
    cardOrder?: number
    /**
     * Solo usada por `DataCards`. Marca esta columna como la fuente de la
     * imagen grande de la card -- arriba del todo, ancho completo, antes
     * del título y los campos (variante "con imagen" del sistema de
     * diseño; sin esto, `DataCards` arma la variante "sin imagen": solo
     * título + campos + acciones). Recibe la fila completa (`row.original`,
     * no el valor de esta columna) para no atar la imagen a un accessor --
     * así una columna que ya combina foto+nombre en su `cell` de
     * `DataTable` (ej. un avatar pegado al nombre en la misma celda) puede
     * seguir haciendo eso en la tabla, mientras `DataCards` usa esta
     * función para la imagen grande y el valor plano de la celda (sin el
     * markup de avatar) como texto del título, evitando una miniatura
     * duplicada dentro de la card. Como mucho una columna por tabla
     * debería definir esto -- si hay más de una, gana la última en el
     * orden de columnas (mismo criterio que `grow`).
     */
    cardImage?: (row: TData) => string
    /** Alt de la imagen de `cardImage` (default: el valor de la celda marcada `cardTitle`, si es string). */
    cardImageAlt?: (row: TData) => string
    /** Alineación de la celda, solo usada por DataTable. */
    align?: 'left' | 'center' | 'right'
    /**
     * Solo usada por `DataTable`. Marca esta columna como la que absorbe el
     * espacio sobrante de la tabla (`width: 100%` de lo que quede) -- el
     * resto de las columnas se ajustan a su contenido (`white-space:
     * nowrap`, sin crecer). Sin esto, `table-layout: auto` reparte el
     * espacio libre de forma arbitraria entre columnas angostas (se ve
     * como columnas "pegadas" entre sí sin razón aparente). Se marca en la
     * columna con contenido más largo/variable (típicamente la que tiene
     * el nombre/título). Como mucho una columna por tabla debería tener
     * `grow: true` -- si hay más de una, gana la última en el orden de
     * columnas.
     */
    grow?: boolean
    /**
     * Solo usada por `DataTable`. Ancho explícito de la columna como clase
     * de Tailwind (ej. `'w-24'`, `'w-40'`) -- se aplica tal cual a la celda
     * de header vía `className`, así que tiene que ser una clase real del
     * sistema de diseño, no un valor arbitrario (ver
     * .claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Cuando se pasa,
     * gana por sobre `grow` para esa columna -- se usa en vez de `grow`
     * para las columnas que necesitan un ancho fijo puntual (ej. una
     * columna de badge/estado angosta), no junto a `grow` en la misma
     * columna.
     */
    width?: string
  }
}

// Espejo de `PageMeta` del backend (ver .claude/rules/backend/RESPONSES_BACKEND.md).
// Se define acá en vez de importar el tipo `meta` de `src/store/apiClient.ts`
// para que este paquete de `components/ui/` no dependa de `store/` -- sigue
// siendo un sistema de diseño transversal, reusable sin acoplarse al cliente
// HTTP concreto de esta app.
export interface DataTablePageMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface UseDataTableOptions<TData> {
  data: TData[]
  columns: ColumnDef<TData, unknown>[]
  /** Metadata de paginación del backend. `undefined` mientras no llegó la primera respuesta. */
  meta: DataTablePageMeta | undefined
  /** Página actual, 1-based (misma convención que el query param `page` del backend). */
  page: number
  pageSize: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  /**
   * El backend soporta orden por un único campo vía el query param `sort`
   * (`sort=campo` ascendente, `sort=-campo` descendente -- ver
   * .claude/rules/backend/ENDPOINTS_BACKEND.md#formato-de-sort). Sigue
   * deshabilitado por default: es opt-in por feature, porque no toda
   * columna es ordenable en cada endpoint (cada uno documenta qué campos
   * acepta) y habilitarlo a ciegas dejaría headers clicables sin efecto si
   * la feature no traduce el estado a ese query param. Cuando se habilita,
   * `useDataTable` fuerza `enableMultiSort: false` -- el backend no soporta
   * multi-columna, así que un click en otra columna reemplaza el orden
   * anterior en vez de acumularlo. Usar `toSortQueryParam` (exportado por
   * este paquete) para convertir el `SortingState` resultante al formato
   * exacto que espera el backend.
   */
  enableSorting?: boolean
  sorting?: SortingState
  onSortingChange?: (sorting: SortingState) => void
  getRowId?: (row: TData, index: number) => string
}

export interface UseDataTableReturn<TData> {
  table: Table<TData>
  pageCount: number
  canPreviousPage: boolean
  canNextPage: boolean
}

// Estado async compartido por DataTable y DataCards, para no acoplar
// ninguno de los dos a `UseQueryResult` de TanStack Query.
export interface DataStateProps {
  isLoading: boolean
  isError: boolean
  errorMessage?: string
}
