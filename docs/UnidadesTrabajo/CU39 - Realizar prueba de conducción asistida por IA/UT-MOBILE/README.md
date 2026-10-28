# Unidades de trabajo — CU39-MOB

**Caso de uso:** Realizar prueba de conducción asistida por IA: la pestaña «Prueba» de «Estudio», con la cámara frontal y un detector de rostros que corre en el teléfono.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter, solo Android) — guarda el resultado por el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU39 - Realizar prueba de conducción asistida por IA.md`](../../../CasosDeUso/CU39%20-%20Realizar%20prueba%20de%20conducci%C3%B3n%20asistida%20por%20IA.md)
**Recurso HTTP / contrato:** `/sesiones-ia`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Decisiones de diseño

- **IA en el dispositivo** (requisito del proyecto): `camera` entrega los cuadros de la cámara frontal y `google_mlkit_face_detection` (que corre en el teléfono) mide el giro (`headEulerAngleY`), el cabeceo (`headEulerAngleX`) y la probabilidad de ojos abiertos. Nada se graba ni se envía: solo el resultado.
- **La lógica de la prueba es una máquina de estados pura** (`MaquinaPrueba`): recibe mediciones y la hora desde afuera, no usa cámara ni reloj, y se prueba sin teléfono. Los umbrales viven en `ConfiguracionPrueba`.
- **El detector va detrás de una interfaz** (`DetectorRostro`) con la implementación de ML Kit y una falsa para los tests. `flutter test` nunca abre la cámara.
- **Evaluación periódica, no por cuadro:** el detector entrega la última medición y un temporizador (150 ms) se la da a la máquina; una medición de más de 600 ms se toma como «no se ve el rostro». El reloj es un provider para controlar el tiempo en los tests.
- **Pantalla en vertical mientras dura la prueba:** el detector de ML Kit recibe los cuadros con la rotación del sensor, que solo es correcta con el teléfono en vertical; la pantalla fija la orientación al empezar y la libera al terminar o salir.
- **Resultado tolerante a la falta de red:** se muestra siempre; guardarlo es un segundo paso que puede fallar y reintentarse (mismo patrón que CU40).
- **Punto a verificar en un teléfono real:** el signo del giro (qué lado es «izquierdo») depende de cómo ML Kit entrega el ángulo con la cámara frontal. Si los espejos salen invertidos se cambia `ConfiguracionPrueba.invertirLados`; no hace falta tocar la lógica. Los umbrales de ángulo (18° y 10°) también se afinan con pruebas reales.
- Permisos: `CAMERA` (y `uses-feature` no obligatorio) en `AndroidManifest.xml`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU39-MOB-01](./UT-CU39-MOB-01.md) | Máquina de la prueba y detector de rostro | CU40-MOB-01 (dependencias de IA) |
| [UT-CU39-MOB-02](./UT-CU39-MOB-02.md) | Pantalla «Prueba» y guardado del resultado | MOB-01, CU39-BE-01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 3 a 5, CA01 a CA04 | Pasos, posturas, tiempos y avisos | 01, 02 |
| Flujo principal 1, 2, 6, 7, CA10 | Inicio con aviso de privacidad, resultado y repetir | 02 |
| A1, A2, CA05 | Permiso y cámara no disponibles | 01, 02 |
| A4, A5, A8, CA06, CA07, CA11 | Cancelar, guardado y reintento | 02 |

## Follow-ups (no se implementan en estas UT)

- Ver el historial de pruebas, elegir categoría y escenario, y calibrar los ángulos con teléfonos reales.
- iOS.

## Qué NO incluyen estas UT

- El almacenamiento en el servidor ([UT-BACKEND](../UT-BACKEND/README.md)).
