# Académico

Horarios, franjas, solicitudes y reservas de clases, clases dictadas, exámenes, banco de preguntas, simulacros y notas. CU11 a CU23.

| | |
| --- | --- |
| Stack | Spring Boot 4.1, Java 21, Maven |
| Arquitectura | capas — ver [`ARQUITECTURA_BACKEND.md`](../../../.claude/rules/ARQUITECTURA_BACKEND.md) |
| Base de datos | MongoDB (`academico`, replica set) |
| Puerto | 8084 |

## Correr

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
./mvnw checkstyle:check
```

Variables para correrlo fuera de Docker: [`.env.example`](.env.example). `GET /health` para verificar.

Los tests de contexto (`AcademicoApplicationTests`) necesitan la base corriendo (`docker compose up mongo`).
