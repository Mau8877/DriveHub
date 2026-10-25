# Unidades de trabajo — CU30-BE

**Caso de uso:** Generar comprobante (Sistema): emitir el comprobante PDF de cada pago confirmado y de cada reembolso ejecutado, y entregarlo al estudiante dueño y a su instructor.
**Microservicio(s) / micro-frontend(s):** `pagos` (Spring Boot, capas, PostgreSQL, MinIO) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU30 - Generar comprobante`](../../../CasosDeUso/CU30%20-%20Generar%20comprobante.md)
**Recurso HTTP / contrato:** `/api/v1/comprobantes/{codigo}/pdf` (ya enrutado por el gateway a `pagos`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El PDF se genera con JasperReports** (una plantilla `.jrxml` con la marca de DriveHub, A5, un solo diseño para compra y reembolso), compilada una vez al arrancar, con la fuente incluida en el proyecto.
2. **Los datos del comprobante salen del propio pago** (nombres, plan, horas, montos y tipo de cambio copiados al comprar, CU26), no de Escuela ni de Identidad: el aviso de la pasarela no trae sesión de nadie y el comprobante tiene que mostrar lo fijado al pagar.
3. **Una falla del PDF nunca afecta al pago:** el comprobante queda creado con `url_pdf` nulo (pendiente) y un proceso lo reintenta cada 5 minutos. Mientras tanto la app muestra "en preparación" (409 `ERR_PAG_14`).
4. **Uno por pago y por tipo** (`UNIQUE (pago_id, tipo)`) y con número propio (`CMP-2026-000123`, secuencia de la base). Un pago reembolsado tiene dos comprobantes.
5. **MinIO privado, sin URLs públicas:** el PDF solo sale por un endpoint autenticado que comprueba que quien lo pide es el estudiante dueño o su instructor asignado. `url_pdf` guarda la clave del objeto.
6. **El comprobante no tiene validez fiscal** y lo dice en su pie.
7. **El comprobante de reembolso lo dispara CU31** cuando la devolución se ejecuta; CU30 solo deja listo `generarDeReembolso`.

## Contrato de datos / recurso HTTP

| Método | Path | Rol | Status | Respuesta |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/comprobantes/{codigo}/pdf` | `ESTUDIANTE` (dueño), `INSTRUCTOR` (alumno asignado) | 200 | bytes `application/pdf` (sin sobre) |

`ComprobanteResumen` (dentro de `PagoDetalleResponse`): `codigo`, `tipo` (`PAGO` o `REEMBOLSO`), `numero`, `disponible`. Errores (con sobre): `404 ERR_PAG_13` comprobante inexistente o ajeno, `409 ERR_PAG_14` comprobante en preparación, `503 ERR_PAG_15` MinIO no disponible. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU30-BE-01](./UT-CU30-BE-01.md) | Migración y modelo del comprobante por tipo | — |
| [UT-CU30-BE-02](./UT-CU30-BE-02.md) | Plantilla JasperReports y generador del PDF | — |
| [UT-CU30-BE-03](./UT-CU30-BE-03.md) | Almacenamiento de los PDF en MinIO | — |
| [UT-CU30-BE-04](./UT-CU30-BE-04.md) | Generar y reintentar el comprobante | 01, 02, 03 |
| [UT-CU30-BE-05](./UT-CU30-BE-05.md) | Disparar el comprobante al confirmar el pago | 04 |
| [UT-CU30-BE-06](./UT-CU30-BE-06.md) | Descarga del comprobante y comprobantes en el detalle | 05 (y UT-CU29-BE-03) |

**Cadena:** `01, 02, 03 → 04 → 05 → 06` (las tres primeras son independientes). La 06 también necesita `EscuelaClient.esMiAlumno` de [UT-CU29-BE-03](../../CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos/UT-BACKEND/UT-CU29-BE-03.md).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 a 5, CA01 | Comprobante de compra numerado con sus datos | 01, 02, 03, 04, 05 |
| CA02 | Comprobante de reembolso con original, retención y devuelto | 02, 04 (lo dispara CU31) |
| A1, CA03 | Un solo comprobante por pago y tipo | 01, 04 |
| A2, CA04 | Falla del PDF: pendiente y reintento | 04 |
| CA05 | Leyenda sin validez fiscal | 02 |
| A4, CA06 | Solo el estudiante dueño y su instructor | 06 |
| CA07 | Datos fijados al pagar | 02, 04 |
| CA08 | Sin comprobante si el pago no está confirmado | 04 |

Los botones de descarga están en [UT-MOBILE](../UT-MOBILE/README.md). El comprobante de reembolso lo dispara CU31.

## Follow-ups (no se implementan en estas UT)

- Enviar el comprobante por correo o notificación push.
- Facturación fiscal (NIT, factura electrónica).
- Plantilla distinta por escuela, con su logo.

## Qué NO incluyen estas UT

- Cuándo se ejecuta un reembolso (CU31).
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Commits sugeridos

Un commit `[BACK]` para `pagos` (UT 01 a 06). Los cambios de `.env.example`, `docker-compose.yml` e `infrastructure/` son `[INFRA]`, los de `.claude/` `[REPO]` y los de `docs/` `[DOCS]`.
