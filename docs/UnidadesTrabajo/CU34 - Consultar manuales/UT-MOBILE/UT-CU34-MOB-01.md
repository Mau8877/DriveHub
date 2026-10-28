# UT-CU34-MOB-01 — Pestaña «Manuales»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU34-MOB-01` |
| **Nombre** | Pestaña «Manuales» |
| **Caso de uso** | CU34-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU34-BE-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

El estudiante estudia con los manuales de su escuela desde el teléfono.

## Objetivo

La pestaña «Manuales» lista los manuales publicados con búsqueda y categoría, y abre su PDF.

## Alcance

- `core/archivos/abridor_de_pdf.dart`: `abridorDePdfProvider` y `SinVisorDePdf` (movidos desde `comprobante_provider.dart`, que los reexporta).
- `features/manuales/`: `Manual`; `ManualesService.publicados` y `descargarPdf`; `manualesServiceProvider`, `manualesFiltroUiProvider`, `manualesListProvider` y `manualDescargaProvider`; `ManualesScreen` (búsqueda, chips de categoría, tarjetas, estado vacío, reintento y aviso de errores).
- `features/estudio/screens/estudio_screen.dart`: la tercera pestaña.
- Tests: `test/features/manuales/manuales_test.dart` (servicio y pantalla); `resultados_test.dart` ajustado a las tres pestañas.

Archivos esperados (crear/tocar): los de `lib/features/{manuales,estudio}`, `core/archivos/abridor_de_pdf.dart`, `comprobante_provider.dart` y los tests.

## Fuera de alcance (no tocar)

- Las pantallas de simulacros y notas, el resto de las secciones y los tutoriales.

## Resultado esperado

El estudiante abre «Estudio > Manuales», filtra y toca «Abrir PDF» para leerlo.

## Validación

1. `flutter test` (el de manuales: 12 casos) y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: un manual publicado desde la web aparece en la pestaña y su PDF se abre.
