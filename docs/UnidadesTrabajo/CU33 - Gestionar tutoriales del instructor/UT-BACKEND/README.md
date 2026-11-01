# Unidades de trabajo — CU33-BE

**Caso de uso:** Gestionar tutoriales del instructor: subir el PDF, crear, listar, editar, publicar y archivar los propios, aislado por escuela y por autor.
**Microservicio(s) / micro-frontend(s):** `contenido` (Laravel, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU33 - Gestionar tutoriales del instructor.md`](../../../CasosDeUso/CU33%20-%20Gestionar%20tutoriales%20del%20instructor.md)
**Recurso HTTP / contrato:** `/api/v1/tutoriales` (ya enrutado por el gateway a `contenido`)

## Antes de implementar cualquier UT

Leer [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Solo PDF y enlaces de YouTube.** El tipo `VIDEO` del diseño original (un video subido al bucket) queda reservado y se rechaza; los videos se enlazan. El enlace se valida con un patrón estricto (https; `youtube.com/watch?v=`, `/shorts/`, `/embed/` y `youtu.be/`, con un id de 11 caracteres), así no se guarda una dirección arbitraria que la app abriría.
- **`escuela_codigo` en la colección `tutoriales`** (decisión del equipo): la escuela sale del claim `escuela` del token, igual que en manuales. Dos índices nuevos por migración: `(escuela_codigo, autor_codigo, estado)` para el listado del instructor y `(escuela_codigo, estado, audiencia)` para la consulta.
- **El instructor solo toca lo suyo:** el service compara el `autor_codigo` con el `sub` del token; un tutorial ajeno responde igual que uno inexistente (`ERR_CON_05`).
- **Mismo patrón que CU32:** el PDF se sube primero (`POST /tutoriales/archivos`) y el tutorial lo referencia; el JSON va en camelCase y el subdocumento `recurso` se guarda en snake_case. Costo aceptado: un PDF subido y no usado queda huérfano.
- **Códigos de error nuevos** `ERR_CON_05` (tutorial no existe), `ERR_CON_06` (estado no permite el cambio) y `ERR_CON_07` (recurso inválido), documentados en `docs/errors/contenido/`.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `POST` | `/api/v1/tutoriales/archivos` (multipart, campo `archivo`) | 201 | `{ url, mime, tamanoBytes, sha256, nombre }` |
| `POST` | `/api/v1/tutoriales` | 201 | `TutorialResource` |
| `GET` | `/api/v1/tutoriales?page&pageSize&search&filter[estado]&filter[tipo]` | 200 | `TutorialResource[]` + `meta` |
| `GET` | `/api/v1/tutoriales/{codigo}` | 200 | `TutorialResource` |
| `PATCH` | `/api/v1/tutoriales/{codigo}` | 200 | `TutorialResource` |
| `POST` | `/api/v1/tutoriales/{codigo}/publicar` | 200 | `TutorialResource` |
| `POST` | `/api/v1/tutoriales/{codigo}/archivar` | 200 | `TutorialResource` |

`TutorialResource`: `{ codigo, titulo, descripcion, categoria, tipo, estado, audiencia[], etiquetas[], recurso{ url, nombre, mime, tamanoBytes }, creadoEn, actualizadoEn }`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU33-BE-01](./UT-CU33-BE-01.md) | Modelo por escuela, errores y recursos del tutorial (PDF y YouTube) | — |
| [UT-CU33-BE-02](./UT-CU33-BE-02.md) | Gestión de tutoriales del instructor | BE-01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| F1 y A2, A3, CA02 a CA04 | Subir el PDF, validar el recurso y crear | 01, 02 |
| Flujo principal, F2, F3, F4, CA01, CA06 a CA09 | Listar, editar, publicar y archivar los propios | 02 |
| A6, A7, CA10, CA11 | Rol Instructor y escuela del token | 02 |

## Follow-ups (no se implementan en estas UT)

- Limpieza de PDF huérfanos; borrar tutoriales; subir videos.

## Qué NO incluyen estas UT

- La consulta de tutoriales publicados ([CU35-BE](../../CU35%20-%20Consultar%20tutoriales/UT-BACKEND/README.md)) ni la app móvil.
