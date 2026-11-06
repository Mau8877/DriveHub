# UT-CU47-BE-04 — Confirmar código y vincular el número

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU47-BE-04` |
| **Nombre** | Confirmar código y vincular el número |
| **Caso de uso** | CU47-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU47-BE-01, UT-CU47-BE-02, UT-CU47-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y `ConfirmarCorreoService` (CU04b).

## Solicitud de negocio

Con el código correcto, el número queda reconocido y no vuelve a pedirse nada.

## Objetivo

`POST /internal/v1/whatsapp/vinculos` comprueba el código y vincula el número a la cuenta.

## Alcance

- Puerto `ConfirmarCodigoWhatsappUseCase` y servicio `ConfirmarCodigoWhatsappService` (`@Transactional`, loguea):
  1. Busca la cuenta por correo y el código vigente de esa cuenta y ese número.
  2. Si no hay, venció, ya se usó o no coincide: suma un intento y lanza `ValidationException` `ERR_IDE_25`; al llegar a 5 intentos invalida el código.
  3. Si coincide: lo marca usado, y **reemplaza** cualquier vínculo anterior del número y de la cuenta (un número, una cuenta; una cuenta, un número), guardando el nuevo.
- Comparación del hash del código en tiempo constante.
- Controller: `POST /internal/v1/whatsapp/vinculos` en `WhatsappController` con `VincularWhatsappRequest` (`telefono`, `correo`, `codigo` de exactamente 6 dígitos); responde `201` con `data: { vinculado: true }`.
- Error `ERR_IDE_25` («El código no es válido o venció»): constante en `IdentidadErrorCodes` y sección en `docs/errors/identidad/ERRORES_IDENTIDAD.md` con su link en `docs/errors/README.md`.
- Logs: INFO al vincular con el `codigo_usuario`; WARN con el motivo del rechazo, sin número, correo ni código.
- Tests: código correcto, incorrecto, vencido, usado, sexto intento tras cinco fallos, reemplazo de vínculo anterior del número, reemplazo del número de la cuenta, y `@WebMvcTest` del sobre.

Archivos esperados (crear/tocar): `domain/port/in/ConfirmarCodigoWhatsapp*.java`, `application/service/ConfirmarCodigoWhatsappService.java`, `VincularWhatsappRequest.java`, `WhatsappController.java`, `IdentidadErrorCodes.java`, `docs/errors/**` y los tests.

## Fuera de alcance (no tocar)

- Emitir la sesión (UT-05); desvincular un número; el registro por chat (CU48).

## Resultado esperado

Tras un código correcto existe un único vínculo entre el número y la cuenta; el código no sirve otra vez.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. Pedir un código (UT-03), leerlo en Mailpit y llamar a `/internal/v1/whatsapp/vinculos` → 201; repetirlo → 400 `ERR_IDE_25`.
3. Cinco códigos incorrectos seguidos invalidan el vigente: el correcto también responde 400.
