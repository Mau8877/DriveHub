# Unidades de trabajo — CU32-BE

**Caso de uso:** Gestionar manuales: subir el PDF, crear, listar, editar, reemplazar el archivo, publicar y archivar, aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `contenido` (Laravel, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU32 - Gestionar manuales.md`](../../../CasosDeUso/CU32%20-%20Gestionar%20manuales.md)
**Recurso HTTP / contrato:** `/api/v1/manuales` (ya enrutado por el gateway a `contenido`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Contenido es el primer servicio no Spring que valida el JWT.** Hasta ahora ningún servicio Laravel/Nest/FastAPI validaba nada. Se agrega un middleware `jwt` (RS256 con el JWKS de Identidad, emisor `drivehub-identidad`) y otro `rol:` declarados en las rutas. Con `firebase/php-jwt` 7.x (la 6.x tiene un aviso de seguridad abierto y Composer la rechaza).
- **El JWKS se pide en cada petición**: la caché del servicio es `array` y `artisan serve` no guarda memoria entre peticiones. Es una llamada interna corta; si pesa, se cachea en un archivo o Redis (`ponytail:` en el código).
- **Filtro por tenant mínimo**, igual que en CU07: `escuela_codigo` en `manuales` y filtro explícito en el repositorio con la escuela del claim.
- **El PDF se sube primero y el manual lo referencia** (`POST /manuales/archivos` → descriptor). Así crear y editar son JSON simples. Costo aceptado: un PDF subido y no usado queda huérfano en el bucket (follow-up).
- **La dirección del archivo es pública** (el bucket `drivehub-contenido` es de lectura pública por diseño): el listado devuelve la URL y el navegador la abre directo, sin pasar por el servicio. `MINIO_URL` (interna, para subir) y `MINIO_PUBLIC_URL` (la del navegador) son distintas.
- **El JSON va en camelCase** (`tamanoBytes`) y el subdocumento `archivo` se guarda en snake_case (`tamano_bytes`), como dice el diseño de la colección; el controller traduce.
- **Búsqueda por expresión regular** sobre título, descripción y etiquetas, escapada, sin distinguir mayúsculas. Evita depender de `$text`, que no busca por prefijo ni por fragmento.
- **Los tests no necesitan MongoDB ni MinIO** (repositorio y almacén simulados, JWKS simulado con un par de claves generado en el test): el job `contenido` del CI no levanta bases.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `POST` | `/api/v1/manuales/archivos` (multipart, campo `archivo`) | 201 | `{ url, mime, tamanoBytes, sha256, nombre }` |
| `POST` | `/api/v1/manuales` | 201 | `ManualResource` |
| `GET` | `/api/v1/manuales?page&pageSize&search&filter[estado]&filter[categoria]` | 200 | `ManualResource[]` + `meta` |
| `GET` | `/api/v1/manuales/{codigo}` | 200 | `ManualResource` |
| `PATCH` | `/api/v1/manuales/{codigo}` | 200 | `ManualResource` |
| `POST` | `/api/v1/manuales/{codigo}/publicar` | 200 | `ManualResource` |
| `POST` | `/api/v1/manuales/{codigo}/archivar` | 200 | `ManualResource` |

Códigos de error nuevos: `ERR_CON_01` a `ERR_CON_04` ([docs/errors](../../../errors/contenido/ERRORES_CONTENIDO.md)).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU32-BE-01](./UT-CU32-BE-01.md) | Autenticación JWT y rol en Contenido | — |
| [UT-CU32-BE-02](./UT-CU32-BE-02.md) | Almacenamiento del PDF en MinIO | — |
| [UT-CU32-BE-03](./UT-CU32-BE-03.md) | Repositorio de manuales y migración por escuela | — |
| [UT-CU32-BE-04](./UT-CU32-BE-04.md) | Servicio de manuales | 02, 03 |
| [UT-CU32-BE-05](./UT-CU32-BE-05.md) | Controller, requests, errores y seed | 01, 04 |

**Cadena:** `01, 02, 03 → 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar con búsqueda, filtros y paginación | 03, 05 |
| F1 | Crear (subir PDF + manual) | 02, 04, 05 |
| F2 | Editar y reemplazar el PDF | 04, 05 |
| F3, F4 | Publicar y archivar | 04, 05 |
| A1 a A8 | Validación, archivo, 404, transición, rol, token sin escuela | 01, 02, 04, 05 |

## Follow-ups (no se implementan en estas UT)

- Limpieza de PDF huérfanos del bucket.
- Cachear el JWKS.
- Que `tutoriales` (CU33, CU35) y la consulta del estudiante (CU34) usen los mismos middleware.

## Qué NO incluyen estas UT

- CU33 a CU35.
- Cambios en el gateway: `manuales` ya está enrutado a `contenido`.
