# Unidades de trabajo — CU37-MOB

**Caso de uso:** Configurar preferencias de notificación: la pantalla «Perfil > Notificaciones» con canales, tipos de aviso y horas de silencio.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU37 - Configurar preferencias de notificación.md`](../../../CasosDeUso/CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)
**Recurso HTTP / contrato:** `/preferencias-notificacion` (`GET` y `PUT`)

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Una feature nueva, `notificaciones`** (`lib/features/notificaciones/`), con `models/`, `services/`, `providers/` y `screens/`. La pantalla se llega desde el Perfil, pero su código no vive en `perfil/`: CU38 (el historial) y la parte móvil de CU36 (el token de Firebase) van a vivir en esa misma feature.
2. **Se guarda con un botón «Guardar»**, no al mover cada interruptor: la configuración completa va en un solo `PUT` y el usuario puede arrepentirse.
3. **Estado en dos capas:** `preferenciasNotificacionProvider` (`AsyncNotifier`, lo que está guardado en el servidor) y `preferenciasFormUiProvider` (el borrador de pantalla que la persona va tocando). «Guardar» solo se habilita si el borrador difiere de lo guardado.
4. **Las horas de silencio con el selector de hora de Flutter** (`showTimePicker`, 24 h). Un interruptor «Horas de silencio» lo activa; apagado, no se envían horas.
5. **El correo sale del perfil** (`Perfil.email`, CU06): la pantalla lo envía con cada guardado sin mostrarlo ni pedirlo.
6. **Las reglas de la franja son las del backend** ([FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md#consistencia-de-reglas-de-negocio-con-el-frontend-web)): las dos horas o ninguna, y no iguales. El backend manda.
7. **Sin valores inventados:** mientras carga o si falla la carga no se muestran interruptores.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU37-MOB-01](./UT-CU37-MOB-01.md) | Modelos, servicio y providers de las preferencias | UT-CU37-BE-03 |
| [UT-CU37-MOB-02](./UT-CU37-MOB-02.md) | Pantalla «Notificaciones» y acceso desde el Perfil | 01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 y 2, CA01, A1 | Abrir, cargar y reintentar | 01, 02 |
| Pasos 3 a 6, CA02, CA07 | Cambiar y guardar | 01, 02 |
| A3, A4, CA06 | Validar la franja de silencio | 01, 02 |
| A2, CA11 | Conservar lo tocado si el guardado falla | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Pedir el permiso de notificaciones del teléfono y registrar el token de Firebase (`firebase_messaging`, parte móvil de [CU36](../../CU36%20-%20Enviar%20notificación/UT-BACKEND/README.md)).
- Mostrar un aviso si el teléfono tiene las notificaciones bloqueadas desde sus ajustes.

## Qué NO incluyen estas UT

- La lista de avisos recibidos (CU38).
- Cambios en el backend ([UT-BACKEND](../UT-BACKEND/README.md)).
- Preferencias en la web.

## Commits sugeridos

Un commit `[MOBILE]` (`feat[MOBILE]: notificaciones -- CU37 ...`) con las dos UT.
