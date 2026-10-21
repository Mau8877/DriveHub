# UT-CU05-FE-03 — Base de la feature `escuelas`: tipos, reglas y servicio

> **Reemplazada** por el piloto de `<dh-gestion>` (ver [README, decisión 4](./README.md)): la feature `escuelas` quedó como esquema + pantalla + ruta única. Se conserva como historia.

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-03` |
| **Nombre** | Base de la feature `escuelas`: tipos, reglas y servicio |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Y las reglas de formularios en [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (mismas reglas que web y mobile).

## Solicitud de negocio

Las tres pantallas de gestión comparten los datos, las validaciones y las llamadas a la API.

## Objetivo

La feature `escuelas` del remoto `escuela` tiene sus tipos, sus reglas de validación y su servicio HTTP, probados.

## Alcance

- Carpetas por la convención de feature (ver ARQUITECTURA_FRONTEND.md): `types/`, `schemas/`, `api/`, `tests/`.
- `types/escuela.types.ts`: `EstadoEscuela` (`'ACTIVA' | 'INACTIVA'`), `EscuelaListada` (`codigo`, `slug`, `nombre`, `estado`, `creadoEn`), `EscuelaEstado` (`codigo`, `slug`, `nombre`, `activa`), `AdministradorRequest`, `CrearEscuelaRequest` (`nombre`, `slug`, `administrador`), `EscuelaCreada`, `FiltroEscuelas` (`page`, `pageSize`, `search`, `estado`) y `PaginaDeEscuelas` (`data` y `meta` como los entrega el `apiInterceptor`).
- `schemas/escuela.reglas.ts`: constantes (nombre 3 a 120, slug 3 a 60 con `^[a-z0-9]+(-[a-z0-9]+)*$`, y para el Administrador: edad mínima 18, contraseña de 9 a 64, nombres y apellidos hasta 80, correo hasta 120, teléfono hasta 20), `slugDesdeNombre(nombre)` (minúsculas, sin tildes, espacios y símbolos a guiones, sin guiones repetidos ni en los extremos) y los validadores (`obligatorioConTexto`, `slugValido`, `fechaNoFutura`, `edadMinima`). **Se duplican a propósito** las reglas del registro del estudiante de `identidad` (ninguna app importa código de otra) y llevan un comentario que lo explica: tienen que seguir iguales.
- `schemas/escuela.mensajes.ts`: mensajes **en español** por campo y tipo de error.
- `api/escuelas.service.ts` (`EscuelasService`, `providedIn: 'root'`, `inject(HttpClient)` con la URL de `apiBaseUrl()`): `listar(filtro)` (`GET /escuelas` con `page`, `pageSize`, `search` y `filter[estado]`), `obtener(codigo)` (`GET /escuelas/por-codigo/{codigo}`, que ya existe y devuelve `codigo`, `slug`, `nombre` y `activa` en cualquier estado), `crear(request)` (`POST /escuelas`), `editar(codigo, nombre)` (`PATCH /escuelas/{codigo}`), `desactivar(codigo)` (`DELETE`) y `reactivar(codigo)` (`POST .../reactivar`). Ningún componente llama a la API directo.
- Tests: `tests/escuelas.service.spec.ts` con `HttpTestingController` (URL, método, parámetros y cuerpo de cada llamada; el listado entrega `data` y `meta`); `tests/escuela.reglas.spec.ts` (`slugDesdeNombre` con tildes y símbolos, límites de cada validador, edad exacta de 18 años).

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/escuelas/types/escuela.types.ts`
- `projects/frontend/escuela/src/app/features/escuelas/schemas/escuela.reglas.ts`
- `projects/frontend/escuela/src/app/features/escuelas/schemas/escuela.mensajes.ts`
- `projects/frontend/escuela/src/app/features/escuelas/api/escuelas.service.ts`
- `projects/frontend/escuela/src/app/features/escuelas/tests/escuelas.service.spec.ts`
- `projects/frontend/escuela/src/app/features/escuelas/tests/escuela.reglas.spec.ts`

## Fuera de alcance (no tocar)

- Pantallas y rutas (UT 04 a 07).
- Importar código de `identidad` o del shell.
- Un `index.ts`: ninguna otra feature necesita algo de esta.

## Resultado esperado

La feature tiene todo lo que las pantallas necesitan, sin interfaz todavía.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
