/*
 * Se carga en el <head>, antes de pintar: aplica el tema guardado (claro/oscuro) para que no
 * haya parpadeo. Los colores salen de @drivehub/design-tokens (clase .dark).
 */
;(function () {
  try {
    const guardado = localStorage.getItem('dh-tema')
    const oscuro = guardado ? guardado === 'oscuro' : false
    document.documentElement.classList.toggle('dark', oscuro)
  } catch (_) {
    /* sin almacenamiento: tema claro */
  }
})()
