# Unidades de trabajo — CU28-BE

**Caso de uso:** Consultar historial de pagos (Estudiante): ver sus compras con su estado, sus horas restantes y el reembolso previsto.
**Microservicio(s) / micro-frontend(s):** `pagos` (Spring Boot, capas, PostgreSQL) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU28 - Consultar historial de pagos`](../../../CasosDeUso/CU28%20-%20Consultar%20historial%20de%20pagos.md)
**Recurso HTTP / contrato:** `/api/v1/pagos` y `/api/v1/pagos/{codigo}` (ya enrutados por el gateway a `pagos`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El estado que ve el estudiante se deriva** del pago y de su reversión (`REEMBOLSO_EN_PROCESO` y `REEMBOLSADO` no son estados guardados). Se calcula en un solo lugar y lo usan el listado, el detalle y el filtro.
2. **`reembolsable` lo decide el servidor**, no la app: pago `CONFIRMADO`, ninguna hora usada (`horas_restantes = horas`) y sin reversión vigente. La app solo muestra el botón cuando viene en `true`. CU31 vuelve a comprobarlo al ejecutar.
3. **La retención del 5 % es configuración** (`REEMBOLSO_RETENCION_PORCENTAJE`) y la calcula `ReembolsoCalculadora`, que CU31 reutiliza; el monto retenido es `pagado − devuelto` para que nunca se pierda un centavo por redondeo.
4. **Solo se listan pagos del concepto `HORAS`.** Los de reserva de clase (CU44) tendrán su propia vista cuando existan.

## Contrato de datos / recurso HTTP

Todos exigen `ESTUDIANTE` (`401` sin sesión, `403` sin el rol).

| Método | Path | Query | Status | `data` |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/pagos` | `page`, `pageSize`, `filter[estado]=PENDIENTE\|CONFIRMADO\|FALLIDO\|REEMBOLSO_EN_PROCESO\|REEMBOLSADO` | 200 | `PagoResumenResponse[]` + `meta` |
| `GET` | `/api/v1/pagos/{codigo}` | — | 200 | `PagoDetalleResponse` |

`PagoResumenResponse`: `codigo`, `planNombre`, `horas`, `montoUsd`, `montoBs`, `estado`, `creadoEn`. `PagoDetalleResponse`: lo anterior más `horasRestantes`, `confirmadoEn`, `reembolsable` y `reembolso` (`montoDevuelto`, `montoRetenido`, `porcentajeRetenido`, o `null`); CU30 suma `comprobantes`. Error: `404 ERR_PAG_07` pago inexistente o ajeno. El saldo de horas sale de `GET /api/v1/billeteras-horas/mia` (CU27).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU28-BE-01](./UT-CU28-BE-01.md) | Consultas de pagos del estudiante | — |
| [UT-CU28-BE-02](./UT-CU28-BE-02.md) | Servicio de consulta: listado, detalle y reembolso previsto | 01 |
| [UT-CU28-BE-03](./UT-CU28-BE-03.md) | Controller del historial y errores documentados | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 y 2, CA01 | Listado propio, paginado, con filtro de estado | 01, 02, 03 |
| Paso 4, CA02 | Detalle con horas restantes y reembolso previsto | 02, 03 |
| A2, CA03 | Pago ajeno o inexistente | 02, 03 |
| F2 pasos 1 y 2, CA05, CA06, CA07 | Cuándo se ofrece el reembolso y cuánto se devuelve | 02 |
| A9, CA10 | Solo estudiante | 03 |

Descargar el comprobante (F1, A3, CA04) es de [CU30](../../CU30%20-%20Generar%20comprobante/UT-BACKEND/README.md); pedir el reembolso (F2 pasos 3 a 5, A4 a A7, CA08, CA09) es de [CU31](../../CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29/UT-BACKEND/README.md). La pantalla está en [UT-MOBILE](../UT-MOBILE/README.md).

## Follow-ups (no se implementan en estas UT)

- Mostrar también los pagos de reserva de clase (CU44) cuando existan.
- Un listado de los pagos de toda la escuela para el Administrador.

## Qué NO incluyen estas UT

- Ejecutar el reembolso (CU31) y generar el comprobante (CU30).
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Commits sugeridos

Un commit `[BACK]` para `pagos` (UT 01 a 03). Los cambios de `docs/` son `[DOCS]`.
