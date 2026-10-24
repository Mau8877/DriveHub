# Caso de uso

| Id    | Nombre               | Paquete                | Prioridad | Estado    |
| ----- | -------------------- | ---------------------- | --------- | --------- |
| CU08a | Perfil de la persona | P1. Identidad y acceso | Alta      | Hecho     |

**Actor principal:** Estudiante (persona que se registra)
**Actores secundarios:** Super admin (al dar de alta una escuela con su Administrador), cualquier persona con sesión (al editar su perfil)
**Objetivo:** Que toda cuenta tenga un perfil con los datos de la persona (identidad, contacto y ubicación), separado de los datos de acceso, y que el estudiante se registre con su documento, su sexo y su ubicación en un mapa.
**Plataforma:** Web y Móvil

> Este CU **modifica** a [CU01](CU01%20-%20Registrar%20estudiante.md), [CU05](CU05%20-%20Gestionar%20escuelas.md) y [CU06](CU06%20-%20Gestionar%20perfil.md), que ya estaban hechos, y es el **prerrequisito de [CU08](CU08%20-%20Gestionar%20estudiantes.md)**. Donde este documento contradiga a esos tres, manda este.

**Descripción:**
Hasta ahora la cuenta (`usuario`) mezclaba el acceso (correo, contraseña, estado) con los datos de la persona (nombres, apellidos, teléfono, foto, fecha de nacimiento). Ahora cada cuenta, sea de Super admin, Administrador de escuela, Instructor o Estudiante, tiene además un **perfil** único: los datos de la persona pasan al perfil y se suman el documento de identidad (CI), el sexo y la ubicación. El estudiante no tiene una tabla propia en el servicio Escuela: todo lo suyo está en la cuenta y el perfil, y lo que sea de negocio (saldo, clases) vive en sus propios servicios y lo referencia por el código de la persona.

Al registrarse, el estudiante, además de lo de siempre, indica su CI, su sexo y su ubicación eligiendo un punto en un mapa; el sistema calcula la dirección a partir de ese punto. Su categoría de licencia arranca en "sin categoría" y solo la escuela la cambia ([CU08](CU08%20-%20Gestionar%20estudiantes.md)). Al dar de alta una escuela, el Super admin también indica CI, sexo y ubicación del Administrador. En su perfil ([CU06](CU06%20-%20Gestionar%20perfil.md)), cualquier persona edita todos esos datos salvo su categoría.

---

## Precondiciones

- La escuela a la que se registra el estudiante existe y está activa ([CU05](CU05%20-%20Gestionar%20escuelas.md)).
- Para editar el perfil, la persona tiene sesión iniciada ([CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- El catálogo de categorías de licencia está cargado (lo hace este CU).

## Flujo principal: el estudiante se registra con su perfil

1. En la app (o en la web, desde el enlace de su escuela), la persona abre el registro.
2. Completa nombres, apellidos, fecha de nacimiento, sexo, CI, teléfono (opcional), correo y contraseña.
3. Elige su ubicación en el mapa (F1).
4. Envía el formulario. El sistema valida los datos, comprueba que el correo y el CI no estén en uso y que sea mayor de edad.
5. El sistema crea la cuenta (rol `ESTUDIANTE`, asociada a la escuela, estado `ACTIVO`) y su perfil, con la dirección calculada desde el punto elegido y la categoría "sin categoría".
6. El sistema informa que el registro fue exitoso (como en CU01: sin iniciar sesión).

## Flujo F1: elegir la ubicación en el mapa

1. La persona toca o hace clic en un punto del mapa, o usa "Mi ubicación".
2. El cliente guarda la latitud y la longitud del punto y muestra el marcador. No calcula la dirección.
3. Al guardar, el sistema calcula la dirección a partir del punto y la guarda junto con las coordenadas.

## Flujo F2: editar el perfil

1. En su perfil, la persona modifica nombres, apellidos, teléfono, fecha de nacimiento, CI, sexo y/o ubicación y guarda.
2. El sistema valida con las mismas reglas del registro y, si cambió el punto, recalcula la dirección.
3. El sistema guarda y muestra el perfil actualizado. La categoría se muestra pero no se puede editar.

## Flujo F3: alta del Administrador de escuela con su perfil

1. En "Nueva escuela" ([CU05](CU05%20-%20Gestionar%20escuelas.md)), el Super admin completa, además de lo de siempre, el CI, el sexo y la ubicación del Administrador.
2. El sistema crea la escuela, la cuenta del Administrador y su perfil, con la dirección calculada.

## Flujos alternativos y excepciones

- **A1 (flujo principal paso 4, F2 paso 2):** Falta un dato obligatorio o su formato es inválido (CI fuera de 5 a 15 caracteres o con símbolos no permitidos, sexo distinto de masculino o femenino, coordenadas fuera de rango o solo una de las dos) → el cliente indica el campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (flujo principal paso 4, F2 paso 2, F3):** El CI ya pertenece a otra cuenta → 409 `ERR_IDE_19`; no se crea ni cambia nada.
- **A3 (flujo principal paso 4, F3):** El correo ya está en uso → 409 `ERR_IDE_01`. Menor de edad → 400 `ERR_IDE_02`. Escuela inexistente o inactiva → 404 `ERR_IDE_03`. Son los de CU01.
- **A4 (F1 paso 3):** No se puede calcular la dirección (el servicio de mapas no responde o no conoce ese punto) → el sistema guarda igual las coordenadas, deja la dirección en blanco y lo registra; la persona puede volver a elegir el punto más tarde. No es un error para el cliente.
- **A5 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Todo usuario tiene un perfil, y uno solo.** El perfil nace con la cuenta, en la misma operación. No hay cuentas sin perfil.
- **Datos del perfil:** nombres, apellidos, teléfono (opcional), foto (opcional), fecha de nacimiento, CI, sexo, dirección, latitud y longitud, y la categoría de licencia (solo estudiantes).
- **Obligatoriedad:** nombres, apellidos, fecha de nacimiento, CI y sexo son obligatorios para toda cuenta. La dirección y las coordenadas son obligatorias al crear un Administrador de escuela, un Instructor o un Estudiante; el Super admin no las tiene. La categoría solo la tiene el estudiante.
- **CI:** de 5 a 15 caracteres, letras, números y guion; **único en toda la plataforma**, igual que el correo. Se guarda sin espacios y en mayúsculas.
- **Sexo:** `MASCULINO` o `FEMENINO`.
- **Ubicación:** el cliente envía solo latitud y longitud (juntas o ninguna). **La dirección siempre la calcula el servidor** a partir del punto, nunca viene del cliente.
- **Categoría de licencia:** el estudiante arranca en "sin categoría" (`SIN`). Solo la escuela la cambia ([CU08](CU08%20-%20Gestionar%20estudiantes.md)); el propio estudiante la ve pero no la edita. El instructor no usa este campo: las categorías que enseña se definen en su propio caso de uso (CU09).
- **El catálogo de categorías de licencia** se carga con las categorías de Bolivia y la opción "sin categoría". Vive en el servicio Escuela.
- **Estado y alta de un estudiante:** el estado es el de la cuenta (`ACTIVO`/`INACTIVO`) y la fecha de incorporación es la de alta de la cuenta. El servicio Escuela deja de tener tabla de estudiantes.
- **Datos existentes:** las cuentas anteriores a este CU reciben valores provisionales (CI inventado y único, sexo y dirección de relleno). Es solo un entorno de desarrollo.
- **Sin pagos:** el saldo y los pagos del estudiante viven en Pagos y se referencian por el código de la persona; este CU no los toca.
- **Mensajes en español**, sin detalles técnicos. El CI, el correo, el teléfono y las coordenadas no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- Registro: existen la cuenta del estudiante y su perfil, con CI, sexo, ubicación, dirección calculada (o en blanco, A4) y categoría `SIN`.
- Editar: el perfil refleja los cambios; la categoría no cambió.
- Alta de escuela: existen la escuela, el Administrador y su perfil.

## Criterios de aceptación

- **CA01:** Dados datos válidos, el sistema debe crear la cuenta del estudiante y su perfil juntos, con la categoría `SIN`, y sin iniciar sesión.
- **CA02:** Dado un CI ya registrado, el sistema debe responder 409 `ERR_IDE_19` sin crear ni modificar nada, en el registro, en la edición del perfil y en el alta del Administrador.
- **CA03:** Dado un punto del mapa, el sistema debe guardar las coordenadas y la dirección calculada por el servidor; si el cliente envía una dirección, la ignora.
- **CA04:** Dado que no se puede calcular la dirección, el sistema debe guardar igual las coordenadas, dejar la dirección en blanco y no devolver error.
- **CA05:** Dado un CI con formato inválido, un sexo inválido o coordenadas incompletas o fuera de rango, el cliente debe indicar el campo sin enviar y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA06:** Dada cualquier persona con sesión, el sistema debe permitirle editar nombres, apellidos, teléfono, fecha de nacimiento, CI, sexo y ubicación, y no debe permitirle cambiar su categoría.
- **CA07:** Dado el alta de una escuela, el sistema debe pedir y guardar el CI, el sexo y la ubicación del Administrador en su perfil.
- **CA08:** Dado el Super admin, el sistema no debe exigirle dirección ni coordenadas.
- **CA09:** Dadas las cuentas y los datos existentes antes de este CU, el sistema debe conservar sus nombres, apellidos, teléfono, foto y fecha de nacimiento en el perfil, y la persona debe poder iniciar sesión como antes.
- **CA10:** Dado el catálogo de categorías de licencia, el sistema debe tener cargadas las categorías de Bolivia y la opción "sin categoría".
- **CA11:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA12:** Dada cualquier operación, el CI, las coordenadas y el teléfono no deben aparecer en los registros del sistema.

## Desarrollador

Mauro
