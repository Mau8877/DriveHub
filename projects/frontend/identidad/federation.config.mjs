import { withNativeFederation, fromPackageJson } from '@angular-architects/native-federation/config'

export default withNativeFederation({
  name: 'identidad',

  exposes: {
    // El shell monta estas rutas con loadChildren bajo /identidad.
    './routes': './src/app/app.routes.ts',
    // Pantallas sin sesión (registro del estudiante): el shell las monta bajo
    // /register, fuera de su layout.
    './routes-publicas': './src/app/routes-publicas.ts',
    // Páginas públicas de la cuenta (verificar el correo, CU04b): el shell las monta
    // bajo /account, fuera de su layout. Los enlaces de los correos apuntan acá.
    './routes-cuenta': './src/app/routes-cuenta.ts',
    // Perfil de la cuenta propia (CU06): el shell lo monta bajo /perfil, dentro de su layout.
    './routes-perfil': './src/app/routes-perfil.ts',
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
