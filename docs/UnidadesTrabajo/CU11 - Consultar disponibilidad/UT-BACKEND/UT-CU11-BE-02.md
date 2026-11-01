# UT-CU11-BE-02 — Reglas de disponibilidad y autos vigentes por instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-02` |
| **Nombre** | Reglas de disponibilidad y autos vigentes por instructor |
| **Caso de uso** | CU11-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md). Como referencia, `ReglasVehiculo` y `AsignacionVehiculoRepositoryImpl.listar` de CU10.

## Solicitud de negocio

Las reglas que deciden si un instructor está disponible, qué estado tienen sus autos y qué es la mañana, la tarde o la noche tienen que estar en un solo lugar y probadas, y hay que saber qué autos tiene cada instructor.

## Objetivo

Existen los enums del dominio con las reglas del CU y una consulta del repositorio que devuelve los autos vigentes de un conjunto de instructores.

## Alcance

- `domain/entity/Disponibilidad` (`DISPONIBLE`, `COMPLETO`, `SIN_HORARIO`, `NO_DISPONIBLE`) con `static Disponibilidad de(EstadoInstructor estado, int totalFranjas, int libres)`: `NO_DISPONIBLE` si el estado no es `ACTIVO`; si es `ACTIVO`, `DISPONIBLE` con alguna libre, `COMPLETO` con franjas pero ninguna libre, `SIN_HORARIO` sin franjas. Un `orden()` para ordenar (disponible, completo, sin horario, no disponible).
- `domain/entity/EstadoAutos` (`DISPONIBLE`, `NO_DISPONIBLE`, `SIN_AUTO`) con `static EstadoAutos de(Collection<EstadoVehiculo> estados)`: `SIN_AUTO` sin autos; `DISPONIBLE` si alguno es `DISPONIBLE`; si no, `NO_DISPONIBLE`.
- `domain/entity/MomentoDelDia` (`MANANA`, `TARDE`, `NOCHE`) con `horaDesde()` (inclusiva) y `horaHasta()` (exclusiva) en `HH:mm`, nulas en los extremos: mañana hasta `12:00`, tarde de `12:00` a `18:00`, noche desde `18:00`.
- `AsignacionVehiculoRepository.vigentesDeInstructores(UUID escuelaCodigo, Collection<UUID> instructorUsuarioCodigos)`: las asignaciones vigentes (sin fecha de fin) de esos instructores, como `AsignacionDeAuto` (el auto, la asignación y el `instructorUsuarioCodigo`), ordenadas por placa. Una colección vacía devuelve vacío sin consultar. Todo filtrado por la escuela del auto y sin borrados.
- Su implementación en `AsignacionVehiculoRepositoryImpl` (JPQL con `entityManager`, como `listar`).
- Tests: `DisponibilidadTest`, `EstadoAutosTest` y `MomentoDelDiaTest` (unitarios, sin Spring: cada regla y sus bordes) y casos nuevos en `AsignacionVehiculoRepositoryImplTest` (Testcontainers): trae solo las vigentes, de esos instructores y de esa escuela; un instructor con dos autos; las cerradas no vienen.

Archivos esperados (crear/tocar): los tres enums, `AsignacionVehiculoRepository`, `AsignacionVehiculoRepositoryImpl` y los cuatro tests.

## Fuera de alcance (no tocar)

- `listar` e `historial` de CU10, y las asignaciones (`asignar`, `cambiar`, `liberar`).
- El cliente de Académico, el caso de uso y el controller (UT 03 a 05).
- Migraciones y modelos JPA: ya alcanzan.
- Validar la categoría del auto contra las del instructor.

## Resultado esperado

Las reglas de la disponibilidad y los momentos del día viven en el dominio con sus pruebas, y el repositorio entrega los autos de un grupo de instructores sin mezclar escuelas.

## Validación

1. `./mvnw test -Dtest='DisponibilidadTest,EstadoAutosTest,MomentoDelDiaTest,AsignacionVehiculoRepositoryImplTest'` en verde (el de repositorio queda `SKIPPED` en Windows sin Docker).
2. `./mvnw checkstyle:check` sin errores.
3. Un instructor `LICENCIA` con franjas libres da `NO_DISPONIBLE`, y uno con autos solo en mantenimiento da `EstadoAutos.NO_DISPONIBLE`.
