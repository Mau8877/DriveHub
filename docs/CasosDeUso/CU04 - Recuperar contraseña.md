# Caso de uso

| Id   | Nombre               | Paquete                | Prioridad | Estado    |
| ---- | -------------------- | ---------------------- | --------- | --------- |
| CU04 | Recuperar contraseña | P1. Identidad y acceso | Alta      | Pendiente |

**Actor principal:** Estudiante, Instructor, Administrador de escuela o Super admin (cualquier persona con cuenta que olvidó su contraseña)
**Actores secundarios:** —
**Objetivo:** Recuperar el acceso a la cuenta eligiendo una contraseña nueva, sin necesitar la anterior.
**Plataforma:** Móvil y Web

**Descripción:**
En el inicio de sesión (web o app) la persona elige "¿Olvidaste tu contraseña?" e ingresa su correo. El sistema responde siempre lo mismo, exista o no la cuenta, para no revelar qué correos están registrados. Si la cuenta existe, está activa y su **correo está verificado** ([CU04b](CU04b%20-%20Verificar%20correo.md)), le envía un correo con un enlace de un solo uso y vigencia corta. El enlace abre una página pública de la web (también desde el teléfono) donde la persona escribe la contraseña nueva. Al guardarla, el sistema cierra todas las sesiones de la cuenta, la desbloquea si estaba bloqueada y lleva a la persona al inicio de sesión: no inicia sesión sola. Si el correo no estaba verificado, no se envía nada; por eso la pantalla avisa que la recuperación solo funciona con el correo verificado.

---

## Precondiciones

- La persona tiene una cuenta con el correo verificado ([CU04b: Verificar correo](CU04b%20-%20Verificar%20correo.md)). No necesita tener sesión abierta.

## Flujo principal

1. En el inicio de sesión, la persona elige "¿Olvidaste tu contraseña?".
2. La persona ingresa su correo y envía el formulario.
3. El sistema valida que el correo esté completo y tenga formato válido.
4. El sistema responde que, si el correo está registrado y verificado, se envió un enlace. La respuesta es la misma en todos los casos.
5. Si la cuenta existe, está activa y su correo está verificado, el sistema invalida los enlaces de recuperación anteriores, genera uno de un solo uso y válido por 30 minutos, y lo envía al correo.
6. La persona abre el enlace y llega a la página pública de restablecer contraseña.
7. La persona escribe la contraseña nueva, la confirma y envía.
8. El sistema valida el enlace y la contraseña (mismas reglas que el registro), guarda la contraseña nueva y consume el enlace.
9. El sistema cierra todas las sesiones de la cuenta (web y móvil), reinicia los intentos fallidos y quita el bloqueo.
10. La página informa que la contraseña cambió y lleva al inicio de sesión.

## Flujos alternativos y excepciones

- **A1 (paso 3):** Falta el correo o su formato es inválido → el cliente indica el campo y no envía la solicitud; el sistema responde 400 `ERR_SYS_01` si llega igual.
- **A2 (paso 5):** El correo no existe, la cuenta está inactiva o el correo no está verificado → el sistema responde igual que en el caso exitoso y no envía nada.
- **A3 (paso 5):** El servidor de correo falla → el sistema responde igual que en el caso exitoso y registra el fallo; no revela nada a quien lo pidió.
- **A4 (paso 8):** El enlace es inválido, venció, ya se usó o fue reemplazado → el sistema responde 400 `ERR_IDE_11` y la página invita a pedir uno nuevo.
- **A5 (paso 8):** La contraseña no cumple las reglas → el sistema responde 400 `ERR_SYS_01` indicando el campo; el enlace sigue vigente.
- **A6 (paso 7):** La confirmación no coincide con la contraseña → el cliente lo indica y no envía.
- **A7 (pasos 2 y 7):** Se superó el límite de peticiones → el sistema responde 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo con el correo verificado:** una cuenta sin verificar no recibe correo de recuperación ([CU04b](CU04b%20-%20Verificar%20correo.md)). La pantalla lo advierte sin revelar si la cuenta existe.
- **No revela cuentas:** la respuesta, el mensaje y el código HTTP de pedir la recuperación son iguales exista o no la cuenta, esté o no verificada.
- **El enlace:** un solo uso, 30 minutos, y pedir otro invalida los anteriores. En la base solo se guarda su hash. Un enlace de verificación de correo no sirve para restablecer, ni al revés.
- **Contraseña nueva:** las mismas reglas que el registro (mínimo 9 caracteres, [CU01](CU01%20-%20Registrar%20estudiante.md)).
- **Al restablecer:** se cierran todas las sesiones de la cuenta, se reinicia el contador de intentos fallidos y se quita el bloqueo. No se inicia sesión automáticamente.
- **Mensajes en español** y sin detalles técnicos. El enlace apunta a la web configurada (`APP_WEB_URL`), también para quien usa la app.
- **Contraseñas, enlaces y tokens nunca se escriben en los registros** ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **Ambos endpoints son públicos** y los protege el límite de peticiones del gateway ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).

## Postcondiciones

- La cuenta tiene la contraseña nueva; la anterior ya no sirve.
- Ninguna sesión previa de la cuenta es renovable y el enlace usado no sirve otra vez.
- La cuenta queda sin bloqueo y con el contador de intentos en 0.

## Criterios de aceptación

- **CA01:** Dado que la cuenta existe, está activa y su correo está verificado, el sistema debe enviar un correo con el enlace de recuperación y responder el mensaje estándar.
- **CA02:** Dado que el correo no existe, la cuenta está inactiva o el correo no está verificado, el sistema debe responder exactamente igual que en CA01 y no enviar nada.
- **CA03:** Dado que el servidor de correo falla, el sistema debe responder igual que en CA01.
- **CA04:** Dado que se pide otro enlace, el sistema debe invalidar los anteriores.
- **CA05:** Dado un enlace válido y una contraseña válida, el sistema debe guardar la contraseña nueva, consumir el enlace y permitir iniciar sesión con ella (y no con la anterior).
- **CA06:** Dado que se restableció la contraseña, el sistema debe cerrar todas las sesiones de la cuenta, de modo que renovar con cualquiera responda 401.
- **CA07:** Dado que la cuenta estaba bloqueada por intentos fallidos, el sistema debe desbloquearla y reiniciar el contador al restablecer.
- **CA08:** Dado que el enlace es inválido, venció, ya se usó, fue reemplazado o es de verificación de correo, el sistema debe responder 400 `ERR_IDE_11` sin cambiar la contraseña.
- **CA09:** Dado que la contraseña no cumple las reglas, el sistema debe responder 400 indicando el campo y conservar el enlace vigente.
- **CA10:** Dado que la confirmación no coincide, el cliente debe indicarlo sin enviar la solicitud.
- **CA11:** Dado cualquier recuperación, ni la contraseña ni los enlaces ni los tokens deben aparecer en los registros del sistema.
- **CA12:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Mauro
