---
globs: projects/mobile/**/*
---

# Consumo de API — Mobile

## Una sola URL: el API Gateway

La app habla **solo con el gateway** (`API_BASE_URL`, ej.
`http://10.0.2.2:8080/api/v1` en el emulador de Android), nunca con un
microservicio directo: el gateway enruta cada recurso al servicio que
corresponde (ver [MICROSERVICIOS.md](../MICROSERVICIOS.md#recursos-http-de-cada-servicio)).
Todos los servicios responden con el mismo sobre, sin importar su stack.

**La dirección del gateway la elige la persona** (CU51): la primera vez que abre la app escribe el servidor y se
guarda en el teléfono. `ApiClient.baseUrlActual` es lo elegido; sin eso, `API_BASE_URL` del `.env` (que hoy solo es la
dirección sugerida y la de los tests). `ApiClient.configurarBaseUrl` cambia la dirección del MISMO `Dio` que ya tienen los
servicios, así que no hay que reiniciar ni volver a crearlos. Nada arma una URL a mano: todo parte de `ApiClient`.
El refresh token en mobile viaja en el body y se guarda en
`flutter_secure_storage` (no hay cookies, ver
[SEGURIDAD_AUTH_BACKEND.md](../backend/SEGURIDAD_AUTH_BACKEND.md)).

## Cliente `dio` centralizado (prerequisito)

`dio` está en `pubspec.yaml`, pero **no existe todavía** ningún cliente
configurado — `lib/core/network/` solo tiene un `.gitkeep`. Antes de
escribir el primer `service` que llame al backend, hay que crear:

- `lib/core/network/api_client.dart`: una instancia única de `Dio` con
  `baseUrl` apuntando a la API (leído de configuración de entorno —
  `flutter_dotenv` está instalado en `pubspec.yaml` pero tampoco está
  wireado todavía, es otro prerequisito ligado a este mismo punto).
- Un `Interceptor` que desempaqueta el sobre estándar del backend
  (`{status, data, message, error, timestamp, meta?}`, mismo contrato que
  [RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)):
  - Si `status == "success"`, el interceptor deja pasar `data` (y `meta`
    cuando exista) como el resultado real de la llamada — los `services` no
    desestructuran el sobre a mano en cada método.
  - Si `status == "failed"`, el interceptor lanza una excepción tipada
    propia (ej. `ApiException(code: error, message: message)`) en vez de
    dejar pasar una respuesta 2xx con `status: "failed"` como si fuera
    éxito, o dejar que el `DioException` crudo (sin el código de negocio)
    se propague tal cual hasta la UI.

## GraphQL (`core/network/graphql_client.dart`, CU45)

Un servicio que ofrece GraphQL junto a REST (hoy `notificaciones`: avisos y preferencias) se puede consumir por
cualquiera de los dos. REST sigue siendo el valor por defecto de todo lo demás.

- **Un solo cliente**: `GraphqlClient` hace `POST /graphql` con `{ query, variables }` sobre el `ApiClient` de siempre
  (Bearer, renovación de sesión ante un 401 del gateway, modo offline). Ninguna feature arma esa petición a mano.
  `consultar` es para lecturas y `mutar` para cambios; ambas devuelven el contenido de `data`.
- **No usa el sobre estándar**: `ApiClient` deja pasar tal cual lo marcado con `OpcionesGraphql.clave`. Desempaquetarlo
  como un sobre REST se tragaría los errores (GraphQL responde 200 con `errors` y `data: null`).
- **Errores**: `errors[0].extensions.code` llega como `ApiException` con su `ERR_...`, igual que en REST, así que las
  pantallas no cambian. También los de una consulta inválida (400). Un fallo de red se relanza como `DioException`.
- **Documentos**: los textos de las consultas viven en `features/{feature}/services/documentos_graphql.dart` como
  constantes `r'''...'''` y piden **solo los campos que leen los modelos**. Tienen que ser válidos contra el esquema del
  servicio; si se cambia un campo en el backend, se cambia acá.
- **Subclase del servicio de REST**: `NotificacionesGraphqlService extends NotificacionesService` (y
  `PreferenciasGraphqlService`) sobrescriben los mismos métodos. El provider elige uno u otro; las pantallas, los
  providers de datos y los tests de pantalla no se enteran. Los modelos no cambian.
- **Bandera de transporte**: `NOTIFICACIONES_TRANSPORTE` en el `.env` (`graphql` por defecto, `rest` para volver) se lee en
  `transporteNotificacionesProvider`. Sirve de vuelta atrás sin recompilar.
- **Offline**: una *consulta* se guarda en la caché del teléfono (clave = ruta + texto + variables) y sin conexión se
  responde con lo último guardado, como un `GET`. Una *mutación* nunca se guarda ni se encola (como hoy las de
  avisos y preferencias). Una respuesta con `errors` no se guarda.
- Solo se pasa a GraphQL lo que lo justifica; no se migra una feature "porque sí".

## Modo offline (`core/offline/`)

La app sigue siendo usable sin internet, y al volver la conexión el **servidor valida todo**: nada se da por
bueno solo porque se hizo sin red. Lo resuelve un interceptor de `ApiClient` (`InterceptorOffline`) más el
`NucleoOffline` (caché, cola y sincronización); las pantallas casi no lo notan.

- **Lecturas (`GET` JSON)**: toda respuesta correcta se guarda en el teléfono (por persona, clave = URL con los
  parámetros ordenados). Si la petición no puede salir (`connectionError`, tiempos agotados), se responde con lo
  último guardado y la respuesta lleva `extra['desdeCache'] = true`. Sin nada guardado, el error de red llega a la
  pantalla como siempre. No se guardan `/auth/*` ni descargas de bytes; una lectura que nunca debe salir de la caché
  usa `OpcionesOffline.sinCache()`.
- **Escrituras: opt-in por `service`.** Solo se encolan las que el `service` marca con
  `options: OpcionesOffline.escritura('Texto para la persona')` **y** que se pueden validar después en el
  servidor. Hoy son todas las de **clases**: reservar (CU14), cancelar y reprogramar (CU15), cancelar como
  instructor (CU16), cerrar y corregir una clase (CU17) y evaluarla (CU18). Las de **pagos** (comprar horas,
  reembolsos) no se marcan. Sin red (la petición no salió: `connectionError` / `connectionTimeout`,
  nunca un `receiveTimeout`, porque ahí no se sabe si el servidor la recibió) la petición se guarda en la cola y
  el interceptor rechaza con `AccionEncolada`. El `service` la relanza (`_llamar`) y **la pantalla debe
  atraparla** y decir «guardado, se enviará al volver internet» en vez de mostrar un error. Una acción que paga,
  mueve dinero o necesita la respuesta del servidor para seguir (comprar horas, pagar) **no** se marca: exige internet.
- **Al volver la conexión** (aviso de `connectivity_plus` + sondeo `HEAD` al gateway cada 15 s mientras esté caída),
  el núcleo envía la cola **de a una y en orden**. Éxito: sale de la cola. Rechazo del servidor (4xx con
  respuesta): se **descarta** y queda como `AccionRechazada` con el mensaje del servidor; `AvisoOffline` (arriba de
respuesta): se **descarta** y queda como `AccionRechazada` con el mensaje del servidor. Las que el servidor
  acepta quedan como `AccionCompletada`. `AvisoOffline` (arriba de todos los shells) avisa **sin conexión** que
  lo que se haga se enviará al volver internet, y **al volver** cuenta cuántas operaciones se cumplieron y cuántas
  no (la hoja «Ver» las lista con el motivo del servidor) hasta que la persona pulsa «Entendido».
- **Actualización optimista**: una escritura encolada declara su `efecto` y los `objetivos` (códigos de la reserva,
  la franja o la clase que toca) y, si hace falta dibujarla, `datos` (`OpcionesOffline.escritura(..., efecto:,
  objetivos:, datos:)`). Las pantallas leen `offlineProvider` y muestran la clase como «pendiente» (insignia
  «Cancelación pendiente», «Cierre pendiente»...), apagan los botones que chocarían y dibujan una reserva hecha sin
  conexión como tarjeta provisional en «Mis clases» (`features/clases/widgets/pendientes_offline.dart`). No se
  modifica ningún dato: es solo cómo se ve. Si el servidor rechaza la acción, sale de la cola y la pantalla vuelve
  sola a lo real; si la acepta, las listas se vuelven a pedir.
- **Rechazo parcial**: reservar varias franjas responde 200 aunque el servidor rechace alguna. Una escritura
  con `tipo` (`OpcionesOffline.escritura(..., tipo: ...)`) tiene un verificador registrado en
  `NucleoOffline.verificadores` que revisa la respuesta y avisa cada parte no cumplida (`verificarReservaOffline`). 401, 429, 5xx o sin red: se conserva y se
  reintenta. Después se refrescan (`invalidate`) todos los providers de datos (`AsyncValue`) que están vivos.
- **Arranque sin internet**: con una sesión guardada, `SesionNotifier.restaurar` entra con la persona guardada
  y **sin access token**, y reintenta renovar cada 30 s; con el token renovado se sincroniza. Un rechazo real del
  backend (ERR_IDE_05..09) sigue cerrando la sesión.
- **Datos locales por persona** (`offline/u_{codigoUsuario}/` en la carpeta privada de la app): cerrar sesión a
  propósito los borra; perder la sesión por el servidor, no (los pendientes se conservan).
- Idempotencia: el backend no tiene claves de idempotencia. Si una acción llegó al servidor pero la respuesta se
  perdió, el reenvío será rechazado por el servidor (ya existe / ya cerrada) y figurará como rechazada: es
  inofensivo, pero por eso solo se encolan acciones que el servidor rechaza al repetirse.

## Servicios por feature

Los servicios que llaman a la API viven en `services/` dentro de cada
feature, siguiendo el scaffold ya creado (`lib/features/auth/services/`):

```
lib/features/usuarios/
├── services/
│   └── usuarios_service.dart   # usa el ApiClient de core/network/
├── providers/
└── screens/
```

Un `service` recibe el `ApiClient` compartido (no crea su propia instancia
de `Dio`), y solo conoce endpoints de **su** feature.

## Modelos de datos (DTOs)

Las clases que representan el JSON de la API viven en `models/` dentro de
la feature (`lib/features/{feature}/models/`):

- Una clase por DTO, nombre `{Recurso}Request` para lo que se envía y
  `{Recurso}` (o `{Recurso}Response` si hace falta distinguir) para lo que
  se recibe.
- Clases inmutables: campos `final` y constructor `const`. `toJson()` en los
  requests y `factory fromJson(Map<String, dynamic>)` en las respuestas,
  escritos a mano (sin `json_serializable` ni generación de código mientras
  no se decida lo contrario).
- Los campos van en `camelCase`, igual que el JSON del backend.
- Solo el `service` de la feature construye y parsea DTOs; los providers y
  las pantallas reciben objetos ya tipados.
- El parseo se prueba con un test unitario cuando el mapeo no es trivial
  (ver [TESTING_MOBILE.md](TESTING_MOBILE.md)).

### Endpoints compartidos entre features

Mismo criterio que en el frontend web (ver
[TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)): si un
endpoint se consume desde más de una feature, su lógica va en
`lib/core/network/` (no en `features/{feature}/services/` de ninguna de las
dos, y no duplicado en ambas). Se mueve ahí recién cuando aparece el segundo
consumidor real, no de forma anticipada.
