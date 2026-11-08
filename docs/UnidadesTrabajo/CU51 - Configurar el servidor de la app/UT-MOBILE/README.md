# Unidades de trabajo — CU51-MOB

**Caso de uso:** Configurar el servidor de la app: pantalla de primera vez para elegir el backend, y poder cambiarlo desde el inicio de sesión.
**Microservicio(s) / micro-frontend(s):** app móvil (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU51 - Configurar el servidor de la app`](../../../CasosDeUso/CU51%20-%20Configurar%20el%20servidor%20de%20la%20app.md)
**Recurso HTTP / contrato:** ninguno propio: la prueba de conexión pide la raíz de la API (`GET /api/v1/`) sin sesión

## Antes de implementar cualquier UT

Leer [`.claude/rules/mobile/`](../../../../.claude/rules/mobile/): [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (redirect global), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Se escribe el servidor, no la ruta de la API.** La persona pone `http://192.168.1.10:8080` y la app agrega `/api/v1` (`/api` pasa a `/api/v1`; otra ruta se respeta). Sin `http://` se asume `http://`.
2. **El validador rechaza lo que `Uri.tryParse` deja pasar.** Dart codifica un espacio del host (`hola mundo` → `hola%20mundo`) en vez de rechazarlo, así que se valida el host con una expresión y se rechaza cualquier espacio. Lo encontró un test.
3. **La dirección cambia en caliente en el MISMO `Dio`.** Los servicios guardan una referencia a `ApiClient.instance`; `configurarBaseUrl` actualiza `options.baseUrl` de ese objeto en lugar de crear otro, así ningún servicio queda hablando con el servidor anterior.
4. **Guardar en `flutter_secure_storage`**, que la app ya usa para la sesión (la dirección no es un secreto, pero no se suma otra dependencia). La lee `main()` antes de crear la sesión y el modo offline.
5. **Sin servidor, el router solo permite `/servidor`.** El chequeo va ANTES del de sesión en el redirect global. Por defecto el estado inicial es «configurado» para que los tests de las demás pantallas no pasen por acá; `main()` lo sustituye con lo leído del teléfono.
6. **«Probar conexión» es informativa.** Distingue un servidor de DriveHub porque responde con el sobre estándar (`status` y `timestamp`), también en un 404 o un 401 de la raíz. No bloquea guardar: la app tiene que poder dejarse configurada sin conexión.
7. **«Cambiar servidor» vive en el inicio de sesión.** Sin esa salida, una dirección mal escrita dejaría a la persona atascada. Con sesión iniciada la ruta `/servidor` redirige al inicio.
8. **La dirección de `API_BASE_URL` del `.env` pasa a ser solo la sugerencia** (y el valor de los tests y del desarrollo con la misma máquina).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU51-MOB-01](./UT-CU51-MOB-01.md) | Dirección del servidor: validación, almacenamiento y cambio en caliente del cliente HTTP | — |
| [UT-CU51-MOB-02](./UT-CU51-MOB-02.md) | Pantalla de servidor y prueba de conexión | 01 |
| [UT-CU51-MOB-03](./UT-CU51-MOB-03.md) | Primera vez en el router, «Cambiar servidor» en el login y arranque en `main()` | 02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA03, CA04, CA05, CA13 | Normalizar y validar la dirección | 01 |
| CA03, CA12 | Guardar y usarla en el momento | 01 |
| CA06, CA07, CA08 | Probar la conexión; guardar sin conexión | 02 |
| CA01, CA02, CA09, A5 | Primera vez y arranques siguientes | 03 |
| CA10, CA11, A6 | Cambiar el servidor desde el login | 03 |

## Follow-ups (no se implementan en estas UT)

- Código QR o enlace de invitación para no escribir la dirección.
- Que el cambio de servidor con una cola de acciones pendientes avise (las pendientes se envían al servidor que esté en uso).
- Mostrar el servidor actual en el perfil.

## Qué NO incluyen estas UT

- Cambios en el backend ni en el gateway.
- Certificados propios.
- Ningún cambio en el panel web.

## Commits sugeridos

Un commit `[MOBILE]` (UT 01 a 03), el de `docs/` como `[DOCS]` y el de `.claude/rules` como `[REPO]`.
