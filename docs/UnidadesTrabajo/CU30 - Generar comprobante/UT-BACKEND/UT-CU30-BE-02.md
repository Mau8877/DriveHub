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
- `features/comprobantes/dto/DatosComprobante` (`record`): `tipo`, `numero`, `escuelaNombre`, `estudianteNombre`, `planNombre`, `horas`, `montoUsd`, `montoBs`, `tipoCambio`, `fecha` (`Instant`, se muestra en hora de Bolivia), `pagoReferencia` (los primeros 8 caracteres del código del pago, para citarlo), `metodoPago` ("Tarjeta · procesado por Stripe") y, solo en el de reembolso, `comprobanteOriginalNumero` (el número del comprobante de la compra), `fechaCompra`, `montoOriginal`, `porcentajeRetenido`, `montoRetenido`, `montoDevuelto`, `montoDevueltoBs` (referencial, al tipo de cambio de la compra) y `motivo` (ya en español: el texto de `MotivoReembolso`, el escrito por el estudiante si eligió "Otro", o "La compra no se pudo completar" en una compensación).
- `resources/reportes/comprobante.jrxml`: una página **A4** vertical con el diseño **«Ruta»** aprobado (modelo 2; el mockup con compra y reembolso está en [comprobante-ruta.dc.html](../../../DriveHub/mockups/comprobante-ruta.dc.html), archivo fuente de la herramienta de diseño: sirve de referencia de medidas, colores y textos, no se abre suelto en el navegador), **un solo diseño** para los dos tipos (las filas del reembolso se muestran solo en el de reembolso). Colores de [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md): Gris Asfalto `#1E293B`, Ámbar `#F59E0B` y Azul Técnico `#0284C7`. Estructura, de arriba abajo:
  - **Franja vertical izquierda** (58 pt) en Gris Asfalto con la línea central discontinua ámbar de la carretera y, arriba, el cuadrado ámbar con la «D» de DriveHub. La franja termina donde empieza el pie.
  - **Encabezado:** «DriveHub · Compra» o «DriveHub · Reembolso» en pequeño; debajo el título "Comprobante de pago" o "Comprobante de reembolso"; a la derecha el número dentro de un **recuadro de señal vial** (doble borde asfalto, fondo del color de acento, número en tipografía monoespaciada).
  - **Color de acento:** Ámbar en la compra y **Azul Técnico en el reembolso** (puntos del detalle, borde de la placa del total).
  - Línea punteada, y los bloques **Escuela** (borde ámbar) y **Estudiante** (borde azul).
  - **Tabla de datos** sobre fondo gris claro: fecha de emisión, referencia del pago, método de pago y tipo de cambio aplicado y estado en la compra; fecha de emisión, compra original (número y fecha), referencia del pago, forma de devolución y **motivo del reembolso** en el reembolso.
  - **Detalle de la operación:** compra, una fila con plan y horas; reembolso, monto original y retención con su porcentaje (en una compensación automática no hay fila de retención).
  - **Placa del total** (fondo asfalto, borde del color de acento): "Total pagado" con su equivalente en Bs, o "Monto devuelto" con su equivalente referencial en Bs.
  - **Pie a todo el ancho de la hoja**, sobre fondo gris claro y debajo de la franja, con la leyenda legal completa: **"Documento sin validez fiscal."** Este comprobante no es una factura, no lleva NIT y no sustituye ningún documento tributario; es la constancia de una operación realizada en DriveHub entre el estudiante y su escuela, con los datos fijados al momento de la operación (un cambio posterior del plan o del tipo de cambio no lo altera); debe conservarse y citarse por su número. **Sin firma, despedida ni otro texto.**
  - Todo en español, con formato de números boliviano (`1.250,00`) y las tildes y la ñ bien dibujadas.
- `ComprobantePdfGenerador` (`@Component`): compila la plantilla **una vez al arrancar** (no en cada comprobante) y `byte[] generar(DatosComprobante)` la llena y exporta a PDF. Un nombre largo se corta en varias líneas sin desbordar; un dato vacío se muestra como "—". Una falla de Jasper se relanza como `IllegalStateException` (la atrapa el service de la UT 04).
- Tests (`ComprobantePdfGeneradorTest`, extrayendo el texto del PDF con PDFBox): empieza con `%PDF`; contiene número, escuela, estudiante, plan, horas y los tres montos; el de reembolso suma original, retención y devuelto, y el de compra no; contiene la leyenda legal completa y nada más después de ella (sin "Buen camino" ni firma); el de reembolso muestra el motivo, el número del comprobante original y el monto devuelto en Bs, y uno con motivo "Otro" muestra el texto escrito; el de una compensación no muestra la fila de retención; tildes y ñ ("Peña Núñez", "Educación") salen bien; un nombre de 160 caracteres y un motivo de 200 no rompen el diseño; es A4 de una página; dos llamadas seguidas no vuelven a compilar la plantilla.

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

Dados los datos de una compra o de un reembolso, se obtiene un PDF A4 numerado, con la marca de DriveHub, los montos y la leyenda de que no tiene validez fiscal.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Un test manual escribe el PDF de ejemplo en `target/` y se revisa a ojo que el diseño sea el de la marca, sin texto cortado.
