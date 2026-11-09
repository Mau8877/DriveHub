# Unidades de trabajo — CU36-MOB

**Caso de uso:** Enviar notificación, parte móvil: la app registra el teléfono para recibir avisos push de Firebase, lo quita al cerrar sesión y refresca la bandeja cuando llega un aviso con la app abierta.
**Microservicio(s) / micro-frontend(s):** app móvil (Flutter) — consume `notificaciones` por el `gateway`; ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU36 - Enviar notificación`](../../../CasosDeUso/CU36%20-%20Enviar%20notificaci%C3%B3n.md) (CA14 a CA18)
**Recurso HTTP / contrato:** `POST /api/v1/dispositivos-push` y `DELETE /api/v1/dispositivos-push/{codigo}` (REST), o las mutaciones `registrarDispositivo` y `quitarDispositivo` de `POST /api/v1/graphql` ([CU45](../../CU45%20-%20Consultar%20datos%20v%C3%ADa%20GraphQL/UT-BACKEND/README.md)). Ya existen: no se toca el backend.

## Antes de implementar cualquier UT

Leer [`.claude/rules/mobile/`](../../../../.claude/rules/mobile/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) (sección «Avisos push»), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El `applicationId` es `com.drivehub.app`** y tiene que coincidir con el `package_name` de `google-services.json`. Android lo trata como una app distinta de la anterior (`com.example.mobile`): no hereda sus datos (por ejemplo, los modelos de IA del CU50, que viven en su carpeta privada). El `namespace` (paquete de las clases) se deja igual.
2. **`google-services.json` va al repositorio** (`android/app/`): es configuración pública del cliente, no un secreto. La clave de la cuenta de servicio del backend (`FIREBASE_CREDENCIALES`) es otra cosa y NO va al repositorio.
3. **El push nunca bloquea la app.** Sin permiso, sin servicios de Google, sin red o sin sesión con token, no pasa nada visible: no hay errores ni reintentos agresivos. Un registro que falló se reintenta en el siguiente cambio de sesión (cada renovación la dispara).
4. **El permiso se pide una vez por ejecución de la app** y, si se niega, no se vuelve a insistir hasta la próxima vez que se abra.
5. **Se registra una vez por persona**, aunque la sesión se renueve cada pocos minutos; con otra persona en el mismo teléfono se registra de nuevo.
6. **Cerrar sesión quita el dispositivo ANTES de perder el token** (quitarlo pide sesión): un gancho `antesDeCerrarSesion` de la sesión, con un máximo de 5 segundos, y un fallo no impide salir. Perder la sesión porque el servidor la rechaza no lo dispara (no fue la persona).
7. **Firebase se inicia la primera vez que se usa**, no al abrir la app, y detrás de una interfaz (`MensajeriaPush`) para que los tests no toquen Firebase.
8. **`getToken` y `onTokenRefresh` se mantienen aunque figuren como obsoletos** en `firebase_messaging` 17: el backend manda con el Admin SDK a tokens de registro de FCM (`sendEachForMulticast`). El reemplazo (`register()` y `onRegistered`) entrega un ID de instalación para «envío directo», que exige cambiar cómo envía el backend. Queda como follow-up.
9. **Con la app abierta Android no muestra el aviso del sistema:** se refrescan el contador y la bandeja y se muestra un aviso breve con el `ScaffoldMessenger` global. Con la app cerrada o en segundo plano lo muestra el sistema solo (el backend manda `notification` con título y mensaje).
10. **Mismo transporte que los avisos:** el registro va por GraphQL o REST según `NOTIFICACIONES_TRANSPORTE`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU36-MOB-01](./UT-CU36-MOB-01.md) | Firebase y `applicationId` en Android | — |
| [UT-CU36-MOB-02](./UT-CU36-MOB-02.md) | Registro del dispositivo: mensajería, servicios y reglas | 01 |
| [UT-CU36-MOB-03](./UT-CU36-MOB-03.md) | Conexión con la sesión y los avisos en primer plano | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| CA | Descripción | UTs |
|----|-------------|-----|
| CA14 | Registrar el teléfono una vez por sesión | 02, 03 |
| CA15 | Quitarlo al cerrar sesión, también sin conexión | 02, 03 |
| CA16 | Sin permiso, sin Google o sin red, sin errores ni insistencia | 02 |
| CA17 | Token renovado | 02 |
| CA18 | Aviso con la app abierta | 03 |

## Follow-ups (no se implementan en estas UT)

- **Dos personas en el mismo teléfono sin cerrar sesión** (la sesión de la primera se perdió por el servidor): el mismo token de Firebase queda registrado para las dos. El backend debería reasignar un token al último que lo registra.
- Migrar a `register()` y `onRegistered` si se cambia el envío del backend.
- Abrir la pantalla relacionada al tocar el aviso (hoy solo se abre la app).
- Aviso del sistema también con la app abierta (`flutter_local_notifications`).
- Registrar en la consola de Firebase la huella SHA-1 si algún día se agrega otro servicio de Google que la pida.
- Firebase para iOS.

## Qué NO incluyen estas UT

- El backend ni el gateway (ya existen).
- La bandeja y las preferencias ([CU38](../../CU38%20-%20Consultar%20historial%20de%20notificaciones/UT-MOBILE/README.md) y CU37).
- El panel web, que ya registra su navegador.

## Commits sugeridos

Un commit `[MOBILE]` (UT 01 a 03), el de `docs/` como `[DOCS]` y el de `.claude/rules` como `[REPO]`.
