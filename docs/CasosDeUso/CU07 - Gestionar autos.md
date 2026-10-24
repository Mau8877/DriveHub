# Caso de uso

| Id   | Nombre         | Paquete             | Prioridad | Estado |
| ---- | -------------- | ------------------- | --------- | ------ |
| CU07 | Gestionar autos | P2. Gestión escolar | Alta      | Hecho  |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Mantener la flota de autos de la escuela: registrarlos, corregir sus datos y controlar si están disponibles, en mantenimiento o dados de baja.
**Plataforma:** Web

**Descripción:**
El Administrador de escuela tiene en su menú la opción "Escuela" y, dentro, "Autos". Ahí ve el listado de los autos de su escuela con búsqueda por placa, marca o modelo, filtro por estado y paginación. Desde el listado registra un auto nuevo (placa, marca, modelo, año, color, transmisión, kilometraje y vencimientos de SOAT e inspección y una foto), edita sus datos y cambia su estado: lo envía a mantenimiento, lo habilita de nuevo, lo da de baja o reactiva uno de baja. Solo un auto `DISPONIBLE` se puede asignar o reservar (CU10, CU14). Un auto de baja no se edita ni va a mantenimiento: solo se puede reactivar. Cada escuela ve y toca únicamente sus propios autos.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.

## Flujo principal (listar)

1. El Administrador elige "Autos" dentro de "Escuela".
2. El sistema muestra los autos de su escuela (placa, marca, modelo, transmisión, estado y kilometraje), del más reciente al más antiguo, paginados.
3. El Administrador puede buscar por placa, marca o modelo y filtrar por estado.

## Flujo F1: registrar un auto

1. Desde el listado, el Administrador elige "Nuevo auto".
2. El sistema muestra el formulario: placa, marca, modelo, año (opcional), color (opcional), transmisión, kilometraje (opcional, 0 por defecto), vencimiento del SOAT (opcional), de la inspección (opcional) y foto (opcional).
   La foto se elige desde el equipo y se sube a Cloudinary; el formulario la muestra al terminar de subir.
3. El Administrador completa el formulario y lo envía.
4. El sistema valida los datos y comprueba que la placa no esté en uso.
5. El sistema registra el auto en estado `DISPONIBLE` y vuelve al listado, donde aparece.

## Flujo F2: editar un auto

1. En el listado, el Administrador elige "Editar" en un auto que no esté de baja.
2. El sistema muestra los datos actuales; la placa y la transmisión se ven pero no se cambian.
3. El Administrador cambia marca, modelo, año, color, kilometraje o vencimientos y envía.
4. El sistema valida y guarda; los campos que no se envían quedan como estaban.

## Flujo F3: enviar a mantenimiento

1. En el listado, el Administrador elige "A mantenimiento" en un auto `DISPONIBLE`.
2. El sistema lo pasa a `MANTENIMIENTO`: deja de poder asignarse o reservarse.

## Flujo F4: habilitar de nuevo

1. En el listado, el Administrador elige "Habilitar" en un auto en `MANTENIMIENTO`.
2. El sistema lo pasa a `DISPONIBLE`.

## Flujo F5: dar de baja

1. En el listado, el Administrador elige "Dar de baja" en un auto `DISPONIBLE` o en `MANTENIMIENTO` y confirma.
2. El sistema lo pasa a `BAJA`: el auto sigue en el listado, pero no se edita ni va a mantenimiento hasta que se reactive.

## Flujo F6: reactivar un auto de baja

1. En el listado, el Administrador elige "Reactivar" en un auto de baja.
2. El sistema lo pasa a `DISPONIBLE`.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4, F2 paso 4):** Falta un dato obligatorio o su formato es inválido (placa de menos de 5 o más de 10 caracteres o con símbolos, marca o modelo vacíos, año fuera de rango, kilometraje negativo, transmisión desconocida) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 4):** La placa ya está registrada → 409 `ERR_ESC_07`; no se crea nada.
- **A3 (F2 paso 4):** El kilometraje nuevo es menor al actual → 400 `ERR_SYS_01`.
- **A4 (F2, F3, F4, F5):** El auto no existe o es de otra escuela → 404 `ERR_ESC_08` (no se revela si existe en otra escuela).
- **A5 (F2, F3, F4, F5):** El cambio no es posible desde el estado actual (editar un auto de baja, enviar a mantenimiento uno que no está disponible, habilitar uno que no está en mantenimiento ni de baja, dar de baja uno que ya lo está) → 409 `ERR_ESC_09`.
- **A6 (cualquier flujo):** La persona no es Administrador de escuela → 403; sin sesión o con sesión vencida → 401.
- **A7 (cualquier flujo):** El token no trae la escuela de la cuenta → 403 `ERR_ESC_10`.
- **A7b (F1, F2):** La foto no es una imagen, pesa más de 5 MB o la subida a Cloudinary falla → el cliente avisa bajo el campo y no cambia la foto; si llega una URL que no es de Cloudinary, el sistema responde 400 `ERR_SYS_01`.
- **A8 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona autos. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Aislamiento por escuela:** cada auto guarda el código de su escuela y toda consulta o cambio se filtra por la escuela del token (claim `escuela`). Una escuela nunca ve ni toca autos de otra.
- **Placa:** de 5 a 10 caracteres, letras, números y guiones; se guarda en mayúsculas sin espacios alrededor. Es única en toda la plataforma (una placa identifica a un solo auto) y **no se edita**.
- **Transmisión** (`MANUAL` o `AUTOMATICO`): no se edita después de registrar el auto.
- **Año:** entre 1980 y el año siguiente al actual. **Kilometraje:** entero mayor o igual a 0; al editar no puede bajar.
- **Estados y transiciones:** `DISPONIBLE → MANTENIMIENTO`, `MANTENIMIENTO → DISPONIBLE`, `DISPONIBLE → BAJA`, `MANTENIMIENTO → BAJA` y `BAJA → DISPONIBLE` (reactivar). Desde `BAJA` no hay otra salida.
- **Dar de baja no es borrado:** el auto conserva su historia (clases pasadas, asignaciones) y se ve en el listado con estado `BAJA`. Reactivarlo lo devuelve a `DISPONIBLE` con sus datos.
- **Foto:** el archivo vive en Cloudinary (carpeta `drivehub/{escuela}/vehiculos`) y el auto guarda solo su URL https. La subida la autoriza el backend con una firma (el API secret nunca llega al navegador). Solo se aceptan URLs de `res.cloudinary.com`, de hasta 300 caracteres. Editar sin elegir foto nueva deja la actual.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Registrar: existe el auto `DISPONIBLE` ligado a la escuela del Administrador.
- Editar: los campos enviados quedan cambiados; placa, transmisión y estado igual.
- Enviar a mantenimiento / habilitar / dar de baja / reactivar: el auto queda en el estado destino.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los autos de su escuela, paginados, con búsqueda por placa, marca o modelo y filtro por estado.
- **CA02:** Dados datos válidos, el sistema debe registrar el auto en estado `DISPONIBLE` ligado a la escuela del Administrador, con la placa en mayúsculas.
- **CA03:** Dada una placa ya registrada, el sistema debe responder 409 `ERR_ESC_07` sin crear el auto.
- **CA04:** Dados datos inválidos, el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA05:** Dados datos válidos de edición, el sistema debe cambiar solo los campos enviados y dejar igual placa, transmisión y estado; un kilometraje menor al actual se rechaza con 400 `ERR_SYS_01`.
- **CA06:** Dado un auto `DISPONIBLE`, enviarlo a mantenimiento lo deja en `MANTENIMIENTO`; habilitarlo después lo deja en `DISPONIBLE`.
- **CA07:** Dado un auto que no está de baja, darlo de baja lo deja en `BAJA`; desde ahí ni se edita ni va a mantenimiento (409 `ERR_ESC_09`), y reactivarlo lo deja `DISPONIBLE`.
- **CA08:** Dada una transición no permitida desde el estado actual, el sistema debe responder 409 `ERR_ESC_09` sin cambiar nada.
- **CA09:** Dado un auto de otra escuela o inexistente, el sistema debe responder 404 `ERR_ESC_08` a cualquier operación.
- **CA10:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA11:** Dado un archivo que es una imagen de hasta 5 MB, el sistema debe subirla a Cloudinary con una firma del backend y guardar su URL en el auto; una URL ajena a Cloudinary se rechaza con 400 `ERR_SYS_01`.
- **CA12:** Dado el menú de la web, la opción de autos debe verse solo para el Administrador de escuela.

## Fuera de alcance

- La **categoría de licencia** del auto: el catálogo de categorías todavía no tiene caso de uso, así que el auto se registra sin categoría.
- El **historial de mantenimiento** (registrar costo, tipo y descripción de cada mantenimiento): se agrega en un caso de uso propio; CU07 solo cambia el estado del auto.
- **Asignar el auto a un instructor** ([CU10](../../CU.md)) y consultar su disponibilidad (CU11).

## Desarrollador

Otsubo
