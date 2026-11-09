# Caso de uso

| Id   | Nombre                          | Paquete                      | Prioridad | Estado      |
| ---- | ------------------------------- | ---------------------------- | --------- | ----------- |
| CU51 | Configurar el servidor de la app | P9. Plataforma (transversal) | Media     | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** Instructor
**Objetivo:** Indicarle a la app a qué servidor de la autoescuela conectarse, una sola vez, y poder cambiarlo después si hace falta.
**Plataforma:** Móvil

> Es lo primero que ve la persona al abrir la app por primera vez, antes de [CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md) y de [CU01: Registrar estudiante](CU01%20-%20Registrar%20estudiante.md): sin servidor, ninguna de las dos puede hablar con nadie.

**Descripción:**
La primera vez que se abre la app, en vez del inicio de sesión aparece una pantalla donde la persona escribe la dirección del servidor de su autoescuela (por ejemplo `http://192.168.1.10:8080`). La pantalla sugiere una dirección, acepta que se escriba sin `http://` y agrega sola la ruta de la API. La persona puede probar la conexión: la app le dice si encontró un servidor de DriveHub, si algo respondió pero no parece uno, o si no respondió nada; la prueba es solo informativa, y la dirección se puede guardar aunque en ese momento no haya conexión. Al guardar, la app recuerda la dirección y sigue al inicio de sesión como siempre. Desde entonces la app arranca directo en el inicio de sesión sin volver a preguntar; si hace falta usar otro servidor, el inicio de sesión ofrece «Cambiar servidor». Si la dirección está mal escrita, la pantalla lo dice y no guarda nada.

---

## Precondiciones

- La app está instalada. No hace falta tener cuenta ni sesión.

## Flujo principal

1. La persona abre la app por primera vez.
2. La app no tiene servidor guardado y muestra la pantalla «Conecta tu app», con una dirección sugerida.
3. La persona escribe o corrige la dirección del servidor.
4. La persona, si quiere, pulsa «Probar conexión»; la app le dice qué encontró.
5. La persona pulsa «Guardar y continuar».
6. La app guarda la dirección en el teléfono, empieza a usarla en el momento y muestra el inicio de sesión.

### Cambiar el servidor más adelante

1. En el inicio de sesión, la persona pulsa «Cambiar servidor».
2. La app muestra la pantalla con la dirección actual.
3. La persona escribe la nueva y pulsa «Guardar y continuar», o pulsa «Cancelar» y vuelve al inicio de sesión sin cambios.

## Flujos alternativos y excepciones

- **A1 (paso 5):** La dirección está vacía o mal escrita (otro protocolo, con espacios, con usuario o parámetros) → la pantalla explica el problema bajo el campo y no guarda nada.
- **A2 (paso 4):** El servidor responde pero no es de DriveHub → la app advierte; la persona puede corregir o guardar igual.
- **A3 (paso 4):** El servidor no responde → la app lo dice, recuerda revisar que el teléfono esté en la misma red y que se puede guardar igual.
- **A4 (paso 5):** No se puede guardar en el teléfono → la app avisa y deja intentar de nuevo.
- **A5 (paso 2):** La persona intenta ir a cualquier otra pantalla sin haber elegido servidor → la app la devuelve a esta pantalla.
- **A6 (cambiar):** La persona ya tiene sesión iniciada → la pantalla no está disponible; para cambiar de servidor primero cierra sesión.

## Reglas de negocio

- **Se pregunta una sola vez.** Con un servidor ya guardado, la app arranca directo en el inicio de sesión (o en el inicio, si hay sesión).
- **Primero el servidor:** mientras no haya uno, no se puede ver el inicio de sesión, el registro ni la recuperación de contraseña.
- **Se escribe solo el servidor:** la ruta de la API (`/api/v1`) la agrega la app. Si la persona escribe `/api`, se toma como `/api/v1` (detrás de un proxy); cualquier otra ruta se respeta.
- **Sin `http://` ni `https://`, la app asume `http://`.** Con otro protocolo, o con espacios o caracteres no válidos en el nombre del servidor, no se acepta.
- **Probar no obliga ni cambia nada:** solo informa. Se reconoce un servidor de DriveHub porque responde con el formato estándar de respuestas de la API, sin necesidad de sesión.
- **Se puede guardar sin conexión:** la validación es del formato, no de la red.
- **El cambio se aplica al instante:** no hace falta reiniciar la app.
- **La dirección que sugiere la app** sale de la configuración de la propia app (para desarrollo, el emulador de Android); la persona puede cambiarla.
- **Cambiar de servidor no borra la sesión ni lo guardado de otras cuentas** (no se puede cambiar con sesión iniciada).
- **No se registra la dirección en los logs más allá de lo que ya hace el cliente HTTP.**

## Postcondiciones

- La app recuerda la dirección elegida y habla con ese servidor desde ese momento y en los siguientes arranques.
- Nada más cambia: ni la sesión, ni los datos de ninguna cuenta.

## Criterios de aceptación

- **CA01:** Dado que la app se abre por primera vez, el sistema debe mostrar la pantalla de servidor y no el inicio de sesión.
- **CA02:** Dado que no hay servidor elegido, el sistema no debe permitir llegar al inicio de sesión, al registro, a la recuperación de contraseña ni al inicio.
- **CA03:** Dada una dirección válida, el sistema debe guardarla, usarla en el momento y mostrar el inicio de sesión.
- **CA04:** Dada una dirección sin `http://`, el sistema debe tomarla como `http://`; dada una sin ruta, agregarle `/api/v1`; dada `/api`, convertirla en `/api/v1`.
- **CA05:** Dada una dirección vacía, con otro protocolo, con espacios o caracteres no válidos, o con usuario, parámetros o `#`, el sistema debe explicar el problema y no guardar nada.
- **CA06:** Dado «Probar conexión» sobre un servidor de DriveHub, el sistema debe avisar que lo encontró; sobre uno que responde otra cosa, advertirlo; sobre uno que no responde, decirlo y recordar que se puede guardar igual.
- **CA07:** Dado «Probar conexión», el sistema no debe guardar la dirección ni cambiar de pantalla.
- **CA08:** Dado que no hay conexión, el sistema debe permitir guardar una dirección válida.
- **CA09:** Dado un servidor ya guardado, el sistema debe arrancar directo en el inicio de sesión sin volver a preguntar.
- **CA10:** Dado el inicio de sesión, el sistema debe ofrecer «Cambiar servidor»; al guardar otro, debe usarlo en el momento y volver al inicio de sesión; al cancelar, no debe cambiar nada.
- **CA11:** Dada una persona con sesión iniciada, el sistema no debe mostrarle la pantalla de servidor.
- **CA12:** Dado un cambio de servidor, todas las llamadas siguientes de la app deben ir al servidor nuevo, incluidas las de servicios que ya estaban creados.
- **CA13:** Dada una dirección con espacios en el nombre del servidor, el sistema no debe aceptarla ni guardarla codificada.

## Fuera de alcance

- **Descubrir el servidor solo** (escaneo de la red, código QR o enlace de invitación).
- **Elegir el servidor entre varias autoescuelas** o guardar más de una dirección.
- **Cambiar de servidor con sesión iniciada** o desde el perfil.
- **Verificar certificados propios** o aceptar certificados no confiables.
- **El panel web:** ya está servido desde el servidor que lo entrega.

## Desarrollador

Leonel
