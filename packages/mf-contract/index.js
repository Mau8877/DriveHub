/**
 * Contrato entre el shell (Angular) y los micro-frontends remotos.
 *
 * * JavaScript plano + index.d.ts (sin build): lo consumen tanto el shell y
 * * los remotos Angular (Native Federation) como los remotos React (Module
 * * Federation para Vite), que no comparten el mismo sistema de federación.
 * * Por eso el contexto vive en globalThis y no en el estado de un módulo:
 * * cada remoto puede terminar con su propia copia de este paquete, pero
 * * todos leen el mismo objeto global que registró el shell.
 * ! Solo el shell llama a registrarContextoShell(). Los remotos nunca hacen
 * ! login por su cuenta: piden el token al shell con obtenerToken().
 */

const CLAVE_CONTEXTO = '__drivehubShell__'
const PREFIJO_EVENTOS = 'drivehub:'

/** Lo llama el shell una sola vez al arrancar. */
export function registrarContextoShell(contexto) {
  globalThis[CLAVE_CONTEXTO] = contexto
}

/** Contexto del shell, o null si el remoto corre suelto (fuera del shell). */
export function obtenerContextoShell() {
  return globalThis[CLAVE_CONTEXTO] ?? null
}

/**
 * Avisa algo a los demás micro-frontends (ej. "se creó un estudiante").
 * Los remotos nunca se llaman entre sí: se comunican solo con eventos.
 */
export function emitirEvento(nombre, detalle) {
  window.dispatchEvent(new CustomEvent(PREFIJO_EVENTOS + nombre, { detail: detalle }))
}

/** Escucha un evento de otro micro-frontend. Devuelve la función para dejar de escuchar. */
export function escucharEvento(nombre, manejador) {
  const listener = (evento) => manejador(evento.detail)
  window.addEventListener(PREFIJO_EVENTOS + nombre, listener)
  return () => window.removeEventListener(PREFIJO_EVENTOS + nombre, listener)
}
