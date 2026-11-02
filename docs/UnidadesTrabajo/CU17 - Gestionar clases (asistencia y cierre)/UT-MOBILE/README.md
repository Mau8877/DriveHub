# Unidades de trabajo — CU17-MOB

**Caso de uso:** Gestionar clases (asistencia y cierre) en la app móvil: desde su agenda, el instructor cierra una clase que ya empezó (asistencia, tema, observaciones y kilómetros) y corrige esos datos después.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU17 - Gestionar clases (asistencia y cierre)`](../../../CasosDeUso/CU17%20-%20Gestionar%20clases%20%28asistencia%20y%20cierre%29.md)
**Recurso HTTP / contrato:** `POST /api/v1/clases`, `GET /api/v1/clases/{codigo}` y `PATCH /api/v1/clases/{codigo}`, y los campos `cerrable` y `claseCodigo` de la agenda (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de `models/`), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers y nombres), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (`Form` + `validator` nativos, mensajes en español, **mismas reglas que el backend**), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit: nada de colores literales ni texto sobre superficies translúcidas), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Tomar como modelo lo que dejaron [CU15-MOB](../../CU15%20-%20Cancelar%20o%20reprogramar%20clase/UT-MOBILE/README.md) y [CU16-MOB](../../CU16%20-%20Consultar%20horario%20de%20clases/UT-MOBILE/README.md) en `lib/features/clases/`.

## Decisiones de diseño

1. **Todo vive en `lib/features/clases/`**, junto a la agenda de CU16. **No se agrega ninguna ruta ni pantalla completa:** cerrar y editar se hacen en una **hoja inferior** (`showModalBottomSheet`), como la hoja de reprogramar de CU15.
2. **Una sola hoja para cerrar y para editar** (`FormularioClaseSheet`), con dos modos. Al cerrar, la asistencia se elige; al editar, se muestra pero no se cambia (decisión del CU).
3. **La app no repite reglas de «cuándo»:** «Cerrar clase» se ofrece solo si `ClaseAgenda.cerrable` y «Editar» solo si `ClaseAgenda.claseCodigo` no es nulo; los dos los calcula el servidor. Sí repite las reglas **de los datos** (tamaños, rangos y «con `Ausente` solo observaciones»), para dar feedback inmediato; el backend sigue mandando.
4. **Reglas de validación idénticas a las del backend** ([FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md#consistencia-de-reglas-de-negocio-con-el-frontend-web)): tema hasta 120 caracteres, observaciones hasta 500, kilometrajes enteros de 0 a 9 999 999 (teclado numérico y solo dígitos), `kmFin >= kmInicio` si hay los dos. Con `Ausente`, el tema y los kilómetros se **ocultan y se vacían**: solo quedan las observaciones. Si cambia una regla, se cambia en el backend, aquí y en las fichas a la vez.
5. **Aviso fijo con `Ausente`:** al elegirlo, el formulario muestra «El estudiante pierde la hora de esta clase.» (regla de CU44: no hay devolución). Nunca se envía un cierre sin que el instructor lo haya visto.
6. **Editar manda solo lo que cambió** (es un `PATCH`): si no cambió nada, «Guardar» cierra la hoja sin llamar al servidor. Borrar un texto lo envía en blanco (el backend lo borra); un kilometraje no se puede borrar: si el campo queda vacío y había un valor, la app avisa «No se puede borrar el kilometraje; corrígelo o déjalo como estaba».
7. **Resultados con `avisar`, y la agenda se refresca:** cerrar con `Presente` o `Tarde` → «Clase cerrada. Quedó completada.»; con `Ausente` → «Clase cerrada. Quedó registrada la inasistencia.»; editar → «Cambios guardados.».
8. **Errores de negocio con su texto** (el `mensaje` del backend, ya en español): `ERR_ACA_35` y `ERR_ACA_37` cierran la hoja, avisan y refrescan la agenda; `ERR_ACA_38` y cualquier otro error de negocio se muestran dentro de la hoja, que sigue abierta con lo que escribió; un error de red también la deja abierta y el botón pasa a «Reintentar». Nunca se da por cerrada una clase sin la respuesta.
9. **Doble toque inocuo:** mientras la llamada está en curso los botones quedan deshabilitados y la hoja no se cierra tocando afuera.
10. **Colores solo del tema** (`Theme.of(context).colorScheme`, `context.cockpit`). Piezas del Cockpit: `AvisoCockpit`, `IndicadorBoton`, `avisar`.

## Prerrequisito para la prueba manual

Hace falta (1) una cuenta de instructor (`instructorexample@gmail.com`) con clases `CONFIRMADA` **que ya empezaron** (una para cerrar con `Presente`, otra con `Ausente`) y otra que todavía no empezó, reservadas por un estudiante con horas (CU14), y (2) el backend de [`UT-BACKEND`](../UT-BACKEND/README.md) (todas las UT) con `academico`, `pagos` e `identidad` levantados.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU17-MOB-01](./UT-CU17-MOB-01.md) | Modelos y `ClasesService`: cerrar, ver y corregir una clase | — |
| [UT-CU17-MOB-02](./UT-CU17-MOB-02.md) | Providers de la clase dictada | 01 |
| [UT-CU17-MOB-03](./UT-CU17-MOB-03.md) | La hoja para cerrar y editar una clase | 02 |
| [UT-CU17-MOB-04](./UT-CU17-MOB-04.md) | «Cerrar clase» y «Editar» en la agenda | 01, 03 |

**Cadena:** `01 → 02 → 03 → 04` (la 04 también necesita los campos nuevos de la 01).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Elegir «Cerrar clase», completar el formulario y confirmar | 01, 02, 03, 04 |
| F1 | Editar una clase cerrada con sus datos precargados | 01, 02, 03, 04 |
| A1, A2, A8 | Clase no cerrable, ajena o que cambió a la vez | 03 |
| A3 | Datos inválidos: feedback inmediato y error del servidor | 03 |
| A4 | Cierre hecho aunque Identidad no devolvió el nombre | 01 |
| A7 | Sin conexión: la hoja queda abierta y se puede reintentar | 03 |
| CA13 | La agenda ofrece cerrar y editar solo cuando corresponde | 04 |

## Estado de la implementación

| UT | Estado |
| --- | --- |
| 01 a 04 | **Hechas** (2026-11-01): `flutter analyze` sin errores y la suite móvil completa en verde (1040 tests) |
| Prueba manual en el teléfono | Pendiente (ver el prerrequisito de arriba; también depende de UT-CU17-BE-10) |

### Notas de implementación

Lo que quedó distinto o más preciso al implementar (vale lo de acá sobre lo que digan las fichas):

- **Dónde quedaron los tests:** todos en `test/features/clases/` (sin subcarpetas `models/`, `providers/`, `widgets/`...), igual que los de CU14 a CU16. Los de modelos y service se sumaron a `clases_models_test.dart` y `clases_service_test.dart`; el doble `ClasesServiceFalso` de `test/helpers/clases_falsos.dart` ganó `cerrarClase`, `claseDictada` y `actualizarClase`, y `claseDictadaDePrueba`.
- **Nombre del provider de lectura:** `claseDictadaDetailProvider` (la convención `{feature}DetailProvider` de [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md#convención-de-nombres)), en `clase_dictada_provider.dart`.
- **La tarjeta no tiene un hueco genérico:** como `alCancelar` en CU16, `ClaseAgendaCard` recibe `alCerrar` y `alEditar` y dibuja cada botón solo si el servidor lo dice (`cerrable`, `claseCodigo`) y se le pasó qué hacer; `ListaDeAgenda` abre las hojas.
- **La hoja nunca se descarta tocando afuera ni deslizando** (`isDismissible: false` y `enableDrag: false`, siempre, no solo mientras envía): para salir está «Volver». Así un toque fuera de lugar no pierde lo escrito.
- **Elegir «Ausente» vacía** el tema y los kilómetros escritos (no solo los oculta), para que no quede nada que se pueda enviar a escondidas; volver a «Presente» los muestra vacíos.
- **El selector de asistencia permite quedar sin elegir** (`emptySelectionAllowed`): arranca sin selección y tocar la elegida la deselecciona; «Cerrar clase» queda apagado hasta que haya una asistencia.
- **Tema y observaciones no cortan lo que se escribe:** muestran el contador (`maxLength`) pero con `MaxLengthEnforcement.none`, así un tema de 121 caracteres se escribe y recibe el mensaje de validación en vez de truncarse en silencio. Los validadores miden el texto sin recortar, como el backend.
- **Los kilómetros solo aceptan dígitos** (`FilteringTextInputFormatter.digitsOnly`), así que un negativo no se puede escribir; el mensaje «Escribe solo números» queda como defensa del validador.
- **Si no se pueden traer los datos para editar** (red o servidor), la hoja muestra el error con «Reintentar» y «Volver» en vez de un formulario vacío.
- **`flutter test` no corre en esta máquina Windows** (`pdfrx` necesita compilar un asset nativo y no hay compilador de C), igual que en CU15 y CU16: las pruebas se corrieron en Linux (imagen `drivehub-flutter:test`) sobre una copia de `projects/mobile`. `flutter analyze` sí corre aquí.

## Follow-ups (no se implementan en estas UT)

- **«Iniciar clase»** como paso aparte (hora real y kilometraje inicial al empezar).
- **Recordatorio** de las clases sin cerrar (notificaciones: CU37 y CU38).
- **La prueba de clase** desde una clase cerrada: CU18.
- **Mostrar al estudiante** el tema de sus clases.

## Qué NO incluyen estas UT

- Cambios en el backend (están en [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Cambiar la asistencia de una clase cerrada, reabrirla o borrar un kilometraje.
- La prueba de clase (CU18).
- Rutas nuevas, ni pantallas completas.
