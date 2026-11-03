# UT-CU17-MOB-03 — La hoja para cerrar y editar una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-MOB-03` |
| **Nombre** | La hoja para cerrar y editar una clase |
| **Caso de uso** | CU17-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU17-MOB-02` |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (`Form` + `TextFormField` + `validator`, mensajes en español, mismas reglas que el backend), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (solo colores del tema; un campo o botón sale del tema, no necesita widget propio), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `reprogramar_sheet.dart` y `dialogo_cancelar_clase_agenda.dart` (UT-CU16-MOB-04). Decisiones 2 a 10 del [README](./README.md).

## Solicitud de negocio

El instructor que terminó una clase quiere dejarla registrada rápido y, si se equivocó, corregirla sin empezar de cero.

## Objetivo

Existe una hoja inferior con el formulario de la clase que sirve para cerrarla y para corregirla, con sus validaciones, errores y avisos.

## Alcance

- `widgets/validadores_clase.dart`: las reglas de los datos en un solo lugar, funciones puras que devuelven el mensaje en español o `null`: `validarTema(String?)` (hasta 120), `validarObservaciones(String?)` (hasta 500), `validarKilometraje(String?)` (vacío vale; solo dígitos; de 0 a 9 999 999), `validarKmFinal(String? kmInicio, String? kmFin)` (si hay los dos, el final no es menor), y las constantes `maxTema`, `maxObservaciones` y `maxKilometraje`.
- `widgets/mensajes_de_cierre.dart`: los textos en un solo lugar: el aviso de `Ausente` («El estudiante pierde la hora de esta clase.»), el resultado de cerrar según la asistencia (decisión 7 del README), «Cambios guardados.», el aviso de «No se puede borrar el kilometraje; corrígelo o déjalo como estaba» y los códigos que cierran la hoja (`codigosQueCierranElCierre = {'ERR_ACA_35', 'ERR_ACA_37'}`).
- `widgets/formulario_clase_sheet.dart`:
  - `Future<void> mostrarCerrarClase(BuildContext, ClaseAgenda)` y `Future<void> mostrarEditarClase(BuildContext, ClaseAgenda)` abren la hoja con `showModalBottomSheet` (`isScrollControlled`, `isDismissible: false` y sin arrastre mientras envía).
  - `FormularioClaseSheet` (`ConsumerStatefulWidget`) con dos modos. Muestra siempre la clase (estudiante, día y hora, con `tituloDeDia` y `rangoDeHoras`).
  - **Cerrar:** un `SegmentedButton<AsistenciaClase>` (`Presente`, `Tarde`, `Ausente`, sin selección por defecto: el instructor tiene que elegir; «Cerrar clase» queda deshabilitado hasta entonces), tema, observaciones (varias líneas, contador), km inicial y km final (teclado numérico, solo dígitos). Con `Ausente` se ocultan y vacían tema y km y aparece el aviso de la hora perdida (`AvisoCockpit` de advertencia). Los `validator` de los campos usan `validadores_clase.dart` y se corren al confirmar.
  - **Editar:** lee `claseDictadaDetailProvider(claseCodigo)` (cargando: indicador; error de red: aviso con «Reintentar»), precarga los campos, muestra la asistencia como texto (no editable) y, si fue `Ausente`, solo las observaciones. «Guardar» manda solo lo que cambió (`ActualizarClaseRequest`); sin cambios cierra sin llamar; un kilometraje que tenía valor y se dejó vacío muestra el aviso de que no se puede borrar y no se envía.
  - **Envío:** llama a `gestionClaseProvider`; en éxito cierra y `avisar(...)` el resultado. Un `ApiException` con `ERR_ACA_35` o `ERR_ACA_37` cierra la hoja y avisa con el `mensaje` del backend (la agenda ya se refrescó); `ERR_ACA_38` u otro error de negocio se muestra dentro de la hoja con un `AvisoCockpit` de error, sin perder lo escrito; un error de red deja la hoja abierta con el aviso y el botón pasa a «Reintentar». Mientras envía, los botones se deshabilitan (`IndicadorBoton`): un doble toque manda una sola petición.
- Tests de widget con `ClasesServiceFalso`: cerrar exige elegir asistencia; cerrar con `Presente` manda el cuerpo (sin los campos vacíos), cierra y avisa «Clase cerrada. Quedó completada.»; con `Ausente` oculta tema y km, muestra el aviso de la hora, manda solo observaciones y avisa la inasistencia; cada validación (tema 121, observaciones 501, km 10 000 000, final menor que el inicial) bloquea el envío con su mensaje y no llama al servidor; un doble toque manda una sola petición; sin red la hoja queda abierta y «Reintentar» vuelve a enviar; `ERR_ACA_37` y `ERR_ACA_35` cierran con el mensaje del backend; `ERR_ACA_38` queda dentro de la hoja con lo escrito; editar precarga los datos, manda solo lo cambiado, un texto borrado se manda vacío, sin cambios no llama, un km vaciado avisa y no se envía, la asistencia no es editable y con `Ausente` solo se edita observaciones; los validadores y los mensajes como funciones puras.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/validadores_clase.dart`, `mensajes_de_cierre.dart`, `formulario_clase_sheet.dart`
- `projects/mobile/test/features/clases/validadores_clase_test.dart`, `formulario_clase_sheet_test.dart`

## Fuera de alcance (no tocar)

- Los botones de la agenda que abren la hoja (UT 04).
- Cambiar la asistencia, borrar un kilometraje y la prueba de clase.
- Cualquier cambio de `AgendaInstructorScreen`, `ClaseAgendaCard` y `ListaDeAgenda`.
- Rutas nuevas, ni pantallas completas.

## Resultado esperado

La hoja cierra y corrige una clase con las mismas reglas que el backend, nunca da una clase por cerrada sin respuesta, y avisa cómo quedó.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Prueba manual (ver prerrequisitos del README, una vez hecha la UT 04): cerrar con `Presente` y datos → queda `Completada`; con `Ausente` → avisa que pierde la hora y queda `No asistió`; los límites de longitud y de kilómetros se avisan al escribir; con el backend detenido la hoja queda abierta y reintenta.
