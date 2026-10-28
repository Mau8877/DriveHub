# Unidades de trabajo — CU21-MOB

**Caso de uso:** Rendir simulacro escrito: la sección «Estudio» de la app del estudiante (simulacros, rendir con cronómetro, corrección e historial).
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU21 - Rendir simulacro escrito.md`](../../../CasosDeUso/CU21%20-%20Rendir%20simulacro%20escrito.md)
**Recurso HTTP / contrato:** `/simulacros/disponibles` e `/intentos-simulacro`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Feature nueva `simulacros/`** con la forma de siempre (`models/`, `services/`, `providers/`, `screens/`, `widgets/`). La sección «Estudio» del `EstudianteShell` muestra `SimulacrosScreen`; cuando lleguen CU34 y CU35 se le suman sus propias pestañas.
- **El cronómetro corre en el teléfono**, desde que llegan las preguntas (no con la hora del servidor), para que un reloj mal puesto no acorte ni alargue el intento. Al llegar a cero la app envía lo marcado; el backend vuelve a controlar el tiempo.
- **El estado del intento** vive en `rendirProvider` (preguntas, marcadas, pregunta actual). Si el envío falla, el intento sigue en pantalla para reintentar.
- **Salir con «atrás»** pide confirmar y avisa que el tiempo sigue corriendo: el intento no se cancela.
- **Rutas:** `/study/simulacros/rendir` y `/study/attempts/:codigo`, solo para el estudiante (el `redirect` del router las protege). Tras finalizar, la corrección se abre en su ruta y se vuelve a pedir al backend.
- **Las opciones son filas tocables** con icono de radio o de casilla (no `Radio` ni `Checkbox` sueltos): una sola mecánica para los tres tipos de pregunta.
- **La corrección no depende solo del color:** cada opción dice «(tu respuesta)» o «(correcta)».

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU21-MOB-01](./UT-CU21-MOB-01.md) | Modelos, servicio y providers de simulacros | BE-03 |
| [UT-CU21-MOB-02](./UT-CU21-MOB-02.md) | Pantallas, rutas y sección «Estudio» | 01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 3 | Listar y confirmar | 01, 02 |
| Flujo principal 4 a 6 | Iniciar, responder, cronómetro y finalizar | 01, 02 |
| Flujo principal 7 | Corrección | 01, 02 |
| A1 a A7 | Errores con mensaje claro, tiempo agotado, reintento | 01, 02 |
| CA09, CA10 | Cronómetro y aviso de preguntas sin responder | 02 |

## Follow-ups (no se implementan en estas UT)

- Reanudar un intento al volver a abrir la app.
- Pestañas de manuales y tutoriales (CU34, CU35) en «Estudio».

## Qué NO incluyen estas UT

- Resultados de todas las evaluaciones (CU23).
