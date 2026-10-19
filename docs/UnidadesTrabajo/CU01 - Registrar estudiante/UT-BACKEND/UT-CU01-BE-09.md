# UT-CU01-BE-09 — Puertos de entrada y salida del registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-09` |
| **Nombre** | Puertos de entrada y salida del registro |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-08 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (un caso de uso = un puerto de entrada; puertos de salida con sufijo `Port`).

## Solicitud de negocio

Antes de escribir el servicio y los adaptadores hay que fijar el contrato del dominio, así el servicio, la persistencia, el cliente de Escuela y el controller se construyen por separado contra las mismas interfaces.

## Objetivo

Existen, solo como interfaces y objetos de datos, el puerto de entrada `RegistrarEstudianteUseCase` y los puertos de salida que necesita.

## Alcance

- `domain/port/in/RegistrarEstudianteUseCase` con un método `Usuario registrar(RegistrarEstudianteComando comando)`, y `RegistrarEstudianteComando` (`record`: `nombres`, `apellidos`, `fechaNacimiento`, `email`, `password`, `telefono`, `escuelaSlug`). Sin campo de rol.
- Puertos de salida en `domain/port/out/`:
  - `UsuarioRepositoryPort`: `boolean existePorEmail(String email)` y `Usuario guardar(Usuario usuario)`.
  - `RolRepositoryPort`: `Optional<Rol> buscarPorNombre(String nombre)`.
  - `EscuelaConsultaPort`: `Optional<EscuelaRef> buscarActivaPorSlug(String slug)`, con `EscuelaRef` (`record`: `codigo`, `slug`, `nombre`) en `domain/port/out/`.
  - `PasswordHasherPort`: `String hashear(String passwordEnClaro)`.
- Todo sin anotaciones de Spring/JPA/Jackson.

Archivos esperados (crear/tocar):

- `.../domain/port/in/RegistrarEstudianteUseCase.java`, `RegistrarEstudianteComando.java`
- `.../domain/port/out/UsuarioRepositoryPort.java`, `RolRepositoryPort.java`, `EscuelaConsultaPort.java`, `EscuelaRef.java`, `PasswordHasherPort.java`

## Fuera de alcance (no tocar)

- Implementaciones de cualquier tipo (UT 10 a 13).
- Puertos de login, refresh, recuperación o roles (otros CU).
- Métodos que este CU no use (buscar por id, actualizar, listar, etc.).

## Resultado esperado

El proyecto compila con los puertos definidos; ninguna clase de `domain/` importa de `application/` ni `infrastructure/`.

## Validación

1. `./mvnw test` en `identidad` pasa (compila).
2. `./mvnw checkstyle:check` sin errores.
3. Búsqueda en `domain/` de `import org.springframework` / `jakarta.persistence` no devuelve nada.
