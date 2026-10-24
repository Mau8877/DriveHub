# Unidades de trabajo — CU08a-FE

**Caso de uso:** Perfil de la persona en la web: un selector de ubicación en mapa, el registro del estudiante con CI, sexo y ubicación, el perfil propio con esos datos y el alta de escuela con el perfil completo del Administrador.
**Micro-frontend(s):** `@drivehub/ui` (UT 01), `@drivehub/gestion` (UT 02), `identidad` (UT 03 y 04) y `escuela` (UT 05) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU08a - Perfil de la persona`](../../../CasosDeUso/CU08a%20-%20Perfil%20de%20la%20persona.md)
**Recurso HTTP / contrato:** pantallas `/register/:escuelaSlug` y el perfil (`identidad`), y `/escuelas` (`escuela`); consumen los endpoints cambiados de [CU08a-BE](../UT-BACKEND/README.md#contrato-de-datos--recurso-http)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md), [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. El backend ([CU08a-BE](../UT-BACKEND/README.md)) hace falta solo para las validaciones manuales.

## Decisiones de diseño

1. **Un solo selector de mapa para toda la web:** el Web Component `<dh-mapa-ubicacion>` en `@drivehub/ui` (Lit, JS plano + `.d.ts`, como el resto del paquete), con **Leaflet** y mosaicos de **OpenStreetMap**. Lo usan el registro, el perfil y los formularios de `<dh-gestion>`. Ninguna app trae su propio mapa.
2. **El mapa solo elige un punto.** Emite latitud y longitud; **no calcula la dirección** (la calcula el servidor al guardar). Muestra, de solo lectura, la dirección ya guardada si se la pasan. Las reglas de OSM se cumplen: atribución visible, sin precargar mosaicos y con `User-Agent`/`Referer` normales del navegador.
3. **`<dh-gestion>` gana dos tipos de campo:** `seleccion` (lista de opciones fijas del esquema o traídas de un recurso de la API) y `ubicacion` (usa `<dh-mapa-ubicacion>` y escribe dos valores del cuerpo, `latitud` y `longitud`). Son capacidades genéricas, no de estudiantes: las usa primero el esquema de escuelas (UT 05) y después CU08.
4. **Las reglas de validación de CI, sexo y coordenadas son iguales** a las del backend y a las de mobile (CI de 5 a 15 caracteres `[A-Za-z0-9-]`, sexo masculino o femenino, coordenadas en rango y juntas). Se repiten en `identidad` (por la regla de que ninguna app importa código de otra) con un comentario que lo explica.
5. **Registro (`identidad`)** sigue siendo la página pública `/register/:escuelaSlug`; sin menú. **Perfil (`identidad`)** es la pantalla de CU06; muestra la categoría del estudiante de solo lectura.
6. **No hay UT de `shell`:** el menú, la sesión y el login no cambian (la respuesta de login conserva su forma).

## Rutas y pantallas

| Ruta | Pantalla | Quién |
|------|----------|-------|
| `/register/:escuelaSlug` | Registro con CI, sexo y mapa | Pública |
| Perfil (menú del avatar) | Perfil con CI, sexo, ubicación y categoría (solo lectura) | Toda persona con sesión |
| `/escuelas` | "Nueva escuela" con el perfil completo del Administrador | `SUPER_ADMIN` |

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU08a-FE-01](./UT-CU08a-FE-01.md) | `<dh-mapa-ubicacion>` | `ui` | — |
| [UT-CU08a-FE-02](./UT-CU08a-FE-02.md) | Campos `seleccion` y `ubicacion` en `<dh-gestion>` | `gestion` | 01 |
| [UT-CU08a-FE-03](./UT-CU08a-FE-03.md) | Registro con CI, sexo y ubicación | `identidad` | 01 |
| [UT-CU08a-FE-04](./UT-CU08a-FE-04.md) | Perfil con CI, sexo, ubicación y categoría | `identidad` | 01 |
| [UT-CU08a-FE-05](./UT-CU08a-FE-05.md) | Esquema de escuelas con el perfil del Administrador | `escuela` | 02 |

**Cadena:** `01 → 02 → 05`; `01 → 03`; `01 → 04`. La 03 y la 04 no dependen entre sí.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Registro con los datos nuevos | 03 |
| F1, A4, CA03, CA04 | Elegir el punto en el mapa | 01, 02, 03, 04, 05 |
| F2, CA06 | Editar el perfil, categoría solo lectura | 04 |
| F3, CA07, CA08 | Alta del Administrador con perfil | 02, 05 |
| A1, A2, CA02, CA05 | Validación en el cliente y `ERR_IDE_19` bajo el campo CI | 03, 04, 05 |

CA09 a CA12 son de backend.

## Follow-ups (no se implementan en estas UT)

- Buscar un lugar por texto en el mapa (geocodificación directa).
- Mostrar el mapa con el punto en el detalle de un estudiante (CU08).
- Servidor de mosaicos propio si el uso de OSM crece.

## Qué NO incluyen estas UT

- El backend ([CU08a-BE](../UT-BACKEND/README.md)) ni la app móvil ([CU08a-MOB](../UT-MOBILE/README.md)).
- Pantallas de estudiantes del Administrador: es [CU08](../../CU08%20-%20Gestionar%20estudiantes/UT-FRONTEND/README.md).
- Cambios en el shell, en los remotos React ni en `pagos`.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend o paquete: `ui`, `gestion`, `identidad` y `escuela`. `docs/` es `[DOCS]`.
