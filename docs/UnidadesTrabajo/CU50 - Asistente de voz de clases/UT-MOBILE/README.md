# Unidades de trabajo — CU50-MOB

**Caso de uso:** Asistente de voz de clases: el estudiante habla, el teléfono entiende y actúa sobre la pantalla de Clases, todo sin internet para entender y con confirmación para cambiar datos.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter, solo Android). No toca el backend: ejecuta las operaciones de [CU14](../../CU14%20-%20Reservar%20horario/UT-MOBILE/README.md) y [CU15](../../CU15%20-%20Cancelar%20o%20reprogramar%20clase/UT-MOBILE/README.md) que ya existen. — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU50 - Asistente de voz de clases.md`](../../../CasosDeUso/CU50%20-%20Asistente%20de%20voz%20de%20clases.md)
**Recurso HTTP / contrato:** ninguno nuevo. Pantalla: el botón de micrófono y la hoja del asistente en «Clases».

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) (sobre todo «IA en el dispositivo»), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas no sustituyen esas reglas.

## Decisiones de diseño

- **Una feature nueva, `features/asistente/`**, con `models/`, `providers/`, `services/`, `screens/` y `widgets/` (solo las que tengan algo). Depende de `clases` (justificado por el CU: actúa sobre su pantalla) únicamente a través de sus **providers**, nunca de sus widgets ni su service; `clases` no depende de `asistente` salvo por el botón de micrófono que se monta en su pantalla (UT-MOB-08).
- **El modelo elige, la app decide.** El modelo devuelve una llamada a función (nombre + argumentos) de un catálogo cerrado; la app valida esa salida (UT-MOB-01), resuelve los datos contra las listas reales (UT-MOB-02) y ejecuta con la lógica de siempre. Una salida que no es del catálogo se descarta. Probado antes de implementar: el `.litertlm` exportado elige la acción correcta en el 87,5 % de 144 frases de prueba, así que **la confirmación obligatoria no es opcional**.
- **Catálogo de acciones = datos.** `assets/ml/acciones_clases.json` es una copia de la pantalla `clases` de `ml/acciones/acciones_pantallas.json` (la fuente que usa el entrenamiento). Una acción nueva se agrega ahí y al entrenamiento. Un test compara los nombres del asset con el enum de acciones para que no se desalineen.
- **Dos modelos, cada uno detrás de una interfaz** (como `ClasificadorTablero` en CU40): `ModeloDeAcciones` (FunctionGemma 270M `.litertlm`, con el plugin `flutter_litert_lm`, que admite herramientas) y `ReconocedorDeVoz` (Whisper tiny multilingüe con `sherpa_onnx`, audio con `record`). Hay una implementación real y una falsa para los tests: `flutter test` nunca carga un modelo ni abre el micrófono.
- **Los modelos no se versionan** (pesan ~284 MB y ~40 MB). Se buscan en una carpeta de la app (`<documentos>/ia/`); en desarrollo se copian con `adb push`. Cómo se entregan al usuario final (descarga, empaquetado) queda como follow-up, porque cambia el tamaño de la app.
- **Prompt como en el entrenamiento:** mensaje `developer` con la frase *"You are a model that can do function calling with the following functions"* y las funciones del catálogo; temperatura baja (0,1). Sin eso el modelo no sabe qué hacer (se comprobó en el teléfono).
- **Solo Android** y CPU como backend por defecto (probado en un Android real: carga y responde).

## Contrato de datos

Acciones del catálogo `clases` (nombre → qué hace en la pantalla):

| Acción | Tipo | Efecto |
|--------|------|--------|
| `ver_mis_clases` | consulta | pestaña «Mis clases» |
| `ver_historial` | consulta | pestaña «Historial» |
| `buscar_horarios` | consulta | pestaña «Reservar» con día, momento e instructor |
| `seleccionar_horario` | consulta | marca la franja de esa hora (no reserva) |
| `limpiar_filtros` | consulta | quita momento e instructor |
| `ver_horas_disponibles` | consulta | muestra el saldo de horas |
| `cancelar_clase` | cambio | identifica la clase y abre el diálogo de cancelar de `clases` (ahí se confirma) |
| `reprogramar_clase` | cambio | identifica la clase y abre la hoja de reprogramar de `clases` (ahí se elige el horario y se confirma) |
| `fuera_de_alcance` | ninguna | avisa que no se puede desde Clases |

Reservar lo marcado se hace con la barra «Confirmar» que ya existe (CU14); el asistente no agrega un camino nuevo de reserva.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU50-MOB-01](./UT-CU50-MOB-01.md) | Acciones del asistente y lectura de la salida del modelo | — |
| [UT-CU50-MOB-02](./UT-CU50-MOB-02.md) | Resolver día, momento, instructor y hora | 01 |
| [UT-CU50-MOB-03](./UT-CU50-MOB-03.md) | Ejecutar las consultas sobre la pantalla | 01, 02 |
| [UT-CU50-MOB-04](./UT-CU50-MOB-04.md) | Identificar la clase que se quiere cancelar o reprogramar | 01, 02 |
| [UT-CU50-MOB-05](./UT-CU50-MOB-05.md) | Modelo de acciones en el teléfono (LiteRT-LM) | 01 |
| [UT-CU50-MOB-06](./UT-CU50-MOB-06.md) | Reconocimiento de voz local (Whisper) | — |
| [UT-CU50-MOB-07](./UT-CU50-MOB-07.md) | Controlador del asistente (estados y errores) | 03, 04, 05, 06 |
| [UT-CU50-MOB-08](./UT-CU50-MOB-08.md) | Botón de micrófono y hoja del asistente | 07 |
| [UT-CU50-MOB-09](./UT-CU50-MOB-09.md) | Permisos y modelos ausentes | 05, 06, 08 |

**Cadena:** `01 → 02 → 03, 04`; `01 → 05`; `06`; `03, 04, 05, 06 → 07 → 08 → 09`. Las 01 a 04 son Dart puro y se prueban sin el teléfono; las 05 y 06 son las que tocan el dispositivo.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 3 | Mantener pulsado, hablar, texto en pantalla | 06, 07, 08 |
| Flujo principal 4 | Elegir la acción con el modelo | 01, 05 |
| Flujo principal 5, CA01, CA02 | Consultas sobre la pantalla | 02, 03 |
| Flujo principal 6 a 8, CA03 a CA05 | Resumen, confirmación y ejecución | 04, 07, 08 |
| A1, A3, CA07 | Audio no entendido, falta un dato, baja confianza | 01, 06, 07 |
| A2, CA06 | Fuera de alcance | 01, 07 |
| A4, A5, CA08, CA09 | Día o instructor que no cuadra, varias clases posibles | 02, 04 |
| A6, A8, A9, A10, CA10, CA11 | Sin clase, sin horas, sin red, regla de CU15 | 04, 07 |
| A11, A12, CA12 | Permiso y modelos ausentes | 09 |
| CA13 | El audio no se guarda ni se envía | 06 |

## Follow-ups (no se implementan en estas UT)

- Entrega de los modelos al usuario final (descarga la primera vez o empaquetado) y su tamaño.
- Mejorar el modelo: `ver_historial` y las frases coloquiales fallan más (medido: 87,5 % de acción correcta); más ejemplos y reentrenar con `ml/acciones/`.
- Un botón que abra los ajustes de la app cuando el micrófono quedó negado (necesita `permission_handler`).
- Mantener el motor de Whisper vivo entre frases (hoy se crea y se libera en cada una) si la latencia en el teléfono lo pide.
- Respuestas habladas, otras pantallas, iOS, instructor y Administrador.

## Qué NO incluyen estas UT

- Cambios al backend, al gateway o a [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md): no hay recurso HTTP nuevo.
- Reimplementar reservar, cancelar o reprogramar: se reutilizan los providers y servicios de `features/clases`.
- Comprar planes por voz ([CU26](../../CU26%20-%20Realizar%20pago/UT-MOBILE/README.md)) y el chat de WhatsApp ([CU49](../../CU49%20-%20Conversar%20por%20WhatsApp/UT-BACKEND/README.md)).
