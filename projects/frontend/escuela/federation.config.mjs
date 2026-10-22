import { withNativeFederation, fromPackageJson } from '@angular-architects/native-federation/config'

export default withNativeFederation({
  name: 'escuela',

  exposes: {
    // El shell monta estas rutas con loadChildren bajo /escuela.
    './routes': './src/app/app.routes.ts',
    // Gestión de escuelas del Super admin (CU05): el shell las monta bajo /escuelas, con el rol
    // SUPER_ADMIN. Aparte de './routes' (las del Administrador, bajo /escuela).
    './routes-escuelas': './src/app/routes-escuelas.ts',
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
    // * Idem @drivehub/gestion (Web Component <dh-gestion>): se incluye en el bundle del remoto que
    // * lo usa; su versión workspace:* tampoco es semver.
    '@drivehub/gestion',
    // * Idem @drivehub/ui (armazón, menú, cabecera): Web Components que se incluyen en cada bundle.
    '@drivehub/ui',
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
