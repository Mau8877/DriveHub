# UT-CU48-BE-06 — Flujo de n8n para registrarse

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-BE-06` |
| **Nombre** | Flujo de n8n para registrarse |
| **Caso de uso** | CU48-BE |
| **Microservicio** | `n8n` |
| **Depende de** | UT-CU48-BE-02, UT-CU48-BE-03, UT-CU48-BE-04 (y el servicio n8n de CU47, ya implementado) |

## Antes de implementar

`infrastructure/n8n/README.md` (de CU47) y [CU48](../../../CasosDeUso/CU48%20-%20Registrarse%20por%20WhatsApp.md).

## Solicitud de negocio

Quien no tiene cuenta debe poder crearla conversando con el bot.

## Objetivo

El flujo `whatsapp-drivehub.json` incluye la rama «Quiero registrarme» completa.

## Alcance

- Nodos de la rama de registro dentro del mismo flujo (sin credenciales dentro del JSON):
  1. Pedir de a uno: nombres, apellidos, fecha de nacimiento, sexo (con botones), CI, correo y ubicación (mensaje de ubicación de WhatsApp: latitud y longitud).
  2. Tras cada dato, `POST /internal/v1/whatsapp/registros/validaciones`; si falla, repetir la pregunta con el motivo que devuelve el sistema.
  3. En correo o CI duplicados, ofrecer «Ya tengo cuenta» (rama de CU47).
  4. Resumen con botones «Confirmar» y «Cancelar»; al confirmar, `POST /internal/v1/whatsapp/registros` y enviar el enlace.
  5. Estado de la conversación en la tabla de n8n con vencimiento de 24 h; «Cancelar» o el vencimiento lo borran.
  6. «Generar otro enlace» si el enlace venció (vuelve a crear la solicitud con los datos aún guardados en el estado, o los pide de nuevo).
- La contraseña **nunca** se pide ni se guarda en n8n; si la persona la escribe en el chat, el bot le recuerda usar el enlace.
- Actualizar `infrastructure/n8n/README.md` con la rama nueva.

Archivos esperados (crear/tocar): `infrastructure/n8n/flujos/whatsapp-drivehub.json`, `infrastructure/n8n/README.md`.

## Fuera de alcance (no tocar)

- Las herramientas y el agente de CU49, y el servicio n8n en el compose (de CU47).

## Resultado esperado

Una persona sin cuenta recorre el registro por chat y recibe un enlace que funciona.

## Validación

1. Importar el flujo y simular una conversación con un número nuevo: cada dato inválido se repite con su motivo y al final llega el enlace.
2. Abrir el enlace (FE-01), fijar la contraseña y volver a escribir al bot: lo reconoce sin pedir login.
3. `git grep -n "password"` en `infrastructure/n8n/` no encuentra ningún campo que guarde contraseñas.
