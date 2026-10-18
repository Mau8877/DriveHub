# Packages

Paquetes compartidos por los micro-frontends web (workspace pnpm, se dependen con `workspace:*`):

- [`design-tokens/`](design-tokens/) — variables CSS y tema de Tailwind 4: la única fuente de colores, radios y tipografía de la web.
- [`mf-contract/`](mf-contract/) — contrato entre el shell y los remotos: contexto de sesión, `mount`/`unmount` de remotos React y bus de eventos del navegador.

Un paquete nuevo se crea recién cuando dos apps necesitan lo mismo de verdad.
