import { withNativeFederation, fromPackageJson } from '@angular-architects/native-federation/config'

export default withNativeFederation({
  name: 'shell',

  //shareScope: autoShareScope({level: 'minor'}),

  shared: fromPackageJson({
    singleton: true,
    strictVersion: true,
    requiredVersion: 'auto',
    build: 'package',
  })
    // includeSecondaries: comparte TODO @angular/core (primitives, rxjs-interop...),
    // igual que los remotos: si un remoto importa un secundario que el shell no
    // comparte, termina cargando su propia copia de Angular.
    .patch(['@angular/core'], { includeSecondaries: { keepAll: true } })
    .get(),

  //sharedMappings: mappingsFromWorkspace({ singleton: true }).filter(['@my-org/*']).get(),

  skip: [
    'rxjs/ajax',
    'rxjs/fetch',
    'rxjs/testing',
    'rxjs/webSocket',
    // * El contrato con los remotos vive en globalThis: no necesita ser una
    // * instancia compartida (y su versión workspace:* no es semver).
    '@drivehub/mf-contract',
    // * Idem @drivehub/gestion (Web Component <dh-gestion>): se incluye en el bundle del remoto que
    // * lo usa; su versión workspace:* tampoco es semver.
    '@drivehub/gestion',
    // * Idem @drivehub/ui (armazón, menú, cabecera): Web Components que se incluyen en cada bundle.
    '@drivehub/ui',
    // Solo lo usa el shell para cargar los remotos React.
    '@module-federation/runtime',
  ],

  // Please read our docs about configuring native federation:
  // https://native-federation.com/docs/v4/angular-adapter/configuration/

  // * Mismas features que los remotos: el formato de remoteEntry.json del
  // * shell y de los remotos tiene que coincidir para que Angular quede como
  // * una sola instancia compartida.
  features: {
    denseChunking: true,
  },
})
