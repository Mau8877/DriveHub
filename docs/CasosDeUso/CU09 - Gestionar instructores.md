# Caso de uso

| Id   | Nombre                  | Paquete             | Prioridad | Estado    |
| ---- | ----------------------- | ------------------- | --------- | --------- |
| CU09 | Gestionar instructores  | P2. Gestión escolar | Alta      | Hecho     |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Ver los instructores de su escuela, encontrarlos, consultar su detalle, darlos de alta con su cuenta y su ficha profesional, y mantener al día su licencia, las categorías que enseñan y su estado.
**Plataforma:** Web

> **Requiere [CU08a: Perfil de la persona](CU08a%20-%20Perfil%20de%20la%20persona.md)**, que define la cuenta y el perfil de la persona (CI, sexo, ubicación) en los que se apoya este CU, y el catálogo de categorías de licencia. Es el equivalente, para el instructor, de [CU08: Gestionar estudiantes](CU08%20-%20Gestionar%20estudiantes.md): la pantalla y las reglas son muy parecidas; cambia el rol y se suma la **ficha profesional** del instructor.
>
> El diseño original lo llama "Gestionar docentes" (CU09). En el proyecto se usa siempre **instructor**, que es el nombre del rol.

**Descripción:**
El Administrador de escuela tiene en su menú la opción "Gestionar instructores". Ahí ve el listado de todos los instructores de su escuela (activos, de licencia e inactivos), con su foto, nombre completo, número de licencia, vencimiento de la licencia, categorías que enseña y estado, paginado y del alta más reciente al alta más antigua. Puede buscar por nombre y filtrar por estado. Desde cada fila abre el detalle del instructor o edita su ficha profesional. También puede dar de alta a un instructor: escribe sus datos personales, su correo y una contraseña inicial, elige su ubicación en un mapa y completa su ficha profesional (número de licencia, vencimiento, fecha de contratación y las categorías que enseña). Solo ve y toca instructores de su propia escuela.

Un instructor es una **cuenta con rol `INSTRUCTOR`** (con su perfil, igual que un estudiante) más una **ficha profesional** que lleva la escuela: licencia, categorías que enseña, fecha de contratación y estado. El instructor usa la app móvil; esta pantalla es de la escuela.

El Administrador **no** edita los datos personales del instructor (nombres, CI, sexo, fecha de nacimiento, teléfono, correo): eso es del propio instructor en su perfil ([CU06](CU06%20-%20Gestionar%20perfil.md)). Lo que sí mantiene es su ficha profesional y su ubicación.

Esta versión **no asigna autos ni estudiantes** al instructor (eso es CU10 y la asignación de alumnos) **ni toca sus horarios** ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)).

---

## Precondiciones

- El Administrador de escuela tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- Están implementados el perfil y el catálogo de categorías de licencia ([CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md)).

## Flujo principal (listar)

1. El Administrador elige "Gestionar instructores" en el menú.
2. El sistema muestra los instructores de su escuela, del alta más reciente a la más antigua, paginados, con: foto, nombre completo, número de licencia, vencimiento de la licencia, categorías que enseña (sus siglas) y estado.
3. El Administrador busca por nombre; el listado se reduce a los instructores cuyo nombre o apellido coincide.
4. El Administrador filtra por estado (activo, de licencia o inactivo). Búsqueda y filtro se combinan.

## Flujo F1: ver detalle de un instructor

1. En el listado, el Administrador elige "Ver detalles" en un instructor.
2. El sistema muestra: foto, nombre completo, edad, fecha de nacimiento, sexo, CI, correo, teléfono, dirección, número de licencia, vencimiento de la licencia, categorías que enseña (con su descripción), fecha de contratación, estado y fecha de alta.
3. El Administrador cierra el detalle y vuelve al listado.

## Flujo F2: dar de alta un instructor

1. Desde el listado, el Administrador elige "Nuevo instructor".
2. El sistema muestra el formulario: nombres, apellidos, fecha de nacimiento, sexo, CI, teléfono (opcional), correo, contraseña inicial, ubicación en el mapa, número de licencia, vencimiento de la licencia, fecha de contratación (hoy por defecto) y las categorías que enseña (al menos una).
3. El Administrador completa el formulario y lo envía.
4. El sistema valida los datos, comprueba que el correo, el CI y el número de licencia no estén en uso, que la persona sea mayor de edad y que las categorías existan.
5. El sistema crea la cuenta (rol `INSTRUCTOR`, de la escuela del Administrador, estado `ACTIVO`, correo sin verificar) con su perfil, con la dirección calculada desde el punto elegido, y la ficha profesional del instructor con estado `ACTIVO`. Se crea todo o no se crea nada.
6. El sistema informa que el instructor quedó creado y vuelve al listado, donde aparece.

## Flujo F3: corregir la ficha profesional

1. En el listado, el Administrador elige "Editar" en un instructor.
2. El sistema muestra el formulario con la ficha profesional y la ubicación editables (número de licencia, vencimiento, fecha de contratación, categorías que enseña, estado y punto del mapa) y el resto de los datos solo de lectura.
3. El Administrador cambia lo que necesite y guarda.
4. El sistema guarda los cambios y, si cambió el punto, recalcula la dirección.

## Flujos alternativos y excepciones

- **A1 (flujo principal paso 2):** La escuela no tiene instructores, o la búsqueda y el filtro no encuentran ninguno → el sistema muestra el listado vacío con un mensaje, sin error.
- **A2 (F1, F3):** El instructor no existe o pertenece a otra escuela → 404 `ERR_ESC_13`; no se distingue entre ambos casos. Una cuenta de la misma escuela que no es de un instructor tampoco se ve.
- **A3 (F2 paso 4, F3 paso 4):** Falta un dato obligatorio o su formato es inválido → el cliente indica cada campo y no envía; si llega igual, 400 `ERR_SYS_01`. También es `ERR_SYS_01`: una licencia que vence hoy o antes al dar de alta, una fecha de contratación futura, ninguna categoría elegida, y una edición sin ningún campo que cambiar.
- **A4 (F2 paso 4):** El correo ya está en uso → 409 `ERR_IDE_01`. El CI ya está en uso → 409 `ERR_IDE_19`. Menor de edad → 400 `ERR_IDE_02`. En los tres casos no se crea nada.
- **A5 (F2 paso 4, F3 paso 4):** El número de licencia ya pertenece a otro instructor → 409 `ERR_ESC_12`; no se crea ni cambia nada.
- **A6 (F2 paso 4, F3 paso 4):** Una categoría elegida no existe o no es una categoría que se pueda enseñar (la opción "sin categoría" no lo es) → 400 `ERR_ESC_14`; no se crea ni cambia nada.
- **A7 (F2 paso 5, F3 paso 4):** No se puede calcular la dirección → se guardan las coordenadas con la dirección en blanco, sin error (como en CU08a, A4).
- **A8 (F2 paso 5):** Falla la creación de la cuenta o de la ficha (un servicio no responde) → el sistema no deja nada a medias: no queda una cuenta sin ficha ni una ficha sin cuenta, y responde un error general sin detalles técnicos.
- **A9 (cualquier flujo):** La persona no es Administrador de escuela → 403; sin sesión o con sesión vencida → 401.
- **A10 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona instructores, y solo los de **su** escuela. La escuela se toma de la cuenta de quien hace la petición, nunca de un parámetro del cliente. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`); el menú solo oculta la opción a los demás roles.
- **Un instructor es una cuenta con rol `INSTRUCTOR` y su perfil** ([CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md)), más su **ficha profesional**. El estudiante y el instructor son cuentas distintas: una persona que es las dos cosas necesita dos cuentas (correo distinto).
- **Ficha profesional:** número de licencia, vencimiento de la licencia, fecha de contratación, categorías que enseña y estado. La categoría del *perfil* (la del estudiante) no se usa en un instructor.
- **Lo único que el Administrador puede cambiar de un instructor existente** es su ficha profesional y su ubicación. Todo lo demás es del instructor.
- **Alta por el Administrador:** mismas reglas de datos que el registro del estudiante ([CU01](CU01%20-%20Registrar%20estudiante.md), [CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md)): contraseña de 9 a 64 caracteres, edad mínima configurable (18), CI único, correo único, ubicación obligatoria. La contraseña inicial la escribe el Administrador y se la comunica al instructor. El correo queda sin verificar.
- **Número de licencia:** de 3 a 30 caracteres, letras, números y guion; se guarda sin espacios y en mayúsculas; **único en toda la plataforma**, igual que el CI.
- **Vencimiento de la licencia:** obligatorio. Al dar de alta debe ser posterior a hoy. Al editar solo se valida si se cambia: dejar sin tocar una licencia ya vencida no impide corregir otros datos. Una licencia vencida se muestra con su fecha; esta versión no bloquea nada por eso.
- **Fecha de contratación:** hoy por defecto; no puede ser futura.
- **Categorías que enseña:** una o más del catálogo de categorías de licencia; sin repetir. La opción "sin categoría" no se puede elegir.
- **Estado de la ficha:**
  - `ACTIVO`: el instructor trabaja con normalidad.
  - `LICENCIA`: está ausente por un tiempo (vacaciones, permiso). No tiene relación con la licencia de conducir.
  - `INACTIVO`: ya no trabaja en la escuela. Es la forma de "dar de baja": **no se borra** al instructor ni su historial.
  El estado se cambia al editar. En esta versión **no impide que inicie sesión** en la app: lo que se haga con el estado en otros procesos (asignar autos o alumnos, ofrecer horarios) lo definen esos casos de uso.
- **Edad:** se calcula en el momento a partir de la fecha de nacimiento; no se guarda.
- **Fecha de alta:** la de creación de la cuenta.
- **Búsqueda por nombre:** coincidencia parcial sobre nombres y apellidos, sin distinguir mayúsculas ni tildes.
- **El listado es paginado** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): `page`, `pageSize`, `search` y `filter[estado]`.
- **Mensajes en español**, sin detalles técnicos. Contraseñas, CI, correo, teléfono, número de licencia y coordenadas no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- Listar y ver detalle no modifican datos.
- Alta: existen la cuenta del instructor con su perfil y su ficha profesional, en la escuela del Administrador. Si algo falló, no existe ninguna de las dos.
- Editar: la ficha profesional y la ubicación quedan cambiadas; todo lo demás igual.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar los instructores de su escuela, en cualquier estado, con foto, nombre completo, número de licencia, vencimiento, categorías que enseña y estado, paginados y del alta más reciente a la más antigua.
- **CA02:** Dado un texto de búsqueda, el sistema debe devolver solo los instructores cuyo nombre o apellido lo contiene, sin distinguir mayúsculas ni tildes.
- **CA03:** Dado el filtro por estado, el sistema debe devolver solo los instructores activos, solo los de licencia o solo los inactivos.
- **CA04:** Dado que no hay resultados, el sistema debe mostrar el listado vacío con un mensaje y no un error.
- **CA05:** Dado un Administrador, el sistema no debe mostrar ni modificar nunca instructores de otra escuela, ni cuentas que no sean de un instructor.
- **CA06:** Dado "Ver detalles" sobre un instructor de su escuela, el sistema debe mostrar sus datos y su ficha; sobre uno inexistente o de otra escuela, debe responder 404 `ERR_ESC_13`.
- **CA07:** Dados datos válidos, el sistema debe crear la cuenta con rol `INSTRUCTOR`, su perfil con la dirección calculada y su ficha profesional con estado `ACTIVO`, en la escuela del Administrador y con el correo sin verificar.
- **CA08:** Dado un correo o un CI ya registrado, o un menor de edad, el sistema debe responder 409 `ERR_IDE_01`, 409 `ERR_IDE_19` o 400 `ERR_IDE_02`, sin crear nada.
- **CA09:** Dado un número de licencia ya registrado, el sistema debe responder 409 `ERR_ESC_12` sin crear ni cambiar nada.
- **CA10:** Dada una categoría que no existe, o la opción "sin categoría", el sistema debe responder 400 `ERR_ESC_14` sin crear ni cambiar nada.
- **CA11:** Dada una licencia que vence hoy o antes (al dar de alta o al cambiar su fecha), una fecha de contratación futura o ninguna categoría elegida, el sistema debe responder 400 `ERR_SYS_01` sin crear ni cambiar nada.
- **CA12:** Dado un fallo a mitad del alta, el sistema no debe dejar una cuenta sin ficha ni una ficha sin cuenta.
- **CA13:** Dada la edición, el sistema debe cambiar solo la ficha profesional y la ubicación del instructor, y no debe aceptar cambios a ningún dato personal, al correo ni a la escuela.
- **CA14:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA15:** Dado el menú de la web, la opción "Gestionar instructores" debe verse solo para el Administrador de escuela.
- **CA16:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA17:** Dada cualquier operación, la contraseña, el CI, el correo, el teléfono, el número de licencia y las coordenadas no deben aparecer en los registros del sistema.
- **CA18:** Dada la lista de estudiantes ([CU08](CU08%20-%20Gestionar%20estudiantes.md)), no debe aparecer ningún instructor, y viceversa.

## Fuera de alcance

- Asignar autos a un instructor (CU10, aún sin documentar) y asignarle estudiantes.
- Que el estado `INACTIVO` o `LICENCIA` bloquee el inicio de sesión, la asignación o los horarios del instructor.
- Avisos de licencias por vencer o vencidas, y buscar por número de licencia.
- Cambiar los datos personales de un instructor (es de su perfil, [CU06](CU06%20-%20Gestionar%20perfil.md)) y borrar a un instructor.

## Desarrollador

Mauro
