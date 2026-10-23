# Mockups de la app mobile

Tres diseños de la app de estudiante e instructor con las mismas pantallas. Son HTML/CSS/JS sin
build: no forman parte de Flutter, del workspace pnpm ni del CI.

> **Diseño oficial: el 3, Cockpit.** Ya está aplicado en toda la app Flutter (`lib/core/theme/` y
> `lib/shared/widgets/`, ver [WIDGETS_UI_MOBILE.md](../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)).
> Los diseños 1 y 2 quedan solo como referencia histórica. El `tema.css` del Cockpit ya trae la
> corrección de legibilidad (tarjetas opacas, resplandores tenues, texto de acento oscuro en claro).

## Cómo verlos

Servir la **raíz del repositorio** (los diseños leen `packages/design-tokens/tokens.css`, la misma
paleta que `AppColors` de Flutter) y abrir `index.html`:

```bash
python -m http.server 8099      # desde la raíz del repo
# http://localhost:8099/projects/mobile/mockup/index.html
```

La galería muestra cada diseño dentro de un teléfono. Arriba se cambia el **rol** (estudiante o
instructor) y el **tema** (claro u oscuro); en cada diseño se elige la pantalla. "Abrir en pantalla
completa" lo abre solo, a tamaño de celular.

## Diseños

| Carpeta | Idea | Navegación | Inicio |
| --- | --- | --- | --- |
| `diseno-1-bento` | Claro, cálido y juguetón: color plano y formas muy redondeadas | Barra inferior con la pestaña activa en una burbuja ámbar | Rejilla de losetas de color sólido |
| `diseno-2-ruta` | Fluido y aireado: cristal, radios grandes, botones redondos | Pestañas flotantes en píldora | Tarjeta en degradado asfalto → azul |
| `diseno-3-cockpit` (**oficial**) | Como el tablero de un auto: resplandores en las esquinas, superficies opacas, acento ámbar | Barra opaca con la pestaña activa en el acento | Ruta dibujada, anillo de progreso y chips de estado |

## Qué incluye cada diseño

Un único `index.html` con navegación por `#/ruta`:

| Ruta | Pantalla | Origen |
| --- | --- | --- |
| `#/login` | Iniciar sesión (`pass` = `incorrecta` o un correo con `bloqueado` muestran los errores) | Existe (`login_screen.dart`) |
| `#/registro` | Crear cuenta de estudiante (código de escuela `autoescuela-example`) | Existe (`registro_screen.dart`) |
| `#/recuperar` | Recuperar contraseña | Existe (`recuperar_screen.dart`) |
| `#/estado/registrado\|verificado\|invalido\|correo` | Resultados de los enlaces del correo | Existe (flujos de CU01, CU04, CU06) |
| `#/perfil` | Perfil con foto (hoja Galería/Cámara/Quitar, diálogo), datos, apariencia y cerrar sesión | Existe (`perfil_screen.dart`) |
| `#/perfil/contrasena`, `#/perfil/correo` | Cambiar contraseña y correo | Existe |
| `#/inicio`, `#/clases` | Inicio y clases (estudiante) o agenda del día (instructor) | **Propuesta**: sus casos de uso no están implementados |

Los textos y las reglas de validación (9 a 64 caracteres en la contraseña, 18 años mínimo, correo
válido) son los de la app y el backend.

## Cómo se llevan a Flutter

La mecánica vive en `shared/` (`mobile.css`, `app.js`, `icons.js`); cada diseño solo aporta su
`tema.css`. Lo que cambia en Flutter es el `ThemeData` (formas, barra, navegación) en `lib/core/theme/` y los
widgets compartidos en `lib/shared/widgets/`; las pantallas, los providers y los servicios se
conservan. El inicio y las clases del mockup llevan datos de ejemplo; en la app son un estado vacío
hasta que existan sus casos de uso.
