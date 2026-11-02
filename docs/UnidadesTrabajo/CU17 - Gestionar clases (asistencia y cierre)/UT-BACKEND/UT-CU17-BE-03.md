# UT-CU17-BE-03 — Errores `ERR_ACA_37` y `ERR_ACA_38`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-03` |
| **Nombre** | Errores `ERR_ACA_37` y `ERR_ACA_38` |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error) (todo error visible está catalogado y el número no se reutiliza) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (formato obligatorio de `docs/errors/` y su índice). Tomar como modelo UT-CU16-BE-05. Decisiones 5 y 11 del [README](./README.md).

## Solicitud de negocio

Cerrar una clase puede fallar de dos maneras nuevas que el instructor tiene que entender: la clase no está en condiciones de cerrarse, o los datos que escribió no son válidos.

## Objetivo

Los códigos de error de CU17 están en el catálogo del servicio y documentados, y el de «no existe» describe también las clases dictadas.

## Alcance

- `AcademicoErrorCodes` (constantes, siguiente libre `ERR_ACA_37`):
  - `CLASE_NO_CERRABLE = "ERR_ACA_37"`: la reserva no está `CONFIRMADA` o todavía no empezó, o cambió mientras se cerraba (`409`, `ConflictException`).
  - `DATOS_DE_CLASE_INVALIDOS = "ERR_ACA_38"`: kilometraje final menor que el inicial, o tema o kilómetros en una clase `AUSENTE` (`400`, `ValidationException`).
- `docs/errors/academico/ERRORES_ACADEMICO.md`: una sección por código nuevo con el formato obligatorio (HTTP, excepción, significado, dónde se lanza: `ClaseService.cerrar()` y `ClaseService.actualizar()` para el `38`; `CierreDeReservaService.buscarCerrable()`, `ClaseService.cerrar()` para el `37`). En la sección de **`ERR_ACA_35`**, ampliar el significado: además de la clase que el instructor quiso cancelar (CU16), es la reserva o la clase dictada que quiso cerrar, ver o corregir (CU17).
- `docs/errors/README.md`: los links a las dos secciones nuevas, en la sección de Académico.
- Sin test propio: son constantes. Los tests de las UT 05 a 07 las verifican al lanzarlas.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Lanzar los errores (UT 05 a 07).
- Los códigos existentes (`ERR_ACA_16`, `ERR_ACA_17`, `ERR_ACA_36`...) y los números ya usados.

## Resultado esperado

Los dos códigos existen y quien los vea en un log o en la API puede llegar en un clic a su explicación.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. `docs/errors/README.md` enlaza `ERR_ACA_37` y `ERR_ACA_38`, y cada link abre su sección.
3. La sección de `ERR_ACA_35` menciona la clase dictada de CU17.
