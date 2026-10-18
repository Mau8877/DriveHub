import { withNativeFederation, fromPackageJson } from '@angular-architects/native-federation/config'

export default withNativeFederation({
  name: 'pagos',

  exposes: {
    // El shell monta estas rutas con loadChildren bajo /pagos.
    './routes': './src/app/app.routes.ts',
  },

  shared: fromPackageJson({
    singleton: true,
    strictVersion: true,
    requiredVersion: 'auto',
    build: 'package',
  })
    // includeSecondaries is an opt-out of ignoreUnusedDeps, so all of
    // @angular/core is shared to prevent mismatches.
    .patch(['@angular/core'], { includeSecondaries: { keepAll: true } })
    .get(),

  skip: [
    'rxjs/ajax',
    'rxjs/fetch',
    'rxjs/testing',
    'rxjs/webSocket',
    // * El contrato con el shell vive en globalThis: no necesita ser una
    // * instancia compartida (y su versión workspace:* no es semver).
    '@drivehub/mf-contract',
  ],

  // Please read our FAQ about sharing libs:
  // https://shorturl.at/jmzH0

  features: {
    // ignoreUnusedDeps is enabled by default now
    // ignoreUnusedDeps: true,

    // Opt-in: groups chunks in remoteEntry.json for smaller metadata file
    denseChunking: true,
  },
})
