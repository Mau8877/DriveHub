# Escuela

Estudiantes, instructores, vehículos, mantenimiento y asignaciones. CU07 a CU11.

| | |
| --- | --- |
| Stack | Spring Boot 4.1, Java 21, Maven |
| Arquitectura | clean architecture — ver [`ARQUITECTURA_CLEAN_BACKEND.md`](../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) |
| Base de datos | PostgreSQL (`escuela`) |
| Puerto | 8082 |

## Correr

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
./mvnw checkstyle:check
```

Variables para correrlo fuera de Docker: [`.env.example`](.env.example). `GET /health` para verificar.

Los tests de contexto (`EscuelaApplicationTests`) necesitan la base corriendo (`docker compose up postgres`).
