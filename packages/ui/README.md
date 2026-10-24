# @drivehub/ui

Armazón y piezas de interfaz de la web de DriveHub (diseño "Asfalto") como Web
Components con [Lit](https://lit.dev). Funcionan igual en Angular y en React,
heredan los tokens de `@drivehub/design-tokens` y no necesitan build.

```js
import '@drivehub/ui'                  // registra <dh-shell>, <dh-sidebar>, <dh-header>, ...
import '@drivehub/ui/componentes.css'  // clases base: dh-btn, dh-input, dh-card, dh-page...
```

```html
<dh-shell id="app">
  <h1>Contenido de la pantalla</h1>
</dh-shell>
<script type="module">
  const app = document.getElementById('app')
  app.items = [{ etiqueta: 'Inicio', ruta: '/', icono: 'home' }]
  app.usuario = { nombres: 'Ana', apellidos: 'Pérez', rol: 'Administrador' }
  app.addEventListener('dh-navegar', (e) => console.log(e.detail.ruta))
</script>
```

Propiedades, eventos, slots, `::part` y variables CSS están en
[`UI_FRONTEND.md`](../../.claude/rules/frontend/UI_FRONTEND.md) y tipados en
`src/index.d.ts`.

Para correr un remoto React suelto: `montarShellSuelto` de
`@drivehub/ui/desarrollo`.

```bash
pnpm --dir packages/ui test
```
