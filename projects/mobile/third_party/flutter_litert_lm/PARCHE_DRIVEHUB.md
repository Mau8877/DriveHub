# Parche de DriveHub sobre `flutter_litert_lm` 0.3.0

Copia del plugin (Apache-2.0, https://github.com/songhieu/flutter_litert_lm) con **un cambio** en
`android/src/main/kotlin/com/songhieu/flutter_litert_lm/FlutterLitertLmPlugin.kt` (`parseConversationConfig`):

- La versión original leía `tools` y `automaticToolCalling` del mapa que manda Dart pero **no los pasaba** a
  `ConversationConfig`. El modelo no recibía las funciones que debía elegir (FunctionGemma), no devolvía llamadas a
  función y generaba hasta el tope de tokens (minutos por frase).
- El parche pasa cada herramienta como `OpenApiTool` y respeta `automaticToolCalling`.

Se usa por `path:` desde `pubspec.yaml`. Cuando una versión publicada del plugin arregle esto, se vuelve a la
dependencia de pub.dev y se borra esta carpeta. Detalle del uso: `docs/UnidadesTrabajo/CU50 - Asistente de voz de clases`.
