# Unidades de trabajo — CU21-BE

**Caso de uso:** Rendir simulacro escrito: listar los simulacros de la escuela, iniciar un intento con preguntas elegidas del banco, finalizarlo con su calificación y consultar el historial y la corrección.
**Microservicio(s) / micro-frontend(s):** `academico` (Spring Boot, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU21 - Rendir simulacro escrito.md`](../../../CasosDeUso/CU21%20-%20Rendir%20simulacro%20escrito.md)
**Recurso HTTP / contrato:** `/api/v1/simulacros/disponibles` y `/api/v1/intentos-simulacro` (ambos ya enrutados por el gateway a `academico`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Intento por estudiante, con copia.** `IntentoSimulacro` suma `escuelaCodigo`, `simulacroCodigo`, y copias de `simulacroNombre`, `duracionMin` y `puntajeAprobacion`; `PreguntaIntento` suma `preguntaCodigo`, `tipo` y `explicacion`. Todo intento se consulta con el estudiante del token.
- **Preguntas elegidas por el servidor**, al azar: por regla, preguntas activas de la escuela, del tema (regex exacta sin distinguir mayúsculas) y de la categoría del simulacro más `GENERAL` (`PreguntaRepositoryCustom.activasDelTema`). Si no alcanzan, 409 `ERR_ACA_28`.
- **Sin respuestas al iniciar:** la respuesta de iniciar no trae `correcta` ni `explicacion`; la corrección solo se devuelve al terminar.
- **Tiempo en el servidor:** el intento vence `duracionMin` después de `iniciadoEn`. Al finalizar se admiten 30 segundos de gracia (el teléfono envía solo al llegar a cero); pasados, el intento queda `EXPIRADO` con 0 puntos y sin nota. Un intento en curso vencido se expira al iniciar otro o al consultarlo.
- **Un intento en curso por simulacro y estudiante:** 409 `ERR_ACA_29`.
- **Calificación en el servicio:** una pregunta es correcta solo si el conjunto de opciones elegidas es igual al de correctas (ids inexistentes se ignoran). Puntaje con dos decimales; aprobado si alcanza el puntaje de aprobación.
- **Nota consolidada:** al finalizar a tiempo se escribe un `Resultado` (`origen = SIMULACRO`) que leerá CU23.
- **Dos controllers sin conflicto de rutas:** el de CU20 mantiene `/api/v1/simulacros` (administrador); CU21 suma `/api/v1/simulacros/disponibles` (la ruta literal gana a `/{codigo}`) y `/api/v1/intentos-simulacro`.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `GET` | `/api/v1/simulacros/disponibles` | 200 | `SimulacroDisponibleResponse[]` (sin paginar: una escuela tiene pocos) |
| `POST` | `/api/v1/intentos-simulacro` | 201 | `IntentoIniciadoResponse` |
| `GET` | `/api/v1/intentos-simulacro?page&pageSize` | 200 | `IntentoResumenResponse[]` + `meta` |
| `GET` | `/api/v1/intentos-simulacro/{codigo}` | 200 | `IntentoResultadoResponse` (409 `ERR_ACA_29` si sigue en curso) |
| `POST` | `/api/v1/intentos-simulacro/{codigo}/finalizar` | 200 | `IntentoResultadoResponse` |

Códigos de error nuevos: `ERR_ACA_28` a `ERR_ACA_31`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU21-BE-01](./UT-CU21-BE-01.md) | Modelos, repositorios y códigos de error del intento | — |
| [UT-CU21-BE-02](./UT-CU21-BE-02.md) | Servicio de intentos: iniciar, finalizar, historial | 01 |
| [UT-CU21-BE-03](./UT-CU21-BE-03.md) | Controller, errores documentados y diseño de BD | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 2 | Simulacros disponibles e historial | 01, 02, 03 |
| Flujo principal 4 | Armar el intento | 01, 02 |
| Flujo principal 7 | Calificar, guardar y corregir | 02, 03 |
| A1 a A8 | No disponible, banco corto, en curso, tiempo, intento ajeno, rol | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Reanudar un intento en curso desde otra sesión (endpoint que devuelva las preguntas pendientes).
- Test de integración con MongoDB real.
- Elegir dificultad al armar el intento.

## Qué NO incluyen estas UT

- CU23 (consulta de resultados) y la pantalla móvil ([UT-MOBILE](../UT-MOBILE/README.md)).
- Cambios en el gateway: `simulacros` e `intentos-simulacro` ya están enrutados.
