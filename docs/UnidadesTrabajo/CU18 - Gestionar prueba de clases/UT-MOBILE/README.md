# Unidades de trabajo — CU18-MOB

**Caso de uso:** Gestionar prueba de clases en la app móvil: desde su agenda, el instructor evalúa una clase ya cerrada con criterios puntuados y consulta después la prueba.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU18 - Gestionar prueba de clases`](../../../CasosDeUso/CU18%20-%20Gestionar%20prueba%20de%20clases.md)
**Recurso HTTP / contrato:** `POST /api/v1/pruebas-clase`, `GET /api/v1/pruebas-clase?filter[claseCodigo]=...`, `GET /api/v1/pruebas-clase/criterios` y los campos `evaluable` y `conPrueba` de la agenda (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de `models/`), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers y nombres), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (validación nativa, mensajes en español, **mismas reglas que el backend**), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit: nada de colores literales ni texto sobre superficies translúcidas), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Tomar como modelo lo que dejó [CU17-MOB](../../CU17%20-%20Gestionar%20clases%20%28asistencia%20y%20cierre%29/UT-MOBILE/README.md) en `lib/features/clases/` (`FormularioClaseSheet`, `gestion_clase_provider`, `validadores_clase`, `mensajes_de_cierre`, `ClasesService`).

## Decisiones de diseño

1. **Todo vive en `lib/features/clases/`**, junto a la agenda. **No se agrega ninguna ruta ni pantalla completa:** evaluar y ver la prueba se hacen en **hojas inferiores** (`showModalBottomSheet`), como cerrar la clase en CU17.
2. **Los criterios vienen del servidor** (`GET /pruebas-clase/criterios`): la app no copia claves, descripciones ni el máximo. Se pide una vez por sesión de la app (provider que se cachea) y la hoja de evaluar espera ese dato.
3. **La app no repite reglas de «cuándo»:** «Evaluar» se ofrece solo si `ClaseAgenda.evaluable` y «Ver prueba» solo si `ClaseAgenda.conPrueba`; los dos los calcula el servidor. Una clase puede tener **los dos** botones (con una o dos pruebas hechas). Sí repite las reglas **de los datos** (todos los criterios puntuados, de 0 a 10 en pasos de 0,5, observaciones hasta 500) para dar feedback inmediato; el backend sigue mandando.
4. **Un criterio sin puntuar es un estado visible.** Cada criterio es una fila con su descripción, un control deslizante de 0 a 10 con 20 divisiones y el valor a la derecha. Arranca **sin puntuar** (valor «—», control atenuado con el color secundario del tema) y se puntúa al tocarlo o moverlo. «Guardar» queda apagado hasta que todos tengan puntaje.
5. **Total en vivo.** Mientras se puntúa, la hoja muestra «Total: 72,5 / 100» y, cuando están todos, «Aprobado» o «Reprobado» (texto + insignia; el color solo refuerza). Es una vista previa con la **misma fórmula** del backend; la que manda es la que vuelve del servidor y esa es la que se muestra al terminar.
6. **El tipo ya hecho no se ofrece.** El selector de tipo (`SegmentedButton` de tres) deshabilita los tipos con prueba, usando las pruebas que trae la lista de la clase. Si el servidor igual responde `ERR_ACA_40`, se trata como en el punto 9.
7. **La prueba es inmutable:** «Ver prueba» es de solo lectura y no tiene «Editar» ni «Borrar».
8. **Resultados con `avisar`, y la agenda se refresca:** evaluar → «Prueba registrada: 80 puntos, aprobado.» o «Prueba registrada: 60 puntos, reprobado.» (con el puntaje y el resultado que devolvió el servidor).
9. **Errores de negocio con su texto** (el `mensaje` del backend, ya en español): `ERR_ACA_35`, `ERR_ACA_39` y `ERR_ACA_40` cierran la hoja, avisan y refrescan la agenda; `ERR_ACA_41` y cualquier otro error de negocio se muestran dentro de la hoja, que sigue abierta con lo que escribió; un error de red también la deja abierta y el botón pasa a «Reintentar». Nunca se da por evaluada una clase sin la respuesta.
10. **Doble toque inocuo y hoja que no se pierde:** mientras la llamada está en curso los botones quedan deshabilitados; la hoja de evaluar no se cierra tocando afuera ni deslizando (para salir está «Volver»), como la de CU17.
11. **Colores solo del tema** (`Theme.of(context).colorScheme`, `context.cockpit`). Piezas del Cockpit: `AvisoCockpit`, `Insignia`, `IndicadorBoton`, `avisar`.

## Prerrequisito para la prueba manual

Hace falta (1) una cuenta de instructor (`instructorexample@gmail.com`) con clases ya cerradas con `Presente` o `Tarde` (CU17) y otra cerrada con `Ausente`, y (2) el backend de [`UT-BACKEND`](../UT-BACKEND/README.md) (todas las UT) con `academico`, `pagos` e `identidad` levantados.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU18-MOB-01](./UT-CU18-MOB-01.md) | Modelos y `ClasesService`: criterios, evaluar y consultar pruebas | — |
| [UT-CU18-MOB-02](./UT-CU18-MOB-02.md) | Providers de la prueba de clase | 01 |
| [UT-CU18-MOB-03](./UT-CU18-MOB-03.md) | La hoja para evaluar una clase | 02 |
| [UT-CU18-MOB-04](./UT-CU18-MOB-04.md) | La hoja para ver las pruebas de una clase | 02 |
| [UT-CU18-MOB-05](./UT-CU18-MOB-05.md) | «Evaluar» y «Ver prueba» en la agenda | 01, 03, 04 |

**Cadena:** `01 → 02 → 03` y `02 → 04`; la `05` junta las tres.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Elegir «Evaluar», puntuar los criterios del tipo y confirmar | 01, 02, 03, 05 |
| F1 | Ver la prueba de una clase | 01, 02, 04, 05 |
| A1, A2, A3 | Clase no evaluable, tipo repetido o clase ajena | 03 |
| A4 | Datos inválidos: feedback inmediato y error del servidor | 03 |
| A7 | Sin conexión: la hoja queda abierta y se puede reintentar | 03 |
| A8 | Reintento que completa la nota: la app muestra la prueba guardada | 03 |
| CA12 | La agenda ofrece «Evaluar» y «Ver prueba» solo cuando corresponde | 05 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 05 | **Hechas** (2026-11-01): `flutter analyze` sin errores y la suite móvil completa en verde (1101 tests, 61 nuevos) |
| Prueba manual en el teléfono | Pendiente (ver el prerrequisito de arriba; también depende de UT-CU18-BE-09) |

### Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **Dónde quedaron los tests:** todos en `test/features/clases/`, en archivos nuevos: `prueba_clase_models_test.dart`, `prueba_clase_service_test.dart`, `prueba_clase_provider_test.dart` (catálogo, pruebas de la clase y evaluar), `validadores_prueba_test.dart`, `evaluar_clase_sheet_test.dart`, `pruebas_de_clase_sheet_test.dart` y `evaluar_clase_agenda_test.dart` (el recorrido por la pantalla); los de la tarjeta se sumaron a `clase_agenda_card_test.dart`. El doble `ClasesServiceFalso` de `test/helpers/clases_falsos.dart` ganó `catalogoDePruebas`, `pruebasDeClase` y `evaluarClase`, y `catalogoDePruebaDePrueba` y `pruebaClaseDePrueba` (dos criterios por tipo, para tests cortos).
- **La hoja no reemplaza el formulario mientras refresca.** Evaluar invalida las pruebas de la clase y la agenda; si la hoja mostrara el indicador de carga durante ese refresco, se perdería lo puntuado y el mensaje de error del envío. La hoja muestra el formulario apenas hay datos (catálogo y pruebas) y el indicador solo la primera vez. Un test (`ERR_ACA_41` y sin conexión) lo cubre.
- **Las acciones de una clase cerrada van en un `Wrap`:** «Editar», «Ver prueba» y «Evaluar» pueden estar a la vez y se acomodan en dos líneas en pantallas angostas (probado a 320 de ancho).
- **El control de cada criterio es un `Slider`** (0 al máximo, divisiones de medio punto), con el valor al lado y «—» mientras no se puntúa; sin puntuar se ve con el color `outline` del tema.
- **`ResultadoPrueba` desconocido se lee como reprobado:** nunca se muestra como aprobada una prueba que la app no entiende.
- **`flutter test` no corre en esta máquina Windows** (necesita compilar un asset nativo), igual que en CU15 a CU17: las pruebas se corrieron en Linux (imagen `drivehub-flutter:test`, con `flutter pub get` y los tests en el mismo contenedor) sobre una copia de `lib`, `test`, `assets` y los `pubspec`. `flutter analyze` sí corre aquí.

## Follow-ups (no se implementan en estas UT)

- **Editar o borrar** una prueba.
- **Que el estudiante vea** sus pruebas con el detalle por criterio.
- **Avisar** al estudiante de su nota (CU37 y CU38).

## Qué NO incluyen estas UT

- Cambios en el backend (están en [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Cerrar o corregir la clase (CU17) y «Mis notas» (CU23).
- Rutas nuevas, ni pantallas completas.
