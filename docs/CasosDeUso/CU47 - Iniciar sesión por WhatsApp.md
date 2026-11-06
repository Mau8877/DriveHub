# Caso de uso

| Id   | Nombre                       | Paquete                | Prioridad | Estado    |
| ---- | ---------------------------- | ---------------------- | --------- | --------- |
| CU47 | Iniciar sesión por WhatsApp  | P1. Identidad y acceso | Media     | Hecho |

**Actor principal:** Estudiante (con cuenta, escribiendo desde WhatsApp)
**Actores secundarios:** n8n (orquesta el chat); Identidad (dueña de las cuentas)
**Objetivo:** Que un estudiante con cuenta se reconozca por su número de WhatsApp, para que el chat actúe con sus permisos sin pedirle la contraseña en cada mensaje.
**Plataforma:** WhatsApp (sin interfaz propia; el chat lo orquesta n8n)

**Descripción:**
La autoescuela tiene su propio número de WhatsApp. Cuando alguien le escribe, n8n pregunta a Identidad quién es ese número. Si el número ya está vinculado a una cuenta de estudiante, el chat sigue como esa persona. Si no está vinculado, el bot ofrece «Ya tengo cuenta» o «Quiero registrarme» ([CU48](CU48%20-%20Registrarse%20por%20WhatsApp.md)). Quien dice que ya tiene cuenta escribe su correo; el sistema le envía un código de 6 dígitos a ese correo y la persona lo escribe en el chat. Con el código correcto, el número queda vinculado a la cuenta y no se vuelve a pedir nada. En cada conversación posterior, n8n le pide a Identidad una sesión de corta duración **de esa persona**, que usa para consultar y operar con los mismos permisos que tendría en la app.

---

## Precondiciones

- La persona tiene una cuenta de estudiante activa en la escuela de este chat ([CU01](CU01%20-%20Registrar%20estudiante.md)) o la crea por [CU48](CU48%20-%20Registrarse%20por%20WhatsApp.md).
- Identidad y el servidor de correo están disponibles.

## Flujo principal

1. La persona escribe al WhatsApp de la escuela.
2. n8n consulta a Identidad por el número. No está vinculado.
3. El bot pregunta si ya tiene cuenta o quiere registrarse. La persona elige «Ya tengo cuenta».
4. La persona escribe el correo de su cuenta.
5. El sistema responde siempre lo mismo («si el correo tiene cuenta, te enviamos un código») y, si la cuenta existe y es válida, envía por correo un código de 6 dígitos, de un solo uso y válido por 10 minutos.
6. La persona escribe el código en el chat.
7. El sistema lo comprueba, vincula el número a la cuenta y confirma por el chat.
8. En ese y en cada mensaje posterior, n8n obtiene de Identidad una sesión de 5 minutos del estudiante y la usa para atender el pedido ([CU49](CU49%20-%20Conversar%20por%20WhatsApp.md)).

## Flujos alternativos y excepciones

- **A1 (paso 2):** El número ya está vinculado → se omiten los pasos 3 a 7 y el chat sigue como esa persona.
- **A2 (paso 5):** El correo no tiene cuenta, la cuenta no es de estudiante o es de otra escuela → el sistema responde igual que si existiera y no envía código (no revela si el correo existe).
- **A3 (paso 5):** La cuenta está bloqueada o inactiva → no se envía código; el bot indica que contacte a la escuela.
- **A4 (paso 5):** El servidor de correo no responde → el bot informa que no se pudo enviar y que reintente más tarde.
- **A5 (paso 7):** Código incorrecto, vencido o ya usado → el bot lo informa; tras 5 intentos fallidos el código se invalida y hay que pedir otro.
- **A6 (paso 7):** El número ya está vinculado a otra cuenta → la vinculación anterior se reemplaza solo después de este mismo código válido, y la sesión de la cuenta anterior deja de poder emitirse.
- **A7 (cualquier paso):** Se supera el límite de pedidos de código por número o por correo → el bot pide esperar (429 `ERR_SYS_02`).
- **A8 (paso 8):** La cuenta se bloquea o desactiva después de vincularse → Identidad no emite la sesión y el bot indica que contacte a la escuela.

## Reglas de negocio

- **Un número, una cuenta; una cuenta, un número.** Vincular un número nuevo a una cuenta reemplaza el anterior.
- **Solo estudiantes de esta escuela:** instructores, administradores y el Super administrador no usan el chat. El chat atiende a la escuela de la semilla del proyecto; el número de destino determina la escuela.
- **El número lo verificó Meta; la cuenta la prueba el código del correo.** Se pide una sola vez por número.
- **Sin contraseña en el chat:** el chat nunca pide ni recibe contraseñas.
- **Código:** 6 dígitos, un solo uso, 10 minutos, máximo 5 intentos; en la base solo se guarda su hash. Pedir uno nuevo invalida el anterior.
- **Sesión por WhatsApp:** el access token que Identidad emite para el número vinculado dura 5 minutos, lleva los roles reales de la persona y se valida en cada servicio igual que el de la app ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)). Ningún servicio confía en n8n: valida el token.
- **Credencial de servicio de n8n:** la ruta que emite la sesión es interna (`/internal/v1/...`, fuera del gateway) y se protege con un secreto compartido (`X-Service-Key`, comparado en tiempo constante), igual que las rutas internas de Pagos ([MICROSERVICIOS.md](../../.claude/rules/MICROSERVICIOS.md)). Solo emite sesiones de cuentas con rol Estudiante, vinculadas y activas.
- **Auditoría:** cada sesión emitida y cada vinculación se registran con el `codigo_usuario`, nunca con el número completo, el correo ni el código.
- El número de teléfono, el correo y el código no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- El número queda vinculado a la cuenta del estudiante.
- n8n puede obtener sesiones cortas de esa persona.

## Criterios de aceptación

- **CA01:** Dado un número no vinculado, el sistema debe informarlo a n8n sin crear nada.
- **CA02:** Dado un correo con cuenta de estudiante activa de esta escuela, el sistema debe enviar un código de 6 dígitos y responder el mismo mensaje que para un correo inexistente.
- **CA03:** Dado un correo sin cuenta, de otro rol o de otra escuela, el sistema debe responder el mismo mensaje y no enviar nada.
- **CA04:** Dado un código correcto y vigente, el sistema debe vincular el número a la cuenta y consumir el código.
- **CA05:** Dado un código incorrecto, vencido o usado, el sistema debe rechazarlo; tras 5 fallos debe invalidarlo.
- **CA06:** Dado un número vinculado, el sistema debe emitir una sesión de 5 minutos con los roles de la cuenta a quien presente la credencial de servicio.
- **CA07:** Dada una petición sin credencial de servicio o con una incorrecta, el sistema debe rechazar la emisión y no revelar si el número está vinculado.
- **CA08:** Dada una cuenta bloqueada, inactiva o que no es de estudiante, el sistema no debe emitir sesión.
- **CA09:** Dado un número ya vinculado a otra cuenta, el sistema debe reemplazar la vinculación solo con un código válido.
- **CA10:** Dados los pedidos de código, el sistema debe limitarlos por número y por correo (429 `ERR_SYS_02`).
- **CA11:** Dado cualquier paso, ni el código, ni el correo ni el número completo deben aparecer en los registros.

## Fuera de alcance

- **Cerrar la vinculación desde el chat o la app** (desvincular un número).
- **Instructores y administradores** por WhatsApp.
- **Varias escuelas o varios números de WhatsApp.**

## Desarrollador

Leonel
