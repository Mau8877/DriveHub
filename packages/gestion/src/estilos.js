import { css } from 'lit'

/**
 * Estilos de <dh-gestion> (diseño "Asfalto", el mismo de @drivehub/ui y del mockup). Solo usan las
 * variables de @drivehub/design-tokens (que atraviesan el Shadow DOM): ningún color se define acá.
 */
export const estilos = css`
  :host {
    display: block;
    color: var(--foreground);
    font-family: var(--font-sans);
    font-size: 0.875rem;
    line-height: 1.4;
  }
  *,
  *::before,
  *::after {
    box-sizing: border-box;
  }
  button {
    font: inherit;
    color: inherit;
    cursor: pointer;
    background: none;
    border: 0;
    padding: 0;
  }
  .ico {
    flex: none;
  }
  .solo-lectores {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
    white-space: nowrap;
  }
  button:focus-visible,
  input:focus-visible {
    outline: 2px solid var(--ring);
    outline-offset: 2px;
  }

  /* Cabecera de página */
  .pagina {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }
  .cabecera {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
  }
  h1 {
    margin: 0;
    font-size: 1.75rem;
    line-height: 1.15;
    font-weight: 750;
    letter-spacing: -0.025em;
  }
  h2 {
    margin: 0;
    font-size: 1.15rem;
    font-weight: 700;
    letter-spacing: -0.01em;
  }
  p {
    margin: 0;
  }
  .descripcion {
    margin-top: 0.35rem;
    color: var(--muted-foreground);
  }
  .secundaria {
    color: var(--muted-foreground);
  }

  .tarjeta {
    background: var(--card);
    color: var(--card-foreground);
    border: 1px solid var(--border);
    border-radius: calc(var(--radius) * 1.4);
    box-shadow: var(--shadow-1);
  }
  .gestion {
    overflow: hidden;
  }

  /* Botones */
  .boton {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    height: 2.65rem;
    padding: 0 1.15rem;
    white-space: nowrap;
    border: 1px solid transparent;
    border-radius: var(--radius);
    font-weight: 650;
    font-size: 0.9rem;
    background: var(--primary);
    color: var(--primary-foreground);
    box-shadow: 0 6px 16px -6px color-mix(in srgb, var(--ambar-vial) 70%, transparent);
    transition: transform 0.2s var(--ease), filter 0.2s, background-color 0.2s;
  }
  .boton:hover:not(:disabled) {
    filter: brightness(1.06);
    transform: translateY(-1px);
  }
  .boton:active:not(:disabled) {
    transform: scale(0.97);
  }
  .boton.secundario {
    background: transparent;
    color: var(--foreground);
    border-color: var(--border);
    box-shadow: none;
  }
  .boton.secundario:hover:not(:disabled) {
    background: var(--accent);
  }
  .boton.peligro {
    background: var(--destructive);
    color: var(--blanco-puro);
    box-shadow: none;
  }
  .boton:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
  .enlace {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.2rem 0.35rem;
    border-radius: 0.4rem;
    color: var(--info);
    font-weight: 600;
    font-size: 0.8125rem;
    transition: background-color 0.2s;
  }
  .enlace:hover:not(:disabled) {
    background: color-mix(in srgb, var(--azul-tecnico) 10%, transparent);
  }
  .enlace.peligro {
    color: var(--destructive);
  }
  .enlace.peligro:hover:not(:disabled) {
    background: color-mix(in srgb, var(--destructive) 10%, transparent);
  }
  .enlace:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .icono-btn {
    display: inline-grid;
    place-items: center;
    width: 2.5rem;
    height: 2.5rem;
    border-radius: var(--radius);
    transition: background-color 0.2s, transform 0.2s var(--ease);
  }
  .icono-btn:hover {
    background: var(--accent);
  }
  .icono-btn:active {
    transform: scale(0.92);
  }

  /* Barra de búsqueda y filtros */
  .barra {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 1rem 1.15rem;
    border-bottom: 1px solid var(--border);
  }
  .buscador {
    position: relative;
    display: flex;
    align-items: center;
    flex: 1;
    min-width: 14rem;
    max-width: 26rem;
  }
  .buscador .ico {
    position: absolute;
    left: 0.85rem;
    color: var(--muted-foreground);
    pointer-events: none;
  }
  input {
    font: inherit;
    color: var(--foreground);
    background: var(--card);
    border: 1px solid var(--input);
    border-radius: var(--radius);
    min-width: 0;
    transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;
  }
  .buscador input {
    width: 100%;
    height: 2.6rem;
    padding: 0 0.9rem 0 2.6rem;
    background: var(--background);
  }
  /* Lista desplegable del filtro (<dh-select>) */
  .filtro-lista {
    --dh-select-alto: 2.6rem;
    min-width: 11rem;
  }
  input:hover:not([readonly]) {
    border-color: color-mix(in srgb, var(--gris-medio) 55%, transparent);
  }
  input:focus {
    outline: none;
    background: var(--card);
    border-color: var(--ambar-vial);
    box-shadow: 0 0 0 4px color-mix(in srgb, var(--ambar-vial) 22%, transparent);
  }
  input::placeholder {
    color: color-mix(in srgb, var(--gris-medio) 70%, transparent);
  }
  input[readonly] {
    background: var(--muted);
    color: var(--muted-foreground);
  }
  .segmentos {
    display: inline-flex;
    padding: 0.2rem;
    gap: 0.15rem;
    border-radius: var(--radius);
    background: var(--muted);
  }
  .segmentos button {
    height: 2rem;
    padding: 0 0.85rem;
    border-radius: calc(var(--radius) - 0.15rem);
    font-size: 0.8125rem;
    font-weight: 600;
    color: var(--muted-foreground);
    white-space: nowrap;
    transition: background-color 0.2s, color 0.2s, box-shadow 0.2s;
  }
  .segmentos button:hover {
    color: var(--foreground);
  }
  .segmentos button[aria-pressed='true'] {
    background: var(--card);
    color: var(--foreground);
    box-shadow: var(--shadow-1);
  }

  /* Tabla */
  .contenedor-tabla {
    overflow-x: auto;
  }
  table {
    width: 100%;
    min-width: 46rem;
    border-collapse: collapse;
    text-align: left;
    /* Anchos fijos: cada columna mide lo mismo en todas las filas y páginas, así los datos
       quedan siempre bajo su encabezado. */
    table-layout: fixed;
  }
  th,
  td {
    text-align: left;
    overflow-wrap: break-word;
  }
  th.nro,
  td.nro {
    width: 4.25rem;
  }
  td.nro {
    color: var(--muted-foreground);
    font-variant-numeric: tabular-nums;
  }
  thead th:last-child {
    width: 14rem;
  }
  th.derecha,
  td.derecha {
    text-align: right;
  }
  thead th {
    padding: 0.8rem 1.15rem;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--muted-foreground);
    background: color-mix(in srgb, var(--muted) 45%, transparent);
  }
  tbody td {
    padding: 0.85rem 1.15rem;
    border-top: 1px solid var(--border);
    vertical-align: middle;
  }
  tbody tr {
    transition: background-color 0.2s;
    animation: fila 0.45s var(--ease) both;
    animation-delay: calc(var(--i, 0) * 38ms);
  }
  tbody tr:hover {
    background: color-mix(in srgb, var(--accent) 70%, transparent);
  }
  td.secundaria {
    color: var(--muted-foreground);
  }
  td.principal {
    font-weight: 650;
  }
  .principal-celda {
    display: flex;
    align-items: center;
    gap: 0.85rem;
  }
  .principal-celda .nombre {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .icono-fila {
    display: inline-grid;
    place-items: center;
    width: 2.2rem;
    height: 2.2rem;
    border-radius: 0.7rem;
    background: color-mix(in srgb, var(--ambar-vial) 18%, transparent);
    flex: none;
  }
  code {
    font-family: var(--font-mono, ui-monospace, monospace);
    font-size: 0.8125rem;
  }
  .acciones {
    display: inline-flex;
    gap: 0.25rem;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  /* Insignias */
  .insignia {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    height: 1.55rem;
    padding: 0 0.65rem;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 650;
    background: var(--muted);
    color: var(--muted-foreground);
  }
  .insignia::before {
    content: '';
    width: 0.45rem;
    height: 0.45rem;
    border-radius: 50%;
    background: currentColor;
  }
  .insignia.exito {
    background: color-mix(in srgb, var(--success) 14%, transparent);
    color: var(--success);
  }
  .insignia.info {
    background: color-mix(in srgb, var(--info) 15%, transparent);
    color: var(--info);
  }
  .insignia.ambar {
    background: color-mix(in srgb, var(--ambar-vial) 22%, transparent);
    color: var(--foreground);
  }

  /* Etiquetas pequeñas (columna y dato de tipo etiquetas) */
  .etiquetas {
    display: inline-flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.3rem;
    max-width: 100%;
  }
  .etiqueta {
    display: inline-flex;
    align-items: center;
    height: 1.35rem;
    padding: 0 0.5rem;
    border-radius: 999px;
    font-size: 0.72rem;
    font-weight: 650;
    background: var(--accent);
    color: var(--accent-foreground);
  }
  .etiqueta.mas {
    background: var(--muted);
    color: var(--muted-foreground);
  }

  /* Esqueleto y estado vacío */
  .brillo {
    display: block;
    height: 2.6rem;
    border-radius: 0.7rem;
    background: linear-gradient(100deg, var(--muted) 30%, color-mix(in srgb, var(--muted) 40%, var(--card)) 50%, var(--muted) 70%);
    background-size: 200% 100%;
    animation: brillo 1.2s linear infinite;
  }
  tr.esqueleto td {
    border: 0;
    padding: 0.45rem 1.15rem;
  }
  .vacio {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.4rem;
    padding: 3.5rem 1rem;
    color: var(--muted-foreground);
    text-align: center;
  }
  .vacio b {
    color: var(--foreground);
    font-size: 1rem;
  }
  .estado {
    padding: 2rem 1rem;
    text-align: center;
    color: var(--muted-foreground);
  }
  .estado.error {
    color: var(--destructive);
  }

  /* Paginación */
  .pie {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.9rem 1.15rem;
    border-top: 1px solid var(--border);
    font-size: 0.8125rem;
  }
  .paginas {
    display: flex;
    gap: 0.3rem;
  }
  .pagina-btn {
    min-width: 2.2rem;
    height: 2.2rem;
    padding: 0 0.5rem;
    display: inline-grid;
    place-items: center;
    border-radius: calc(var(--radius) * 0.7);
    font-weight: 600;
    font-size: 0.8125rem;
    color: var(--muted-foreground);
    transition: background-color 0.2s, color 0.2s;
  }
  .pagina-btn:hover:not(:disabled):not([aria-current]) {
    background: var(--accent);
    color: var(--foreground);
  }
  .pagina-btn[aria-current] {
    background: var(--primary);
    color: var(--primary-foreground);
  }
  .pagina-btn:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }

  /* Drawer (crear / editar) */
  .fondo {
    position: fixed;
    inset: 0;
    z-index: 100;
    display: grid;
    grid-template-rows: minmax(0, 1fr);
    place-items: stretch end;
    background: rgb(15 23 42 / 50%);
    backdrop-filter: blur(5px);
    animation: aparece 0.25s;
  }
  .fondo.centrado {
    place-items: center;
    padding: 1rem;
  }
  .drawer {
    width: min(32rem, 100%);
    height: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;
    background: var(--card);
    border-left: 1px solid var(--border);
    box-shadow: var(--shadow-2);
    animation: drawer 0.4s var(--ease);
  }
  .drawer-cabeza {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    padding: 1.4rem 1.5rem 1rem;
    border-bottom: 1px solid var(--border);
  }
  .drawer-cabeza p {
    margin-top: 0.25rem;
    font-size: 0.8125rem;
  }
  .drawer-cuerpo {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 1.25rem 1.5rem 0;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
  fieldset {
    border: 0;
    padding: 0;
    margin: 0 0 0.5rem;
    min-width: 0;
  }
  legend {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    font-weight: 700;
    font-size: 0.9rem;
    margin-bottom: 1rem;
    padding: 0;
  }
  .paso {
    display: inline-grid;
    place-items: center;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 50%;
    background: var(--primary);
    color: var(--primary-foreground);
    font-size: 0.75rem;
    font-weight: 800;
  }
  .drawer-pie {
    position: sticky;
    bottom: 0;
    margin: auto -1.5rem 0;
    padding: 1rem 1.5rem;
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    background: var(--card);
    border-top: 1px solid var(--border);
  }

  /* Detalle de solo lectura */
  .dato {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    margin-bottom: 1rem;
    min-width: 0;
  }
  .dato.foto {
    align-items: flex-start;
  }
  .dato-titulo {
    font-size: 0.8125rem;
    font-weight: 600;
    color: var(--muted-foreground);
  }
  .dato-valor {
    overflow-wrap: anywhere;
  }
  .dato-valor.sin-valor {
    color: var(--muted-foreground);
  }

  /* Campos */
  .campo {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
    margin-bottom: 1rem;
    min-width: 0;
  }
  .campo label {
    font-size: 0.8125rem;
    font-weight: 600;
  }
  .opcional {
    font-weight: 400;
    color: var(--muted-foreground);
  }
  .campo input {
    width: 100%;
    height: 2.75rem;
    padding: 0 0.9rem;
  }
  .campo .etiqueta-campo {
    font-size: 0.8125rem;
    font-weight: 600;
  }
  .campo.con-error input {
    border-color: var(--destructive);
    box-shadow: 0 0 0 4px color-mix(in srgb, var(--destructive) 14%, transparent);
  }
  .con-ojo {
    position: relative;
    display: flex;
    align-items: center;
  }
  .con-ojo input {
    padding-right: 3rem;
  }
  .ojo {
    position: absolute;
    right: 0.35rem;
    width: 2.2rem;
    height: 2.2rem;
    display: grid;
    place-items: center;
    border-radius: calc(var(--radius) * 0.7);
    color: var(--muted-foreground);
  }
  .ojo:hover {
    background: var(--accent);
    color: var(--foreground);
  }
  .grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 1rem;
  }
  .ayuda {
    color: var(--muted-foreground);
    font-size: 0.78rem;
  }
  .error-campo {
    color: var(--destructive);
    font-size: 0.78rem;
    animation: aparece 0.25s var(--ease);
  }
  .aviso-caja {
    display: flex;
    gap: 0.65rem;
    align-items: flex-start;
    padding: 0.75rem 0.9rem;
    border-radius: var(--radius);
    font-size: 0.8125rem;
    border: 1px solid transparent;
  }
  .aviso-caja.error {
    background: color-mix(in srgb, var(--destructive) 10%, transparent);
    border-color: color-mix(in srgb, var(--destructive) 30%, transparent);
    color: var(--destructive);
  }

  /* Diálogo de confirmación */
  .dialogo {
    width: min(26rem, 100%);
    padding: 1.6rem;
    text-align: center;
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: calc(var(--radius) * 1.4);
    box-shadow: var(--shadow-2);
    animation: dialogo 0.3s var(--ease);
  }
  .dialogo-ico {
    display: grid;
    place-items: center;
    width: 3.2rem;
    height: 3.2rem;
    margin: 0 auto 1rem;
    border-radius: 1rem;
    background: color-mix(in srgb, var(--azul-tecnico) 13%, transparent);
    color: var(--info);
  }
  .dialogo-ico.peligro {
    background: color-mix(in srgb, var(--destructive) 13%, transparent);
    color: var(--destructive);
  }
  .dialogo p {
    margin: 0.5rem 0 0;
    color: var(--muted-foreground);
  }
  .dialogo-acciones {
    display: flex;
    gap: 0.75rem;
    justify-content: center;
    margin-top: 1.4rem;
  }

  /* Aviso (toast) */
  .avisos {
    position: fixed;
    z-index: 200;
    right: 1.25rem;
    bottom: 1.25rem;
  }
  .toast {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.8rem 1rem;
    border-radius: var(--radius);
    background: var(--gris-asfalto);
    color: var(--blanco-puro);
    box-shadow: var(--shadow-2);
    font-size: 0.875rem;
    font-weight: 500;
    animation: toast 0.35s var(--ease);
  }
  .toast .ico {
    color: var(--success);
  }
  .toast.error .ico {
    color: #f87171;
  }

  @keyframes fila {
    from {
      opacity: 0;
      transform: translateY(8px);
    }
  }
  @keyframes brillo {
    to {
      background-position: -200% 0;
    }
  }
  @keyframes aparece {
    from {
      opacity: 0;
    }
  }
  @keyframes drawer {
    from {
      transform: translateX(100%);
    }
  }
  @keyframes dialogo {
    from {
      opacity: 0;
      transform: scale(0.94) translateY(8px);
    }
  }
  @keyframes toast {
    from {
      opacity: 0;
      transform: translateY(14px) scale(0.96);
    }
  }

  @media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
      animation: none !important;
      transition: none !important;
    }
  }

  /* Móvil: la tabla pasa a tarjetas y el drawer sube desde abajo */
  @media (max-width: 720px) {
    .pagina {
      padding: 1rem;
    }
    .barra .segmentos {
      width: 100%;
    }
    .barra .segmentos button {
      flex: 1;
    }
    .buscador {
      max-width: none;
    }
    table {
      min-width: 0;
      table-layout: auto;
    }
    thead {
      display: none;
    }
    table,
    tbody,
    tr,
    td {
      display: block;
    }
    tbody tr {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.5rem 1rem;
      padding: 0.9rem 1rem;
      border-top: 1px solid var(--border);
    }
    tbody td {
      border: 0;
      padding: 0;
    }
    td[data-label]::before {
      content: attr(data-label);
      display: block;
      font-size: 0.66rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--muted-foreground);
      margin-bottom: 0.15rem;
    }
    td.nro {
      display: none;
    }
    td.principal {
      grid-column: 1 / -1;
    }
    td.principal::before {
      display: none;
    }
    td.derecha {
      grid-column: 1 / -1;
      text-align: left;
    }
    .acciones {
      justify-content: flex-start;
      margin-left: -0.35rem;
    }
    .fondo {
      place-items: end stretch;
    }
    .drawer {
      width: 100%;
      height: 92%;
      border-radius: calc(var(--radius) * 1.4) calc(var(--radius) * 1.4) 0 0;
    }
    .grid-2 {
      grid-template-columns: 1fr;
    }
    .avisos {
      left: 1rem;
      right: 1rem;
    }
  }
`
