# Caso de uso

| Id   | Nombre                      | Paquete                | Prioridad | Estado    |
| ---- | --------------------------- | ---------------------- | --------- | --------- |
| CU48 | Registrarse por WhatsApp    | P1. Identidad y acceso | Media     | En progreso |

**Actor principal:** Estudiante (persona aún sin cuenta, escribiendo desde WhatsApp)
**Actores secundarios:** n8n (orquesta el chat); Identidad (dueña de las cuentas)
**Objetivo:** Crear su cuenta de estudiante desde el chat de WhatsApp de la escuela, eligiendo la contraseña fuera del chat.
**Plataforma:** WhatsApp y una página web pública (solo para la contraseña)

**Descripción:**
Una persona sin cuenta escribe al WhatsApp de la escuela y elige «Quiero registrarme». El bot le pide, uno por uno, nombres, apellidos, fecha de nacimiento, sexo, CI, correo y su ubicación (con el mensaje de ubicación de WhatsApp); el teléfono es el del propio chat. El sistema valida cada dato apenas llega, con las mismas reglas de [CU01](CU01%20-%20Registrar%20estudiante.md) y [CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md). Al terminar, el sistema guarda una solicitud y entrega por el chat un enlace de un solo uso a una página web pública donde la persona elige su contraseña. Al enviarla, se crea la cuenta (con el mismo resultado que CU01), el número queda vinculado y el chat avisa que ya puede reservar y comprar horas ([CU49](CU49%20-%20Conversar%20por%20WhatsApp.md)). **La contraseña nunca viaja por WhatsApp.**

---

## Precondiciones

- El número no está vinculado a ninguna cuenta ([CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md)).
- La escuela del chat existe y está activa.

## Flujo principal

1. La persona elige «Quiero registrarme».
2. El bot pide los datos de a uno: nombres, apellidos, fecha de nacimiento, sexo, CI, correo y ubicación.
3. Cada dato se valida al llegar; si es inválido, el bot lo dice y lo vuelve a pedir.
4. Con todos los datos válidos, el bot muestra el resumen y pide confirmar.
5. La persona confirma. El sistema guarda la solicitud y genera un enlace de un solo uso válido por 30 minutos.
6. El bot manda el enlace por el chat.
7. La persona abre el enlace, ve la página pública con su nombre y elige una contraseña (mínimo 9 caracteres).
8. El sistema crea la cuenta `ACTIVO` con rol `ESTUDIANTE`, su perfil y la escuela del chat, vincula el número y consume el enlace.
9. La página informa que la cuenta está lista; el bot lo avisa por el chat.

## Flujos alternativos y excepciones

- **A1 (paso 3):** Un dato falta o tiene formato inválido, la persona es menor de 18 años, o el CI o el sexo no cumplen las reglas de CU08a → el bot lo informa y no avanza.
- **A2 (paso 3 o 5):** El correo o el CI ya pertenecen a una cuenta → el bot indica que ya existe una cuenta con ese dato y ofrece «Ya tengo cuenta» ([CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md)).
- **A3 (paso 3):** La persona envía otra cosa cuando se espera la ubicación → el bot la vuelve a pedir; si la dirección no se puede calcular, se continúa con ella en blanco como en CU08a.
- **A4 (paso 4):** La persona cancela o deja de responder → la conversación se descarta a las 24 horas y no se crea nada.
- **A5 (paso 7):** El enlace es inválido, venció o ya se usó → la página lo indica; el bot ofrece generar otro.
- **A6 (paso 7):** La contraseña tiene menos de 9 caracteres → la página lo rechaza indicando el mínimo.
- **A7 (paso 8):** Entre la solicitud y la contraseña el correo o el CI quedaron ocupados por otra cuenta → no se crea nada y la página lo informa.
- **A8 (paso 8):** El número ya está vinculado a otra cuenta → la cuenta se crea y el número queda en la nueva solo si la persona lo confirma en el chat con el código de [CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md); si no, queda sin vincular.
- **A9 (cualquier paso):** Se supera el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Mismas reglas que CU01 y CU08a:** edad mínima 18 años (configurable), CI de 5 a 15 caracteres y único en la plataforma, correo único, sexo masculino o femenino, ubicación obligatoria. El rol siempre es `ESTUDIANTE` y la escuela siempre la del chat.
- **La contraseña solo se fija en la web,** mediante el enlace. El enlace es de un solo uso, vale 30 minutos y en la base solo se guarda su hash.
- **La solicitud no es una cuenta:** hasta fijar la contraseña no se puede iniciar sesión ni operar. Las solicitudes sin completar se eliminan al vencer el enlace.
- **El teléfono es el del chat,** ya verificado por WhatsApp; no se le pide a la persona.
- **El correo no queda verificado:** se verifica después como siempre ([CU04b](CU04b%20-%20Verificar%20correo.md)).
- **No se duplica la creación de la cuenta:** completar la solicitud usa la misma regla de negocio que CU01 y no una copia.
- Contraseña, CI, correo, teléfono, coordenadas y el enlace no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- Los mensajes del chat y los de la página están en español.

## Postcondiciones

- Existe una cuenta `ACTIVO` con rol `ESTUDIANTE`, su perfil y la escuela del chat, con el número vinculado.
- El enlace usado ya no sirve.

## Criterios de aceptación

- **CA01:** Dados datos válidos y confirmados, el sistema debe guardar una solicitud y entregar un enlace de un solo uso, sin crear aún la cuenta.
- **CA02:** Dado un dato inválido (formato, edad menor de 18, sexo, CI), el sistema debe rechazarlo con su motivo sin avanzar ni guardar la solicitud.
- **CA03:** Dado un correo o un CI ya registrados, el sistema debe rechazarlos (409 `ERR_IDE_19` para el CI) sin guardar la solicitud.
- **CA04:** Dado un enlace válido y una contraseña de al menos 9 caracteres, el sistema debe crear la cuenta con el mismo resultado que CU01, vincular el número y consumir el enlace.
- **CA05:** Dado un enlace inválido, vencido o usado, el sistema debe responder 400 sin crear nada.
- **CA06:** Dada una contraseña de menos de 9 caracteres, el sistema debe responder 400 y no crear la cuenta.
- **CA07:** Dado cualquier solicitud, la contraseña no debe pasar por WhatsApp ni por n8n.
- **CA08:** Dado que el correo o el CI se ocuparon antes de completar, el sistema debe responder 409 y no crear la cuenta.
- **CA09:** Dada una petición con otro rol u otra escuela, el sistema debe ignorarlos y usar `ESTUDIANTE` y la escuela del chat.
- **CA10:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA11:** Dado cualquier paso, ningún dato personal, contraseña ni enlace debe aparecer en los registros.

## Fuera de alcance

- **Registro de instructores** (los crea el administrador, [CU09](CU09%20-%20Gestionar%20instructores.md)).
- **Verificar el correo desde el chat** (sigue siendo CU04b).
- **Registro en más de una escuela.**

## Desarrollador

Leonel
