# UT-CU08-FE-02 — Esquema y pantalla "Gestionar estudiantes"

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-02` |
| **Nombre** | Esquema y pantalla "Gestionar estudiantes" |
| **Caso de uso** | CU08-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU08-FE-01, UT-CU08a-FE-02 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (el esquema, la tabla, la pantalla delgada y su test), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (carpetas de una feature). Como modelo, `features/escuelas/` del mismo remoto.

## Solicitud de negocio

El Administrador necesita una pantalla para encontrar a sus estudiantes, ver sus datos, darlos de alta y corregir su categoría y ubicación.

## Objetivo

La feature `estudiantes` del remoto `escuela` tiene su esquema y su pantalla, probados.

## Alcance

- `features/estudiantes/schemas/estudiantes.esquema.ts`: `ESQUEMA_ESTUDIANTES: EsquemaGestion` con:
  - `recurso: 'estudiantes'`, `titulo: 'Gestionar estudiantes'`, `descripcion: 'Estudiantes de tu escuela.'`, `nombreSingular: 'estudiante'`.
  - Columnas: `nombreCompleto` (`principal`, `icono: 'user'`, "Nombre"), `edad` ("Edad"), `categoriaSigla` ("Categoría", `codigo: true`), `fechaIncorporacion` (`tipo: 'fecha'`, "Incorporación") y `estado` (`tipo: 'insignia'`; `ACTIVO` → "Activo" tono `exito`, `INACTIVO` → "Inactivo").
  - `busqueda: { placeholder: 'Buscar por nombre' }` y filtro `filter[estado]` con "Estado", `todas: 'Todos'` y opciones `ACTIVO` "Activos" / `INACTIVO` "Inactivos".
  - `detalle` (título "Datos del estudiante"): secciones con `nombreCompleto`, `edad`, `fechaNacimiento` (fecha), `sexo` (insignia: "Masculino"/"Femenino"), `ci`, `email`, `telefono`, `direccion`, `categoriaSigla`, `estado` (insignia como arriba) y `fechaIncorporacion` (fecha), con `mitad` donde los pares quepan.
  - `crear` ("Nuevo estudiante", `etiquetaEnviar: 'Crear estudiante'`, mensaje "Estudiante creado correctamente."), secciones: *Datos personales* (`nombres` y `apellidos` requeridos hasta 80, `mitad`; `fechaNacimiento` requerida con `fechaNoFutura` y `edadMinima:18`, `mitad`; `sexo` `seleccion` con "Masculino"/"Femenino", requerido, `mitad`; `ci` requerido de 5 a 15 con patrón `[A-Za-z0-9-]+`, `mitad`; `telefono` hasta 20); *Acceso* (`email` requerido hasta 120; `password` requerida de 9 a 64 con la ayuda "La escribes tú y se la comunicas al estudiante"); *Ubicación* (`ubicacion` requerida, escribe `latitud` y `longitud`); *Licencia* (`categoriaSigla` `seleccion` con `opcionesDe: { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion' }` y `porDefecto: 'SIN'`). Mensajes en español iguales a los de registro y perfil.
  - `editar` (`metodo: 'PATCH'`, "Editar estudiante"): `cargar` y `ruta` por defecto (`{id}`); campos `nombreCompleto`, `ci` y `email` en `soloLectura`; `categoriaSigla` (misma `seleccion`) y `ubicacion` (con `direccionDe: 'direccion'`) editables. Solo estos dos viajan en el `PATCH`.
  - `erroresDeCampo`: `ERR_IDE_01` → `email` ("Este correo ya está registrado"), `ERR_IDE_02` → `fechaNacimiento` ("El estudiante debe tener al menos 18 años"), `ERR_IDE_19` → `ci` ("Este CI ya está registrado") y `ERR_IDE_21` → `categoriaSigla` ("La categoría ya no existe").
  - `mensajes`: `vacio: 'Todavía no hay estudiantes en tu escuela.'`, `sinResultados: 'Sin resultados.'`, `errorGuardar: 'No pudimos guardar el estudiante. Inténtalo de nuevo más tarde.'`.
  - **Sin** `acciones` ni columna o filtro de saldo (comentario en el archivo: Pagos pendiente, ver el CU). Un comentario recuerda que las reglas de validación del alta tienen que seguir iguales a las del backend, la web de registro y mobile.
- `features/estudiantes/screens/estudiantes-gestion.ts`: componente `EstudiantesGestion` (selector `app-estudiantes-gestion`, `CUSTOM_ELEMENTS_SCHEMA`, `import '@drivehub/gestion'`) que solo pone `<dh-gestion [esquema]="esquema">`. Igual que `EscuelasGestion`.
- Tests en `features/estudiantes/tests/`:
  - `estudiantes.esquema.spec.ts`: recurso, columnas y sus campos exactos (los del contrato de backend), filtro con las dos opciones, búsqueda, `detalle` con sus campos, campos del `crear` con sus reglas (con `validar` de `@drivehub/gestion`: CI de 4 caracteres o con espacios falla, contraseña de 8 falla, menor de 18 falla), `editar` con `soloLectura` en los datos personales y editables solo `categoriaSigla` y la ubicación, `erroresDeCampo` y que **no** haya `acciones` ni nada de saldo.
  - `estudiantes-gestion.spec.ts`: la pantalla renderiza `<dh-gestion>` con el esquema.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/estudiantes/schemas/estudiantes.esquema.ts`
- `projects/frontend/escuela/src/app/features/estudiantes/screens/estudiantes-gestion.ts`
- `projects/frontend/escuela/src/app/features/estudiantes/tests/estudiantes.esquema.spec.ts`
- `projects/frontend/escuela/src/app/features/estudiantes/tests/estudiantes-gestion.spec.ts`

## Fuera de alcance (no tocar)

- La ruta y el menú (UT 03 y 04).
- Cambios en `<dh-gestion>` (UT 01 y CU08a-FE-02) o en `features/escuelas/`.
- Llamar a la API a mano, tipos o servicios propios: lo hace el componente.
- Estilos propios: solo tokens y clases de `@drivehub/ui`.
- Activar/desactivar un estudiante.

## Resultado esperado

Existe la pantalla de estudiantes lista para montarse en una ruta.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
