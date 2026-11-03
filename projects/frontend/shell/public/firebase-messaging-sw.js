// Service worker del push web (CU36). Sin el SDK de Firebase: solo muestra el aviso que llega
// con el panel cerrado o en segundo plano y abre el panel al tocarlo.
// * El mensaje de FCM llega como { notification: { title, body }, data: { ... } }.
// * Cualquier dato del aviso viaja en `data` como texto.

self.addEventListener('push', (evento) => {
  let carga = {}
  try {
    carga = evento.data ? evento.data.json() : {}
  } catch {
    // Un push sin JSON válido no se muestra
    return
  }
  const notificacion = carga.notification || {}
  const titulo = notificacion.title || 'DriveHub'
  evento.waitUntil(
    self.registration.showNotification(titulo, {
      body: notificacion.body || '',
      icon: '/favicon.ico',
      data: carga.data || {},
    })
  )
})

self.addEventListener('notificationclick', (evento) => {
  evento.notification.close()
  evento.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((ventanas) => {
      const abierta = ventanas.find((v) => 'focus' in v)
      return abierta ? abierta.focus() : self.clients.openWindow('/')
    })
  )
})
