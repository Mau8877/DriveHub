# Caso de uso

| Id   | Nombre         | Paquete                | Prioridad | Estado    |
| ---- | -------------- | ---------------------- | --------- | --------- |
| CU03 | Cerrar sesión | P1. Identidad y acceso | Alta      | Pendiente |

**Actor principal:** Estudiante, Instructor, Administrador de escuela o Super admin (cualquier persona con una sesión abierta)
**Actores secundarios:** —
**Objetivo:** Terminar la sesión del dispositivo que está usando, de modo que nadie pueda seguir usándola desde ahí.
**Plataforma:** Móvil y Web

**Descripción:**
La persona elige "Cerrar sesión" en la web o en la app. El sistema invalida la sesión renovable de **ese dispositivo**, el cliente borra todo lo que guardaba de la sesión (el acceso en memoria y, en la web, la cookie; en la app, el almacenamiento seguro) y lleva a la persona al inicio de sesión. Cerrar sesión siempre termina con la persona fuera, aunque la sesión ya estuviera vencida o cerrada, o aunque el servidor no responda: lo que el cliente guarda se borra de todos modos. La sesión del **otro** dispositivo (web o móvil) no se toca.

Este CU cubre cerrar la sesión del dispositivo actual. Iniciar y mantener la sesión es [CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md).

---

## Precondiciones

- La persona tiene una sesión abierta en el dispositivo (ver [CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).

## Flujo principal

1. La persona elige "Cerrar sesión" (menú del layout en la web; pantalla de inicio en la app).
2. El cliente le pide al sistema cerrar la sesión de este dispositivo, enviando la sesión renovable que guarda (en la web viaja sola en la cookie protegida; en la app, el cliente la envía).
3. El sistema invalida esa sesión renovable.
4. El sistema responde que la sesión quedó cerrada y, en la web, ordena borrar la cookie.
5. El cliente borra la sesión que guardaba (acceso en memoria del shell, o almacenamiento seguro del teléfono) y cancela la renovación en segundo plano.
6. El cliente lleva a la persona al inicio de sesión.

## Flujos alternativos y excepciones

- **A1 (paso 3):** La sesión renovable no existe, ya venció, ya estaba cerrada o no llegó (por ejemplo, la cookie ya expiró) → el sistema responde igual que en el caso exitoso, sin revocar nada más, y en la web igual ordena borrar la cookie. Cerrar sesión es idempotente.
- **A2 (paso 2):** El sistema no responde o hay un error de red → el cliente igual borra la sesión local y lleva a la persona al inicio de sesión (paso 5 y 6). La sesión renovable queda vigente en el servidor hasta que venza.
- **A3 (paso 2):** Se superó el límite de peticiones → el sistema responde 429 (`ERR_SYS_02`); el cliente igual borra la sesión local (como en A2).
- **A4 (paso 5):** La persona tiene la web abierta en otra pestaña → esa pestaña conserva su acceso en memoria hasta que venza (máximo 15 minutos); al intentar renovar recibe 401 y la lleva al inicio de sesión ([CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md), A8).

## Reglas de negocio

- **Alcance:** se cierra solo la sesión del dispositivo desde el que se pide (`WEB` o `MOVIL`); la del otro dispositivo sigue vigente. No existe en este CU un "cerrar todas las sesiones".
- **Qué se invalida:** la sesión renovable (`refresh_token`, marcada `revocado`). El acceso (JWT de 15 minutos) es sin estado y no se invalida antes de tiempo ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)): sigue siendo válido hasta que venza, pero sin sesión renovable ya no puede extenderse.
- **No exige acceso vigente:** cerrar sesión no requiere un acceso válido, porque la sesión puede haber vencido y la persona debe poder salir igual. El endpoint es público; la identificación es la propia sesión renovable, que solo tiene el cliente.
- **Idempotente y sin revelar estados:** la respuesta es la misma exista o no la sesión, para no confirmar a un tercero si un token es válido.
- **El cliente siempre limpia:** el borrado local no depende de la respuesta del servidor.
- **Un cierre de sesión no revoca las sesiones de otras cuentas ni del otro dispositivo,** aunque el token recibido pertenezca a otro dispositivo (en ese caso se trata como A1).
- **Registros:** el cierre exitoso se registra a nivel `INFO` con el código de la persona y el dispositivo; nunca el token ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- La sesión renovable de ese dispositivo quedó revocada y no se puede usar para renovar.
- El cliente no conserva acceso ni sesión renovable, y la persona está en el inicio de sesión.
- La sesión del otro dispositivo, si existía, sigue vigente.

## Criterios de aceptación

- **CA01:** Dado que la persona tiene una sesión abierta, el sistema debe revocar la sesión renovable de ese dispositivo al cerrar sesión, de modo que renovar con ella responda 401.
- **CA02:** Dado que la persona cierra sesión en la web, el sistema debe ordenar borrar la cookie `refreshToken` y el shell debe borrar el acceso de la memoria y llevar al inicio de sesión.
- **CA03:** Dado que la persona cierra sesión en la app, el cliente debe borrar el almacenamiento seguro y llevar al inicio de sesión.
- **CA04:** Dado que la sesión renovable no existe, venció o ya estaba cerrada, el sistema debe responder con éxito igual que en el caso normal y no revocar ninguna otra sesión.
- **CA05:** Dado que la persona tiene sesión en la web y en la app, el sistema debe conservar la del otro dispositivo al cerrar la de uno.
- **CA06:** Dado que el servidor no responde o responde con error, el cliente debe borrar igualmente la sesión local y llevar al inicio de sesión.
- **CA07:** Dado que el acceso ya venció, el sistema debe permitir cerrar sesión igual (sin exigir un acceso vigente).
- **CA08:** Dado que se cerró la sesión, el cliente no debe poder volver a recuperarla al recargar la página ni al reabrir la app sin ingresar de nuevo las credenciales.
- **CA09:** Dado cualquier cierre de sesión, ningún token debe aparecer en los registros del sistema.
- **CA10:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`) y el cliente debe borrar igualmente la sesión local.

## Desarrollador

Mauro
