# Packages

Paquetes compartidos por los micro-frontends web (workspace pnpm, se dependen con `workspace:*`):

- [`design-tokens/`](design-tokens/) — variables CSS y tema de Tailwind 4: la única fuente de colores, radios y tipografía de la web (paleta de marca: gris asfalto, ámbar vial, azul técnico).
- [`mf-contract/`](mf-contract/) — contrato entre el shell y los remotos: contexto de sesión, `mount`/`unmount` de remotos React y bus de eventos del navegador.
- [`gestion/`](gestion/) — `<dh-gestion>`, Web Component (Lit) que dibuja cualquier pantalla "Gestionar" (listado, crear, editar, acciones) a partir de un esquema; sirve igual en Angular y React. Ver [GESTION_FRONTEND.md](../.claude/rules/frontend/GESTION_FRONTEND.md).

- [`ui/`](ui/) — armazón y componentes de interfaz como Web Components (Lit): `<dh-shell>`, `<dh-sidebar>`, `<dh-header>`, `<dh-auth-layout>`, `<dh-avatar>`, `<dh-icono>`, más las clases base `dh-*` (botones, campos, tarjetas). Es el diseño "Asfalto" de la web; ver [UI_FRONTEND.md](../.claude/rules/frontend/UI_FRONTEND.md).

Un paquete nuevo se crea recién cuando dos apps necesitan lo mismo de verdad.
