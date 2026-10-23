# Caso de uso

| Id   | Nombre                | Paquete             | Prioridad | Estado    |
| ---- | --------------------- | ------------------- | --------- | --------- |
| CU08 | Gestionar estudiantes | P2. Gestión escolar | Alta      | Pendiente |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Ver los estudiantes de su escuela, encontrarlos, consultar su detalle, darlos de alta con su cuenta y corregir su categoría y su ubicación.
**Plataforma:** Web

> **Requiere [CU08a: Perfil de la persona](CU08a%20-%20Perfil%20de%20la%20persona.md)**, que define la cuenta y el perfil del estudiante (CI, sexo, ubicación, categoría) en los que se apoya este CU.

**Descripción:**
El Administrador de escuela tiene en su menú la opción "Gestionar estudiantes". Ahí ve el listado de todos los estudiantes de su escuela (activos e inactivos), con nombre completo, edad, categoría, fecha de incorporación y estado, paginado y de la incorporación más reciente a la más antigua. Puede buscar por nombre y filtrar por estado (activo o inactivo). Desde cada fila abre el detalle del estudiante o edita su categoría y su ubicación. También puede dar de alta a un estudiante: escribe sus datos, su correo y una contraseña inicial, y elige su ubicación en un mapa. Solo ve y toca estudiantes de su propia escuela.

El Administrador **no** edita los datos personales del estudiante (nombres, CI, sexo, fecha de nacimiento, teléfono, correo): eso es del propio estudiante en su perfil ([CU06](CU06%20-%20Gestionar%20perfil.md)). Tampoco activa ni desactiva cuentas en esta versión.

Esta versión **no incluye nada de pagos** (monto pendiente, montos pagados) **ni de clases tomadas**: ver [Pendiente para cuando exista Pagos](#pendiente-para-cuando-exista-pagos).

---

## Precondiciones

- El Administrador de escuela tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- Están implementados el perfil y el catálogo de categorías de licencia ([CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md)).

## Flujo principal (listar)

1. El Administrador elige "Gestionar estudiantes" en el menú.
2. El sistema muestra los estudiantes de su escuela, de la incorporación más reciente a la más antigua, paginados, con: nombre completo, edad, categoría, fecha de incorporación y estado.
3. El Administrador busca por nombre; el listado se reduce a los estudiantes cuyo nombre o apellido coincide.
4. El Administrador filtra por estado (activo o inactivo). Búsqueda y filtro se combinan.

## Flujo F1: ver detalle de un estudiante

1. En el listado, el Administrador elige "Ver detalles" en un estudiante.
2. El sistema muestra: nombre completo, edad, fecha de nacimiento, sexo, CI, correo, teléfono, dirección, categoría, estado y fecha de incorporación.
3. El Administrador cierra el detalle y vuelve al listado.

## Flujo F2: dar de alta un estudiante

1. Desde el listado, el Administrador elige "Nuevo estudiante".
2. El sistema muestra el formulario: nombres, apellidos, fecha de nacimiento, sexo, CI, teléfono (opcional), correo, contraseña inicial, ubicación en el mapa y categoría (por defecto "sin categoría").
3. El Administrador completa el formulario y lo envía.
4. El sistema valida los datos, comprueba que el correo y el CI no estén en uso y que la persona sea mayor de edad.
5. El sistema crea la cuenta (rol `ESTUDIANTE`, de la escuela del Administrador, estado `ACTIVO`, correo sin verificar) y su perfil, con la dirección calculada desde el punto elegido.
6. El sistema informa que el estudiante quedó creado y vuelve al listado, donde aparece.

## Flujo F3: corregir la categoría o la ubicación

1. En el listado, el Administrador elige "Editar" en un estudiante.
2. El sistema muestra el formulario con la categoría y la ubicación editables, y el resto de los datos solo de lectura.
3. El Administrador cambia la categoría y/o el punto del mapa y guarda.
4. El sistema guarda la categoría y, si cambió el punto, recalcula la dirección.

## Flujos alternativos y excepciones

- **A1 (flujo principal paso 2):** La escuela no tiene estudiantes, o la búsqueda y el filtro no encuentran ninguno → el sistema muestra el listado vacío con un mensaje, sin error.
- **A2 (F1, F3):** El estudiante no existe o pertenece a otra escuela → 404 `ERR_IDE_20`; no se distingue entre ambos casos.
- **A3 (F2 paso 4):** Falta un dato obligatorio o su formato es inválido → el cliente indica cada campo y no envía; si llega igual, 400 `ERR_SYS_01`.
- **A4 (F2 paso 4):** El correo ya está en uso → 409 `ERR_IDE_01`. El CI ya está en uso → 409 `ERR_IDE_19`. Menor de edad → 400 `ERR_IDE_02`. En los tres casos no se crea nada.
- **A5 (F2 paso 4, F3 paso 4):** La categoría elegida no existe → 400 `ERR_IDE_21`.
- **A6 (F2 paso 5, F3 paso 4):** No se puede calcular la dirección → se guardan las coordenadas con la dirección en blanco, sin error (como en CU08a, A4).
- **A7 (cualquier flujo):** La persona no es Administrador de escuela → 403; sin sesión o con sesión vencida → 401.
- **A8 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona estudiantes, y solo los de **su** escuela. La escuela se toma de la cuenta de quien hace la petición, nunca de un parámetro del cliente. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`); el menú solo oculta la opción a los demás roles.
- **Un estudiante es una cuenta con rol `ESTUDIANTE` y su perfil**, en Identidad ([CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md)). No hay otra tabla de estudiantes. Por eso el recurso `estudiantes` lo atiende Identidad.
- **Lo único que el Administrador puede cambiar de un estudiante existente es su categoría y su ubicación.** Todo lo demás es del estudiante.
- **Alta por el Administrador:** mismas reglas de datos que el registro del estudiante ([CU01](CU01%20-%20Registrar%20estudiante.md) y [CU08a](CU08a%20-%20Perfil%20de%20la%20persona.md)): contraseña de al menos 9 caracteres, edad mínima configurable (18), CI único, correo único. La contraseña inicial la escribe el Administrador y se la comunica al estudiante. El correo queda sin verificar.
- **Categoría:** una del catálogo de categorías de licencia; "sin categoría" es la opción por defecto.
- **Edad:** se calcula en el momento a partir de la fecha de nacimiento; no se guarda.
- **Fecha de incorporación:** fecha de alta de la cuenta. **Estado:** el de la cuenta, `ACTIVO` o `INACTIVO`; una cuenta bloqueada temporalmente por intentos fallidos ([CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) cuenta como `ACTIVO`. Los inactivos se listan igual.
- **Búsqueda por nombre:** coincidencia parcial sobre nombres y apellidos, sin distinguir mayúsculas ni tildes.
- **El listado es paginado** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): `page`, `pageSize`, `search` y `filter[estado]`.
- **Mensajes en español**, sin detalles técnicos. Contraseñas, CI, correo, teléfono y coordenadas no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- Listar y ver detalle no modifican datos.
- Alta: existen la cuenta del estudiante y su perfil, en la escuela del Administrador.
- Editar: la categoría y la ubicación quedan cambiadas; todo lo demás igual.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar los estudiantes de su escuela, activos e inactivos, con nombre completo, edad, categoría, fecha de incorporación y estado, paginados y de la incorporación más reciente a la más antigua.
- **CA02:** Dado un texto de búsqueda, el sistema debe devolver solo los estudiantes cuyo nombre o apellido lo contiene, sin distinguir mayúsculas ni tildes.
- **CA03:** Dado el filtro por estado, el sistema debe devolver solo los estudiantes activos o solo los inactivos.
- **CA04:** Dado que no hay resultados, el sistema debe mostrar el listado vacío con un mensaje y no un error.
- **CA05:** Dado un Administrador, el sistema no debe mostrar ni modificar nunca estudiantes de otra escuela.
- **CA06:** Dado "Ver detalles" sobre un estudiante de su escuela, el sistema debe mostrar sus datos; sobre uno inexistente o de otra escuela, debe responder 404 `ERR_IDE_20`.
- **CA07:** Dados datos válidos, el sistema debe crear la cuenta y el perfil del estudiante en la escuela del Administrador, con la categoría elegida, la dirección calculada y el correo sin verificar.
- **CA08:** Dado un correo o un CI ya registrado, o un menor de edad, el sistema debe responder 409 `ERR_IDE_01`, 409 `ERR_IDE_19` o 400 `ERR_IDE_02`, sin crear nada.
- **CA09:** Dada una categoría que no existe, el sistema debe responder 400 `ERR_IDE_21` sin crear ni cambiar nada.
- **CA10:** Dada la edición, el sistema debe cambiar solo la categoría y la ubicación del estudiante, y no debe aceptar cambios a ningún otro dato.
- **CA11:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA12:** Dado el menú de la web, la opción "Gestionar estudiantes" debe verse solo para el Administrador de escuela.
- **CA13:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA14:** Dada cualquier operación, la contraseña, el CI, el correo, el teléfono y las coordenadas no deben aparecer en los registros del sistema.

## Pendiente para cuando exista Pagos

> **No forma parte de este caso de uso.** Queda anotado para que, al implementar Pagos, se vuelva sobre esta pantalla y no se olvide.

Cuando Pagos esté implementado hay que **volver a este CU** (o abrir uno nuevo que lo extienda) para sumar:

- **Columna "Monto pendiente"** en el listado.
- **Filtro por saldo:** "con monto pendiente" y "sin saldo" (monto pendiente igual a cero).
- **Detalle:** montos pagados y clases tomadas (estas últimas vienen de Académico).

Decisiones que habrá que tomar en ese momento (no se resuelven ahora):

- De dónde sale el monto pendiente: `inscripcion_plan.saldo` (ya calculado) o la suma de las cuotas pendientes y vencidas.
- Qué pasa con un estudiante sin plan: ¿"sin saldo" o fuera del filtro? Y con varios planes: ¿se suman solo los activos?
- **Quién junta las dos fuentes.** Los estudiantes viven en Identidad y el saldo en Pagos. Identidad no puede leer la base de Pagos: tendrá que pedirle el saldo por REST (como Escuela le pide cosas a Identidad hoy), y el filtro por saldo obliga a decidir qué servicio pagina y filtra. Otra salida es GraphQL (CU45) para las pantallas que juntan datos de varios servicios.

## Desarrollador

Mauro
