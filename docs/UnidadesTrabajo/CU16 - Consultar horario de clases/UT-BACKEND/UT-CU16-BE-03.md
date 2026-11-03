# UT-CU16-BE-03 — Repositorio de franjas: bloquear una franja reservada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-03` |
| **Nombre** | Repositorio de franjas: bloquear una franja reservada |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb). Tomar como modelo `liberar` de `FranjaRepositoryImpl` (CU14).

## Solicitud de negocio

Si el instructor cancela una clase es porque no puede atender: su franja no debe volver a ofrecerse a otro estudiante.

## Objetivo

El repositorio de franjas puede dejar `BLOQUEADA` una franja que estaba reservada por una reserva, sin tocar ninguna otra.

## Alcance

- `FranjaRepositoryCustom` / `FranjaRepositoryImpl`: `boolean bloquearReservada(UUID codigoFranja, Long reservaId)`:
  - Actualiza con un solo `updateFirst` atómico, solo si la franja es `RESERVADA` por esa `reservaId` y no está borrada: `estado = BLOQUEADA` y quita `reservaId`.
  - Devuelve `true` si cambió algo, `false` si no (la franja no existe, ya no es de esa reserva o ya estaba bloqueada). Repetirlo es inocuo.
- Tests con MongoDB real (`FranjaRepositoryTest`): una franja `RESERVADA` por esa reserva pasa a `BLOQUEADA` sin `reservaId` y devuelve `true`; una `RESERVADA` por otra reserva no cambia y devuelve `false`; una `LIBRE` o ya `BLOQUEADA` no cambia; una borrada no cambia; llamarlo dos veces → la segunda `false`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryCustom.java`, `FranjaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Quién llama a `bloquearReservada` (UT 04).
- `liberar` y el resto de operaciones de franja: no cambian.
- Desbloquear: ya lo hace el instructor con CU12.

## Resultado esperado

Existe la operación que convierte la franja de una clase cancelada por el instructor en `BLOQUEADA`, segura de repetir.

## Validación

1. `./mvnw test` en `academico` pasa (los de MongoDB real se omiten solos sin Docker).
2. `./mvnw checkstyle:check` sin errores.
