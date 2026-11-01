# Unidades de trabajo — CU33-MOB

**Caso de uso:** Gestionar tutoriales del instructor: «Mis tutoriales» y el formulario de crear y editar, con el visor de PDF y el reproductor de video de la app.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU33 - Gestionar tutoriales del instructor.md`](../../../CasosDeUso/CU33%20-%20Gestionar%20tutoriales%20del%20instructor.md)
**Recurso HTTP / contrato:** `/tutoriales`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Decisiones de diseño

- **Visor de PDF y reproductor dentro de la app** (decisión del equipo): `pdfrx` para el PDF (`shared/widgets/visor_pdf_screen.dart`, ruta `/pdf`, porque lo usan manuales y tutoriales) y `youtube_player_iframe` para el video (`features/tutoriales`, ruta `/video`). Reciben lo que abren por `extra`; sin él vuelven al inicio. El selector de archivos es `file_picker`.
- **Entrada del instructor:** la barra inferior admite 5 destinos y ya están ocupados, así que «Tutoriales» es un acceso en su Inicio que abre `/tutorials` (solo Instructor: el redirect global saca a otro rol). La pantalla tiene dos pestañas: «Mis tutoriales» (esta UT) y «Biblioteca» ([CU35](../../CU35%20-%20Consultar%20tutoriales/UT-MOBILE/README.md)).
- **El PDF se sube al elegirlo**, antes de guardar, igual que en la web de manuales; al editar, el recurso solo se manda si cambió (el tutorial conserva el que tiene).
- **Selector de PDF como provider** (`selectorDePdfProvider`) para sustituirlo en los tests.
- **Acciones del instructor** en un `TutorialAcciones` (provider) que deja la lista al día; un error de negocio llega como `ApiException` y la pantalla muestra su mensaje.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU33-MOB-01](./UT-CU33-MOB-01.md) | Datos de tutoriales, visor de PDF y reproductor de video | BE-02 |
| [UT-CU33-MOB-02](./UT-CU33-MOB-02.md) | Mis tutoriales y formulario | MOB-01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, F3, F4, CA01, CA07 | Lista con filtro, publicar y archivar | 02 |
| F1, F2, A1 a A3, CA02 a CA06 | Formulario de crear y editar con PDF o enlace | 02 |
| A8, CA12 | Sin conexión; abrir el PDF o el video dentro de la app | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- «Cargar más» (hoy se trae la página 1 de 50).

## Qué NO incluyen estas UT

- La biblioteca de publicados (CU35) ni la web.
