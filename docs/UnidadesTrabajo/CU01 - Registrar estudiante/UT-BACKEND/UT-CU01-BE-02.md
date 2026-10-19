# UT-CU01-BE-02 — Repositorio de escuelas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-02` |
| **Nombre** | Repositorio de escuelas |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU01-BE-01 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (interfaz en `domain/repository`, implementación en `infrastructure/persistence/repository`) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`@DataJpaTest`/Testcontainers con `assumeTrue`).

## Solicitud de negocio

Hay que poder buscar una escuela por su `slug` para validar el enlace de registro del estudiante.

## Objetivo

El dominio puede pedir una escuela activa por `slug` sin conocer JPA.

## Alcance

- `domain/repository/EscuelaRepository` con un único método: `Optional<Escuela> buscarActivaPorSlug(String slug)` (solo `ACTIVA` y sin `deleted_at`).
- `infrastructure/persistence/repository/EscuelaJpaRepository extends JpaRepository<EscuelaModel, Long>` con `Optional<EscuelaModel> findBySlugAndEstado(String slug, EstadoEscuela estado)`.
- `infrastructure/persistence/repository/EscuelaRepositoryImpl` que implementa la interfaz de dominio y traduce `EscuelaModel` a `Escuela` (mapper privado o clase `EscuelaPersistenceMapper`).
- Test de repositorio: encuentra una escuela activa, no encuentra una `INACTIVA`, no encuentra un `slug` inexistente.

Archivos esperados (crear/tocar):

- `.../domain/repository/EscuelaRepository.java`
- `.../infrastructure/persistence/repository/EscuelaJpaRepository.java`, `EscuelaRepositoryImpl.java`
- `projects/backend/escuela/src/test/java/.../EscuelaRepositoryImplTest.java`

## Fuera de alcance (no tocar)

- Métodos de guardar, listar o actualizar escuelas (los necesitará el CU de gestión de escuelas, y el seeder de UT 05 guarda con su propio camino).
- Repositorios del resto de entidades de Escuela.
- El caso de uso y el controller (UT 03 y 04).

## Resultado esperado

`EscuelaRepository.buscarActivaPorSlug("escuelaexample")` devuelve la escuela cuando existe y está activa, y vacío en cualquier otro caso.

## Validación

1. `./mvnw test` en `escuela` pasa, incluido el test nuevo (si Docker no está disponible el test queda SKIPPED, ver TESTING_BACKEND.md).
2. `./mvnw checkstyle:check` sin errores.
