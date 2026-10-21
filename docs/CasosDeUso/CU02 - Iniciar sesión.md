# Caso de uso

| Id   | Nombre         | Paquete                | Prioridad | Estado    |
| ---- | -------------- | ---------------------- | --------- | --------- |
| CU02 | Iniciar sesión | P1. Identidad y acceso | Alta      | Pendiente |

**Actor principal:** Estudiante, Instructor, Administrador de escuela o Super admin (cualquier persona con cuenta)
**Actores secundarios:** —
**Objetivo:** Identificarse una sola vez y poder usar todas las pantallas de su plataforma sin volver a iniciar sesión, manteniendo la sesión vigente mientras la use.
**Plataforma:** Móvil y Web

**Descripción:**
La persona abre la web o la app móvil, ingresa su correo y su contraseña, y el sistema la identifica. Si los datos son correctos y su rol puede usar esa plataforma, el sistema abre una sesión **única** para toda la aplicación: en la web, el shell guarda la sesión y todas las pantallas (identidad, escuela, pagos, exámenes, contenido, asistente) la comparten, así que pasar de una a otra no vuelve a pedir inicio de sesión; en la app, la sesión queda guardada de forma segura en el teléfono. La sesión se mantiene sola mientras la persona la usa: el sistema la renueva en segundo plano antes de que venza, y también la recupera al recargar la página o abrir otra pestaña sin pedir la contraseña de nuevo. Si la sesión ya no se puede renovar (venció, se cerró o fue reemplazada por otro inicio de sesión del mismo dispositivo), el sistema lleva a la persona al inicio de sesión. Si los datos son incorrectos, la cuenta está bloqueada o inactiva, la escuela está suspendida o el rol no puede usar esa plataforma, el sistema lo informa sin abrir sesión.

Este CU cubre iniciar sesión y mantenerla (renovación). Cerrar sesión es [CU03](CU03%20-%20Cerrar%20sesi%C3%B3n.md) y recuperar la contraseña es [CU04](CU04%20-%20Recuperar%20contrase%C3%B1a.md).

---

## Precondiciones

- La persona tiene una cuenta (por ejemplo, creada con [CU01: Registrar estudiante](CU01%20-%20Registrar%20estudiante.md) o por un administrador).

## Flujo principal

1. La persona abre la web (o la app) sin sesión; el sistema la lleva a la pantalla de inicio de sesión. Si venía de una dirección concreta de la web, el sistema la recuerda.
2. La persona ingresa su correo y su contraseña y envía el formulario.
3. El sistema valida que ambos campos estén completos.
4. El sistema verifica las credenciales y que la cuenta esté activa y no bloqueada.
5. El sistema verifica que la escuela de la cuenta esté activa (el Super admin no pertenece a ninguna escuela).
6. El sistema verifica que el rol de la cuenta puede usar la plataforma desde la que entra (web o móvil).
7. El sistema cierra las sesiones anteriores de esa cuenta **en ese mismo dispositivo** (web o móvil), abre la sesión nueva, reinicia el contador de intentos fallidos y registra la fecha del último ingreso.
8. El cliente guarda la sesión: el shell en la web, el almacenamiento seguro en la app.
9. El sistema lleva a la persona a la dirección que había pedido o, si no había ninguna, a la pantalla de inicio.

## Mantener la sesión

10. Mientras la persona usa la aplicación, el cliente renueva la sesión en segundo plano antes de que venza el acceso, sin pedir nada.
11. Si una pantalla recibe un rechazo por sesión vencida, el cliente renueva la sesión una vez y repite la petición; si no se puede renovar, lleva a la persona al inicio de sesión.
12. Al recargar la página o abrir otra pestaña en la web, el shell intenta recuperar la sesión sin pedir la contraseña; si no puede, lleva al inicio de sesión.
13. En cada renovación el sistema verifica de nuevo que la cuenta y la escuela sigan activas.

## Flujos alternativos y excepciones

- **A1 (paso 3):** Falta el correo o la contraseña → el sistema indica el campo faltante y no envía la solicitud.
- **A2 (paso 4):** El correo no existe o la contraseña es incorrecta → el sistema responde 401 con el mismo mensaje en ambos casos ("Correo o contraseña incorrectos"); si el correo existe, suma un intento fallido.
- **A3 (paso 4):** Con el quinto intento fallido consecutivo la cuenta pasa a bloqueada durante 15 minutos y el sistema responde 423; mientras dure el bloqueo, aunque la contraseña sea correcta, responde 423 con el aviso de cuándo podrá reintentar. Pasado el tiempo, la cuenta se desbloquea sola.
- **A4 (paso 4):** La cuenta está inactiva → el sistema responde 403 y no abre sesión.
- **A5 (paso 5):** La escuela de la cuenta está inactiva o suspendida → el sistema responde 403 y no abre sesión.
- **A6 (paso 6):** El rol no puede usar la plataforma desde la que entra (estudiante o instructor en la web; Administrador de escuela o Super admin en la app) → el sistema responde 403 indicando qué plataforma debe usar y no abre sesión.
- **A7 (paso 12):** La persona abre el inicio de sesión teniendo ya una sesión vigente → el sistema la lleva a la pantalla de inicio sin pedir datos.
- **A8 (pasos 10 a 12):** La sesión no se puede renovar (vencida, ya cerrada, reemplazada por otro inicio de sesión del mismo dispositivo, o ya usada) → el sistema responde 401 y el cliente lleva a la persona al inicio de sesión.
- **A9 (paso 13):** Al renovar, la cuenta quedó inactiva o bloqueada, o la escuela quedó suspendida → el sistema rechaza la renovación y el cliente lleva a la persona al inicio de sesión con el motivo.

## Reglas de negocio

- **Sesión única en la web:** el shell es el único que guarda la sesión; las pantallas de cada micro-frontend la reciben de él y **nunca** piden credenciales ni inician sesión por su cuenta ([ARQUITECTURA_FRONTEND.md](../../.claude/rules/ARQUITECTURA_FRONTEND.md)).
- **Una sesión activa por dispositivo:** hay un dispositivo "web" y uno "móvil". Iniciar sesión en la web cierra la sesión web anterior de esa cuenta, pero no la del móvil, y viceversa.
- **Quién entra por cada plataforma:** en la web, solo Super admin y Administrador de escuela; en la app, solo Estudiante e Instructor.
- **Duración:** el acceso dura 15 minutos y la sesión renovable, 7 días ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)). Cada renovación entrega una sesión renovable nueva e invalida la anterior (rotación): una sesión renovable ya usada no sirve otra vez.
- **Dónde se guarda la sesión:** en la web, el acceso vive solo en la memoria del shell y la sesión renovable en una cookie inaccesible para el código de la página; en la app, ambos en almacenamiento seguro del teléfono. Nunca en `localStorage`.
- **Bloqueo:** 5 intentos fallidos consecutivos bloquean la cuenta 15 minutos; un ingreso correcto reinicia el contador. Los intentos solo se cuentan para correos que existen.
- **Mensajes:** el rechazo por credenciales es siempre el mismo, exista o no el correo. Los mensajes están en español y no exponen detalles técnicos.
- **La escuela de la cuenta** viaja en la sesión para que los demás servicios puedan filtrar por escuela más adelante; el Super admin no tiene escuela.
- **Contraseñas y sesiones** nunca se escriben en los registros del sistema ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **El inicio de sesión es público y está protegido por el límite de peticiones del gateway** ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).

## Postcondiciones

- Existe una sesión activa en ese dispositivo y las anteriores del mismo dispositivo quedaron cerradas.
- El contador de intentos fallidos de la cuenta es 0 y su último ingreso está actualizado.
- Todas las pantallas de la plataforma reconocen la sesión sin pedir credenciales de nuevo.

## Criterios de aceptación

- **CA01:** Dado que la cuenta está activa, su escuela está activa y su rol puede usar la plataforma, el sistema debe abrir la sesión con las credenciales correctas y devolver al cliente el acceso, la sesión renovable (en la web, solo mediante la cookie protegida) y los datos básicos de la persona (código, nombres, apellidos y roles).
- **CA02:** Dado que la persona inició sesión en la web, el sistema debe permitirle pasar de un micro-frontend a otro sin pedir credenciales.
- **CA03:** Dado que la persona abre la web sin sesión, el sistema debe llevarla al inicio de sesión y, tras ingresar, a la dirección que había pedido.
- **CA04:** Dado que el correo no existe o la contraseña es incorrecta, el sistema debe responder 401 con el mismo mensaje en ambos casos y no abrir sesión.
- **CA05:** Dado que falta el correo o la contraseña, el cliente debe indicar el campo faltante sin enviar la solicitud.
- **CA06:** Dado que la cuenta acumula 5 intentos fallidos consecutivos, el sistema debe bloquearla 15 minutos y responder 423, incluso si luego se envía la contraseña correcta, hasta que pase el tiempo.
- **CA07:** Dado que pasaron los 15 minutos del bloqueo, el sistema debe aceptar de nuevo las credenciales correctas y reiniciar el contador de intentos.
- **CA08:** Dado que la cuenta está inactiva, el sistema debe responder 403 y no abrir sesión.
- **CA09:** Dado que la escuela de la cuenta está inactiva o suspendida, el sistema debe responder 403 y no abrir sesión; el Super admin no se ve afectado.
- **CA10:** Dado que un Estudiante o Instructor intenta entrar por la web, o un Administrador de escuela o Super admin por la app, el sistema debe responder 403 indicando la plataforma correcta y no abrir sesión.
- **CA11:** Dado que la persona inicia sesión en un dispositivo donde ya tenía una sesión, el sistema debe cerrar la anterior de ese dispositivo y conservar la del otro dispositivo.
- **CA12:** Dado que el acceso está por vencer, el cliente debe renovar la sesión en segundo plano sin interrumpir a la persona.
- **CA13:** Dado que una petición recibe un rechazo por sesión vencida, el cliente debe renovar una vez, repetir la petición y, si no se puede renovar, llevar a la persona al inicio de sesión.
- **CA14:** Dado que la persona recarga la página o abre otra pestaña en la web con la sesión vigente, el shell debe recuperar la sesión sin pedir la contraseña.
- **CA15:** Dado que se renueva con una sesión ya usada, vencida o cerrada, el sistema debe responder 401 y no entregar acceso.
- **CA16:** Dado que una renovación es exitosa, el sistema debe entregar una sesión renovable nueva e invalidar la anterior.
- **CA17:** Dado que al renovar la cuenta quedó inactiva o bloqueada, o su escuela suspendida, el sistema debe rechazar la renovación y el cliente debe llevar a la persona al inicio de sesión.
- **CA18:** Dado que la persona abre el inicio de sesión con una sesión vigente, el sistema debe llevarla al inicio sin pedir datos.
- **CA19:** Dado cualquier inicio de sesión, ni la contraseña ni los tokens deben aparecer en los registros del sistema.
- **CA20:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Mauro
