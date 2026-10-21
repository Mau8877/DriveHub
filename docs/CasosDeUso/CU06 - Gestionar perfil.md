# Caso de uso

| Id   | Nombre           | Paquete                | Prioridad | Estado    |
| ---- | ---------------- | ---------------------- | --------- | --------- |
| CU06 | Gestionar perfil | P1. Identidad y acceso | Media     | Pendiente |

**Actor principal:** Estudiante, Instructor, Administrador de escuela o Super admin (cualquier persona con sesión, sobre su propia cuenta)
**Actores secundarios:** —
**Objetivo:** Mantener al día los datos personales, la foto, la contraseña y el correo de la propia cuenta.
**Plataforma:** Web y Móvil

**Descripción:**
En la barra superior de la web, el menú del avatar tiene la opción **"Editar perfil"** (en la app, la pestaña "Perfil" de la barra inferior). Al elegirla, la persona ve su perfil: nombres, apellidos, fecha de nacimiento, teléfono, foto, correo, y, solo de lectura, su rol, su escuela y si su correo está verificado. Desde ahí puede editar sus datos personales (nombres, apellidos, teléfono y fecha de nacimiento), subir, reemplazar o quitar su foto, cambiar su contraseña y cambiar su correo. Toda foto que se sube pasa antes por una revisión básica: solo se admiten imágenes JPG, PNG o WebP de hasta 5 MB y 4000×4000 píxeles, y se rechaza cualquier archivo que no sea realmente una imagen legible del formato que dice ser. Cambiar la contraseña exige la actual y cierra las sesiones de los demás dispositivos, conservando la actual. Cambiar el correo exige la contraseña actual y no tiene efecto hasta que la persona abre el enlace que el sistema envía al correo nuevo.

Este CU agrupa cinco operaciones (ver, editar datos, gestionar foto, cambiar contraseña, cambiar correo) por decisión del equipo. Los datos de negocio del estudiante y del instructor (los gestiona Escuela, CU08 y CU09) no forman parte de este CU.

---

## Precondiciones

- La persona tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).

## Flujo principal (ver perfil)

1. La persona abre el menú del avatar en la barra superior y elige "Editar perfil".
2. El sistema muestra su perfil: nombres, apellidos, fecha de nacimiento, teléfono, foto, correo, rol, escuela (el Super admin no tiene) y si el correo está verificado.
3. Desde el perfil, la persona puede ir a cualquiera de los flujos siguientes.

## Flujo F1: editar datos personales

1. En el perfil, la persona modifica nombres, apellidos, teléfono y/o fecha de nacimiento y guarda.
2. El sistema valida los datos con las mismas reglas del registro ([CU01](CU01%20-%20Registrar%20estudiante.md)).
3. El sistema guarda los cambios y muestra el perfil actualizado.

## Flujo F2: gestionar la foto

1. En el perfil, la persona elige subir una foto y selecciona un archivo.
2. El sistema revisa el archivo: que pese hasta 5 MB y no supere 4000×4000 píxeles, que su contenido (no su extensión ni el tipo que declara el cliente) corresponda a JPG, PNG o WebP, y que pueda leerse completo como imagen.
3. El sistema guarda la foto como la foto de la cuenta, descarta la anterior si existía y muestra la nueva en el perfil y en el avatar de la barra superior.
4. Para quitar la foto, la persona elige "Quitar foto": el sistema borra la foto y la cuenta vuelve a mostrar el avatar por defecto.

## Flujo F3: cambiar la contraseña

1. En el perfil, la persona elige cambiar la contraseña e ingresa la contraseña actual, la nueva y la confirmación.
2. El sistema valida que la actual sea correcta y que la nueva cumpla las reglas del registro (mínimo 9 caracteres) y sea distinta de la actual.
3. El sistema guarda la contraseña nueva.
4. El sistema cierra las sesiones de los demás dispositivos (web y móvil) y conserva la sesión actual.
5. El cliente informa que la contraseña cambió.

## Flujo F4: cambiar el correo

1. En el perfil, la persona elige cambiar el correo e ingresa la contraseña actual y el correo nuevo.
2. El sistema valida la contraseña actual, el formato del correo nuevo y que ninguna otra cuenta lo use.
3. El sistema invalida los enlaces de cambio de correo anteriores, genera uno nuevo de un solo uso y válido por 24 horas, y lo envía al correo **nuevo**. El correo vigente de la cuenta no cambia todavía.
4. El cliente informa que se envió un correo a la dirección nueva y que el cambio se completa al abrir el enlace.
5. La persona abre el enlace, que lleva a una página pública de la web.
6. El sistema comprueba el enlace, cambia el correo de la cuenta, lo marca como verificado y consume el enlace.
7. El sistema envía un aviso informativo al correo anterior indicando que el correo de la cuenta cambió.
8. La página informa que el correo quedó cambiado; al volver a la aplicación, el cliente actualiza la sesión con el correo nuevo.

## Flujos alternativos y excepciones

- **A1 (F1, paso 2):** Un dato no cumple las reglas (campo obligatorio vacío, longitud excedida, menor de 18 años) → el cliente indica el campo y no envía; el sistema responde 400 `ERR_SYS_01` si llega igual.
- **A2 (F2, paso 2):** El archivo no es JPG, PNG ni WebP, o su contenido no corresponde al formato declarado (por ejemplo, un ejecutable renombrado como imagen) → el sistema responde 400 y no guarda nada.
- **A3 (F2, paso 2):** El archivo pesa más de 5 MB o la imagen supera 4000×4000 píxeles → el sistema responde 400 y no guarda nada.
- **A4 (F2, paso 2):** El archivo está corrupto o no se puede leer completo como imagen (por ejemplo, una imagen con datos ajenos o un archivo disfrazado) → el sistema responde 400, no lo guarda y deja constancia en los registros (sin el contenido del archivo).
- **A5 (F2, paso 4):** La cuenta no tiene foto → el sistema responde de forma idempotente, sin error.
- **A6 (F3 y F4, paso 2):** La contraseña actual es incorrecta → el sistema responde 400 con un código propio (no 401, para que el cliente no lo confunda con una sesión vencida); no cambia nada, no suma intentos fallidos ni bloquea la cuenta.
- **A7 (F3, paso 2):** La contraseña nueva no cumple las reglas → el sistema responde 400 `ERR_SYS_01` indicando el campo. Si es igual a la actual → el sistema responde 400 con un código propio.
- **A8 (F3, paso 1):** La confirmación no coincide con la contraseña nueva → el cliente lo indica y no envía.
- **A9 (F4, paso 2):** El correo nuevo ya lo usa otra cuenta, o es igual al actual → el sistema responde 409 y no envía nada.
- **A10 (F4, paso 3):** El servidor de correo no responde → el sistema responde 500 con un mensaje genérico, el enlace generado no se entrega y el correo de la cuenta no cambia.
- **A11 (F4, paso 6):** El enlace es inválido, venció, ya se usó, fue reemplazado por uno más nuevo, o el correo nuevo pasó a ser de otra cuenta mientras tanto → el sistema responde 400 y la página indica que el enlace no sirve y que se puede pedir otro desde el perfil.
- **A12 (todos los flujos con sesión):** La sesión venció → el sistema responde 401 y el cliente la renueva como en [CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md) (A8).
- **A13 (cualquier petición):** Se superó el límite de peticiones → el sistema responde 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo la cuenta propia:** la persona gestiona únicamente su cuenta; la identidad sale de la sesión, nunca de un dato que mande el cliente.
- **Datos de solo lectura:** el rol, la escuela y el estado de verificación del correo no se editan desde el perfil.
- **El correo no se edita como un dato más:** solo cambia por F4.
- **Reglas de los campos:** las mismas que en el registro ([CU01](CU01%20-%20Registrar%20estudiante.md)) y las mismas en la web y en la app. El backend manda.
- **Foto:** formatos JPG, PNG y WebP; máximo 5 MB y 4000×4000 píxeles; una foto por cuenta; subir una nueva reemplaza y descarta la anterior. Se valida el contenido real del archivo, no solo su extensión ni el tipo que declara el cliente.
- **Revisión básica de la foto:** antes de guardarla se comprueba su tamaño, su formato real por el contenido y que se pueda leer completa como imagen. No es un antivirus ni se apoya en un servicio externo. Un archivo rechazado no deja rastro en el almacenamiento.
- **Contraseña actual obligatoria** para cambiar la contraseña y para pedir el cambio de correo; un error de la actual no bloquea la cuenta (lo limita el rate limit del gateway).
- **Contraseña nueva:** mínimo 9 caracteres, distinta de la actual.
- **Al cambiar la contraseña:** se cierran las sesiones de los demás dispositivos y se conserva la actual; el contador de intentos fallidos no se modifica.
- **Cambio de correo:** el correo vigente se conserva hasta confirmar el nuevo; el enlace es de un solo uso, válido 24 horas, y pedir otro invalida los anteriores. En la base solo se guarda su hash ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)). Un enlace de este tipo no sirve para verificar el correo ni para restablecer la contraseña, ni al revés ([CU04b](CU04b%20-%20Verificar%20correo.md), [CU04](CU04%20-%20Recuperar%20contrase%C3%B1a.md)).
- **Unicidad del correo:** el correo es único en toda la plataforma, se guarda en minúsculas y se vuelve a comprobar al confirmar el enlace.
- **El correo nuevo queda verificado** al confirmar el enlace, porque abrirlo demuestra que es de la persona; las sesiones no se cierran por este cambio.
- **Aviso al correo anterior:** informativo, sin enlaces ni acciones; si su envío falla, el cambio ya hecho no se revierte y se registra el fallo.
- **El enlace apunta a la web** configurada para el entorno (`APP_WEB_URL`), también para quien usa la app.
- **Mensajes en español** y sin detalles técnicos.
- **Registros:** solo se registra el hecho de cada operación con el código de la persona; nunca contraseñas, enlaces, tokens ni el contenido de las fotos ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **En desarrollo** los correos llegan a la bandeja de Mailpit (http://localhost:8025), no a un buzón real ([INFRAESTRUCTURA.md](../INFRAESTRUCTURA.md)).

## Postcondiciones

- F1: la cuenta tiene los datos personales actualizados.
- F2: la cuenta tiene la foto nueva (y la anterior ya no existe) o no tiene foto.
- F3: la cuenta tiene la contraseña nueva, la anterior ya no sirve y solo la sesión actual sigue activa.
- F4: la cuenta tiene el correo nuevo verificado, el enlace usado no sirve otra vez, el correo anterior recibió el aviso y la sesión muestra el correo nuevo.

## Criterios de aceptación

- **CA01:** Dado que la persona tiene sesión, el menú del avatar de la barra superior debe ofrecer "Editar perfil", que abre su perfil con sus datos, su rol y su escuela (si tiene).
- **CA02:** Dado datos válidos, el sistema debe guardar nombres, apellidos, teléfono y fecha de nacimiento y mostrar el perfil actualizado.
- **CA03:** Dado un dato inválido (campo obligatorio vacío, longitud excedida o menor de 18 años), el sistema debe responder 400 `ERR_SYS_01` indicando el campo y no cambiar nada.
- **CA04:** Dado un archivo JPG, PNG o WebP de hasta 5 MB que se lee completo como imagen, el sistema debe guardarlo como foto de la cuenta, descartar la anterior y mostrarlo en el perfil y en el avatar.
- **CA05:** Dado un archivo de otro formato, o cuyo contenido no corresponde a una imagen del formato declarado, el sistema debe responder 400 sin guardarlo.
- **CA06:** Dado un archivo de más de 5 MB o una imagen de más de 4000×4000 píxeles, el sistema debe responder 400 sin guardarlo.
- **CA07:** Dado un archivo corrupto o que no se puede leer completo como imagen, el sistema debe rechazarlo con 400, no guardarlo y dejar constancia en los registros sin su contenido.
- **CA08:** Dado que la persona quita su foto, el sistema debe borrarla y mostrar el avatar por defecto; si no tenía foto, no debe devolver error.
- **CA09:** Dada la contraseña actual correcta y una nueva válida y distinta, el sistema debe guardarla, cerrar las sesiones de los demás dispositivos y conservar la actual; iniciar sesión funciona con la nueva y no con la anterior.
- **CA10:** Dada una contraseña actual incorrecta, el sistema debe responder 400 con su código propio, sin cambiar nada, sin sumar intentos fallidos y sin bloquear la cuenta.
- **CA11:** Dada una contraseña nueva que no cumple las reglas, el sistema debe responder 400 `ERR_SYS_01` indicando el campo; si es igual a la actual, debe responder 400 con su código propio. En ambos casos no cambia nada.
- **CA12:** Dada la contraseña actual correcta y un correo nuevo disponible, el sistema debe enviar el enlace al correo nuevo sin cambiar todavía el correo de la cuenta, e invalidar los enlaces de cambio anteriores.
- **CA13:** Dado un correo nuevo que ya usa otra cuenta o es igual al actual, el sistema debe responder 409 sin enviar nada.
- **CA14:** Dado un enlace de cambio de correo válido, el sistema debe cambiar el correo, marcarlo verificado, consumir el enlace y enviar un aviso al correo anterior.
- **CA15:** Dado un enlace inválido, vencido, usado, reemplazado, de otro tipo, o cuyo correo ya pertenece a otra cuenta, el sistema debe responder 400 sin cambiar el correo.
- **CA16:** Dado que el servidor de correo falla al pedir el cambio, el sistema debe responder 500 genérico, no entregar el enlace y conservar el correo de la cuenta.
- **CA17:** Dado que se cambió el correo, el cliente debe mostrar el correo nuevo al volver a la aplicación sin cerrar sesión.
- **CA18:** Dada cualquier operación de este caso de uso, ninguna contraseña, enlace, token ni contenido de archivo debe aparecer en los registros del sistema.
- **CA19:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Mauro
