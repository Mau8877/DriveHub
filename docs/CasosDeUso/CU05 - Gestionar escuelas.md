# Caso de uso

| Id   | Nombre            | Paquete                | Prioridad | Estado    |
| ---- | ----------------- | ---------------------- | --------- | --------- |
| CU05 | Gestionar escuelas | P1. Identidad y acceso | Alta      | Hecho |

**Actor principal:** Super admin
**Actores secundarios:** Administrador de escuela (la cuenta que se crea junto con la escuela)
**Objetivo:** Dar de alta las escuelas clientes de la plataforma con su Administrador, mantener su nombre y activarlas o desactivarlas.
**Plataforma:** Web

> El PDF de diseño llama a este caso de uso "Gestionar roles y permisos"; estaba equivocado. El CU05 del proyecto es este (ver [CASOS_DE_USO.md](../../.claude/rules/CASOS_DE_USO.md)).

**Descripción:**
El Super admin tiene en su menú la opción "Gestionar escuelas". Ahí ve el listado de todas las escuelas de la plataforma (activas e inactivas), con búsqueda, filtro por estado y paginación. Desde el listado crea una escuela nueva: completa los datos de la escuela y los de su Administrador (incluida la contraseña inicial) y el sistema crea ambas cosas juntas; si algo falla, no queda nada a medias. También puede cambiar el nombre de una escuela, desactivarla y volver a activarla. Desactivar una escuela es reversible (borrado lógico): mientras está inactiva, nadie de esa escuela, empezando por su Administrador, puede iniciar sesión y verá que la escuela está inactiva; al reactivarla todos recuperan el acceso con sus mismas credenciales.

Este CU agrupa las cinco operaciones de gestión (listar, crear, editar, desactivar, reactivar) por decisión del equipo. Crear al Instructor no es parte de este CU (CU01_2, a definir).

---

## Precondiciones

- El Super admin tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).

## Flujo principal (listar)

1. El Super admin elige "Gestionar escuelas" en el menú.
2. El sistema muestra las escuelas de la plataforma (nombre, slug, estado y fecha de alta), de la más reciente a la más antigua, paginadas.
3. El Super admin puede buscar por nombre o slug y filtrar por estado (activa o inactiva).

## Flujo F1: crear una escuela con su Administrador

1. Desde el listado, el Super admin elige "Nueva escuela".
2. El sistema muestra el formulario: nombre y slug de la escuela; nombres, apellidos, fecha de nacimiento, correo, contraseña y teléfono (opcional) del Administrador.
3. El Super admin completa el formulario y lo envía.
4. El sistema valida los datos y comprueba que el slug no esté en uso.
5. El sistema crea la escuela y la cuenta del Administrador (rol `ADMIN_ESCUELA`, asociada a esa escuela, estado `ACTIVO`, correo sin verificar).
6. El sistema informa que la escuela quedó creada y vuelve al listado, donde aparece activa.

## Flujo F2: editar el nombre

1. En el listado, el Super admin elige "Editar" en una escuela activa.
2. El sistema muestra el nombre actual; el Super admin lo cambia y envía.
3. El sistema valida y guarda el nombre nuevo. El slug y el Administrador no cambian.

## Flujo F3: desactivar una escuela

1. En el listado, el Super admin elige "Desactivar" en una escuela activa y confirma.
2. El sistema marca la escuela como inactiva (borrado lógico).
3. Desde ese momento, quien pertenezca a la escuela no puede iniciar sesión ni renovarla, y ve que la escuela está inactiva; el enlace de registro de estudiantes de esa escuela deja de funcionar.

## Flujo F4: reactivar una escuela

1. En el listado, el Super admin filtra o encuentra una escuela inactiva y elige "Reactivar".
2. El sistema quita el borrado lógico: la escuela vuelve a estar activa y todas sus cuentas pueden iniciar sesión otra vez.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4):** Falta un dato obligatorio o su formato es inválido (slug con mayúsculas o espacios, correo mal formado, contraseña de menos de 9 caracteres) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 4):** El slug ya lo usa otra escuela, activa o inactiva → 409 `ERR_ESC_03`; no se crea nada.
- **A3 (F1 paso 5):** El correo del Administrador ya pertenece a otra cuenta → 409 `ERR_IDE_01`; no se crea la escuela.
- **A4 (F1 paso 5):** El Administrador no alcanza la edad mínima → 400 `ERR_IDE_02`; no se crea nada.
- **A5 (F1 paso 5):** Falla la creación de una de las dos partes (servicio caído, tiempo de espera, error interno) → el sistema no deja una escuela sin su Administrador ni una cuenta de Administrador activa sin escuela: deshace lo que alcanzó a crear y responde 500 `ERR_SYS_00`. Si deshacerlo también falla, la cuenta sobrante no puede iniciar sesión (su escuela no existe) y el fallo queda registrado para limpiarlo.
- **A6 (F2 paso 3):** El nombre es inválido (vacío o de más de 120 caracteres) → 400 `ERR_SYS_01`.
- **A7 (F2, F3, F4):** La escuela no existe → 404 `ERR_ESC_02`.
- **A8 (F2):** La escuela está inactiva → 409 `ERR_ESC_06`; primero hay que reactivarla.
- **A9 (F3):** La escuela ya está inactiva → 409 `ERR_ESC_04`.
- **A10 (F4):** La escuela ya está activa → 409 `ERR_ESC_05`.
- **A11 (cualquier flujo):** La persona no es Super admin → 403; sin sesión o con sesión vencida → 401.
- **A12 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Super admin** gestiona escuelas. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`), no en la pantalla; el menú solo oculta la opción a los demás roles.
- **Una escuela tiene un solo Administrador**, que se crea junto con ella y no se edita desde este CU (cambiar al Administrador queda fuera de alcance).
- **Slug:** letras minúsculas, números y guiones, de 3 a 60 caracteres, sin guión al inicio ni al final. **No se edita** después de crear la escuela: ya viaja en enlaces de registro compartidos. Es único incluso frente a escuelas inactivas.
- **Nombre:** de 3 a 120 caracteres. Es lo único editable.
- **Administrador:** mismas reglas de datos que el registro del estudiante ([CU01](CU01%20-%20Registrar%20estudiante.md)): contraseña de al menos 9 caracteres y edad mínima configurable (18). La contraseña inicial la escribe el Super admin y se la comunica a la persona. El correo es único en toda la plataforma.
- **Desactivar es borrado lógico** (`deleted_at`) y es la única fuente del estado de la escuela; no hay otro campo de estado. No toca las cuentas de la escuela: Identidad ya comprueba la escuela al iniciar y al renovar la sesión, por lo que **todas** las cuentas de una escuela inactiva (Administrador, Instructores y Estudiantes) quedan sin acceso, y lo recuperan al reactivarla.
- **Una sesión ya abierta** no se corta al instante: sigue válida hasta que vence su token de acceso (15 minutos) o intenta renovarse; entonces es rechazada.
- **Crear escuela y Administrador es una operación entre dos servicios** (Escuela e Identidad), sin transacción común. Se coordina como una saga simplificada y sin estado: la escuela se prepara, se crea el Administrador, y solo entonces se confirma la escuela; si algo falla, se compensa. No se usa la saga formal de [`SAGA_RESERVA_PAGO.md`](../DriveHub/SAGA_RESERVA_PAGO.md) (propia de CU44).
- **Mensajes en español**, sin detalles técnicos. Contraseñas y tokens **nunca** se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **El listado es paginado** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)).

## Postcondiciones

- Crear: existe la escuela activa y su Administrador, vinculados por el código de la escuela; el Administrador aún no inició sesión.
- Editar: el nombre queda cambiado; todo lo demás igual.
- Desactivar: la escuela tiene `deleted_at`; sus cuentas siguen existiendo pero no pueden iniciar ni renovar sesión.
- Reactivar: la escuela no tiene `deleted_at`; sus cuentas vuelven a poder iniciar sesión.

## Criterios de aceptación

- **CA01:** Dado un Super admin con sesión, el sistema debe listar las escuelas activas e inactivas con nombre, slug, estado y fecha de alta, paginadas, con búsqueda por nombre o slug y filtro por estado.
- **CA02:** Dados datos válidos de escuela y Administrador, el sistema debe crear la escuela activa y la cuenta del Administrador con rol `ADMIN_ESCUELA` asociada a ella, sin iniciar sesión con esa cuenta.
- **CA03:** Dado un slug ya usado por una escuela activa o inactiva, el sistema debe responder 409 `ERR_ESC_03` sin crear la escuela ni la cuenta.
- **CA04:** Dado un correo de Administrador que ya existe, el sistema debe responder 409 `ERR_IDE_01` sin crear la escuela.
- **CA05:** Dado un Administrador menor de la edad mínima, el sistema debe responder 400 `ERR_IDE_02` sin crear nada.
- **CA06:** Dados datos inválidos, el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA07:** Dado que falla la creación del Administrador o la confirmación de la escuela, el sistema no debe dejar una escuela sin Administrador ni una cuenta de Administrador que pueda iniciar sesión.
- **CA08:** Dado un nombre válido, el sistema debe cambiar el nombre de la escuela y dejar igual su slug y su Administrador; sobre una escuela inactiva debe responder 409 `ERR_ESC_06`.
- **CA09:** Dado que se desactiva una escuela, el sistema debe impedir iniciar y renovar sesión a todas sus cuentas con el mensaje de escuela inactiva, y rechazar el registro de estudiantes con su enlace.
- **CA10:** Dada una escuela inactiva que se reactiva, el sistema debe permitir otra vez el inicio de sesión de todas sus cuentas con sus mismas credenciales y sin perder datos.
- **CA11:** Dada una escuela ya inactiva (o ya activa), el sistema debe responder 409 `ERR_ESC_04` al desactivar (o `ERR_ESC_05` al reactivar); si no existe, 404 `ERR_ESC_02`.
- **CA12:** Dada una persona que no es Super admin, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA13:** Dado el menú de la web, la opción "Gestionar escuelas" debe verse solo para el Super admin, y el Super admin no debe ver las opciones del Administrador de escuela.
- **CA14:** Dada cualquier operación, ni la contraseña del Administrador ni los tokens deben aparecer en los registros del sistema.
- **CA15:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Mauro
