# Unidades de trabajo — CU08a-MOB

**Caso de uso:** Perfil de la persona en la app: un selector de ubicación en mapa, el registro del estudiante con CI, sexo y ubicación, y el perfil propio con esos datos y la categoría de solo lectura.
**Plataforma:** app móvil (Flutter), `projects/mobile`
**Fuente CU:** [`CU08a - Perfil de la persona`](../../../CasosDeUso/CU08a%20-%20Perfil%20de%20la%20persona.md)
**Recurso HTTP / contrato:** los endpoints cambiados de [CU08a-BE](../UT-BACKEND/README.md#contrato-de-datos--recurso-http) (`POST /usuarios`, `GET` y `PATCH /usuarios/me`)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (DTOs en `models/`, solo el `service` los arma), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (validador nativo, reglas iguales a las del backend y a las de la web), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit, legibilidad, widgets compartidos), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El estudiante se registra desde la app**, así que aquí está el mapa más importante. Se usa **`flutter_map`** con mosaicos de OpenStreetMap (con atribución visible) y `latlong2`; para "Mi ubicación", `geolocator`. Las versiones se fijan en `pubspec.yaml`.
2. **Un solo widget de mapa compartido**, `SelectorUbicacion` en `lib/shared/widgets/`, porque lo usan dos features (`auth` y `perfil`), como pide WIDGETS_UI_MOBILE.md. Emite latitud y longitud y **no** calcula la dirección (la calcula el servidor).
3. **Las reglas de CI, sexo y coordenadas** son las del backend y la web (CI de 5 a 15 caracteres `[A-Za-z0-9-]`, sexo masculino o femenino, coordenadas en rango y juntas).
4. **Permisos de ubicación:** si la persona los niega, el mapa sigue funcionando por toque; no se la bloquea. Android e iOS declaran el permiso en sus manifiestos (UT 01).
5. **Categoría:** el estudiante la ve en su perfil, de solo lectura; no hay forma de editarla desde la app.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU08a-MOB-01](./UT-CU08a-MOB-01.md) | `SelectorUbicacion` (mapa) | — |
| [UT-CU08a-MOB-02](./UT-CU08a-MOB-02.md) | Registro con CI, sexo y ubicación | 01 |
| [UT-CU08a-MOB-03](./UT-CU08a-MOB-03.md) | Perfil con CI, sexo, ubicación y categoría | 01 |

**Cadena:** `01 → 02`; `01 → 03`. La 02 y la 03 son independientes.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Registro desde la app con los datos nuevos | 02 |
| F1, CA03, CA04 | Elegir el punto en el mapa | 01, 02, 03 |
| F2, CA06 | Editar el perfil, categoría solo lectura | 03 |
| A1, A2, CA02, CA05 | Validación en el cliente y `ERR_IDE_19` bajo el campo CI | 02, 03 |

CA07 a CA12 son de backend o web (el alta del Administrador no existe en la app).

## Follow-ups (no se implementan en estas UT)

- Buscar un lugar por texto en el mapa.
- Mapas sin conexión.
- Pedir el permiso de ubicación en el primer arranque.

## Qué NO incluyen estas UT

- El backend ([CU08a-BE](../UT-BACKEND/README.md)) ni la web ([CU08a-FE](../UT-FRONTEND/README.md)).
- El alta del Administrador ni de estudiantes por el Administrador: la app no es para administradores.
- Foto, contraseña y correo del perfil.

## Commits sugeridos

Un commit `[MOBILE]` por UT o uno solo para todo el caso; `docs/` es `[DOCS]`.
