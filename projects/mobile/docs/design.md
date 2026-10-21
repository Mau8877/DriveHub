# DriveHub móvil · Guía de diseño (propuesta B «Cálida y amigable»)

App móvil de DriveHub para **Estudiante** e **Instructor**. Flutter con Material 3. Idioma de toda la interfaz: español neutro. El administrador de la escuela usa un panel web aparte y no forma parte de este documento.

Referencia de pantalla: 390 × 844 dp (móvil vertical). Los mockups viven en el lienzo «DriveHub Mockups» (artboards `B.dc.html` claro y `B-Dark.dc.html` oscuro). Ante cualquier duda, el mockup manda sobre este texto, salvo en las secciones marcadas como *derivado*, que son decisiones nuevas para pantallas no dibujadas.

---

## 1. Principios de diseño y tono

### 1.1 Principios

1. **Una acción principal por pantalla, siempre en ámbar.** Es el único botón relleno de ámbar; el resto son contorno, texto o chips. Si una pantalla parece tener dos acciones principales, una de ellas debe bajar a secundaria.
2. **Cálida, clara y redondeada.** Tarjetas grandes con esquinas muy redondeadas, botones tipo píldora, espacio generoso. La interfaz debe sentirse amable para quien está aprendiendo a conducir.
3. **El ámbar es protagonista, pero con medida.** Se usa en: acción principal, pestaña activa de la barra inferior, tarjeta «hero» (versión suave), elemento seleccionado (día, franja, asistencia) y avatar. No se usa para texto pequeño ni para errores.
4. **Nunca depender solo del color.** Todo estado lleva ícono y texto (por ejemplo, Libre = círculo, Reservada = check, Bloqueada = símbolo de prohibido).
5. **Ilustraciones sencillas y puntuales.** Solo un auto de perfil sobre una línea de camino, hecho con formas planas de la paleta. Aparece en: Login, hero del Inicio (Estudiante e Instructor), y estado vacío. No se añaden más ilustraciones sin necesidad.
6. **Estados siempre previstos.** Toda lista o recurso remoto define su estado vacío, de carga y de error, con mensaje en español.
7. **Acciones destructivas o irreversibles piden confirmación** (cancelar reserva, cerrar clase, bloquear franja con reserva).

### 1.2 Tono de los textos

- Español neutro, trato de **tú**, frases cortas, voz activa. Cercano, sin jerga técnica ni informalidad excesiva.
- Los botones son **verbos en infinitivo**: «Reservar este horario», «Pagar cuota 3 · Bs 350», «Cerrar clase».
- Los errores dicen **qué pasó y qué hacer**: «No pudimos cargar tus pagos. Revisa tu conexión e inténtalo de nuevo.»
- Sin culpar al usuario. Mejor «Correo o contraseña incorrectos. Revisa tus datos.» que «Datos inválidos».
- Sin signos de exclamación en errores. Solo se permiten en saludos y logros: «¡Qué bueno verte de nuevo!».
- Formatos: moneda **Bs 350** (con espacio, punto de miles: Bs 1.400); hora **24 h** (`10:00 – 11:00`, con raya en medio); fechas cortas «Lun 26 oct», «28 oct», largas «Miércoles 21 de octubre». Mayúscula solo en la primera palabra de cada texto.

---

## 2. Tokens de diseño

### 2.1 Paleta de marca (obligatoria)

| Nombre | Hex | Uso |
|---|---|---|
| Gris Asfalto | `#1E293B` | Primario, tipografía principal, navegación |
| Ámbar Vial | `#F59E0B` | Acento, acción principal, resaltados, insignias |
| Azul Técnico | `#0284C7` | Estados, indicadores, horas programadas |
| Gris Claro | `#F1F5F9` | Fondo (tema claro) |
| Blanco | `#FFFFFF` | Tarjetas y contenedores (tema claro) |
| Gris Medio | `#475569` | Texto secundario y bordes suaves |

Solo se admiten rojo y verde para error y éxito. Los tonos derivados de la tabla 2.2 (transparencias y variantes más claras u oscuras de estos mismos colores) son los únicos permitidos fuera de la paleta.

### 2.2 Colores semánticos

Los tokens usan el nombre de `ColorScheme` de Material 3 cuando existe un equivalente. Los que no, son extensión propia (`AppSemantic`, ver sección 8).

| Token | Rol | Claro | Oscuro |
|---|---|---|---|
| `background` (scaffold) | Fondo de pantalla | `#F1F5F9` | `#131B2A` |
| `surface` | Tarjetas, campos, NavigationBar, diálogos | `#FFFFFF` | `#1E293B` |
| `onSurface` | Texto principal e íconos | `#1E293B` | `#F1F5F9` |
| `onSurfaceVariant` | Texto secundario | `#475569` | `#A3B1C6` |
| `outline` | Bordes suaves, divisores | `#475569` al 28 % | `#F1F5F9` al 20 % |
| `primary` | Marca neutra: segmentos activos, avatar de contraste | `#1E293B` | `#F1F5F9` |
| `onPrimary` | Texto sobre primary | `#FFFFFF` | `#1E293B` |
| `secondary` | **Acento ámbar**: acción principal, selección, tab activa | `#F59E0B` | `#F59E0B` |
| `onSecondary` | Texto/ícono sobre ámbar | `#1E293B` | `#1E293B` |
| `secondaryContainer` | Ámbar suave: tarjeta hero, chip «Pendiente» | `#F59E0B` al 22 % | `#F59E0B` al 22 % |
| `onSecondaryContainer` | Texto sobre ámbar suave | `#1E293B` | `#F1F5F9` |
| `tertiary` | Azul técnico (relleno, íconos, cifras grandes) | `#0284C7` | `#0284C7` |
| `tertiaryContainer` | Fondo de chip azul («Reservada», «En curso») | `#0284C7` al 12 % | `#38BDF8` al 16 % |
| `onTertiaryContainer` | **Azul de texto pequeño** | `#0369A1` | `#38BDF8` |
| `error` | Errores | `#B91C1C` | `#F87171` |
| `onError` | Texto sobre error relleno | `#FFFFFF` | `#1E293B` |
| `errorContainer` | Fondo de chip de error | `#B91C1C` al 10 % | `#F87171` al 14 % |
| `success` *(ext.)* | Éxito (texto/ícono) | `#15803D` | `#4ADE80` |
| `successContainer` *(ext.)* | Fondo de chip de éxito | `#15803D` al 12 % | `#4ADE80` al 14 % |
| `neutralContainer` *(ext.)* | Chips neutros, pistas de progreso, esqueletos | `#1E293B` al 10 % | `#F1F5F9` al 14 % |
| `segmentActive` *(ext.)* | Fondo del segmento/día/pestaña seleccionado | `#1E293B` | `#F1F5F9` al 20 % |
| `onSegmentActive` *(ext.)* | Texto sobre el segmento activo | `#FFFFFF` | `#FFFFFF` |

**Reglas de uso del color**

- **Azul `#0284C7`**: relleno, íconos y cifras grandes (≥ 20 sp negrita). Para **texto pequeño** usa siempre `onTertiaryContainer` (`#0369A1` / `#38BDF8`), porque `#0284C7` sobre blanco da cerca de 4,1:1.
- **Ámbar**: siempre con texto/ícono asfalto encima (≈ 7:1). Nunca texto ámbar sobre fondo claro.
- **Rojo y verde**: acompañados siempre de ícono y texto.
- Los tonos «al X %» son transparencias del color indicado sobre la superficie en la que se dibujan.

### 2.3 Tipografía

Familia única: **Nunito** (Google Fonts). Pesos usados: 400, 600, 700, 800, 900. Si no se puede empaquetar, el fallback es la fuente del sistema (`Roboto`). Números tabulares recomendados en montos y horas (`FontFeature.tabularFigures()`).

| Estilo | Tamaño / interlineado | Peso | Uso | Slot M3 |
|---|---|---|---|---|
| `display` | 30 / 1.1 | 900 | Hora de la próxima clase, monto grande en Pagos | `displaySmall` |
| `headlineAppBar` | 26 / 1.1 | 900 | Título de sección en AppBar | `headlineMedium` |
| `headlineGreeting` | 22 / 1.15 | 900 | Saludo («Hola, Camila»), nombre en detalle de clase | `headlineSmall` |
| `titleCard` | 18 / 1.25 | 800 | Título de tarjeta («Mañanas entre semana») | `titleLarge` |
| `titleSlot` | 20 / 1.2 | 900 | Hora de una franja (azul de texto) | `titleMedium` |
| `bodyLarge` | 16 / 1.4 | 600 | Texto de campos y cuerpo | `bodyLarge` |
| `bodyMedium` | 14 / 1.4 | 600 | Texto secundario («Marcos Vargas», fechas) | `bodyMedium` |
| `labelButton` | 17 / 1.2 | 800 | Botones principales | `labelLarge` |
| `labelField` | 14 / 1.2 | 800 | Etiqueta sobre campos | `labelMedium` |
| `labelSection` | 13 / 1.2 | 800 | Subtítulos de sección («Hoy · 3 clases») | `labelSmall` |
| `labelChip` | 12.5 / 1.2 | 800 | Texto de chips | `labelSmall` |
| `labelNav` | 12 / 1.2 | 700 (activa 900) | Etiquetas de NavigationBar | `labelMedium` |

Se respeta el escalado de texto del sistema hasta **200 %**. Los contenedores deben crecer en alto (no usar alturas fijas para texto), y a escalas grandes los chips pasan a una segunda línea.

### 2.4 Espaciado

Base de 4 dp. Escala: `4, 6, 8, 10, 12, 16, 20, 24`.

| Token | Valor | Uso |
|---|---|---|
| `space.xs` | 4 | Entre ícono y etiqueta en NavigationBar |
| `space.sm` | 8 | Entre chips, entre días, entre iconos de AppBar |
| `space.md` | 12 | Separación vertical entre tarjetas y bloques |
| `space.lg` | 16 | Margen horizontal de pantalla; padding de contenido |
| `space.xl` | 20 | Padding horizontal de tarjetas; margen del formulario de Login |
| `space.xxl` | 24 | Separación entre secciones grandes |

- Padding de tarjeta: **18 vertical × 20 horizontal**.
- Padding de pantalla: `12 arriba · 16 laterales · 16 abajo`.
- Altura mínima táctil: **48 dp** (botones, campos, chips tocables); iconos de AppBar 44 dp con área de toque ≥ 48 dp.

### 2.5 Radios

| Token | Valor | Uso |
|---|---|---|
| `radius.card` | 28 | Tarjetas, hero, NavigationBar (esquinas superiores) |
| `radius.button` | 28 | Botones (píldora, alto 56) y campos de texto |
| `radius.slot` | 26 | Tarjeta de franja seleccionable |
| `radius.day` | 24 | Selector de día |
| `radius.tile` | 22 | Mosaico de franja del instructor |
| `radius.textarea` | 24 | Campos multilínea |
| `radius.chip` | 14 | Chips de estado (alto 28) |
| `radius.segment` | 28 exterior / 24 interior | Segmentos |
| `radius.full` | 999 | Avatares, burbujas de ícono, botones de ícono, círculos de día |

### 2.6 Elevaciones

El diseño es casi plano: la profundidad viene de sombras suaves, no de bordes.

| Token | Sombra | Uso |
|---|---|---|
| `elevation.card` | `0 6 18` color asfalto al 9 % | Tarjetas, campos, botones de ícono. **Oscuro: sin sombra** (se usa `outline` al 20 %). |
| `elevation.cta` | `0 6 14` ámbar al 35 % | Botón de acción principal |
| `elevation.nav` | `0 -4 14` asfalto al 7 % | NavigationBar |
| `elevation.none` | — | Hero, tarjetas vacías con borde punteado, chips |

En Material 3 se fija `surfaceTintColor: Colors.transparent` para evitar tintes automáticos.

### 2.7 Movimiento

| Token | Duración | Curva | Uso |
|---|---|---|---|
| `motion.press` | 100 ms | `easeOut` | Feedback de presión (overlay) |
| `motion.fast` | 150 ms | `easeOut` | Cambio de selección (día, franja, segmento) |
| `motion.standard` | 250 ms | `easeInOutCubic` | Cambio de pestaña, expansión de tarjetas |
| `motion.enter` | 300 ms | `easeOutCubic` | Entrada de pantalla, diálogos, SnackBar |
| `motion.exit` | 200 ms | `easeIn` | Salida de diálogo/SnackBar |
| `motion.skeleton` | 1200 ms | `linear`, en bucle | Esqueleto de carga |

Respetar «Reducir movimiento» del sistema: sin transiciones de desplazamiento ni esqueleto animado (queda estático).

---

## 3. Componentes

Convención de estados para **todos** los componentes interactivos:

| Estado | Regla general |
|---|---|
| Normal | Estilo base |
| Presionado | Overlay de `onSurface` al 12 % (sobre ámbar: asfalto al 12 %) durante `motion.press`, más escala 0.98 en botones |
| Enfocado (teclado/TalkBack) | Anillo de 2 dp en `onSurface`, separado 2 dp |
| Deshabilitado | Contenido a 38 % de opacidad, sin sombra; **debe haber una razón visible** (texto de ayuda o paso previo) |
| Error | Borde 2 dp `error` + ícono de alerta + mensaje debajo |
| Cargando | Indicador circular de 20 dp en lugar del ícono; el texto cambia a gerundio; el control no responde a toques |

### 3.1 Botones

Altura **56 dp** (mínimo 48), radio píldora, ancho completo por defecto, texto `labelButton`, ícono opcional de 24 dp a la izquierda.

| Variante | Fondo | Texto | Borde | Uso |
|---|---|---|---|---|
| **Primario (ámbar)** | `secondary` | `onSecondary` | — · sombra `elevation.cta` | **Una sola vez por pantalla** |
| **Secundario (contorno)** | `surface` | `onSurface` | 2 dp `onSurface` | Acción complementaria («Evaluar alumno», «Solicitar clase» en estado vacío) |
| **Texto / enlace** | transparente | `onTertiaryContainer` | — | «¿Olvidaste tu contraseña?», «Ver comprobante» (altura táctil 48) |
| **Chip-acción** | `neutralContainer` o `errorContainer` | `onSurface` / `error` | — | «Reprogramar» y «Cancelar» dentro de tarjeta (alto 40, área táctil 48) |

| Estado | Primario | Secundario |
|---|---|---|
| Presionado | Overlay asfalto 12 %, escala 0.98 | Overlay 12 % |
| Deshabilitado | Ámbar al 38 %, texto al 38 %, sin sombra | Borde y texto al 38 % |
| Cargando | Spinner 20 dp asfalto + texto «Guardando…» / «Ingresando…» / «Reservando…» | Spinner + texto |
| Error | No aplica al botón: el error se muestra en el campo o en SnackBar | — |

### 3.2 Campos de formulario

Alto 56 dp (multilínea ≥ 92 dp, radio 24), radio 28, fondo `surface`, sombra `elevation.card`, borde 2 dp transparente. Etiqueta **fija sobre el campo** (`labelField`, 6 dp de separación, sangría 6 dp). Ícono inicial opcional de 24 dp en `onSurfaceVariant`. Texto `bodyLarge`.

| Estado | Apariencia |
|---|---|
| Normal | Borde transparente |
| Enfocado | Borde 2 dp `onSurface` |
| Con valor | Texto `onSurface` |
| Deshabilitado | Contenido al 38 %, sin sombra |
| Error | Borde 2 dp `error`; debajo: ícono de alerta 17 dp + mensaje en `error`, 13.5 sp / 700 |
| Cargando | Spinner 20 dp al final del campo (p. ej. validación remota) |

Tipos específicos:
- **Contraseña**: ícono de candado, botón de mostrar/ocultar como botón de ícono al final.
- **Hora** (inicio/fin): ícono de reloj; al tocar abre selector de hora de 24 h.
- **Fecha** (vigencia): ícono de calendario; al tocar abre selector de fecha.
- **Selector de días**: siete botones circulares de 44 dp (L M X J V S D), alternables; seleccionado = `segmentActive`.
- **Duración**: control segmentado (45 / 60 / 90 min) con check en el elegido.

### 3.3 Tarjetas

| Variante | Fondo | Notas |
|---|---|---|
| **Estándar** | `surface` | Radio 28, padding 18 × 20, sombra `elevation.card` |
| **Hero** | `secondaryContainer` | Radio 28, sin sombra; incluye la ilustración del auto abajo a la derecha (110–132 dp de ancho) |
| **Lista** (fila) | `surface` | Padding 14 × 16 (o 10 × 16 si es compacta), burbuja de ícono 40 dp a la izquierda |
| **Vacía / pausada** | transparente | Borde punteado 2 dp `outline`, sin sombra |
| **Error** | `surface` | Borde 2 dp `error` |
| **Seleccionable** (franja) | `surface` | Ver 3.5 |

Estados: presionada (overlay 12 %), seleccionada (borde 3 dp ámbar), deshabilitada (borde punteado, texto tachado cuando es una hora no disponible), cargando (esqueleto, ver 3.9).

### 3.4 Burbujas de ícono

Círculo de 40 dp con ícono de 24 dp. Variantes: **azul** (`tertiaryContainer` + `onTertiaryContainer`), **ámbar** (`secondaryContainer` + `onSecondaryContainer`), **éxito** (`successContainer` + `success`), **neutra** (`neutralContainer` + `onSurface`).

### 3.5 Franja seleccionable (Estudiante)

Alto 68 dp, radio 26, borde de 3 dp. Izquierda: burbuja de ícono. Centro: hora (`titleSlot`, azul de texto) y debajo el instructor (`bodyMedium`).

| Estado | Apariencia |
|---|---|
| Libre | Fondo `surface`, burbuja azul con reloj, borde transparente |
| Seleccionada | Borde ámbar 3 dp, burbuja ámbar con check, subtítulo «Marcos Vargas · elegida» |
| No disponible | Sin sombra, borde punteado 2 dp, burbuja neutra con candado, hora tachada, subtítulo «No disponible» |
| Presionada | Overlay 12 % |

### 3.6 Chips de estado

Alto 28, radio 14, padding horizontal 12, ícono 15 dp + texto `labelChip`. **Siempre ícono y texto.**

**Franjas** (instructor)

| Estado | Fondo | Texto/ícono | Ícono | Texto | Mosaico en cuadrícula |
|---|---|---|---|---|---|
| **Libre** | `neutralContainer` | `onSurface` | círculo | «Libre» | Mosaico `surface`, borde transparente |
| **Reservada** | `tertiaryContainer` | `onTertiaryContainer` | check | «Reservada» (+ nombre corto: «Camila S.») | Mosaico azul suave |
| **Bloqueada** | `neutralContainer` | `onSurfaceVariant` | prohibido | «Bloqueada» | Mosaico gris suave, sin sombra |

**Pagos**

| Estado | Fondo | Texto/ícono | Ícono | Texto |
|---|---|---|---|---|
| **Pendiente** | `secondaryContainer` | `onSecondaryContainer` | reloj | «Pendiente» |
| **Pagado** | `successContainer` | `success` | check | «Pagado» (+ fecha opcional) |
| **Por vencer** | `neutralContainer` | `onSurface` | reloj | «Por vencer» |
| **Vencido** *(derivado)* | `errorContainer` | `error` | alerta | «Vencido» |

**Clases**

| Estado | Fondo | Ícono | Texto |
|---|---|---|---|
| Confirmada / Completada | `successContainer` | check | «Confirmada» / «Completada» |
| Siguiente / En curso | `tertiaryContainer` | reloj | «Siguiente» / «En curso» |
| Pendiente | `neutralContainer` | reloj | «Pendiente» |
| Cancelada *(derivado)* | `errorContainer` | equis | «Cancelada» |

**Plantillas**: «Activa» (éxito, check) y «Pausada» (neutro, reloj).

Los chips son informativos y no tocables (sin estados de presión). Los chips-acción de 3.1 sí lo son.

### 3.7 NavigationBar (Material 3)

5 destinos, alto **84 dp**, fondo `surface`, esquinas superiores de 28 dp, sombra `elevation.nav`. Indicador tipo píldora de 64 × 34 dp.

| Elemento | Activo | Inactivo |
|---|---|---|
| Indicador | `secondary` (ámbar) | — |
| Ícono | 24 dp, `onSecondary` (asfalto) | 24 dp, `onSurfaceVariant` |
| Etiqueta | `labelNav`, peso 900, `onSurface` | `labelNav`, peso 700, `onSurfaceVariant` |

Etiquetas siempre visibles (`labelBehavior: alwaysShow`). Los destinos de cada rol están en la sección 5. Notificaciones y Asistente IA **no** van aquí.

### 3.8 AppBar

Sin fondo propio (transparente sobre `background`), sin elevación, `scrolledUnderElevation: 0`. Alto mínimo 48 + padding 14 arriba y 6 abajo.

Composición (de izquierda a derecha):

1. **Título de sección** (`headlineAppBar`) o, en el Inicio, **avatar + saludo**. En subpantallas, un botón **Volver** (44 dp, ícono flecha) antes del título (24 sp).
2. **Notificaciones** (campana).
3. **Asistente IA** (destellos).
4. **Cerrar sesión** (puerta con flecha), solo en pantallas principales (no en subpantallas como «Nueva plantilla» o «Gestionar clase»).

Cada acción es un botón de ícono **circular de 44 dp**, fondo `surface`, sombra `elevation.card`, con `tooltip` y `semanticLabel`.

**«Verificar correo»**: botón en contorno (alto 48, radio 24, borde 2 dp `onSurface`, ícono de sobre) **debajo** de la fila del AppBar, alineado a la izquierda. **Solo aparece si el correo no está verificado** y desaparece al verificarlo (con `motion.standard`). Es secundario: nunca ámbar.

Insignia de notificaciones: círculo ámbar de 10 dp con borde `background` de 2 dp sobre la campana cuando hay no leídas; el conteo se anuncia por accesibilidad.

### 3.9 Carga (esqueleto)

Barras redondeadas (radio 7) de 14 dp de alto en `neutralContainer`, con brillo animado según `motion.skeleton`. Un esqueleto de tarjeta repite la estructura real (burbuja + 2 líneas). Mensaje accesible debajo: «Cargando tus pagos…» (varía por pantalla).

### 3.10 SnackBar

Flotante, radio 20, margen 16, fondo `onSurface` (asfalto en claro, blanco hueso en oscuro), texto `bodyMedium` en `surface`, ícono inicial de 20 dp, acción opcional en texto ámbar (en claro: ámbar sobre asfalto, contraste correcto). Duración 4 s (6 s si lleva acción). Entra con `motion.enter`.

| Tipo | Ícono | Ejemplo |
|---|---|---|
| Éxito | check | «Reserva confirmada para mar 27 · 10:00.» |
| Informativo | reloj | «Franja bloqueada.» + acción «Deshacer» |
| Error | alerta | «No pudimos guardar los cambios. Inténtalo de nuevo.» + acción «Reintentar» |

### 3.11 Diálogos

Radio 28, fondo `surface`, padding 24, ancho máximo 320. Título `titleCard`, cuerpo `bodyLarge`, **máximo dos botones**: a la derecha la acción confirmatoria. Scrim al 40 %. Entra con `motion.enter`.

| Diálogo | Título | Cuerpo | Botones |
|---|---|---|---|
| Cancelar reserva | «¿Cancelar esta reserva?» | «Jue 29 oct · 09:00. Podrás reservar otra franja cuando quieras.» | «Mantener» (texto) · «Cancelar reserva» (texto en `error`) |
| Cerrar clase | «¿Cerrar la clase?» | «Al cerrarla ya no podrás editar la asistencia.» | «Volver» (texto) · «Cerrar clase» (ámbar) |
| Cerrar sesión | «¿Cerrar sesión?» | «Tendrás que ingresar de nuevo.» | «Cancelar» · «Cerrar sesión» |
| Bloquear franja con reserva *(derivado)* | «Esta franja tiene una reserva» | «Si la bloqueas, avisaremos a Camila S.» | «Volver» · «Bloquear» |

En diálogos solo el botón de confirmación inofensiva es ámbar; el destructivo va en texto rojo con ícono, no relleno.

### 3.12 Estados vacíos

Estructura centrada: ilustración o ícono grande, título `titleCard`, descripción `bodyMedium` en `onSurfaceVariant`, **como máximo una acción** (botón secundario o primario según si es la acción central de la pantalla).

| Estado | Visual | Título | Descripción | Acción |
|---|---|---|---|---|
| Sin clases (Estudiante) | Auto sobre camino en tarjeta hero | «Aún no tienes clases reservadas» | «Cuando reserves una, la verás aquí.» | «Solicitar clase» (contorno) |
| Sin franjas libres *(derivado)* | Ícono de calendario | «No hay franjas libres este día» | «Prueba con otro día de la semana.» | — |
| Sin pagos *(derivado)* | Ícono de billetera | «Todavía no tienes pagos» | «Tus cuotas y comprobantes aparecerán aquí.» | — |
| Sin plantillas *(derivado)* | Ícono de reloj | «Aún no tienes plantillas de horario» | «Crea una para generar tus franjas.» | «Nueva plantilla» (ámbar) |
| Error de carga | Círculo de alerta rojo suave | «No pudimos cargar tus pagos» | «Revisa tu conexión e inténtalo de nuevo.» | «Reintentar» (ámbar) |

---

## 4. Patrones de pantalla

### 4.1 Lista con paginación

- Contenedor: `ListView` de tarjetas de lista, separación `space.md`.
- Carga inicial: 3–4 esqueletos de tarjeta. Paginación por **scroll infinito**: al llegar al 80 % del final se pide la siguiente página (20 elementos).
- Pie de lista: indicador circular de 24 dp centrado mientras carga la siguiente página; si falla, una fila «No pudimos cargar más.» con botón de texto «Reintentar».
- Fin de lista: texto discreto «Eso es todo por ahora.» (opcional).
- Pull-to-refresh con indicador ámbar.
- Filtros (si existen): control segmentado arriba, fijo; al cambiar de filtro se reinicia la paginación y muestra esqueletos.
- Vacío y error: ver 3.12.

### 4.2 Formulario con validación inline

- Etiquetas siempre visibles sobre cada campo. Campos obligatorios no llevan asterisco; los opcionales dicen «(opcional)» en la etiqueta.
- Validación **al perder el foco** y al enviar, nunca mientras se escribe por primera vez. Una vez que el campo tiene error, se revalida al escribir.
- El mensaje aparece **debajo del campo**, con ícono y texto (no solo color). El primer campo con error recibe el foco y se anuncia por accesibilidad.
- El botón principal está habilitado desde el principio; al enviar con errores no se bloquea, valida y muestra los mensajes. (Se deshabilita solo mientras envía: estado «Guardando…».)
- Error de servidor: SnackBar de error + mensaje en el campo implicado si aplica.
- Al guardar con éxito: SnackBar de éxito y regreso a la pantalla anterior.

**Mensajes de validación (catálogo)**

| Caso | Mensaje |
|---|---|
| Campo vacío | «Completa este campo.» |
| Correo mal formado | «Escribe un correo válido, por ejemplo nombre@correo.com.» |
| Contraseña corta | «Usa al menos 8 caracteres.» |
| Credenciales incorrectas | «Correo o contraseña incorrectos. Revisa tus datos.» |
| Sin días elegidos | «Elige al menos un día.» |
| Hora de fin ≤ inicio | «La hora de fin debe ser posterior al inicio.» |
| Rango no admite la duración | «El horario no alcanza para una clase de 60 min.» |
| Vigencia inválida | «La fecha final debe ser posterior a la inicial.» |
| Plantilla solapada | «Ya tienes una plantilla activa en esos días y horas.» |
| Sin franja elegida | «Elige una franja para reservar.» |
| Sin conexión | «Sin conexión. Revisa tu internet e inténtalo de nuevo.» |

### 4.3 Detalle

Estructura: AppBar con botón Volver y título → tarjeta hero o tarjeta principal con la entidad (avatar/ícono, nombre, estado en chip, hora destacada) → secciones con `labelSection` → campos o filas de datos → botones al pie (secundario arriba, primario ámbar abajo, separación 10). El contenido hace scroll; los botones de acción permanecen **fijos sobre la NavigationBar** si caben, o al final del scroll si no.

### 4.4 Calendario semanal

Se compone de dos piezas:

1. **Selector de semana**: título «Semana 26 – 30 oct» (`titleCard`) y dos botones de ícono (anterior/siguiente, 44 dp). Swipe horizontal también cambia de semana.
2. **Vista del día**: fila de días (`radius.day`, 62 × 78 dp: abreviatura + número) con scroll horizontal si hay más de 5; el seleccionado tiene fondo ámbar. Debajo, las franjas del día:
   - **Estudiante**: lista de franjas seleccionables (3.5).
   - **Instructor**: cuadrícula de 2 columnas de mosaicos de franja (alto 64, radio 22; hora en `titleSlot`, estado con ícono + texto), agrupados por día con subtítulo («Lunes 26»).
   - Encima de la cuadrícula, resumen con tres chips: «9 Libres», «7 Reservadas», «4 Bloqueadas».

Un punto indicador pequeño bajo el día señala que hay reservas (además del texto accesible «tiene 2 clases»). Al elegir un día se anuncia por accesibilidad «Martes 27, 4 franjas».

Para «Mi horario» (Estudiante) y «Consultar mi horario de clases» (Instructor), la misma pieza alterna entre **Lista** (agrupada por día) y **Semana** (la vista de arriba), mediante un control segmentado.

---

## 5. Mapa de navegación

Barra inferior de 5 destinos por rol. Cada destino conserva su propia pila de navegación (estado preservado al cambiar de pestaña). Las pantallas secundarias se apilan encima y ocultan la barra **solo si son de pantalla completa** (por ahora todas la mantienen visible, como en los mockups).

Accesos fijos del AppBar en pantallas principales: **Notificaciones** (CU38, con historial), **Asistente IA** (CU41/CU42: chat, analizar foto del tablero CU40, historial de ayudas) y **Cerrar sesión**.

### 5.1 Estudiante

| # | Destino | Icono | Contenido | Pantalla |
|---|---|---|---|---|
| 1 | **Inicio** | casa | Próxima clase, cuota pendiente, avance, acceso rápido «Solicitar clase» | Pantalla 2 (mockup) |
| 2 | **Clases** | calendario | Pestañas «Reservar» y «Mi horario». Solicitar clase (CU13), reservar franja (CU14), cancelar o reprogramar (CU15), consultar horario en lista o semana (CU16) | Pantalla 3 (mockup) |
| 3 | **Estudio** | libro | Simulacro escrito (CU21): preguntas con temporizador · Manuales (CU34): lista y lector de PDF · Tutoriales (CU35): lista y reproductor | *No dibujada*, ver 6.10 |
| 4 | **Pagos** | billetera | Realizar un pago (CU26), historial de pagos y cuotas con estado y comprobante (CU28) | Pantalla 4 (mockup) |
| 5 | **Perfil** | usuario | Datos personales (CU06), resultados y notas (CU23), preferencias de notificación (CU37) | *No dibujada*, ver 6.11 |

### 5.2 Instructor

| # | Destino | Icono | Contenido | Pantalla |
|---|---|---|---|---|
| 1 | **Inicio** | casa | Clases de hoy, alumnos del día, accesos rápidos | Pantalla 5 (mockup) |
| 2 | **Horarios** (CU12) | reloj | Pestañas «Plantillas» y «Franjas». Lista de plantillas, crear plantilla, ver franjas con su estado | Pantallas 6a, 6b y 7 (mockups) |
| 3 | **Clases** | portapapeles | Consultar mi horario de clases (CU16) · Gestionar una clase: asistencia y cierre (CU17) · Prueba de clases: evaluar al alumno con criterios (CU18) | Pantalla 8 (mockup) y ver 6.12 |
| 4 | **Pagos** (CU29) | billetera | Estado de pagos de mis alumnos | *No dibujada*, ver 6.13 |
| 5 | **Perfil** | usuario | Datos personales (CU06), mis tutoriales (CU33), resultados de mis alumnos (CU23), preferencias de notificación (CU37) | *No dibujada*, ver 6.11 |

> En el mockup, «Gestionar clase» muestra «Clases» como destino activo, y el formulario «Nueva plantilla» mantiene «Horarios» activo.

### 5.3 Flujos principales entre pantallas

- **Inicio (Estudiante)** → botón «Solicitar clase» → pestaña Clases › Reservar.
- **Clases (Estudiante)** → «Reservar este horario» → SnackBar de éxito y la reserva pasa a «Tu reserva activa».
- **Inicio (Instructor)** → «Gestionar clase» → pantalla 8. Tarjetas «Nueva plantilla» → 6b, «Mis franjas» → 7, «Pagos alumnos» → destino Pagos.
- **Horarios › Plantillas** → «Nueva plantilla» → formulario 6b → guardar → vuelve a la lista con SnackBar «Plantilla guardada.».
- **Gestionar clase** → «Evaluar alumno» → Prueba de clases (CU18).
- **Login** → éxito → Inicio del rol correspondiente (el rol lo define el backend).

---

## 6. Especificación pantalla por pantalla

Medidas de pantalla 390 × 844. Todas las pantallas principales: `Scaffold` con fondo `background`, AppBar según 3.8, cuerpo con padding `12 · 16 · 16`, NavigationBar fija abajo (excepto Login). Las 8 pantallas solicitadas siguen la numeración del lienzo (6 se divide en 6a y 6b).

### 6.1 Pantalla 1 · Login

**Objetivo:** ingresar con correo y contraseña. Sin NavigationBar ni AppBar.

**Estructura (de arriba abajo)**
1. Bloque ámbar de 330 dp de alto, esquinas inferiores de 56 dp, con logo (ícono de volante + «DriveHub», 30 sp/900), mensaje «¡Qué bueno verte de nuevo!» (24 sp/800, máx. 230 dp de ancho) e ilustración del auto (blanco, ventanas azules, ruedas asfalto) a la derecha. Texto en asfalto.
2. Formulario: campo **Correo** (ícono de sobre), campo **Contraseña** (ícono de candado), enlace «¿Olvidaste tu contraseña?», y al fondo el botón primario **«Ingresar»**.
3. Texto de ayuda centrado: «¿Necesitas ayuda? Escríbele a tu autoescuela.»

**Estados**

| Estado | Comportamiento |
|---|---|
| Inicial | Campos vacíos, botón habilitado |
| Error de validación | Mensaje bajo el campo: «Completa este campo.» / «Escribe un correo válido…» |
| **Error de credenciales** (mostrado en el mockup) | Campo de contraseña con borde `error` y mensaje «Correo o contraseña incorrectos. Revisa tus datos.» con ícono de alerta |
| Cargando | Botón con spinner y «Ingresando…», campos deshabilitados |
| Sin conexión | SnackBar de error «Sin conexión. Revisa tu internet e inténtalo de nuevo.» |

### 6.2 Pantalla 2 · Inicio del Estudiante

**Objetivo:** resumen del día. **Destino activo:** Inicio.

**Estructura**
1. **AppBar de saludo**: avatar ámbar con inicial, fecha «Miércoles 21 de octubre» (`bodyMedium`) y «Hola, Camila» (22 sp/900); acciones campana, asistente, cerrar sesión. Debajo, botón **«Verificar correo»** (visible porque el correo no está verificado).
2. **Hero «Tu próxima clase»**: «Hoy · 15:00» (30 sp/900), «Práctica en circuito», «con Marcos Vargas», chip **Confirmada**, ilustración del auto.
3. **Tarjeta de cuota**: burbuja ámbar con billetera, «Cuota pendiente», «Bs 350», «Cuota 3 de 4», chip **Vence 28 oct**.
4. **Tarjeta de avance**: «Tu avance» · «8 de 20 clases», barra de progreso ámbar de 12 dp sobre `neutralContainer`.
5. Botón primario **«Solicitar clase»** (ícono +) fijo abajo.

**Estados**

| Estado | Comportamiento |
|---|---|
| Cargando | Esqueletos del hero y de las tarjetas |
| Sin próxima clase | Hero reemplazado por estado vacío: «Aún no tienes clases reservadas» + «Solicitar clase» (contorno); el botón inferior sigue siendo el primario |
| Sin cuotas pendientes | Tarjeta de cuota con chip «Pagado» y texto «Estás al día.» |
| Correo verificado | El botón «Verificar correo» desaparece |
| Error | Estado de error con «Reintentar» |

### 6.3 Pantalla 3 · Clases del Estudiante (reservar horario)

**Objetivo:** pedir una clase y reservar una franja disponible. **Destino activo:** Clases.

**Estructura**
1. AppBar «Clases» con acciones.
2. Control segmentado **«Reservar | Mi horario»** (altura 52).
3. Subtítulo «¿Qué día te viene bien?» y fila de 5 días (62 × 78 dp) con el seleccionado en ámbar (en el mockup, Mar 27).
4. Subtítulo «Franjas libres el martes» y lista de **franjas seleccionables** (3.5): 08:00 – 09:00 (libre), 10:00 – 11:00 (elegida, borde ámbar con check), 11:00 – 12:00 (No disponible).
5. Tarjeta «Tu reserva activa»: «Jue 29 oct · 09:00» con chips-acción **«Reprogramar»** y **«Cancelar»** (CU15).
6. Botón primario **«Reservar este horario»** fijo abajo.

**Estados**

| Estado | Comportamiento |
|---|---|
| Sin franja elegida | Botón primario **deshabilitado** con texto de ayuda sobre él: «Elige una franja para reservar.» |
| Franja elegida | Botón habilitado; al tocar, estado «Reservando…» y SnackBar de éxito |
| Cargando franjas | 3 esqueletos de franja |
| Día sin franjas | Estado vacío «No hay franjas libres este día» |
| Conflicto (franja tomada mientras elegía) | SnackBar de error «Esa franja ya no está disponible. Elige otra.» y la franja pasa a «No disponible» |
| Cancelar | Diálogo de cancelación (3.11) |
| Reprogramar | La reserva activa vuelve a modo selección: el botón cambia a «Reprogramar a esta franja» |
| «Mi horario» | Vista Lista/Semana según 4.4, con chips de estado de clase |

### 6.4 Pantalla 4 · Pagos del Estudiante

**Objetivo:** pagar la cuota pendiente y consultar el historial. **Destino activo:** Pagos.

**Estructura**
1. AppBar «Pagos» con acciones.
2. **Tarjeta resumen**: anillo de progreso (96 dp, trazo 14, ámbar sobre `neutralContainer`) con «2/4», «Saldo pendiente», «Bs 700», «2 cuotas pagadas de 4». Debajo, botón primario **«Pagar cuota 3 · Bs 350»**.
3. Control segmentado de filtros **«Todas | Pendientes | Pagadas»** (altura 46).
4. **Lista de cuotas** (tarjetas de lista con burbuja de ícono):
   - Cuota 3 · «Vence 28 oct» · Bs 350 · chip **Pendiente** (burbuja ámbar, reloj).
   - Cuota 2 · Bs 350 · chip **Pagado** · enlace «Ver comprobante» (burbuja de éxito, check).
   - Cuota 1 · Bs 350 · chip **Pagado** · «Ver comprobante».
   - Cuota 4 (más abajo, con scroll) · «Vence 28 nov» · chip **Por vencer**.

**Estados**

| Estado | Comportamiento |
|---|---|
| Cargando | Esqueleto de resumen + 3 esqueletos de fila |
| Error | Tarjeta de error «No pudimos cargar tus pagos» + «Reintentar» (se muestra en el panel de estados) |
| Sin saldo pendiente | Botón primario oculto; tarjeta dice «Estás al día» con chip «Pagado» |
| Pagando (CU26) | Botón con spinner «Procesando pago…»; al terminar, SnackBar «Pago registrado.» y la cuota pasa a Pagado |
| Pago fallido | SnackBar de error «No pudimos procesar el pago. Inténtalo de nuevo.» |
| Comprobante | Abre el PDF/imagen en pantalla de detalle con botón «Compartir» |
| Filtro sin resultados | Vacío «No hay cuotas en esta categoría» |

### 6.5 Panel de estados (vacío, cargando y error)

Pantalla de referencia con tres bloques que sirven de modelo para todos los estados del sistema:
- **Vacío**: tarjeta hero con ilustración, «Aún no tienes clases reservadas», «Cuando reserves una, la verás aquí.», botón de contorno «Solicitar clase».
- **Cargando**: tarjeta con burbuja gris y dos barras de esqueleto, texto «Cargando tus pagos…».
- **Error**: tarjeta con borde `error`, círculo de alerta, «No pudimos cargar tus pagos», «Revisa tu conexión e inténtalo de nuevo.», botón ámbar «Reintentar».

### 6.6 Pantalla 5 · Inicio del Instructor

**Objetivo:** ver el día y llegar rápido a la gestión. **Destino activo:** Inicio.

**Estructura**
1. AppBar de saludo: avatar ámbar «M», «Buen día, Marcos», fecha, tres acciones. Sin «Verificar correo» (correo verificado).
2. **Hero «Siguiente clase · en 25 min»**: «10:00 – 11:00», «Diego Paz», «Práctica en circuito · Vehículo 04», ilustración.
3. Botón primario **«Gestionar clase»**.
4. Subtítulo «Hoy · 3 clases» y 3 filas: **Lucía Mamani 08:00 · Completada**, **Diego Paz 10:00 · Siguiente**, **Camila Suárez 15:00 · Pendiente**.
5. Fila de 3 **accesos rápidos** (96 dp de alto, radio 24, burbuja ámbar suave): «Nueva plantilla», «Mis franjas», «Pagos alumnos».

**Estados**

| Estado | Comportamiento |
|---|---|
| Sin clases hoy | Hero reemplazado por vacío «Hoy no tienes clases» + «Ver mis franjas» (contorno); botón primario se oculta |
| Cargando / Error | Esqueletos / estado de error con «Reintentar» |
| Clase en curso | El hero muestra chip «En curso» y el botón cambia a «Continuar clase» |

### 6.7 Pantalla 6a · Horarios del Instructor: lista de plantillas

**Objetivo:** ver y administrar plantillas. **Destino activo:** Horarios.

**Estructura**
1. AppBar «Horarios» con acciones.
2. Control segmentado **«Plantillas | Franjas»** (activo: Plantillas).
3. **Tarjetas de plantilla**:
   - «Mañanas entre semana» · chip **Activa** · 7 círculos de día (L M X J V S D; activos con fondo asfalto) · «08:00 – 12:00 · 60 min» (azul de texto) · «Hasta el 31 dic 2026 · 20 franjas por semana».
   - «Sábados» · Activa · día S activo · «09:00 – 13:00 · 60 min» · «Hasta el 30 nov 2026 · 4 franjas por semana».
   - «Tardes de verano» · chip **Pausada** · «Lun, Mié, Vie · 15:00 – 18:00 · 90 min» (tarjeta con borde punteado).
4. Botón primario **«Nueva plantilla»** (ícono +) fijo abajo.

**Estados**

| Estado | Comportamiento |
|---|---|
| Cargando | Esqueletos de tarjeta de plantilla |
| Vacía | «Aún no tienes plantillas de horario» + «Nueva plantilla» (ámbar) |
| Error | Estado de error con «Reintentar» |
| Toque en tarjeta | Abre el detalle/edición de la plantilla (mismo formulario 6b con datos cargados, más acciones «Pausar» y «Eliminar» con diálogo) |

### 6.8 Pantalla 6b · Nueva plantilla (formulario)

**Objetivo:** crear una plantilla que genera franjas. **Destino activo:** Horarios. AppBar con botón Volver y título «Nueva plantilla» (24 sp), sin «Cerrar sesión».

**Estructura (formulario, patrón 4.2)**
1. **Nombre** (texto, p. ej. «Mañanas entre semana»).
2. **¿Qué días atiendes?**: siete botones circulares L M X J V S D (en el mockup, L–V activos).
3. Dos campos de hora: **Desde las** («08:00») y **Hasta las** («12:00»).
4. **Duración de cada clase**: segmentado 45 min / **60 min** (con check) / 90 min.
5. Dos campos de fecha: **Vigente desde** («26 oct 2026») y **Hasta** («31 dic 2026»).
6. **Vista previa** en tarjeta ámbar suave con ícono de reloj: «Se crearán 4 franjas por día, 20 por semana.» (se recalcula en vivo).
7. Botón primario **«Guardar plantilla»** fijo abajo.

**Estados:** validaciones del catálogo 4.2 (sin días, fin ≤ inicio, rango que no admite la duración, vigencia inválida, solapamiento). Guardando: «Guardando…». Éxito: vuelve a 6a con SnackBar «Plantilla guardada.». Salir con cambios sin guardar: diálogo «¿Descartar los cambios?».

### 6.9 Pantalla 7 · Franjas del Instructor y sus estados

**Objetivo:** ver las franjas generadas y bloquearlas. **Destino activo:** Horarios.

**Estructura**
1. AppBar «Horarios» con acciones. Control segmentado con **Franjas** activo.
2. Selector de semana: «Semana 26 – 30 oct» y botones anterior/siguiente.
3. Chips-resumen: **9 Libres**, **7 Reservadas**, **4 Bloqueadas** (cada uno con su ícono).
4. Subtítulo «Lunes 26» y cuadrícula de 2 columnas de **mosaicos**: 08:00 Libre · 09:00 Camila S. (Reservada) · 10:00 Diego P. (Reservada) · 11:00 Bloqueada.
5. Subtítulo «Martes 27» y mosaicos: 08:00 Libre · 09:00 Lucía M. (Reservada) · 10:00 Libre · 11:00 Libre.
6. Botón primario **«Bloquear franja»** fijo abajo.

**Estados y comportamiento**

| Estado | Comportamiento |
|---|---|
| Mosaico libre | Toque → selecciona (borde ámbar 3 dp) para bloquear |
| Mosaico reservado | Toque → detalle de la reserva (alumno, hora, estado) |
| Mosaico bloqueado | Toque → opción «Desbloquear» |
| Ninguna franja elegida | Botón primario **deshabilitado** con ayuda «Elige una franja libre para bloquearla.» |
| Bloquear franja libre | Sin diálogo; SnackBar «Franja bloqueada.» + «Deshacer» |
| Bloquear franja reservada | Diálogo de confirmación (3.11) |
| Cargando / Vacío / Error | Esqueletos de mosaico / «Aún no tienes franjas. Crea una plantilla para generarlas.» / error con «Reintentar» |

### 6.10 Estudio (Estudiante) *(derivado, no dibujada)*

Pestaña con tres secciones como tarjetas grandes en la parte superior (Simulacro escrito, Manuales, Tutoriales; burbuja ámbar suave + título + descripción). Cada una abre una lista con patrón 4.1.
- **Simulacro escrito (CU21)**: pantalla de pregunta con temporizador visible en el AppBar (chip «12:30»), opciones como tarjetas seleccionables (patrón franja), botón primario «Siguiente», último paso «Terminar simulacro»; resultado con anillo de progreso (patrón de Pagos) y resumen.
- **Manuales (CU34)**: lista de tarjetas con ícono de documento; el lector de PDF usa AppBar con Volver y título del manual.
- **Tutoriales (CU35)**: lista de tarjetas con miniatura y duración; reproductor con controles estándar.

### 6.11 Perfil (ambos roles) *(derivado, no dibujada)*

Cabecera con avatar ámbar grande, nombre y correo; tarjeta «Datos personales» (CU06) con filas editables; tarjeta «Resultados y notas» (CU23, Estudiante) o «Resultados de mis alumnos» (Instructor); tarjeta «Mis tutoriales» (CU33, solo Instructor); tarjeta «Preferencias de notificación» (CU37) con interruptores (ámbar cuando están activos, con texto «Activado/Desactivado»). Un solo botón primario «Guardar cambios» aparece solo si hay cambios.

### 6.12 Clases (Instructor): consulta y prueba *(derivado, no dibujada)*

- **Consultar mi horario de clases (CU16)**: patrón 4.4 con segmentado Lista/Semana; cada fila abre la pantalla 8.
- **Prueba de clases (CU18)**: formulario con criterios (lista de filas «criterio + valoración en 5 pasos») y observaciones; botón primario «Guardar evaluación».

### 6.13 Pagos (Instructor) *(derivado, no dibujada)*

Resumen con dos tarjetas («Al día» / «Con deuda») y lista de alumnos con chip de estado de pago (Pagado, Pendiente, Vencido) y monto pendiente; filtro segmentado; sin acciones de cobro. Estado vacío: «Aún no hay pagos de tus alumnos.»

### 6.14 Pantalla 8 · Gestionar una clase (asistencia y cierre)

**Objetivo:** tomar asistencia, observar y cerrar la clase. **Destino activo:** Clases. AppBar con Volver, título «Gestionar clase» (24 sp), campana y asistente.

**Estructura**
1. **Hero** centrado: avatar de 64 dp con iniciales «DP» sobre fondo blanco, «Diego Paz», «Práctica en circuito · Vehículo 04», «10:00 – 11:00» (azul de texto, 20 sp/900) y chip **En curso**.
2. Subtítulo «¿Cómo fue la asistencia?» y tres botones grandes (76 dp de alto, radio 24): **Presente** (seleccionado: fondo ámbar, ícono de check), **Ausente** (ícono de prohibido), **Tardanza** (ícono de reloj).
3. Campo multilínea **Observaciones** (92 dp, radio 24).
4. Botón secundario **«Evaluar alumno»** (→ Prueba de clases, CU18).
5. Botón primario **«Cerrar clase»**.

**Estados**

| Estado | Comportamiento |
|---|---|
| Asistencia sin elegir | «Cerrar clase» deshabilitado con ayuda «Marca la asistencia para cerrar la clase.» |
| Ausente | Se oculta «Evaluar alumno» y la observación se vuelve opcional |
| Cerrar clase | Diálogo de confirmación (3.11) → estado «Cerrando…» → SnackBar «Clase cerrada.» y regreso a Clases |
| Clase ya cerrada | Pantalla en modo lectura: chip «Completada», segmentos y campo deshabilitados, sin botón de cierre |
| Cargando / Error | Esqueleto del hero / error con «Reintentar» |
| Cambios sin guardar al salir | Diálogo «¿Descartar los cambios?» |

---

## 7. Accesibilidad

**Contraste** (valores aproximados; validar con herramienta antes de entregar)

| Combinación | Ratio | Uso permitido |
|---|---|---|
| Asfalto `#1E293B` sobre blanco / gris claro | > 13:1 | Todo el texto |
| Gris Medio `#475569` sobre blanco | ≈ 7,6:1 | Texto secundario |
| Asfalto sobre ámbar `#F59E0B` | ≈ 7:1 | Botón primario, indicador de NavigationBar |
| Azul `#0369A1` sobre blanco | ≈ 5,9:1 | Texto azul pequeño |
| Azul `#0284C7` sobre blanco | ≈ 4,1:1 | **Solo** relleno, íconos o texto ≥ 18 sp negrita |
| Blanco sobre ámbar | < 3:1 | **Prohibido** |
| `#A3B1C6` sobre `#1E293B` (oscuro) | > 5:1 | Texto secundario oscuro |

**Reglas**

1. **Objetivos táctiles ≥ 48 × 48 dp**, con separación mínima de 8 dp entre objetivos adyacentes. Los elementos visualmente menores (iconos de AppBar a 44, chips de acción de 40) amplían su área táctil a 48.
2. **Nunca solo color**: cada estado lleva ícono y texto. Los errores incluyen ícono de alerta y mensaje; los estados de franja y pago, el chip completo.
3. **Texto**: mínimo 12 sp en chips y etiquetas, 14 sp en cuerpo. Soportar escalado hasta 200 % sin truncar mensajes de error ni cortar botones: los contenedores crecen y los chips pasan a dos líneas.
4. **Lectores de pantalla (TalkBack/VoiceOver)**:
   - Todo botón de ícono lleva `tooltip`/`semanticLabel` («Notificaciones», «Asistente IA», «Cerrar sesión», «Volver», «Ver comprobante»).
   - Orden de lectura: título → contenido → acción principal → NavigationBar.
   - Las tarjetas se leen como una unidad: «Cuota 3, vence 28 de octubre, 350 bolivianos, pendiente».
   - Las franjas se anuncian: «10:00 a 11:00, Marcos Vargas, seleccionada» / «11:00 a 12:00, no disponible».
   - Los selectores de día y los segmentos anuncian «seleccionado» y su posición («Martes 27, 2 de 5»).
   - Cambios dinámicos (error de validación, SnackBar, estado de carga) se anuncian con `liveRegion`.
5. **Foco y teclado**: orden lógico, anillo visible de 2 dp, sin trampas de foco en diálogos (el foco regresa al disparador al cerrar).
6. **Formularios**: etiqueta visible (no solo hint), `autofillHints` para correo y contraseña, `textInputAction` correcta, error asociado al campo.
7. **Movimiento**: respetar «Reducir movimiento»; sin parpadeos ni animaciones de más de 5 s sin forma de pausar.
8. **Temas**: el tema sigue el sistema por defecto, con opción manual. Ambos temas cumplen los mismos ratios.
9. **Medios**: videos de Tutoriales con subtítulos cuando existan; PDF de Manuales con texto seleccionable.
10. **Idioma**: declarar `Locale('es')` en la app para que los lectores pronuncien correctamente.

---

## 8. Mapeo a Flutter (`ThemeData`)

Requisitos: Flutter ≥ 3.27 (Material 3 por defecto, `CardThemeData`, `DialogThemeData`, `Color.withValues`), paquete `google_fonts` para Nunito (o fuente empaquetada).

Decisión clave: en `ColorScheme`, **`secondary` es el ámbar**, no `primary`. Por eso los botones rellenos se tematizan explícitamente (no usar el color por defecto de `FilledButton`).

### 8.1 `AppColors` (tokens crudos)

```dart
import 'package:flutter/material.dart';

abstract final class AppColors {
  // Marca
  static const asfalto = Color(0xFF1E293B);
  static const ambar   = Color(0xFFF59E0B);
  static const azul    = Color(0xFF0284C7);
  static const grisClaro = Color(0xFFF1F5F9);
  static const blanco  = Color(0xFFFFFFFF);
  static const grisMedio = Color(0xFF475569);

  // Claro
  static const lightBackground = grisClaro;
  static const lightSurface = blanco;
  static const lightOnSurface = asfalto;
  static const lightOnSurfaceVariant = grisMedio;
  static final lightOutline = grisMedio.withValues(alpha: .28);
  static const lightBlueText = Color(0xFF0369A1);
  static final lightBlueContainer = azul.withValues(alpha: .12);
  static const lightError = Color(0xFFB91C1C);
  static final lightErrorContainer = lightError.withValues(alpha: .10);
  static const lightSuccess = Color(0xFF15803D);
  static final lightSuccessContainer = lightSuccess.withValues(alpha: .12);
  static final lightNeutralContainer = asfalto.withValues(alpha: .10);
  static const lightSegmentActive = asfalto;

  // Oscuro
  static const darkBackground = Color(0xFF131B2A);
  static const darkSurface = asfalto;
  static const darkOnSurface = grisClaro;
  static const darkOnSurfaceVariant = Color(0xFFA3B1C6);
  static final darkOutline = grisClaro.withValues(alpha: .20);
  static const darkBlueText = Color(0xFF38BDF8);
  static final darkBlueContainer = darkBlueText.withValues(alpha: .16);
  static const darkError = Color(0xFFF87171);
  static final darkErrorContainer = darkError.withValues(alpha: .14);
  static const darkSuccess = Color(0xFF4ADE80);
  static final darkSuccessContainer = darkSuccess.withValues(alpha: .14);
  static final darkNeutralContainer = grisClaro.withValues(alpha: .14);
  static final darkSegmentActive = grisClaro.withValues(alpha: .20);

  // Compartidos
  static final amberContainer = ambar.withValues(alpha: .22);
}
```

### 8.2 Tabla de correspondencia token → `ColorScheme` / tema

| Token (sección 2.2) | Destino en Flutter | Claro | Oscuro |
|---|---|---|---|
| `background` | `ThemeData.scaffoldBackgroundColor` | `lightBackground` | `darkBackground` |
| `surface` | `ColorScheme.surface` (y `CardTheme`, `NavigationBarTheme`, `DialogTheme`) | `lightSurface` | `darkSurface` |
| `onSurface` | `ColorScheme.onSurface` | `lightOnSurface` | `darkOnSurface` |
| `onSurfaceVariant` | `ColorScheme.onSurfaceVariant` | `lightOnSurfaceVariant` | `darkOnSurfaceVariant` |
| `outline` | `ColorScheme.outline` / `outlineVariant` | `lightOutline` | `darkOutline` |
| `primary` / `onPrimary` | `ColorScheme.primary` / `onPrimary` | `asfalto` / `blanco` | `grisClaro` / `asfalto` |
| `secondary` / `onSecondary` | `ColorScheme.secondary` / `onSecondary` | `ambar` / `asfalto` | `ambar` / `asfalto` |
| `secondaryContainer` | `ColorScheme.secondaryContainer` / `onSecondaryContainer` | `amberContainer` / `asfalto` | `amberContainer` / `grisClaro` |
| `tertiary` | `ColorScheme.tertiary` / `onTertiary` | `azul` / `blanco` | `azul` / `blanco` |
| `tertiaryContainer` | `ColorScheme.tertiaryContainer` / `onTertiaryContainer` | `lightBlueContainer` / `lightBlueText` | `darkBlueContainer` / `darkBlueText` |
| `error` / `onError` | `ColorScheme.error` / `onError` | `lightError` / `blanco` | `darkError` / `asfalto` |
| `errorContainer` | `ColorScheme.errorContainer` / `onErrorContainer` | `lightErrorContainer` / `lightError` | `darkErrorContainer` / `darkError` |
| `success`, `successContainer`, `neutralContainer`, `segmentActive`, `onSegmentActive` | `AppSemantic` (ThemeExtension) | ver 8.3 | ver 8.3 |

### 8.3 `AppSemantic` (tokens sin equivalente en `ColorScheme`)

```dart
@immutable
class AppSemantic extends ThemeExtension<AppSemantic> {
  const AppSemantic({
    required this.success, required this.successContainer,
    required this.neutralContainer,
    required this.segmentActive, required this.onSegmentActive,
    required this.blueText, required this.cardShadow,
  });
  final Color success, successContainer, neutralContainer;
  final Color segmentActive, onSegmentActive, blueText;
  final List<BoxShadow> cardShadow;

  static final light = AppSemantic(
    success: AppColors.lightSuccess, successContainer: AppColors.lightSuccessContainer,
    neutralContainer: AppColors.lightNeutralContainer,
    segmentActive: AppColors.lightSegmentActive, onSegmentActive: AppColors.blanco,
    blueText: AppColors.lightBlueText,
    cardShadow: [BoxShadow(color: AppColors.asfalto.withValues(alpha: .09), blurRadius: 18, offset: const Offset(0, 6))],
  );
  static final dark = AppSemantic(
    success: AppColors.darkSuccess, successContainer: AppColors.darkSuccessContainer,
    neutralContainer: AppColors.darkNeutralContainer,
    segmentActive: AppColors.darkSegmentActive, onSegmentActive: AppColors.blanco,
    blueText: AppColors.darkBlueText,
    cardShadow: const [], // en oscuro no hay sombra
  );

  @override
  AppSemantic copyWith({Color? success, Color? successContainer, Color? neutralContainer,
      Color? segmentActive, Color? onSegmentActive, Color? blueText, List<BoxShadow>? cardShadow}) =>
    AppSemantic(
      success: success ?? this.success, successContainer: successContainer ?? this.successContainer,
      neutralContainer: neutralContainer ?? this.neutralContainer,
      segmentActive: segmentActive ?? this.segmentActive, onSegmentActive: onSegmentActive ?? this.onSegmentActive,
      blueText: blueText ?? this.blueText, cardShadow: cardShadow ?? this.cardShadow);

  @override
  AppSemantic lerp(ThemeExtension<AppSemantic>? other, double t) => t < .5 ? this : (other as AppSemantic? ?? this);
}
```

### 8.4 Tokens de forma, espaciado y movimiento

```dart
abstract final class AppSpace { static const xs=4.0, sm=8.0, md=12.0, lg=16.0, xl=20.0, xxl=24.0; }
abstract final class AppRadius {
  static const card = 28.0, button = 28.0, slot = 26.0, day = 24.0,
               tile = 22.0, textarea = 24.0, chip = 14.0;
}
abstract final class AppMotion {
  static const press = Duration(milliseconds: 100), fast = Duration(milliseconds: 150),
               standard = Duration(milliseconds: 250), enter = Duration(milliseconds: 300),
               exit = Duration(milliseconds: 200);
  static const curveEnter = Curves.easeOutCubic, curveStandard = Curves.easeInOutCubic, curveExit = Curves.easeIn;
}
```

### 8.5 `AppTheme`

```dart
abstract final class AppTheme {
  static ThemeData light() => _build(
    Brightness.light,
    ColorScheme(
      brightness: Brightness.light,
      primary: AppColors.asfalto, onPrimary: AppColors.blanco,
      secondary: AppColors.ambar, onSecondary: AppColors.asfalto,
      secondaryContainer: AppColors.amberContainer, onSecondaryContainer: AppColors.asfalto,
      tertiary: AppColors.azul, onTertiary: AppColors.blanco,
      tertiaryContainer: AppColors.lightBlueContainer, onTertiaryContainer: AppColors.lightBlueText,
      error: AppColors.lightError, onError: AppColors.blanco,
      errorContainer: AppColors.lightErrorContainer, onErrorContainer: AppColors.lightError,
      surface: AppColors.lightSurface, onSurface: AppColors.lightOnSurface,
      onSurfaceVariant: AppColors.lightOnSurfaceVariant,
      outline: AppColors.lightOutline, outlineVariant: AppColors.lightOutline,
      surfaceTint: Colors.transparent,
    ),
    AppColors.lightBackground, AppSemantic.light,
  );

  static ThemeData dark() => _build(
    Brightness.dark,
    ColorScheme(
      brightness: Brightness.dark,
      primary: AppColors.grisClaro, onPrimary: AppColors.asfalto,
      secondary: AppColors.ambar, onSecondary: AppColors.asfalto,
      secondaryContainer: AppColors.amberContainer, onSecondaryContainer: AppColors.grisClaro,
      tertiary: AppColors.azul, onTertiary: AppColors.blanco,
      tertiaryContainer: AppColors.darkBlueContainer, onTertiaryContainer: AppColors.darkBlueText,
      error: AppColors.darkError, onError: AppColors.asfalto,
      errorContainer: AppColors.darkErrorContainer, onErrorContainer: AppColors.darkError,
      surface: AppColors.darkSurface, onSurface: AppColors.darkOnSurface,
      onSurfaceVariant: AppColors.darkOnSurfaceVariant,
      outline: AppColors.darkOutline, outlineVariant: AppColors.darkOutline,
      surfaceTint: Colors.transparent,
    ),
    AppColors.darkBackground, AppSemantic.dark,
  );

  static ThemeData _build(Brightness b, ColorScheme cs, Color bg, AppSemantic sem) {
    final text = _textTheme(cs);
    final pill = StadiumBorder();
    return ThemeData(
      useMaterial3: true,
      brightness: b,
      colorScheme: cs,
      scaffoldBackgroundColor: bg,
      textTheme: text,
      extensions: [sem],

      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent, elevation: 0, scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent, foregroundColor: cs.onSurface,
        titleTextStyle: text.headlineMedium, centerTitle: false,
      ),

      navigationBarTheme: NavigationBarThemeData(
        height: 84, backgroundColor: cs.surface, surfaceTintColor: Colors.transparent,
        elevation: 8, shadowColor: AppColors.asfalto.withValues(alpha: .07),
        indicatorColor: cs.secondary, indicatorShape: const StadiumBorder(),
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        iconTheme: WidgetStateProperty.resolveWith((s) => IconThemeData(
          size: 24, color: s.contains(WidgetState.selected) ? cs.onSecondary : cs.onSurfaceVariant)),
        labelTextStyle: WidgetStateProperty.resolveWith((s) => text.labelMedium!.copyWith(
          fontWeight: s.contains(WidgetState.selected) ? FontWeight.w900 : FontWeight.w700,
          color: s.contains(WidgetState.selected) ? cs.onSurface : cs.onSurfaceVariant)),
      ),

      // Acción principal = ámbar
      filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(56), shape: pill,
        backgroundColor: cs.secondary, foregroundColor: cs.onSecondary,
        disabledBackgroundColor: cs.secondary.withValues(alpha: .38),
        disabledForegroundColor: cs.onSecondary.withValues(alpha: .38),
        textStyle: text.labelLarge,
      )),
      outlinedButtonTheme: OutlinedButtonThemeData(style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(56), shape: pill,
        foregroundColor: cs.onSurface, backgroundColor: cs.surface,
        side: BorderSide(color: cs.onSurface, width: 2), textStyle: text.labelLarge,
      )),
      textButtonTheme: TextButtonThemeData(style: TextButton.styleFrom(
        minimumSize: const Size(48, 48), foregroundColor: cs.onTertiaryContainer,
        textStyle: text.labelMedium!.copyWith(fontSize: 15),
      )),

      inputDecorationTheme: InputDecorationTheme(
        filled: true, fillColor: cs.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        border: _border(Colors.transparent), enabledBorder: _border(Colors.transparent),
        focusedBorder: _border(cs.onSurface), errorBorder: _border(cs.error),
        focusedErrorBorder: _border(cs.error),
        errorStyle: text.bodyMedium!.copyWith(color: cs.error, fontSize: 13.5, fontWeight: FontWeight.w700),
        hintStyle: text.bodyLarge!.copyWith(color: cs.onSurfaceVariant),
        labelStyle: text.labelMedium, floatingLabelBehavior: FloatingLabelBehavior.never,
      ),

      cardTheme: CardThemeData(
        color: cs.surface, surfaceTintColor: Colors.transparent, elevation: 0, margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.card)),
      ), // la sombra suave se aplica con sem.cardShadow en un DecoratedBox

      chipTheme: ChipThemeData(
        labelStyle: text.labelSmall, side: BorderSide.none,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.chip)),
        padding: const EdgeInsets.symmetric(horizontal: 12), backgroundColor: sem.neutralContainer,
      ),

      segmentedButtonTheme: SegmentedButtonThemeData(style: SegmentedButton.styleFrom(
        minimumSize: const Size.fromHeight(52), shape: pill, side: BorderSide.none,
        backgroundColor: sem.neutralContainer, foregroundColor: cs.onSurfaceVariant,
        selectedBackgroundColor: sem.segmentActive, selectedForegroundColor: sem.onSegmentActive,
        textStyle: text.labelMedium!.copyWith(fontSize: 15, fontWeight: FontWeight.w800),
      )),

      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating, backgroundColor: cs.onSurface,
        contentTextStyle: text.bodyMedium!.copyWith(color: cs.surface),
        actionTextColor: cs.secondary, insetPadding: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),

      dialogTheme: DialogThemeData(
        backgroundColor: cs.surface, surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.card)),
        titleTextStyle: text.titleLarge, contentTextStyle: text.bodyLarge,
        barrierColor: Colors.black.withValues(alpha: .40),
      ),

      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: cs.secondary, linearTrackColor: sem.neutralContainer, circularTrackColor: sem.neutralContainer),
      dividerTheme: DividerThemeData(color: cs.outline, thickness: 1),
      pageTransitionsTheme: const PageTransitionsTheme(builders: {
        TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      }),
    );
  }

  static OutlineInputBorder _border(Color c) => OutlineInputBorder(
    borderRadius: BorderRadius.circular(AppRadius.button), borderSide: BorderSide(color: c, width: 2));

  static TextTheme _textTheme(ColorScheme cs) {
    final base = GoogleFonts.nunitoTextTheme();
    TextStyle s(double size, FontWeight w, double h, [Color? c]) =>
        GoogleFonts.nunito(fontSize: size, fontWeight: w, height: h, color: c ?? cs.onSurface,
          fontFeatures: const [FontFeature.tabularFigures()]);
    return base.copyWith(
      displaySmall:   s(30, FontWeight.w900, 1.10),
      headlineMedium: s(26, FontWeight.w900, 1.10),
      headlineSmall:  s(22, FontWeight.w900, 1.15),
      titleLarge:     s(18, FontWeight.w800, 1.25),
      titleMedium:    s(20, FontWeight.w900, 1.20),   // hora de franja (colorear con blueText)
      bodyLarge:      s(16, FontWeight.w600, 1.40),
      bodyMedium:     s(14, FontWeight.w600, 1.40, cs.onSurfaceVariant),
      labelLarge:     s(17, FontWeight.w800, 1.20),   // botones
      labelMedium:    s(14, FontWeight.w800, 1.20),   // etiqueta de campo, nav (12 en nav)
      labelSmall:     s(12.5, FontWeight.w800, 1.20), // chips
    );
  }
}
```

### 8.6 Notas de integración

- **Botón de acción principal**: `FilledButton` (ya queda ámbar por el tema). **Prohibido** usar `FilledButton` ámbar más de una vez por pantalla.
- **Chips de estado**: usar un widget propio `StatusChip` que reciba `{icon, label, bg, fg}` tomados de `ColorScheme` y `AppSemantic` según las tablas de 3.6; no usar `Chip` de Material para estados informativos.
- **Sombra de tarjetas, campos y botones de ícono**: `DecoratedBox` con `AppSemantic.cardShadow` (vacía en oscuro). `CardTheme` solo da forma y color.
- **Texto azul pequeño** (horas, enlaces): `AppSemantic.blueText`. Nunca `colorScheme.tertiary` para texto de menos de 18 sp.
- **Nombres de tokens ↔ widgets**: `AppColors.*` es la única fuente de hex; ningún color literal en widgets.
- **Tema**: `MaterialApp(theme: AppTheme.light(), darkTheme: AppTheme.dark(), themeMode: ThemeMode.system, locale: const Locale('es'))`.
- Las ilustraciones del auto se dibujan con `CustomPainter` o SVG con colores tomados del tema (cuerpo ámbar o blanco, ventanas azul técnico, ruedas asfalto).

---

## 9. Decisiones y supuestos

- Se eligió la propuesta **B** con tema claro y oscuro. Todos los tonos adicionales a la paleta son transparencias o variantes de los mismos colores (el azul de texto pequeño y el gris secundario del tema oscuro).
- El **azul de texto pequeño** (`#0369A1`) es una decisión de accesibilidad: el Azul Técnico original no alcanza 4,5:1 sobre blanco.
- Las secciones marcadas como *derivado* (Estudio, Perfil, Clases e Instructor, Pagos del Instructor, estado «Vencido», estado «Cancelada», diálogos adicionales) **no aparecen en los mockups**; siguen los patrones del sistema y deben validarse con diseño antes de implementarse.
- Los datos de los mockups (nombres, montos en Bs, horarios) son ejemplos.
- Los ratios de contraste son aproximados; validar con una herramienta antes del cierre de diseño.
- **Pendientes sin diseñar:** Notificaciones con su historial (CU38) y Asistente IA con chat, foto del tablero e historial (CU40–CU42).