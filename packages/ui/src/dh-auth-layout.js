import { LitElement, css, html } from 'lit'

import { base, marcaVisual } from './comunes.js'
import { icono } from './iconos.js'

const PUNTOS_POR_DEFECTO = [
  { icono: 'calendar', texto: 'Agenda de clases sin cruces' },
  { icono: 'wallet', texto: 'Planes y cuotas al día' },
  { icono: 'clipboard', texto: 'Simulacros y exámenes' },
]

/**
 * <dh-auth-layout>: pantalla dividida para las páginas sin sesión (inicio de sesión, registro,
 * recuperar contraseña, enlaces del correo): arte de marca a la izquierda y el formulario a la
 * derecha. En pantallas chicas queda solo el formulario.
 *
 * Propiedades: `titulo` y `mensaje` (texto grande del arte), `puntos` ({ icono, texto }[]),
 *              `marca` ({ prefijo, resaltado, ruta }).
 * Slots: (por defecto) el formulario o tarjeta, `arriba` (enlaces arriba a la derecha), `pie`.
 * Partes (::part): arte, panel, tarjeta.
 */
export class DhAuthLayout extends LitElement {
  static properties = {
    titulo: { type: String },
    mensaje: { type: String },
    puntos: { type: Array },
    marca: { type: Object },
  }

  static styles = [
    base,
    css`
      :host {
        display: block;
        min-height: 100svh;
        background: var(--card);
      }
      .auth {
        min-height: 100svh;
        display: grid;
        grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
      }
      .arte {
        position: relative;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        gap: 2rem;
        padding: clamp(1.5rem, 4vw, 3rem);
        padding-bottom: clamp(11rem, 36svh, 19rem);
        color: var(--blanco-puro);
        background: radial-gradient(120% 80% at 0% 0%, color-mix(in srgb, var(--gris-medio) 55%, var(--gris-asfalto)) 0%, var(--gris-asfalto) 60%);
        isolation: isolate;
      }
      .arte::after {
        content: '';
        position: absolute;
        inset: 0;
        z-index: -1;
        opacity: 0.35;
        background-image: radial-gradient(color-mix(in srgb, var(--blanco-puro) 18%, transparent) 1px, transparent 1px);
        background-size: 22px 22px;
        mask-image: linear-gradient(180deg, #000, transparent 70%);
      }
      .marca {
        display: flex;
        align-items: center;
        gap: 0.7rem;
        font-size: 1.25rem;
        font-weight: 800;
        letter-spacing: -0.02em;
      }
      .marca b {
        color: var(--ambar-vial);
      }
      .copia {
        max-width: 30rem;
      }
      h2 {
        margin: 0;
        font-size: clamp(1.8rem, 3.4vw, 2.7rem);
        line-height: 1.08;
        letter-spacing: -0.03em;
        font-weight: 800;
      }
      .copia p {
        margin: 0.9rem 0 1.5rem;
        opacity: 0.75;
        font-size: 1.05rem;
      }
      ul {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 0.7rem;
        font-size: 0.9rem;
      }
      li {
        display: flex;
        align-items: center;
        gap: 0.7rem;
      }
      li .ico {
        color: var(--ambar-vial);
      }
      .escena {
        position: absolute;
        z-index: -1;
        right: -6%;
        bottom: 0;
        width: min(36rem, 88%);
      }
      .calzada {
        fill: color-mix(in srgb, var(--gris-medio) 62%, var(--gris-asfalto));
      }
      .borde {
        stroke: var(--blanco-puro);
        stroke-opacity: 0.35;
        stroke-width: 3;
        fill: none;
      }
      .linea {
        stroke: var(--ambar-vial);
        stroke-width: 4;
        stroke-dasharray: 16 18;
        fill: none;
        animation: ruta 1s linear infinite;
      }
      @keyframes ruta {
        to {
          stroke-dashoffset: -68;
        }
      }
      .auto {
        animation: rebota 2.6s ease-in-out infinite;
      }
      @keyframes rebota {
        50% {
          transform: translate(166px, 193px);
        }
      }
      .carroceria {
        fill: var(--ambar-vial);
      }
      .vidrio,
      .rueda {
        fill: var(--gris-asfalto);
      }
      .rueda {
        stroke: var(--blanco-puro);
        stroke-width: 2;
      }
      .faro {
        fill: var(--blanco-puro);
      }
      .poste {
        fill: var(--gris-medio);
      }
      .senal {
        fill: var(--azul-tecnico);
      }
      .panel {
        display: flex;
        flex-direction: column;
        min-height: 100svh;
        padding: 1.25rem clamp(1.25rem, 4vw, 3rem);
        background: var(--card);
      }
      .arriba {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 1rem;
        min-height: 2.5rem;
        font-size: 0.8125rem;
      }
      .tarjeta {
        width: 100%;
        max-width: 27rem;
        margin: auto;
        padding: 2rem 0;
        animation: entra 0.6s var(--ease, ease);
      }
      @keyframes entra {
        from {
          opacity: 0;
          transform: translateY(10px);
        }
      }
      .pie {
        text-align: center;
        font-size: 0.75rem;
        color: var(--muted-foreground);
      }
      @media (max-width: 900px) {
        .auth {
          grid-template-columns: 1fr;
        }
        .arte {
          display: none;
        }
      }
    `,
  ]

  constructor() {
    super()
    this.titulo = 'Bienvenido de vuelta'
    this.mensaje = 'Gestiona clases, alumnos y pagos de tu autoescuela desde un solo lugar.'
    this.puntos = PUNTOS_POR_DEFECTO
    this.marca = null
  }

  render() {
    return html`
      <div class="auth">
        <section class="arte" part="arte">
          <a class="marca" href=${this.marca?.ruta ?? '/'}>${marcaVisual(this.marca, 36)}</a>
          <div class="copia">
            <h2>${this.titulo}</h2>
            <p>${this.mensaje}</p>
            <ul>
              ${(this.puntos ?? []).map((p) => html`<li>${icono(p.icono, 18)}${p.texto}</li>`)}
            </ul>
          </div>
          <svg class="escena" viewBox="0 0 400 300" aria-hidden="true" preserveAspectRatio="xMidYMax slice">
            <path class="calzada" d="M60 300 L172 40 L228 40 L340 300 Z" />
            <path class="borde" d="M60 300 L172 40 M340 300 L228 40" />
            <path class="linea" d="M200 40 V300" />
            <g class="auto" transform="translate(166 196)">
              <rect class="carroceria" width="68" height="46" rx="12" />
              <rect class="vidrio" x="9" y="7" width="50" height="17" rx="6" />
              <rect class="faro" x="6" y="30" width="12" height="6" rx="3" />
              <rect class="faro" x="50" y="30" width="12" height="6" rx="3" />
              <circle class="rueda" cx="16" cy="44" r="6" />
              <circle class="rueda" cx="52" cy="44" r="6" />
            </g>
            <g transform="translate(300 120)"><rect class="poste" x="-2" y="20" width="4" height="60" /><path class="senal" d="M0 0 L22 14 L0 28 L-22 14 Z" /></g>
            <g transform="translate(96 150)"><rect class="poste" x="-2" y="20" width="4" height="50" /><circle class="senal" r="15" cy="10" /></g>
          </svg>
        </section>
        <section class="panel" part="panel">
          <div class="arriba"><slot name="arriba"></slot></div>
          <div class="tarjeta" part="tarjeta"><slot></slot></div>
          <div class="pie"><slot name="pie">© 2026 DriveHub</slot></div>
        </section>
      </div>
    `
  }
}
