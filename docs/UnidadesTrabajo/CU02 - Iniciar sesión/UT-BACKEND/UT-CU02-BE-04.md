# UT-CU02-BE-04 — Puertos y objetos de dominio de la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-04` |
| **Nombre** | Puertos y objetos de dominio de la sesión |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-01 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md)

## Solicitud de negocio

Antes de escribir los servicios y adaptadores hay que fijar el contrato del dominio, así se construyen por separado contra las mismas interfaces.

## Objetivo

Existen, solo como interfaces y objetos de datos, los puertos de entrada `IniciarSesionUseCase` y `RenovarSesionUseCase` y los puertos de salida que necesitan.

## Alcance

- `domain/port/in`: `IniciarSesionUseCase.iniciar(IniciarSesionComando)` y `RenovarSesionUseCase.renovar(RenovarSesionComando)`, ambos devuelven `SesionEmitida`.
- `IniciarSesionComando` (`record`: `email`, `password`, `Dispositivo dispositivo`), `RenovarSesionComando` (`record`: `refreshToken`, `Dispositivo dispositivo`) y `SesionEmitida` (`record`: `accessToken`, `expiraEnSegundos`, `refreshToken` en claro, `Usuario usuario`).
- `domain/model/AccesoPorPlataforma`: regla pura `boolean permite(Dispositivo, Set<Rol>)` — `WEB` permite `SUPER_ADMIN` y `ADMIN_ESCUELA`; `MOVIL` permite `ESTUDIANTE` e `INSTRUCTOR`; con varios roles basta uno permitido. Con su test unitario.
- `domain/model/RefreshToken`: agregar `@Builder(toBuilder = true)` y `Instant creadoEn` si hace falta.
- Puertos de salida: ampliar `UsuarioRepositoryPort` (`Optional<Usuario> buscarPorEmail(String)`, `Optional<Usuario> buscarPorCodigo(UUID)`, `Usuario actualizar(Usuario)`); `RefreshTokenRepositoryPort` (`guardar`, `Optional<RefreshToken> buscarPorHash`, `revocarActivos(UUID usuarioCodigo, Dispositivo)`, `revocar(RefreshToken)`); `PasswordHasherPort.coincide(String enClaro, String hash)`; `EmisorTokenAccesoPort.emitir(Usuario, Instant emitidoEn)` que devuelve el JWT; `GeneradorTokenOpacoPort` (`generar()` y `hashear(String)`); `EscuelaConsultaPort.buscarPorCodigo(UUID)` y `EscuelaRef` con `boolean activa`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/` (2 interfaces + 3 records)
- `.../domain/port/out/` (puertos nuevos y ampliados)
- `.../domain/model/AccesoPorPlataforma.java`, `RefreshToken.java`
- `.../domain/model/AccesoPorPlataformaTest.java`

## Fuera de alcance (no tocar)

- Implementaciones de cualquier tipo (UT 05 a 10).
- Puertos de cierre de sesión o recuperación de contraseña (CU03, CU04).
- Anotaciones de Spring/JPA/Jackson en `domain/`.

## Resultado esperado

El proyecto compila con los puertos definidos y la regla de acceso por plataforma probada.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. Buscar `import org.springframework` o `jakarta.persistence` en `domain/` no devuelve nada.
3. `./mvnw checkstyle:check` sin errores.
