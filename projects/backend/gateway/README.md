# API Gateway

Único punto de entrada a `/api/v1/**`: enruta cada recurso a su servicio, aplica CORS y rate limit.

| | |
| --- | --- |
| Stack | Spring Cloud Gateway (Boot 4.0), Java 21, Maven |
| Arquitectura | solo enrutamiento — ver [`GATEWAY_BACKEND.md`](../../../.claude/rules/backend/GATEWAY_BACKEND.md) |
| Base de datos | — |
| Puerto | 8080 |

## Correr

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
./mvnw checkstyle:check
```

Variables para correrlo fuera de Docker: [`.env.example`](.env.example). `GET /health` para verificar.
