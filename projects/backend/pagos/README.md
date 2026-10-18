# Pagos

Planes de pago, cuotas, pagos con pasarela (QR), comprobantes y reversiones. CU25 a CU31.

| | |
| --- | --- |
| Stack | Spring Boot 4.1, Java 21, Maven |
| Arquitectura | capas — ver [`ARQUITECTURA_BACKEND.md`](../../../.claude/rules/ARQUITECTURA_BACKEND.md) |
| Base de datos | PostgreSQL (`pagos`) |
| Puerto | 8083 |

## Correr

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
./mvnw checkstyle:check
```

Variables para correrlo fuera de Docker: [`.env.example`](.env.example). `GET /health` para verificar.

Los tests de contexto (`PagosApplicationTests`) necesitan la base corriendo (`docker compose up postgres`).
