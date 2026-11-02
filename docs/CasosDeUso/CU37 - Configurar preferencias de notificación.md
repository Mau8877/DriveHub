# Caso de uso

| Id   | Nombre                                  | Paquete            | Prioridad | Estado    |
| ---- | --------------------------------------- | ------------------ | --------- | --------- |
| CU37 | Configurar preferencias de notificación | P7. Notificaciones | Media     | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** Instructor
**Objetivo:** Elegir por qué canales y de qué asuntos quiere recibir avisos, y en qué horas no quiere que le suene el teléfono.
**Plataforma:** Móvil

> Las preferencias las respeta [CU36: Enviar notificación](CU36%20-%20Enviar%20notificaci%C3%B3n.md) en cada aviso. Lo que ya se envió se consulta en [CU38: Consultar historial de notificaciones](CU38%20-%20Consultar%20historial%20de%20notificaciones.md).

**Descripción:**
El Estudiante o el Instructor abre su Perfil y entra a «Notificaciones». El sistema muestra tres canales (correo, notificaciones push y dentro de la app) y cuatro tipos de aviso (clases y reservas, pagos, exámenes y recordatorios), cada uno con un interruptor, y unas horas de silencio opcionales. Si la persona nunca las configuró, todo aparece activado y sin silencio. La persona cambia lo que quiere y guarda; desde el siguiente aviso, el sistema solo envía lo que ella eligió. Las horas de silencio detienen únicamente las notificaciones push (las que hacen sonar el teléfono): el correo y la bandeja de la app siguen funcionando. Si la configuración no se puede cargar o guardar, la app lo dice y deja reintentar sin perder lo que la persona tocó.

---

## Precondiciones

- La persona tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) como Estudiante o Instructor.

## Flujo principal

1. La persona abre «Perfil > Notificaciones».
2. El sistema muestra sus preferencias guardadas o, si nunca las configuró, las de por defecto (todo activado, sin silencio).
3. La persona activa o desactiva canales y tipos de aviso.
4. La persona, si quiere, activa las «Horas de silencio» y elige desde qué hora hasta qué hora.
5. La persona pulsa «Guardar».
6. El sistema guarda las preferencias, las muestra tal como quedaron y confirma con un aviso breve.

## Flujos alternativos y excepciones

- **A1 (paso 2):** No hay conexión o el servicio no responde → la app avisa y ofrece «Reintentar»; no muestra interruptores con valores inventados.
- **A2 (paso 6):** El guardado falla (conexión, servicio) → la app avisa, conserva lo que la persona había cambiado y deja volver a pulsar «Guardar».
- **A3 (paso 4):** Elige una sola de las dos horas, o las dos iguales → la app no deja guardar y lo explica; si el servicio lo recibe igual, responde 400 `ERR_NOT_03`.
- **A4 (paso 4):** Desactiva las «Horas de silencio» → al guardar, el silencio queda sin configurar.
- **A5 (paso 5):** El correo de la cuenta no tiene un formato válido → 400 `ERR_SYS_01` (no debería pasar: lo toma del perfil).
- **A6 (cualquier paso):** La persona no es Estudiante ni Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A7 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo las propias:** cada persona ve y cambia únicamente sus preferencias; quién es sale del token, nunca de la URL ni del cuerpo.
- **Por defecto:** sin preferencias guardadas rige todo activado y sin silencio. Abrir la pantalla no guarda nada; se guarda al pulsar «Guardar».
- **Guardar reemplaza todo:** cada guardado envía la configuración completa; lo que no se envía (por ejemplo, las horas de silencio) queda sin configurar.
- **Se permite apagar todo:** la persona puede desactivar todos los canales o todos los tipos.
- **Silencio solo para el push,** en hora de Bolivia, `HH:mm`; la franja puede cruzar la medianoche (22:00 a 06:00). Se configuran las dos horas o ninguna, y no pueden ser iguales.
- **Los mismos ajustes valen para todos los dispositivos** de la persona (teléfono y navegador).
- **Correo:** la app envía con cada guardado el correo de la cuenta de la persona (el de su perfil, [CU06](CU06%20-%20Gestionar%20perfil.md)); el servicio solo comprueba que tenga formato de correo. [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) lo usa cuando el aviso no trae uno.
- **Efecto desde el siguiente aviso:** lo ya enviado no cambia.
- **Apagar un canal no borra el historial** ([CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md)).

## Postcondiciones

- Las preferencias de la persona quedan guardadas (una sola configuración por persona) y [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) las aplica desde el siguiente aviso.

## Criterios de aceptación

- **CA01:** Dada una persona que nunca configuró nada, el sistema debe devolver todo activado y sin silencio, sin guardar nada.
- **CA02:** Dada una configuración guardada, el sistema debe devolverla igual en la siguiente consulta, y guardar de nuevo no debe crear una segunda configuración.
- **CA03:** Dado un tipo de aviso desactivado, [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) no debe enviar ni guardar avisos de ese tipo a la persona.
- **CA04:** Dado un canal desactivado, [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) no debe enviar por ese canal a la persona.
- **CA05:** Dada una franja de silencio, el sistema no debe enviar notificaciones push dentro de ella y debe seguir enviando el correo y la bandeja.
- **CA06:** Dada una franja con una sola hora o con las dos iguales, el sistema debe responder 400 `ERR_NOT_03` y no cambiar nada.
- **CA07:** Dado un guardado sin franja, el sistema debe dejar el silencio sin configurar aunque antes lo tuviera.
- **CA08:** Dada otra persona, el sistema no debe mostrarle ni permitirle cambiar las preferencias ajenas.
- **CA09:** Dada una persona que no es Estudiante ni Instructor, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA10:** Dado un correo con formato inválido, el sistema debe responder 400 `ERR_SYS_01`.
- **CA11:** Dado un fallo al guardar, la app debe conservar lo que la persona había cambiado y permitir reintentar.

## Fuera de alcance

- **Ver el historial y marcar como leída** ([CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md)).
- **Preferencias en la web:** el panel de la escuela recibe los avisos con los ajustes por defecto; no hay pantalla para cambiarlos.
- **Elegir canal por cada tipo de aviso** (una matriz de 3 por 4): hoy canales y tipos son independientes.
- **Mantener al día el correo** cuando la persona lo cambia en su perfil: se actualiza la próxima vez que guarda sus preferencias.
- **Pedir el permiso de notificaciones del teléfono** y registrar su token de Firebase ([CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md), parte móvil pendiente).

## Desarrollador

Leonel
