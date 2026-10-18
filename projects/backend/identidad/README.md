# Identidad

Usuarios, roles, permisos, sesiones (refresh token) y recuperación de contraseña. Dueño del identificador de persona (`codigo_usuario`). CU01 a CU06.

| | |
| --- | --- |
| Stack | Spring Boot 4.1, Java 21, Maven |
| Arquitectura | hexagonal — ver [`ARQUITECTURA_HEXAGONAL_BACKEND.md`](../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) |
| Base de datos | PostgreSQL (`identidad`) |
| Puerto | 8081 |

## Correr

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
./mvnw checkstyle:check
```

Variables para correrlo fuera de Docker: [`.env.example`](.env.example). `GET /health` para verificar.

Los tests de contexto (`IdentidadApplicationTests`) necesitan la base corriendo (`docker compose up postgres`).
