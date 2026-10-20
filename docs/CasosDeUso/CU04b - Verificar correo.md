# Caso de uso

| Id    | Nombre           | Paquete                | Prioridad | Estado    |
| ----- | ---------------- | ---------------------- | --------- | --------- |
| CU04b | Verificar correo | P1. Identidad y acceso | Media     | Hecho     |

**Actor principal:** Estudiante, Instructor, Administrador de escuela o Super admin (cualquier persona con sesión y el correo sin verificar)
**Actores secundarios:** —
**Objetivo:** Demostrar que el correo de la cuenta es suyo, para poder recuperar la contraseña con él ([CU04](CU04%20-%20Recuperar%20contrase%C3%B1a.md)).
**Plataforma:** Móvil y Web

**Descripción:**
Mientras el correo de la cuenta no está verificado, la barra superior de la web y la barra de la app muestran el botón **"Verificar correo"**. La persona lo pulsa y el sistema le envía un correo con un enlace de un solo uso. Al abrirlo (en cualquier navegador, aunque sea en otro dispositivo) el sistema marca el correo como verificado y la página lo informa. Al volver a la aplicación, el botón desaparece. Verificar el correo **no es obligatorio** para iniciar sesión ni para usar la plataforma: solo condiciona la recuperación de la contraseña. Si el enlace es inválido, venció, ya se usó o fue reemplazado por otro más nuevo, el sistema lo informa y la persona puede pedir uno nuevo.

---

## Precondiciones

- La persona tiene una sesión abierta ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su correo no está verificado.

## Flujo principal

1. La persona, con sesión y el correo sin verificar, ve el botón "Verificar correo" en la barra superior.
2. La persona pulsa el botón y el cliente le pide al sistema enviar el correo de verificación.
3. El sistema comprueba la sesión y que el correo siga sin verificar.
4. El sistema invalida los enlaces de verificación anteriores, genera uno nuevo de un solo uso y válido por 24 horas, y lo envía al correo de la cuenta.
5. El cliente informa que se envió un correo y a qué dirección.
6. La persona abre el enlace del correo, que lleva a una página pública de la web.
7. El sistema comprueba el enlace, marca el correo como verificado y lo consume.
8. La página informa que el correo quedó verificado.
9. Al volver a la aplicación, el cliente actualiza la sesión y el botón desaparece.

## Flujos alternativos y excepciones

- **A1 (paso 3):** El correo ya está verificado → el sistema responde 409 y el cliente actualiza la sesión y oculta el botón.
- **A2 (paso 3):** La sesión venció → el sistema responde 401 y el cliente la renueva como en [CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md) (A8).
- **A3 (paso 4):** El servidor de correo no responde → el sistema responde 500 con un mensaje genérico y el cliente avisa que no se pudo enviar y que reintente más tarde. El enlace generado no se entrega.
- **A4 (paso 7):** El enlace es inválido, venció, ya se usó o fue reemplazado por uno más nuevo → el sistema responde 400 y la página indica que el enlace no sirve y que se puede pedir otro desde la aplicación.
- **A5 (pasos 2 y 7):** Se superó el límite de peticiones → el sistema responde 429 (`ERR_SYS_02`).

## Reglas de negocio

- **La verificación no bloquea nada:** iniciar sesión y usar la plataforma no dependen de ella; solo la recuperación de contraseña la exige ([CU04](CU04%20-%20Recuperar%20contrase%C3%B1a.md)).
- **Todas las cuentas empiezan sin verificar,** incluidas las ya existentes.
- **El enlace:** de un solo uso, válido 24 horas, y pedir uno nuevo invalida los anteriores. En la base solo se guarda su hash ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Confirmar es público:** el enlace se abre fuera de la sesión (otro navegador u otro dispositivo); lo que demuestra el control del correo es tener el enlace.
- **Pedir el correo exige sesión** y se envía siempre a la dirección de la cuenta, nunca a una que mande el cliente.
- **El enlace apunta a la web** configurada para el entorno (`APP_WEB_URL`), también para quien usa la app.
- **El botón se oculta** cuando la sesión indica que el correo está verificado; el cliente actualiza la sesión al volver a la aplicación.
- **Cambiar el correo** (CU06, pendiente) deberá reiniciar la verificación.
- **Los enlaces y tokens nunca se escriben en los registros** ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **En desarrollo** los correos llegan a la bandeja de Mailpit (http://localhost:8025), no a un buzón real ([INFRAESTRUCTURA.md](../INFRAESTRUCTURA.md)).

## Postcondiciones

- El correo de la cuenta queda verificado y el enlace usado no sirve otra vez.
- La sesión de la persona muestra el correo como verificado y el botón ya no aparece.

## Criterios de aceptación

- **CA01:** Dado que la persona tiene sesión y el correo sin verificar, el cliente debe mostrar el botón "Verificar correo" en la barra superior (web y app); con el correo verificado, no.
- **CA02:** Dado que la persona pulsa el botón, el sistema debe enviar un correo con el enlace de verificación a la dirección de su cuenta e informarlo.
- **CA03:** Dado que el enlace es válido, el sistema debe marcar el correo como verificado, consumir el enlace y la página debe informarlo.
- **CA04:** Dado que el enlace es inválido, venció, ya se usó o fue reemplazado, el sistema debe responder 400 sin verificar nada.
- **CA05:** Dado que la persona pide otro correo, el sistema debe invalidar los enlaces anteriores.
- **CA06:** Dado que el correo ya está verificado, el sistema debe responder 409 al pedir el correo.
- **CA07:** Dado que se verificó el correo, el cliente debe ocultar el botón al volver a la aplicación sin cerrar sesión.
- **CA08:** Dado que el correo no está verificado, el sistema debe permitir iniciar sesión y usar la plataforma con normalidad.
- **CA09:** Dado que el servidor de correo falla, el sistema debe responder 500 genérico y no entregar el enlace.
- **CA10:** Dado cualquier envío o verificación, ningún enlace ni token debe aparecer en los registros del sistema.
- **CA11:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Mauro
