# UT-CU22-BE-01 — Modelos, cliente de estudiantes y códigos de error

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-01` |
| **Nombre** | Modelos, cliente de estudiantes y códigos de error |
| **Caso de uso** | CU22-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Cada escuela programa sus exámenes y ninguna ve los de otra; el listado muestra el nombre del estudiante.

## Objetivo

Los modelos pertenecen a una escuela, existe el cliente que trae nombres de estudiantes y los códigos de error del módulo.

## Alcance

- `ExamenConduccion`: `escuelaCodigo`, `creadoPorCodigo`, setters y el índice compuesto.
- `Resultado`: `escuelaCodigo` y setters (lo escribe CU22 y lo leerá CU23).
- `IdentidadClient.nombresDeEstudiantes(codigos, bearer)` sobre `POST /api/v1/estudiantes/consulta`, en lotes, con 503 `ERR_ACA_17` ante cualquier falla.
- `AcademicoErrorCodes`: `EXAMEN_NO_ENCONTRADO` (`ERR_ACA_24`), `EXAMEN_NO_ESTA_PROGRAMADO` (`ERR_ACA_25`), `EXAMEN_YA_PROGRAMADO` (`ERR_ACA_26`), `ESTUDIANTE_NO_ENCONTRADO` (`ERR_ACA_27`).

Archivos esperados (crear/tocar): `ExamenConduccion.java`, `Resultado.java`, `IdentidadClient.java`, `AcademicoErrorCodes.java`.

## Fuera de alcance (no tocar)

- Consultas de resultados (CU23) y `IntentoSimulacro` (CU21).

## Resultado esperado

El servicio compila y arranca con los índices nuevos.

## Validación

1. `./mvnw test` en `academico` (incluye `IdentidadClientTest`).
2. `./mvnw checkstyle:check` sin errores.
