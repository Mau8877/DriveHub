import { createInstance } from '@module-federation/runtime'
import type { RemotoReact } from '@drivehub/mf-contract'

/**
 * Carga de los remotos React (Module Federation para Vite).
 *
 * * Los remotos Angular se cargan con Native Federation (federation.manifest.json
 * * + loadRemoteModule en app.routes.ts). Los React usan otro formato de
 * * remoteEntry, así que van por el runtime de Module Federation, con su
 * * propio manifiesto: public/remotos-react.manifest.json ({ nombre: url }).
 * * Ninguno de los dos manifiestos requiere recompilar el shell: en cada
 * * entorno se despliega el JSON con las URLs que correspondan.
 * * El shell no comparte React con los remotos (el shell es Angular): cada
 * * remoto trae su propio React dentro de su bundle.
 */
const MANIFIESTO = 'remotos-react.manifest.json'

let instancia: Promise<ReturnType<typeof createInstance>> | null = null

function obtenerInstancia() {
  instancia ??= fetch(MANIFIESTO)
    .then((respuesta) => respuesta.json() as Promise<Record<string, string>>)
    .then((remotos) =>
      createInstance({
        name: 'shell',
        remotes: Object.entries(remotos).map(([name, entry]) => ({ name, entry, type: 'module' })),
      })
    )
  return instancia
}

/** Trae el módulo './mount' de un remoto React (mount/unmount). */
export async function cargarRemotoReact(nombre: string): Promise<RemotoReact> {
  const mf = await obtenerInstancia()
  const modulo = await mf.loadRemote<RemotoReact>(`${nombre}/mount`)
  if (!modulo) throw new Error(`No se pudo cargar el remoto React "${nombre}"`)
  return modulo
}
