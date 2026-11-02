import { Injectable } from '@angular/core'

/** Valores públicos de la app web de Firebase (public/firebase.config.json). */
export interface ConfigFirebaseWeb {
  apiKey: string
  authDomain: string
  projectId: string
  messagingSenderId: string
  appId: string
  vapidKey: string
}

/**
 * Lo que el shell necesita del SDK de Firebase Web, aislado para poder sustituirlo en los tests.
 *
 * * El SDK se carga con `import()` recién cuando hay sesión y permiso: no pesa en el arranque.
 * * Los avisos que llegan con la pestaña abierta (`onMessage`) también se muestran como
 * * notificación del sistema, igual que los de segundo plano.
 * ! El token nunca se loguea.
 */
@Injectable({ providedIn: 'root' })
export class FirebaseWeb {
  async obtenerToken(config: ConfigFirebaseWeb): Promise<string | null> {
    const [{ getApps, initializeApp }, { getMessaging, getToken, onMessage }] = await Promise.all([
      import('firebase/app'),
      import('firebase/messaging'),
    ])
    const registro = await navigator.serviceWorker.register('/firebase-messaging-sw.js')
    const app =
      getApps()[0] ??
      initializeApp({
        apiKey: config.apiKey,
        authDomain: config.authDomain,
        projectId: config.projectId,
        messagingSenderId: config.messagingSenderId,
        appId: config.appId,
      })
    const mensajeria = getMessaging(app)
    const token = await getToken(mensajeria, {
      vapidKey: config.vapidKey,
      serviceWorkerRegistration: registro,
    })

    onMessage(mensajeria, (carga) => {
      void registro.showNotification(carga.notification?.title ?? 'DriveHub', {
        body: carga.notification?.body ?? '',
        icon: '/favicon.ico',
        data: carga.data ?? {},
      })
    })
    return token || null
  }
}
