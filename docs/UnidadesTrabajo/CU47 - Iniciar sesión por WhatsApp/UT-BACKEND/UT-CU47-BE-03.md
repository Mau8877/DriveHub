# UT-CU47-BE-03 — Pedir código por correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU47-BE-03` |
| **Nombre** | Pedir código por correo |
| **Caso de uso** | CU47-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU47-BE-01, UT-CU47-BE-02 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y el servicio `SolicitarVerificacionCorreoService` (CU04b) como modelo del envío de correo.

## Solicitud de negocio

Quien dice tener cuenta recibe un código en su correo para demostrar que la cuenta es suya.

## Objetivo

`POST /internal/v1/whatsapp/codigos` envía un código de 6 dígitos al correo de una cuenta de estudiante válida y responde igual en todos los casos.

## Alcance

- Puerto de entrada `SolicitarCodigoWhatsappUseCase` (`solicitar(SolicitarCodigoWhatsappComando)`) y servicio `application/service/SolicitarCodigoWhatsappService` (`@Transactional`, loguea):
  1. Cuenta los pedidos de la última hora por número y por usuario; si pasan de 5, lanza `LimiteExcedidoException`.
  2. Busca la cuenta por correo. Si no existe, no es `ESTUDIANTE`, no es de la escuela `WHATSAPP_ESCUELA_CODIGO`, o está `BLOQUEADO`/`INACTIVO`: **termina sin enviar** (el controller responde igual).
  3. Invalida los códigos anteriores, genera un código de 6 dígitos con `SecureRandom`, guarda solo su hash, vence a los 10 minutos y lo envía con `EnviadorCorreoPort` (plantilla nueva en `PlantillaCorreo`).
- Si el envío del correo falla, lanza el error genérico 500 y **no** deja el código vigente.
- Excepción nueva `LimiteExcedidoException` (429, `ERR_SYS_02`) en `common/exception/` y su fila en [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) (decisión pendiente del equipo, ver el README).
- Controller `infrastructure/adapter/in/web/WhatsappController` con `POST /internal/v1/whatsapp/codigos` y `CodigoWhatsappRequest` (`telefono` en formato E.164, `correo`; validados con `@Valid`).
- Respuesta `202` con `data: null` y el mismo mensaje siempre.
- Logs: INFO al enviar con el `codigo_usuario`; WARN al exceder el límite; nunca el número, el correo ni el código.
- Documentar `ERR_SYS_02`-uso interno si hace falta en `docs/errors/`; si se crea un código `ERR_IDE_NN` nuevo, seguir [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
- Tests: servicio con puertos simulados (envía, no existe, otro rol, otra escuela, bloqueada, límite por número, límite por correo, correo caído) y `@WebMvcTest` del controller verificando el sobre y que la respuesta es idéntica con y sin cuenta.

Archivos esperados (crear/tocar): `domain/port/in/SolicitarCodigoWhatsapp*.java`, `application/service/SolicitarCodigoWhatsappService.java`, `common/exception/LimiteExcedidoException.java`, `WhatsappController.java`, `CodigoWhatsappRequest.java`, `PlantillaCorreo.java`, `EXCEPCIONES_BACKEND.md` y los tests.

## Fuera de alcance (no tocar)

- Confirmar el código y crear el vínculo (UT-04), emitir sesión (UT-05) y el flujo de n8n (UT-06).

## Resultado esperado

Una cuenta válida recibe un correo con el código; cualquier otro caso devuelve exactamente la misma respuesta sin enviar nada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. Con el stack levantado, `POST /internal/v1/whatsapp/codigos` con la credencial y un correo existente → 202 y el correo llega a Mailpit (http://localhost:8025); con un correo inexistente → 202 idéntico y Mailpit sin correo nuevo.
3. Seis pedidos seguidos desde el mismo número → el sexto responde 429 `ERR_SYS_02`.
