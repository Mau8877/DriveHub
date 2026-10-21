# UT-CU06-BE-14 — Prueba de integración

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-14` |
| **Nombre** | Prueba de integración |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-11, UT-CU06-BE-12, UT-CU06-BE-13 |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

Confirmar de punta a punta, con base y almacenamiento reales, que el perfil se gestiona como dice el CU.

## Objetivo

Una prueba de integración del CU06 que recorre los flujos principales y los rechazos clave.

## Alcance

- `PerfilIntegracionTest` con Testcontainers (PostgreSQL y MinIO, patrón `assumeTrue` de Docker, un servidor SMTP de prueba) y la aplicación completa:
  - registrar una cuenta, iniciar sesión y `GET /usuarios/me` coincide con lo registrado;
  - `PATCH` cambia nombres y teléfono; menor de 18 → `ERR_IDE_02`;
  - `PUT /foto` con un PNG válido guarda el objeto en `drivehub-identidad`, el perfil trae `fotoUrl` y la URL descarga la imagen; una segunda foto reemplaza y borra la primera; un `.exe` renombrado, un PNG con bytes pegados al final y un archivo de más de 5 MB se rechazan (`ERR_IDE_16`/`ERR_IDE_17`) sin dejar objetos nuevos; `DELETE /foto` la borra y repetirlo no falla; el login y la renovación incluyen `fotoUrl`;
  - `cambiar-contrasena`: con la actual incorrecta → `ERR_IDE_14` y la cuenta no se bloquea tras varios intentos; con la correcta, el login funciona con la nueva y no con la anterior, y la sesión del otro dispositivo deja de renovarse (401) mientras la del dispositivo actual sigue;
  - `cambiar-correo`: pedirlo envía el enlace **al correo nuevo** y el correo de la cuenta sigue igual; pedir otro invalida el primero; un correo en uso → `ERR_IDE_01`; confirmar cambia el correo (se puede iniciar sesión con el nuevo), lo deja verificado, el enlace no sirve otra vez y el correo anterior recibe el aviso; un enlace de verificación o de recuperación no sirve para confirmar el cambio;
  - **CA19:** los registros capturados durante toda la prueba no contienen contraseñas, tokens, enlaces ni URLs prefirmadas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/test/java/com/drivehub/identidad/PerfilIntegracionTest.java`

## Fuera de alcance (no tocar)

- Código de producción: si la prueba encuentra un fallo, se corrige en la UT que lo causó.
- El rate limit (lo cubre el `gateway`).

## Resultado esperado

El CU06 de backend queda verificado de punta a punta contra Postgres y MinIO reales.

## Validación

1. `./mvnw test` en `identidad` pasa con `PerfilIntegracionTest` en verde (o `SKIPPED` sin Docker; en CI corre de verdad).
2. `./mvnw checkstyle:check` sin errores.
