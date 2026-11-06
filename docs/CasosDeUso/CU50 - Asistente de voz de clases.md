# Caso de uso

| Id   | Nombre                    | Paquete          | Prioridad | Estado    |
| ---- | ------------------------- | ---------------- | --------- | --------- |
| CU50 | Asistente de voz de clases | P8. Asistente IA | Media     | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Consultar y gestionar sus clases hablándole a la app, sin tocar los menús.
**Plataforma:** Móvil

**Descripción:**
En la pantalla de Clases, el Estudiante mantiene pulsado el micrófono y dice lo que quiere («¿tengo práctica el viernes?», «busca horarios por la mañana con Carlos», «cancela mi próxima clase»). **La voz se convierte en texto y el texto se convierte en una acción de la pantalla, las dos cosas dentro del teléfono**: el audio no sale del dispositivo y todo funciona sin internet para entender la petición. La app muestra lo que entendió y ejecuta la acción sobre la pantalla de Clases: consultar las clases por venir o el historial, buscar horarios libres, marcar un horario, quitar filtros, ver las horas que le quedan, y cancelar o reprogramar una clase. **Reservar, cancelar y reprogramar nunca se ejecutan solos:** la app siempre muestra un resumen y pide confirmación expresa antes de cambiar algo. Si la persona pide algo que no se puede hacer desde la pantalla de Clases, o la app no está segura de haber entendido, no hace nada y lo dice. Las operaciones que cambian datos (reservar, cancelar, reprogramar) necesitan conexión, porque las ejecutan los mismos procesos que la pantalla ([CU14](CU14%20-%20Reservar%20horario.md) y [CU15](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md)); entender la petición no.

---

## Precondiciones

- El Estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- Los modelos de voz y de acciones vienen empaquetados en la app (se entrenan y se exportan con `projects/mobile/ml/`).
- El Estudiante concedió el permiso de micrófono a la app.
- Las acciones sobre clases existen en la pantalla: [CU14](CU14%20-%20Reservar%20horario.md) (reservar), [CU15](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md) (cancelar y reprogramar) y [CU16](CU16%20-%20Consultar%20horario%20de%20clases.md) (consultar). El asistente no las reemplaza: las dispara.

## Flujo principal

1. El Estudiante abre la pestaña de Clases y mantiene pulsado el botón del micrófono.
2. Dice su petición y suelta el botón.
3. La app convierte el audio en texto en el teléfono y lo muestra en pantalla.
4. La app elige, con el modelo del teléfono, una de las acciones de la pantalla de Clases y sus datos (día, momento del día, instructor, hora).
5. Si la acción es de consulta (ver mis clases, ver el historial, buscar horarios, quitar filtros, ver mis horas), la app la ejecuta de inmediato sobre la pantalla: cambia a la pestaña que corresponde y aplica los filtros pedidos.
6. Si la acción cambia datos (reservar el horario marcado, cancelar o reprogramar una clase), la app muestra un resumen («Cancelar la clase del viernes 14:00 con Carlos») con «Confirmar» y «Cancelar».
7. El Estudiante confirma.
8. La app ejecuta la operación con las mismas reglas que la pantalla y muestra el resultado (clase reservada y horas que le quedan, clase cancelada, clase reprogramada).

## Flujos alternativos y excepciones

- **A1 (paso 3):** El audio no se entiende o está vacío → la app lo dice y pide repetir; no ejecuta nada.
- **A2 (paso 4):** La petición no es de la pantalla de Clases (otro tema, otra pantalla, conversación general) → la app avisa que eso no se puede hacer desde acá y no ejecuta nada.
- **A3 (paso 4):** El modelo no está seguro de qué acción es, o falta un dato obligatorio (por ejemplo, la hora al elegir un horario) → la app pregunta o pide repetir; no adivina.
- **A4 (paso 4):** El día o el instructor dicho no coincide con ninguno de la lista de la persona → la app lo avisa y muestra las opciones disponibles; no aplica un filtro inventado.
- **A5 (paso 6):** La petición de cancelar o reprogramar es ambigua (hay varias clases que cuadran) → la app muestra las que cuadran y la persona elige una; no actúa sobre ninguna sin elegir.
- **A6 (paso 6):** No hay clase que cancelar o reprogramar, o no hay horarios libres → la app lo dice y no abre confirmación.
- **A7 (paso 7):** El Estudiante toca «Cancelar» o no confirma → no se cambia nada.
- **A8 (paso 8):** Reservar sin horas suficientes → la app lo dice y ofrece ir a comprar un plan ([CU26](CU26%20-%20Realizar%20pago.md)); no se reserva.
- **A9 (paso 8):** Sin conexión o el servidor no responde → la app avisa que no pudo hacer el cambio y no cambia nada; las consultas sobre lo ya cargado siguen funcionando.
- **A10 (paso 8):** El horario ya no está libre, la clase ya no se puede cancelar o reprogramar por las reglas de [CU15](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md) → la app muestra el motivo que dé la pantalla y no cambia nada.
- **A11 (paso 1):** El permiso de micrófono no está concedido → la app lo pide; si se niega, el botón queda inactivo y la pantalla sigue funcionando con sus controles normales.
- **A12 (paso 2 o 4):** Los modelos de voz o de acciones no están en la app → la app avisa que el asistente no está disponible; el resto de la pantalla sigue funcionando.

## Reglas de negocio

- **Todo se entiende en el teléfono:** la conversión de voz a texto y la elección de la acción corren dentro del dispositivo, sin internet y sin enviar el audio ni el texto a ningún servidor ([ARQUITECTURA_MOBILE.md](../../.claude/rules/ARQUITECTURA_MOBILE.md)). El servidor solo interviene al ejecutar una operación que cambia datos, igual que si la persona la hubiera hecho con los botones.
- **El modelo solo elige la acción, no la ejecuta:** su salida es una acción de un catálogo cerrado con sus datos. La app valida esa salida y ejecuta la operación con la misma lógica y los mismos permisos que la pantalla; un resultado que no es una acción del catálogo se descarta.
- **Catálogo cerrado de acciones de la pantalla de Clases:** ver mis clases, ver el historial, buscar horarios, seleccionar un horario, limpiar filtros, cancelar una clase, reprogramar una clase, ver las horas disponibles y «fuera de alcance». Una acción nueva se agrega al catálogo y al entrenamiento, no al código.
- **Confirmación obligatoria para cambiar datos:** reservar, cancelar y reprogramar siempre piden confirmación expresa con un resumen de lo que va a pasar. Elegir un horario («el de las 10») solo lo marca; no reserva.
- **Mismas reglas que la pantalla:** las horas, los plazos de cancelación y reprogramación, la disponibilidad y los permisos son los de [CU14](CU14%20-%20Reservar%20horario.md) y [CU15](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md). El asistente no tiene atajos.
- **Día e instructor se resuelven contra las listas de la persona:** «viernes», «mañana» o «la profe Lucía» se comparan con los días y los instructores que la app ya muestra; un nombre que no cuadra no se inventa.
- **Dudas, no suposiciones:** ante una petición ambigua, incompleta o de baja confianza, la app pregunta o no hace nada. Una acción mal elegida no puede cambiar datos sin que la persona la vea y la confirme.
- **Solo el Estudiante** usa esta función; la persona sale de la sesión, nunca de lo dicho.
- **Privacidad:** el audio no se guarda. El texto reconocido no sale del teléfono.

## Postcondiciones

- Las consultas dejan la pantalla en la pestaña y con los filtros pedidos; no cambia ningún dato.
- Una operación confirmada y exitosa deja la clase reservada, cancelada o reprogramada, igual que si se hubiera hecho con los botones, y las horas del Estudiante actualizadas.
- En los demás casos no cambia ningún dato.

## Criterios de aceptación

- **CA01:** Dada una petición de voz de consulta («¿tengo práctica el viernes?»), la app debe mostrar el texto reconocido y llevar a la pantalla de clases con el filtro pedido, sin internet.
- **CA02:** Dada una petición de buscar horarios con día, momento del día o instructor, la app debe aplicar esos filtros en Reservar, resolviendo el día y el instructor contra las listas de la persona.
- **CA03:** Dada una petición de cancelar o reprogramar una clase, la app debe mostrar un resumen con «Confirmar» y «Cancelar» y no cambiar nada hasta que la persona confirme.
- **CA04:** Dada una confirmación, la app debe ejecutar la operación con las reglas de CU14 o CU15 y mostrar el resultado.
- **CA05:** Dado que la persona no confirma o toca «Cancelar», no debe cambiar ningún dato.
- **CA06:** Dada una petición que no es de la pantalla de Clases, la app debe avisar que no se puede hacer desde acá y no ejecutar nada.
- **CA07:** Dado un audio que no se entiende, un dato obligatorio que falta o una acción de baja confianza, la app debe pedir repetir o precisar y no ejecutar nada.
- **CA08:** Dada una petición de cancelar o reprogramar con varias clases posibles, la app debe mostrar las candidatas y pedir que la persona elija una.
- **CA09:** Dado un día o un instructor que no coincide con la lista, la app debe avisarlo y mostrar las opciones, sin aplicar un filtro inventado.
- **CA10:** Dada una reserva sin horas suficientes, la app debe avisarlo y ofrecer ir a comprar un plan, sin reservar.
- **CA11:** Dada una operación que cambia datos sin conexión, la app debe avisar que no se pudo y no cambiar nada.
- **CA12:** Dado un permiso de micrófono denegado o los modelos ausentes, la app debe avisarlo sin cerrarse y dejar la pantalla utilizable con sus controles normales.
- **CA13:** Dado cualquier audio, la app no debe guardarlo ni enviarlo fuera del teléfono.

## Casos límite a probar

Pruebas manuales en un teléfono real, además de los criterios de aceptación. Cada fila dice qué se hace y qué debe pasar. Se prueban con la voz de la persona (no con voz sintética) y con ruido de fondo normal.

| # | Qué se hace | Qué debe pasar |
| - | ----------- | -------------- |
| L01 | Pulsar y soltar sin hablar | El asistente avisa «No te escuché» y no ejecuta nada |
| L02 | Pedir algo que no es de Clases («cuéntame un chiste», «¿qué hora es?») | Explica qué sí puede hacer y no ejecuta nada |
| L03 | Pedir marcar un horario sin decir la hora («reserva uno») | Pide más detalle y no marca nada |
| L04 | Pedir cancelar o reprogramar sin tener clases por venir | Dice «No tienes clases por venir» y no abre ninguna confirmación |
| L05 | Pedir cancelar «mi clase del lunes» teniendo dos ese día | Muestra las dos para elegir; no actúa sobre ninguna hasta que la persona elige |
| L06 | Pedir una clase de un día en el que no tiene ninguna | Lo dice («No tienes clases ese día») y no cambia la pantalla de forma engañosa |
| L07 | Pedir horarios con un instructor que no existe | Dice que no lo encontró y muestra con quiénes hay horarios; no aplica ningún filtro |
| L08 | Pedir un instructor por nombre parcial, con tilde mal dicha o con tratamiento («la profe Lucia») | Lo reconoce; si el nombre cuadra con varios, pide elegir |
| L09 | Pedir marcar un horario con 0 horas disponibles | Dice que no tiene horas y no marca nada |
| L10 | Pedir marcar un horario que ya marcó | Dice que ya está marcado; no lo desmarca |
| L11 | Pedir marcar un horario que no existe a esa hora | Lo dice; no marca nada |
| L12 | Cancelar una clase a menos de 12 horas | El resumen avisa que perderá la hora, y la cancelación sigue disponible |
| L13 | Reprogramar una clase a menos de 12 horas | Dice que ya no se puede reprogramar y sugiere cancelarla |
| L14 | Pedir cancelar y tocar «No» o cerrar el panel | No cambia ningún dato |
| L15 | Pedir cancelar, tocar «Continuar» y volver desde el diálogo sin confirmar | No cambia ningún dato y el asistente queda inactivo |
| L16 | Pedir una consulta sin conexión | Avisa que no pudo consultarlo y la app no se cierra |
| L17 | Pedir cancelar sin conexión y confirmar en el diálogo | El diálogo avisa del problema y permite reintentar; nada queda a medias |
| L18 | Mantener pulsado más de 30 segundos | Procesa solo lo que cabe y no se cuelga |
| L19 | Soltar el botón apenas se toca (antes de que el micrófono arranque) | No queda ninguna grabación abierta |
| L20 | Pulsar el micrófono dos veces seguidas muy rápido | No se abren dos grabaciones |
| L21 | Negar el permiso de micrófono y volver a intentar | Explica cómo darlo; el resto de la pantalla sigue usable |
| L22 | Quitar o no tener los modelos en el teléfono | Avisa «no está disponible» y la pantalla funciona con sus botones |
| L23 | Hablar con una frase larga, con muletillas o con la acción dicha de forma indirecta («oye, ¿cuándo era que me tocaba manejar?») | Si no la entiende, lo dice; nunca ejecuta una acción distinta de la pedida |
| L24 | Hablar con ruido de fondo, con la voz baja o en otro acento | Entiende o avisa que no entendió; nunca cambia una clase por una mala transcripción sin que la persona la vea y confirme |
| L25 | Pedir una consulta, y enseguida otra distinta | Cada una se procesa con su propia frase, sin arrastrar la anterior |
| L26 | Salir de la sección Clases mientras escucha o procesa | No se cuelga ni ejecuta nada al volver |
| L27 | Girar la pantalla o mandar la app a segundo plano mientras escucha | La grabación se descarta y no queda abierta |

## Mejoras pendientes

Lo que ya se observó probando el asistente en un teléfono real y que se puede mejorar sin cambiar el comportamiento del CU:

- **Velocidad:** entender una frase tarda alrededor de 13 segundos (unos 2 de voz y 11 del modelo). Casi todo es procesar el texto de las funciones que se le explica al modelo. Reducir ese texto y volver a entrenar con él es la mejora más grande. La GPU del teléfono probado no sirvió (no da resultados correctos).
- **Precisión del modelo:** en una prueba de 144 frases acertó la acción en 87,5 % de los casos. Falla más con «ver historial» y con frases coloquiales o indirectas. Hace falta más ejemplos de entrenamiento de esos tipos.
- **Errores de la transcripción de voz:** la voz a texto a veces cambia una palabra («Cancella», «Muestra-me»); el modelo suele salvarlo, pero conviene medir cuánto con voces reales y, si hace falta, usar un modelo de voz más preciso.
- **Entrega de los modelos al usuario:** hoy hay que copiarlos a mano al teléfono (más de 380 MB entre los dos). Falta decidir cómo llegan a una persona real (descarga la primera vez o empaquetado) y qué pasa mientras no están.
- **Permiso de micrófono denegado:** el aviso explica cómo darlo en los ajustes, pero no hay un botón que los abra.
- **Mantener el modelo cargado:** hoy se procesa cada frase desde cero; mantener lo ya procesado entre frases sin que una influya en la otra reduciría el tiempo.
- **Más pantallas y roles:** solo la sección Clases del Estudiante. Otras pantallas, el Instructor y el Administrador necesitarían su propio catálogo de acciones y su propio caso de uso.

## Fuera de alcance

- **Otras pantallas:** pagos y planes, exámenes, simulacros, perfil, contenido. Cada una necesitaría su propio catálogo de acciones y su propio caso de uso.
- **Comprar un plan por voz:** el asistente solo ofrece ir a la compra ([CU26](CU26%20-%20Realizar%20pago.md)); no paga.
- **Respuestas habladas:** el asistente responde en pantalla, no con voz sintetizada.
- **Conversación libre:** no es un chat; solo entiende las acciones del catálogo.
- **El chat de WhatsApp** ([CU49](CU49%20-%20Conversar%20por%20WhatsApp.md)), que corre en un servidor y excluye las notas de voz.
- **iOS:** por ahora solo Android.
- **El instructor y el Administrador:** esta versión es del Estudiante.

## Desarrollador

Leonel
