import { css } from 'lit'

/**
 * Estilos de <dh-gestion>. Solo usan las variables de @drivehub/design-tokens (que atraviesan el
 * Shadow DOM): ningún valor de color se define acá.
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

  .pagina {
    padding: 1.5rem;
  }
  .cabecera {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.25rem;
  }
  h1 {
    margin: 0;
    font-size: 1.5rem;
    font-weight: 700;
  }
  h2 {
    margin: 0 0 0.75rem;
    font-size: 1rem;
    font-weight: 600;
  }
  .descripcion {
    margin: 0.25rem 0 0;
    color: var(--muted-foreground);
  }

  .tarjeta {
    background: var(--card);
    color: var(--card-foreground);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: 0 1px 2px rgb(30 41 59 / 6%);
  }

  /* Botones */
  button,
  a.boton {
    font: inherit;
    cursor: pointer;
  }
  .boton {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    border: 1px solid transparent;
    border-radius: calc(var(--radius) * 0.8);
    padding: 0.5rem 1rem;
    font-weight: 600;
    background: var(--primary);
    color: var(--primary-foreground);
  }
  .boton:hover:not(:disabled) {
    filter: brightness(0.95);
  }
  .boton.secundario {
    background: var(--card);
    color: var(--foreground);
    border-color: var(--border);
  }
  .boton.secundario:hover:not(:disabled) {
    background: var(--accent);
  }
  .boton:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .enlace {
    background: none;
    border: 0;
    padding: 0;
    color: var(--info);
    font-weight: 500;
    text-decoration: underline;
    text-underline-offset: 2px;
  }
  .enlace.peligro {
    color: var(--destructive);
  }
  .enlace:disabled {
    opacity: 0.5;
  }
  button:focus-visible,
  input:focus-visible,
  select:focus-visible {
    outline: 2px solid var(--ring);
    outline-offset: 2px;
  }

  /* Aviso */
  .aviso {
    margin-bottom: 1rem;
    padding: 0.625rem 0.875rem;
    border-radius: calc(var(--radius) * 0.8);
    border: 1px solid var(--border);
    background: var(--card);
    border-left: 4px solid var(--success);
  }
  .aviso.error {
    border-left-color: var(--destructive);
    color: var(--destructive);
  }

  /* Filtros */
  .filtros {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    padding: 0.875rem;
    margin-bottom: 1rem;
  }
  input,
  select {
    font: inherit;
    color: var(--foreground);
    background: var(--card);
    border: 1px solid var(--input);
    border-radius: calc(var(--radius) * 0.8);
    padding: 0.5rem 0.75rem;
    min-width: 0;
  }
  input[type='search'] {
    min-width: 16rem;
  }
  input[readonly] {
    background: var(--muted);
    color: var(--muted-foreground);
  }

  /* Tabla */
  .contenedor-tabla {
    overflow-x: auto;
  }
  table {
    width: 100%;
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
    width: 4rem;
    color: var(--muted-foreground);
  }
  thead th:last-child {
    width: 13rem;
  }
  thead th {
    padding: 0.75rem 1rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--muted-foreground);
    border-bottom: 1px solid var(--border);
    background: var(--muted);
  }
  tbody td {
    padding: 0.75rem 1rem;
    border-bottom: 1px solid var(--border);
    vertical-align: middle;
  }
  tbody tr:last-child td {
    border-bottom: 0;
  }
  tbody tr:hover {
    background: var(--accent);
  }
  .secundaria {
    color: var(--muted-foreground);
  }
  .acciones {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.75rem;
  }

  /* Insignias */
  .miniatura {
    display: block;
    width: 4.5rem;
    height: 3rem;
    object-fit: cover;
    border-radius: calc(var(--radius) * 0.6);
    border: 1px solid var(--border);
  }
  .vista-previa {
    display: block;
    width: 100%;
    max-width: 20rem;
    border-radius: var(--radius);
    border: 1px solid var(--border);
  }
  .insignia {
    display: inline-block;
    padding: 0.125rem 0.625rem;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
    background: var(--muted);
    color: var(--muted-foreground);
  }
  .insignia.exito {
    background: color-mix(in srgb, var(--success) 18%, var(--card));
    color: var(--success);
  }
  .insignia.info {
    background: color-mix(in srgb, var(--info) 15%, var(--card));
    color: var(--info);
  }
  .insignia.ambar {
    background: var(--primary);
    color: var(--primary-foreground);
  }

  /* Paginación y estados */
  .pie {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.75rem 1rem;
    border-top: 1px solid var(--border);
    color: var(--muted-foreground);
  }
  .pie .botones {
    display: flex;
    gap: 0.5rem;
  }
  .estado {
    padding: 2rem 1rem;
    text-align: center;
    color: var(--muted-foreground);
  }
  .estado.error {
    color: var(--destructive);
  }

  /* Formulario */
  form {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    max-width: 42rem;
  }
  fieldset {
    margin: 0;
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  legend {
    padding: 0 0.5rem;
    font-weight: 600;
  }
  .campo {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }
  .campo label {
    font-weight: 500;
  }
  .campo .con-boton {
    display: flex;
    gap: 0.5rem;
  }
  .campo .con-boton input {
    flex: 1;
  }
  .ayuda {
    color: var(--muted-foreground);
    font-size: 0.75rem;
  }
  .error-campo {
    color: var(--destructive);
    font-size: 0.75rem;
  }
  .pie-formulario {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }
`
