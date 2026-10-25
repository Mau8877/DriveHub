# UT-CU30-BE-02 — Plantilla JasperReports y generador del PDF

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-BE-02` |
| **Nombre** | Plantilla JasperReports y generador del PDF |
| **Caso de uso** | CU30-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El comprobante tiene que verse profesional y de marca, y mostrar los datos fijados al pagar.

## Objetivo

`ComprobantePdfGenerador` produce el PDF del comprobante de compra y del de reembolso a partir de una plantilla Jasper.

## Alcance

- Dependencias en el `pom.xml` (versiones fijadas en properties): `net.sf.jasperreports:jasperreports`, `jasperreports-pdf` y `jasperreports-fonts` (o el TTF de la marca incluido en `resources/`), y `org.apache.pdfbox:pdfbox` solo en `test`. El servicio corre con `-Djava.awt.headless=true`; el contenedor **no** debe depender de fuentes instaladas en el sistema (se incluye la fuente en el proyecto).
- `features/comprobantes/dto/DatosComprobante` (`record`): `tipo`, `numero`, `escuelaNombre`, `estudianteNombre`, `planNombre`, `horas`, `montoUsd`, `montoBs`, `tipoCambio`, `fecha` (`Instant`, se muestra en hora de Bolivia), `pagoReferencia` (los primeros 8 caracteres del código del pago, para citarlo) y, solo en el de reembolso, `montoOriginal`, `porcentajeRetenido`, `montoRetenido` y `montoDevuelto`.
- `resources/reportes/comprobante.jrxml`: una página A5 vertical con **un solo diseño** para los dos tipos (las filas del reembolso se muestran solo en el de reembolso): encabezado con la marca DriveHub y el nombre de la escuela en Gris Asfalto (`#1E293B`) con acento Ámbar (`#F59E0B`) tomados de [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md); título "Comprobante de pago" o "Comprobante de reembolso"; número y fecha; bloque de datos (estudiante, plan, horas); bloque de montos en USD, equivalente en Bs y tipo de cambio; en el reembolso, original, retención con su porcentaje y devuelto; pie con la leyenda **"Documento sin validez fiscal"**. Todo en español, con formato de números boliviano (`1.250,00`) y las tildes y la ñ bien dibujadas.
- `ComprobantePdfGenerador` (`@Component`): compila la plantilla **una vez al arrancar** (no en cada comprobante) y `byte[] generar(DatosComprobante)` la llena y exporta a PDF. Un nombre largo se corta en varias líneas sin desbordar; un dato vacío se muestra como "—". Una falla de Jasper se relanza como `IllegalStateException` (la atrapa el service de la UT 04).
- Tests (`ComprobantePdfGeneradorTest`, extrayendo el texto del PDF con PDFBox): empieza con `%PDF`; contiene número, escuela, estudiante, plan, horas y los tres montos; el de reembolso suma original, retención y devuelto, y el de compra no; contiene la leyenda; tildes y ñ ("Peña Núñez", "Educación") salen bien; un nombre de 160 caracteres no rompe el diseño; es A5 de una página; dos llamadas seguidas no vuelven a compilar la plantilla.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/dto/DatosComprobante.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/service/ComprobantePdfGenerador.java
- projects/backend/pagos/src/main/resources/reportes/comprobante.jrxml
- projects/backend/pagos/src/main/resources/reportes/fuentes/ (la fuente de la marca)
- projects/backend/pagos/pom.xml
- projects/backend/pagos/Dockerfile (si hace falta la opción headless)
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobantePdfGeneradorTest.java

## Fuera de alcance (no tocar)

- Numerar, guardar o servir el PDF (UT 03 a 06).
- Una plantilla distinta por escuela.
- Facturas con NIT.

## Resultado esperado

Dados los datos de una compra o de un reembolso, se obtiene un PDF A5 numerado, con la marca de DriveHub, los montos y la leyenda de que no tiene validez fiscal.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Un test manual escribe el PDF de ejemplo en `target/` y se revisa a ojo que el diseño sea el de la marca, sin texto cortado.
