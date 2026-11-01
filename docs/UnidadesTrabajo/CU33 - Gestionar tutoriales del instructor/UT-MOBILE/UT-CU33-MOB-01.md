# UT-CU33-MOB-01 — Datos de tutoriales, visor de PDF y reproductor de video

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU33-MOB-01` |
| **Nombre** | Datos de tutoriales, visor de PDF y reproductor de video |
| **Caso de uso** | CU33-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU33-BE-02 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

El instructor y quien lo consulta ven los PDF y los videos sin salir de la app.

## Objetivo

Existen los modelos, el servicio y el estado de tutoriales, el visor de PDF y el reproductor de video, y los manuales abren su PDF en el visor.

## Alcance

- Dependencias `file_picker`, `pdfrx` y `youtube_player_iframe`.
- `features/tutoriales/`: `Tutorial` (con `TipoTutorial`, `EstadoTutorial`, `AudienciaTutorial`), `TutorialRequest` y `ArchivoSubido`, `idDeYoutube`; `TutorialesService`; `tutorialesServiceProvider`, `misTutorialesFiltroUiProvider`, `misTutorialesListProvider`, `tutorialAccionesProvider`, `selectorDePdfProvider`; las validaciones del formulario.
- `shared/widgets/visor_pdf_screen.dart` (`VisorPdfScreen`, `VisorPdfArgs`) y `features/tutoriales/screens/reproductor_video_screen.dart`; rutas `/pdf` y `/video`.
- Manuales (CU34): «Abrir PDF» abre el visor de la app en lugar de bajar el archivo y usar el visor del teléfono; se quita la descarga de `ManualesService`.
- Tests: `test/features/tutoriales/tutoriales_test.dart` (validaciones, `TutorialRequest`, `TutorialesService`, reproductor) y `manuales_test.dart` ajustado.

Archivos esperados (crear/tocar): `pubspec.yaml`/`pubspec.lock`, los de `lib/features/tutoriales/{models,services,providers,validators}`, el visor y el reproductor, `app_router.dart`, los de `lib/features/manuales` y los tests.

## Fuera de alcance (no tocar)

- Las pantallas de «Mis tutoriales», el formulario y la biblioteca (UT siguientes y CU35); los comprobantes de pago, que siguen bajando su PDF con el token.

## Resultado esperado

Un PDF se lee dentro de la app y un enlace de YouTube se reproduce dentro de la app.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: «Abrir PDF» de un manual abre el visor de la app.
