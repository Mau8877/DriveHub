# UT-CU09-FE-04 — Pantalla de gestión de instructores (esquema)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-04` |
| **Nombre** | Pantalla de gestión de instructores (esquema) |
| **Caso de uso** | CU09-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU09-FE-02, UT-CU09-FE-03 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (carpetas iguales en React y Angular), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md). Como modelo directo, `features/estudiantes/` del mismo remoto: su `schemas/estudiantes.esquema.ts`, su pantalla y su prueba.

## Solicitud de negocio

El Administrador gestiona los instructores de su escuela con la misma pantalla que ya conoce de estudiantes.

## Objetivo

Existe la pantalla "Instructores" completa: listado con búsqueda y filtro, detalle, alta y edición.

## Alcance

- `projects/frontend/escuela/src/app/features/instructores/`:
  - `schemas/instructores.esquema.ts`: `EsquemaGestion` con `recurso: 'instructores'`, título "Instructores", descripción y texto de creación ("Nuevo instructor").
    - **Columnas**: `foto` (nombre completo como `nombre`), `nombreCompleto` (principal), `nroLicencia` (`codigo`), `licenciaVence` (fecha), `categorias` (`etiquetas`, `etiqueta: 'sigla'`), `estado` (insignia: `ACTIVO` éxito, `LICENCIA` ámbar, `INACTIVO` neutro, con textos "Activo", "Licencia", "Inactivo").
    - **Búsqueda** por nombre. **Filtro** `estado` con las tres opciones (control segmentado con "Todos").
    - **Detalle** de solo lectura con secciones: datos personales (foto, nombre, edad, fecha de nacimiento, sexo, CI, correo, teléfono), ubicación (dirección y coordenadas) y ficha profesional (licencia, vencimiento, contratación, categorías con `etiquetas`, estado, fecha de alta).
    - **Crear** (`POST`), secciones numeradas: *Datos personales* (nombres y apellidos en mitad, fecha de nacimiento con `fechaNoFutura` y `edadMinima: 18`, sexo, CI, teléfono opcional), *Acceso* (correo y contraseña con las mismas reglas de estudiantes), *Ubicación* (el campo `ubicacion` de estudiantes con la misma configuración, centrado en Santa Cruz de la Sierra) y *Ficha profesional* (número de licencia: obligatorio, 3 a 30, patrón `[A-Za-z0-9-]`, se muestra en mayúsculas; vencimiento con `fechaFutura`; contratación con `porDefecto: 'hoy'` y `hastaHoy` (hoy es válido); categorías: `seleccion` `multiple`, `opcionesDe` el catálogo de categorías de licencia, `valorDe: 'sigla'`, `excluir: ['SIN']`, `requerido`).
    - **Editar** (`PATCH`): editables solo *Ficha profesional* (el vencimiento con `soloSiCambia`; el estado como `seleccion` con las tres opciones) y *Ubicación*; los datos personales y el correo se muestran como **solo lectura** (nombre completo, CI, correo). No se envían campos de solo lectura.
    - **Acciones por fila**: "Ver detalles" y "Editar". Sin eliminar.
    - **`erroresDeCampo`**: `ERR_IDE_01` → `email`, `ERR_IDE_19` → `ci`, `ERR_IDE_02` → `fechaNacimiento`, `ERR_ESC_12` → `nroLicencia`, `ERR_ESC_14` → `categorias`. Mensajes en español, iguales a los del backend.
  - `screens/instructores-gestion.ts`: componente delgado con `CUSTOM_ELEMENTS_SCHEMA`, `import '@drivehub/gestion'` y `<dh-gestion [esquema]="esquema">`, igual que `estudiantes-gestion.ts`.
  - `tests/instructores.esquema.spec.ts`: recurso; columnas y tipos; filtro de estado; acciones; `erroresDeCampo`; reglas con `validar` de `@drivehub/gestion` (licencia vacía, de 2 y de 31 caracteres, con símbolo; vencimiento hoy rechazado y mañana aceptado; contratación futura rechazada; categorías vacías; menor de 18); que `SIN` esté excluida; que la edición no incluya datos personales como editables; que el vencimiento use `soloSiCambia` en la edición.
- Las reglas de validación coinciden con las del backend.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/instructores/schemas/instructores.esquema.ts`
- `projects/frontend/escuela/src/app/features/instructores/screens/instructores-gestion.ts`
- `projects/frontend/escuela/src/app/features/instructores/tests/instructores.esquema.spec.ts`

## Fuera de alcance (no tocar)

- La ruta del remoto (UT 05) y el menú (UT 06).
- Cambios en `@drivehub/gestion` o `@drivehub/ui` (UT 01 a 03): si falta algo, se vuelve a esas UT.
- La pantalla de estudiantes.
- Asignaciones de autos o alumnos.

## Resultado esperado

Con la ruta de la UT 05, el Administrador lista, busca, filtra, ve, crea y edita instructores.

## Validación

1. `pnpm --dir projects/frontend/escuela test`, `lint` y `build` sin errores.
2. Con el stack levantado (UT 05 y backend hechos): crear un instructor con dos categorías lo muestra en la tabla con sus etiquetas; repetir la licencia marca el error bajo el campo; editar permite cambiar el estado y no los datos personales.
