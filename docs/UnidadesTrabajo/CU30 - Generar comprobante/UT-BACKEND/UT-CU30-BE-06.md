# UT-CU30-BE-06 — Descarga del comprobante y comprobantes en el detalle

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-BE-06` |
| **Nombre** | Descarga del comprobante y comprobantes en el detalle |
| **Caso de uso** | CU30-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU30-BE-05, UT-CU29-BE-03 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante y su instructor descargan el comprobante en la app, y nadie más.

## Objetivo

`GET /api/v1/comprobantes/{codigo}/pdf` y la lista de comprobantes en el detalle del pago.

## Alcance

- `ComprobanteService.obtenerPdf(escuelaCodigo, personaCodigo, esInstructor, codigoComprobante)` con `@PreAuthorize("hasAnyRole('ESTUDIANTE','INSTRUCTOR')")`: el comprobante debe ser de la escuela del token; un `ESTUDIANTE` solo puede ver los de **sus** pagos; un `INSTRUCTOR` solo los de estudiantes que tiene asignados (`EscuelaClient.esMiAlumno`, CU29). Si no existe o no puede verlo, `NotFoundException` `ERR_PAG_13` (no se distingue). Si el comprobante existe pero su PDF todavía no (pendiente), `ConflictException` `ERR_PAG_14`.
- `ComprobanteController` (`features/comprobantes/controller/`): `GET /api/v1/comprobantes/{codigo}/pdf` responde los bytes con `Content-Type: application/pdf`, `Content-Disposition: inline; filename="{numero}.pdf"` y `Cache-Control: no-store`. **Es una excepción al sobre `ApiResponse`** (un archivo): se suma a la lista de excepciones de [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md); los errores sí usan el sobre.
- `PagoDetalleResponse` (CU28) suma `comprobantes: List<ComprobanteResumen(codigo, tipo, numero, disponible)>` (`disponible` es verdadero si ya tiene PDF); y `PagoAlumnoResponse` (CU29) suma `comprobanteCodigo` (el de tipo `PAGO`, o `null`).
- `ERR_PAG_13` (comprobante inexistente o ajeno, 404) y `ERR_PAG_14` (comprobante en preparación, 409) al catálogo y a `docs/errors/`.
- Tests: service con repositorio, almacén y cliente mockeados (dueño sí; otro estudiante → `ERR_PAG_13`; instructor con alumno asignado sí y sin asignación → `ERR_PAG_13`; otra escuela → `ERR_PAG_13`; pendiente → `ERR_PAG_14`; el reembolso es otro comprobante del mismo pago); `@WebMvcTest` (cabeceras y bytes, `404`/`409` con sobre, `401`, `403` para `ADMIN_ESCUELA`); el detalle del pago trae sus comprobantes (el de compra y, si lo hay, el de reembolso).

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/controller/ComprobanteController.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/service/ComprobanteService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/dto/ComprobanteResumen.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/PagoDetalleResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/PagoAlumnoResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobanteControllerTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobanteServiceDescargaTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md
- .claude/rules/backend/RESPONSES_BACKEND.md

## Fuera de alcance (no tocar)

- El gateway: `comprobantes` ya está enrutado a `pagos`.
- Enviar el comprobante por correo o notificación.
- Mostrar el PDF en la app (UT-MOBILE).

## Resultado esperado

El estudiante dueño y su instructor asignado descargan el mismo PDF; cualquier otra persona recibe 404.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el token del estudiante, `GET /api/v1/comprobantes/{codigo}/pdf` devuelve un PDF que abre; con el de otro estudiante, 404.
