# Unidades de trabajo — CU38-MOB

**Caso de uso:** Consultar historial de notificaciones: la bandeja de avisos, el contador de no leídas en la campana y marcar como leídos.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU38 - Consultar historial de notificaciones.md`](../../../CasosDeUso/CU38%20-%20Consultar%20historial%20de%20notificaciones.md)
**Recurso HTTP / contrato:** `/notificaciones`, `/notificaciones/conteo-no-leidas`, `/notificaciones/{codigo}/leer` y `/notificaciones/leer-todas`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Misma feature `notificaciones`** (`lib/features/notificaciones/`) que ya tiene las preferencias (CU37): se suman archivos a `models/`, `services/`, `providers/` y `screens/`; no se crea otra feature.
2. **El acceso es la campana que ya existe** en la barra superior de `ShellConBarra` (hoy muestra un snackbar «Todavía no tienes avisos nuevos»): pasa a abrir la ruta `/notificaciones` y a mostrar el contador como insignia. No se agrega una pestaña a la barra inferior.
3. **Lista con carga por páginas al llegar al final**, de 20 en 20, con la `Pagina<T>` de `core/network/pagina.dart` y el mismo patrón de `mis_alumnos_provider.dart`. Un refresco (deslizar hacia abajo) vuelve a la primera página.
4. **Filtros con fichas** (`ChoiceChip`): «Todas», «No leídas» y los cuatro tipos; se combinan «No leídas» con un tipo. Cambiar el filtro vuelve a pedir la primera página.
5. **Abrir un aviso** muestra el mensaje completo en una hoja inferior y lo marca leído en el servidor. La marca se refleja **en seguida** en la lista y el contador; si el servicio falla, la hoja igual se ve y la lista conserva el aviso como no leído (se reintenta al abrirlo otra vez, A5 del CU). Con 404 se quita el aviso de la lista.
6. **El contador es un provider aparte** (`conteoNoLeidasProvider`) que se refresca al abrir la app, al volver a la bandeja y después de marcar; sin red muestra la campana sin insignia, sin error. Se descarta si cambia la persona de la sesión, igual que las preferencias.
7. **Sin valores inventados:** mientras carga la primera página, un indicador; si falla, `EstadoVacio` con «Reintentar»; lo ya cargado se conserva si falla una página posterior.

## Contrato de datos

`Notificacion` (lo que devuelve el backend): `codigoNotificacion`, `tipo` (`RESERVA`, `PAGO`, `EXAMEN`, `RECORDATORIO`), `titulo`, `mensaje`, `leida`, `creadaEn` (ISO). Los tipos se muestran en español: «Clases y reservas», «Pagos», «Exámenes», «Recordatorios».

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU38-MOB-01](./UT-CU38-MOB-01.md) | Modelos, servicio y providers de la bandeja | UT-CU38-BE-04 |
| [UT-CU38-MOB-02](./UT-CU38-MOB-02.md) | Pantalla «Notificaciones» y detalle del aviso | 01 |
| [UT-CU38-MOB-03](./UT-CU38-MOB-03.md) | Campana con contador en la barra superior | 01, 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Paso 1, CA04 | Contador de no leídas | 01, 03 |
| Pasos 2 y 3, CA01, CA08, A1 | Abrir la bandeja y verla vacía | 01, 02, 03 |
| Pasos 4 y 5, CA03 | Filtrar y cargar más | 01, 02 |
| Paso 7, A3, A4, A5, CA05 | Abrir un aviso y marcarlo leído | 01, 02 |
| Marcar todas, A6, CA06 | «Marcar todas como leídas» | 01, 02 |
| A2, CA12 | Reintentar sin perder lo cargado | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Ir a la pantalla relacionada (clase, pago, examen) desde el aviso, cuando el backend entregue los `datos`.
- Actualizar la bandeja al llegar un push con la app abierta (depende del token de Firebase de CU36).
- La campana en el panel web.

## Qué NO incluyen estas UT

- Cambios en el backend ([UT-BACKEND](../UT-BACKEND/README.md)).
- Las preferencias (CU37) y el permiso de notificaciones del teléfono.
- Borrar avisos o marcarlos como no leídos.

## Commits sugeridos

Un commit `[MOBILE]` (`feat[MOBILE]: notificaciones -- CU38 historial de notificaciones ...`) con las tres UT.
