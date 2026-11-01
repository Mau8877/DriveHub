# Unidades de trabajo — CU39-BE

**Caso de uso:** Realizar prueba de conducción asistida por IA: el servidor guarda la prueba que la app evaluó en el teléfono.
**Microservicio(s) / micro-frontend(s):** `asistente-ia` (FastAPI, capas, DynamoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU39 - Realizar prueba de conducción asistida por IA.md`](../../../CasosDeUso/CU39%20-%20Realizar%20prueba%20de%20conducci%C3%B3n%20asistida%20por%20IA.md)
**Recurso HTTP / contrato:** `/api/v1/sesiones-ia` (ya enrutado por el gateway a `asistente-ia`)

## Antes de implementar cualquier UT

Leer [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **El servidor no evalúa la prueba:** la app (IA local en el móvil) valida los pasos con la cámara y manda el resultado ya calculado. Por eso hay un solo `POST` que guarda una prueba **terminada**; no hay endpoints de «iniciar», «fotograma» ni «finalizar», y no se guarda ninguna imagen ni video.
- **Una prueba = una sesión `FINALIZADA` + un mensaje por paso.** Se reutilizan los modelos `SesionIA` y `MensajeIA` y sus tablas `SesionesIA` y `MensajesIA`; cada paso queda como un mensaje `ASSISTANT` con texto legible («Paso 2: Mira el espejo izquierdo (logrado, 2.1 s)»). Los estados `EN_CURSO` y el tipo `CHAT` quedan para otros casos de uso.
- **Transacción:** la sesión y sus mensajes se escriben juntos con `transact_write_items` (hasta 13 operaciones): o queda todo o no queda nada.
- **Validación en el contrato:** puntaje de 0 a 100, 1 a 12 pasos con `orden` 1, 2, 3…, textos acotados. Las reglas que Pydantic no cubre (numeración consecutiva) las valida el service con `ERR_SYS_01`; no hay código de error nuevo.
- **Listar o ver pruebas pasadas no está** (fuera de alcance del CU): se agrega con el caso de uso que lo pida.

## Contrato de datos / recurso HTTP

| Método | Path | Status | Cuerpo | `data` |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/sesiones-ia` | 201 | JSON de la prueba | `SesionResponse` |

Cuerpo: `{ categoria?, escenario?, puntaje 0..100, retroalimentacion (≤ 1000), modelo (≤ 60), duracionSeg 1..3600, pasos[1..12]{ orden, instruccion (≤ 120), aprobado, tiempoMs } }`.
`SesionResponse`: `{ codigo, tipo, categoria, escenario, puntaje, retroalimentacion, modelo, duracionSeg, pasos[], creadaEn }`. Nunca salen `usuarioCodigo` ni `sk`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU39-BE-01](./UT-CU39-BE-01.md) | Guardar la prueba de conducción | CU40-BE-01 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 6, CA06 | Guardar la sesión con un mensaje por paso | 01 |
| A6, A7, CA08, CA09 | Resultado inválido, rol y sesión | 01 |

## Follow-ups (no se implementan en estas UT)

- Listar y ver las pruebas pasadas; borrar.

## Qué NO incluyen estas UT

- La evaluación de la prueba, la cámara y la pantalla ([UT-MOBILE](../UT-MOBILE/README.md)).
