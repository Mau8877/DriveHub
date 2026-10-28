# Caso de uso

| Id   | Nombre                             | Paquete          | Prioridad | Estado |
| ---- | ---------------------------------- | ---------------- | --------- | ------ |
| CU42 | Consultar historial de ayudas IA   | P8. Asistente IA | Media     | Hecho  |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Volver a ver las ayudas del tablero que analizó antes y decir si le sirvieron.
**Plataforma:** Móvil

**Descripción:**
En «Estudio > Tablero» el Estudiante elige «Ver mi historial de ayudas» y ve, de la más reciente a la más antigua, las ayudas que se guardaron al analizar luces del tablero ([CU40](CU40%20-%20Analizar%20foto%20del%20tablero.md)): la miniatura de la foto (si la guardó), la luz, la fecha, la gravedad y si le sirvió. Al tocar una ayuda ve su detalle: la foto, qué significa la luz y qué hacer, la confianza y las otras luces posibles. Ahí puede marcar si la ayuda le sirvió o no, y puede cambiar su opinión. Cada persona ve solo sus propias ayudas.

---

## Precondiciones

- El Estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- Tiene ayudas guardadas por [CU40](CU40%20-%20Analizar%20foto%20del%20tablero.md); si no, la pantalla lo dice.

## Flujo principal

1. El Estudiante entra a «Estudio > Tablero» y elige «Ver mi historial de ayudas».
2. El sistema muestra sus ayudas, de la más reciente a la más antigua, paginadas.
3. El Estudiante toca una ayuda.
4. La app muestra su detalle: foto, luz, fecha, confianza, qué significa y qué hacer, y otras posibilidades.
5. El Estudiante marca «Sí, me sirvió» o «No me sirvió» y el sistema guarda su opinión.

## Flujos alternativos y excepciones

- **A1 (paso 2):** No tiene ayudas guardadas → la app lo dice con un estado vacío.
- **A2 (paso 2):** No hay conexión o el servidor no responde → la app avisa y ofrece «Reintentar».
- **A3 (paso 2):** La miniatura de una ayuda ya no carga → se muestra un icono en su lugar.
- **A4 (paso 5):** No se pudo guardar la opinión → la app avisa y la opinión queda como estaba.
- **A5 (consulta de una ayuda puntual):** La ayuda no existe o es de otra persona → 404 `ERR_IA_03` (no se revela si existe).
- **A6 (cualquier paso):** La paginación es inválida (`page` o `pageSize` fuera de rango) → 400 `ERR_SYS_01`.
- **A7 (cualquier paso):** La persona no es Estudiante → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.

## Reglas de negocio

- **Cada persona ve solo lo suyo:** la persona sale del token, nunca de la URL ni del cuerpo; una ayuda ajena responde igual que una inexistente.
- **Orden:** de la ayuda más reciente a la más antigua.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): 20 por página por defecto y 100 como máximo; la app pide la primera página de 50.
- **La opinión es de la persona:** `util` puede ser verdadero, falso o ninguno (todavía no opinó); se puede cambiar cuantas veces quiera. No cambia el resto de la ayuda.
- **No se borran ayudas** desde este caso de uso.
- **Solo lectura salvo la opinión:** la ayuda en sí (luces, explicación, modelo) no se edita.

## Postcondiciones

- Ver el historial no cambia ningún dato.
- Marcar la opinión deja `util` guardado en esa ayuda.

## Criterios de aceptación

- **CA01:** Dado un Estudiante con sesión, el sistema debe listar solo sus ayudas, de la más reciente a la más antigua, paginadas.
- **CA02:** Dada una página fuera del historial, el sistema debe devolver una lista vacía con el total correcto.
- **CA03:** Dada una ayuda propia, el sistema debe devolver su detalle con las luces, la explicación, el modelo, la miniatura (si hay) y la opinión.
- **CA04:** Dada una ayuda de otra persona o inexistente, el sistema debe responder 404 `ERR_IA_03` a la consulta y a la opinión.
- **CA05:** Dada la opinión de una ayuda, el sistema debe guardarla y devolverla, y permitir cambiarla.
- **CA06:** Dada una paginación inválida o una opinión que no sea verdadero/falso, el sistema debe responder 400 `ERR_SYS_01`.
- **CA07:** Dada una persona que no es Estudiante, el sistema debe responder 403, y 401 si no hay sesión válida.
- **CA08:** Dado que no hay ayudas, la app debe mostrar un estado vacío; dado un error de red, un aviso con reintento.
- **CA09:** Dada una ayuda recién guardada por CU40, debe aparecer al abrir el historial sin recargar la app.

## Fuera de alcance

- **Borrar ayudas** o limpiar miniaturas.
- **Buscar o filtrar** el historial.
- **Cargar más páginas:** hoy se trae la primera de 50.
- **Volver a analizar** una ayuda guardada.

## Desarrollador

Otsubo
