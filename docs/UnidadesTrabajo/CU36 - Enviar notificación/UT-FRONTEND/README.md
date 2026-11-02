# Unidades de trabajo — CU36-FE

**Caso de uso:** Enviar notificación (Sistema): el panel web registra el navegador de la persona para recibir push.
**Microservicio(s) / micro-frontend(s):** `shell` (Angular) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU36 - Enviar notificación`](../../../CasosDeUso/CU36%20-%20Enviar%20notificaci%C3%B3n.md)
**Recurso HTTP / contrato:** `POST /api/v1/dispositivos-push` y `DELETE /api/v1/dispositivos-push/{codigo}` (ver [UT-BACKEND](../UT-BACKEND/README.md)). No hay pantalla nueva.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Solo el shell**, que es el dueño de la sesión: los remotos no registran nada.
2. **Firebase Web con el SDK modular** (`firebase/app`, `firebase/messaging`), cargado con `import()` solo cuando hay sesión y permiso, para no pesar en el arranque.
3. **Configuración en tiempo de ejecución** (`public/firebase.config.json`, como los manifiestos de federación): son valores públicos de la app web de Firebase. Con algún valor vacío la función queda desactivada y la web funciona igual.
4. **Service worker propio y mínimo** (`public/firebase-messaging-sw.js`), sin el SDK: muestra el aviso que llega con el panel cerrado o en segundo plano y, al hacer clic, abre o enfoca el panel.
5. **Nunca se pide el permiso por sorpresa en el login**: se pide cuando hay sesión y solo si el navegador aún no lo decidió. Si la persona lo niega, no se vuelve a insistir.
6. **No bloquea nada**: cualquier falla del registro se ignora (queda en la consola); la sesión y la navegación no dependen de ello.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU36-FE-01](./UT-CU36-FE-01.md) | Servicio de push web, service worker y configuración | — |
| [UT-CU36-FE-02](./UT-CU36-FE-02.md) | Registrar al iniciar sesión y quitar al cerrarla | 01 (y UT-CU36-BE-09) |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Registrar, paso 4, CA13 | Registro y recepción en el navegador | 01, 02 |
| B3 | Permiso denegado | 01 |
| Quitar, CA14 | Al cerrar sesión | 02 |

## Follow-ups (no se implementan en estas UT)

- La campana con la lista de avisos del panel (CU38).
- Preferencias de notificación en la web (CU37 es móvil).
- Abrir la pantalla concreta según los `datos` del aviso.

## Qué NO incluyen estas UT

- Cambios en los remotos.
- El backend ([UT-BACKEND](../UT-BACKEND/README.md)).
- La app móvil.

## Commits sugeridos

Un commit `[FRONT]` para `shell` (UT 01 y 02). Si cambia `pnpm-lock.yaml`, va con `[REPO]`.
