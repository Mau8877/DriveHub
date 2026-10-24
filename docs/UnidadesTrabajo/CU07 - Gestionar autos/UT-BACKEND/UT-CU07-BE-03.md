# UT-CU07-BE-03 — Casos de uso: registrar, listar y obtener

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-03` |
| **Nombre** | Casos de uso: registrar, listar y obtener |
| **Caso de uso** | CU07-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU07-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El Administrador necesita dar de alta sus autos y verlos, sin tocar los de otras escuelas.

## Objetivo

Tres casos de uso con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`.

## Alcance

- DTOs: `RegistrarVehiculoInput`, `ListarVehiculosInput`, `VehiculoOutput` (reusa `PaginaOutput`).
- `ReglasVehiculo` (paquete-privada): validaciones compartidas con editar (placa `^[A-Z0-9-]{5,10}$`, marca y modelo hasta 40, color hasta 20, año de 1980 al próximo, km >= 0) y el mapeo a `VehiculoOutput`.
- `RegistrarVehiculoUseCase`: normaliza la placa a mayúsculas, valida, rechaza placa en uso (`ERR_ESC_08`), guarda `DISPONIBLE` con km 0 por defecto y registra `INFO`.
- `ListarVehiculosUseCase`: página 1 y 20 por página por defecto (máximo 100), sin log por lectura.
- `ObtenerVehiculoUseCase`: 404 `ERR_ESC_09` si no existe en la escuela.

Archivos esperados (crear/tocar): los tres casos de uso, `ReglasVehiculo`, los tres DTOs y sus tests (`RegistrarVehiculoUseCaseTest`, `ListarYObtenerVehiculosUseCaseTest`).

## Fuera de alcance (no tocar)

- Editar y cambiar de estado (UT-04).
- Leer la escuela del token: lo hace el controller (UT-05); el caso de uso la recibe en el input.

## Resultado esperado

Registrar, listar y obtener funcionan con el repositorio mockeado y rechazan todo dato inválido antes de tocarlo.

## Validación

1. `./mvnw test -Dtest='RegistrarVehiculoUseCaseTest,ListarYObtenerVehiculosUseCaseTest'` en verde.
2. Una placa en minúsculas queda guardada en mayúsculas y un dato inválido no llega al repositorio.
