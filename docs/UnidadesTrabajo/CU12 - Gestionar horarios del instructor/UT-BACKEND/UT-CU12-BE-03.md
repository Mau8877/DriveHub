# UT-CU12-BE-03 — Catálogo `AcademicoErrorCodes` y su documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-03` |
| **Nombre** | Catálogo `AcademicoErrorCodes` y su documentación |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (formato obligatorio de cada sección y del índice). Tomar como modelo `EscuelaErrorCodes` y `docs/errors/escuela/ERRORES_ESCUELA.md`.

## Solicitud de negocio

Quien vea un código `ERR_ACA_*` en una respuesta o en un log tiene que poder encontrar qué significa sin leer el código.

## Objetivo

Los 10 códigos del CU12 existen como constantes del servicio y están documentados e indexados.

## Alcance

- `common/exception/AcademicoErrorCodes` (clase final con constructor privado, como `EscuelaErrorCodes`), con estas constantes:

  | Constante | Código |
  |-----------|--------|
  | `HORA_FIN_NO_POSTERIOR` | `ERR_ACA_01` |
  | `DURACION_INVALIDA` | `ERR_ACA_02` |
  | `VIGENCIA_INVALIDA` | `ERR_ACA_03` |
  | `HORARIO_SE_CRUZA` | `ERR_ACA_04` |
  | `HORARIO_NO_ENCONTRADO` | `ERR_ACA_05` |
  | `HORARIO_YA_INACTIVO` | `ERR_ACA_06` |
  | `FRANJA_NO_ENCONTRADA` | `ERR_ACA_07` |
  | `FRANJA_NO_ESTA_LIBRE` | `ERR_ACA_08` |
  | `FRANJA_NO_ESTA_BLOQUEADA` | `ERR_ACA_09` |
  | `FRANJA_YA_EMPEZO` | `ERR_ACA_10` |

  Más `VALIDACION = "ERR_SYS_01"`, igual que en Escuela, para las validaciones de negocio que no cubren las anotaciones del DTO.
- `docs/errors/academico/ERRORES_ACADEMICO.md` con una sección por código (HTTP, Excepción, Significado, Dónde se lanza) según la tabla de contrato del [README](./README.md#errores-nuevos-err_aca_). En **Dónde se lanza**: `PlantillaHorarioService.crear()` (01 a 04), `PlantillaHorarioService.desactivar()` (05, 06), `FranjaService.bloquear()` / `desbloquear()` (07 a 10). El 05 también aplica a `desactivar()`; el 07 a ambos métodos de la franja.
- `docs/errors/README.md`: pasar la fila `ACA` de la tabla de prefijos de "(sin errores todavía)" a un link a la carpeta, y agregar la sección `### Académico (ACA)` con los 10 links a su ancla (`academico/ERRORES_ACADEMICO.md#err_aca_01`...).
- Sin test: son constantes y documentación.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Lanzar las excepciones (UT 07 a 09).
- Códigos de otros casos de uso de Académico: cada CU agrega los suyos, con el siguiente número libre (`ERR_ACA_11` en adelante).
- Los códigos `ERR_SYS_*`: ya están documentados.

## Resultado esperado

Existe el catálogo de errores de Académico con sus 10 códigos del CU12, documentados e indexados.

## Validación

1. `./mvnw checkstyle:check` en `academico` sin errores.
2. Cada uno de los 10 links de la sección "Académico (ACA)" de `docs/errors/README.md` abre su sección en `ERRORES_ACADEMICO.md`.
