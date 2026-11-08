---
globs: projects/mobile/**/*
---

# Arquitectura Mobile

**Feature-based**, mismo criterio conceptual que el frontend web (ver
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md)): el código se
organiza por funcionalidad de negocio (`features/{feature}/`), y lo
transversal a toda la app vive en `core/`.

## Estructura de `lib/`

Ya scaffoldeada — la estructura real que existe hoy en el repo (`core/network`
y `core/theme` ya implementados; `core/routing` todavía vacío; `layouts/`
y `features/health/` como ejemplo de referencia real; `features/auth/`
como ejemplo de referencia vacío):

```
lib/
├── core/
│   ├── network/     # ApiClient (Dio + interceptor de sobre), ya implementado (ver CONSUMO_API_MOBILE.md)
│   ├── offline/     # Modo sin internet: caché de lecturas, cola de acciones, sincronización (ver CONSUMO_API_MOBILE.md)
│   ├── routing/     # GoRouter único, con el redirect global -- todavía vacío (ver NAVEGACION_MOBILE.md)
│   └── theme/       # AppColors/AppTheme/CockpitColores (diseño Cockpit, espejo de design-tokens) y temaProvider (apariencia)
├── layouts/
│   ├── inicio_por_rol.dart  # Elige el shell de /inicio según los roles de la sesión
│   ├── shared/      # ShellConBarra: AppBar + barra inferior común a los shells por rol
│   └── {rol}/       # Shell por rol (EstudianteShell, InstructorShell; AdminShell sin usar)
├── shared/
│   └── widgets/     # Piezas del diseño Cockpit y widgets de 2+ features (ver WIDGETS_UI_MOBILE.md)
├── features/
│   ├── inicio/      # Pestaña Inicio del shell (saludo, tarjeta de próxima clase, accesos): estado vacío hasta sus casos de uso
│   ├── clases/      # Pestaña Clases/Agenda del shell: estado vacío hasta los casos de uso de Académico
│   ├── offline/     # Puente con Riverpod del modo offline (providers/) y `AvisoOffline` (widgets/); el motor está en core/offline/
│   ├── push/        # Avisos push de Firebase (CU36): registro del teléfono, aviso en primer plano
│   ├── servidor/    # Pantalla de primera vez para elegir el backend (CU51): validación, almacenamiento, prueba de conexión
│   ├── health/      # Feature de diagnóstico end-to-end (service+provider+screen) contra el backend real
│   └── {feature}/   # Un feature de negocio (ver estructura abajo)
└── main.dart         # Entry point, monta MaterialApp + ProviderScope (sin GoRouter todavía)
```

`core/` es la única carpeta de nivel superior con código transversal real
además de `features/` — no hay una carpeta `common/`/`utils/` genérica
todavía; si aparece una necesidad concreta de utilidades puramente
transversales (no ligadas a red, routing ni tema), se define esa carpeta
en su momento, no se anticipa vacía.

`features/health/` no es una feature de negocio real — existe para probar
de punta a punta que `ApiClient` (Dio + interceptor) y Riverpod
(provider + `AsyncValue`) funcionan contra el backend real antes de que
exista la primera feature real. Se borra o se reemplaza el día que haya un
mejor lugar para un chequeo de conectividad, mismo criterio que
`features/home/` en el frontend web.

`layouts/{rol}/` sigue el mismo patrón que
[ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md#estructura-de-src) del
lado web: un shell delgado por rol (`EstudianteShell`, `InstructorShell`)
que no conoce lógica de negocio, solo declara las secciones de la barra
inferior de ese rol (máximo 5) sobre el `ShellConBarra` común. `/inicio`
monta `InicioPorRol`, que elige el shell según los roles de la sesión.
Una sección nueva se agrega al shell del rol con el primer caso de uso que
la necesite; mientras no tenga pantalla muestra un cuerpo de prueba.
`AdminShell` sigue sin conectarse: la app móvil no es para administradores.

## Estructura de un feature (`lib/features/{feature}/`)

Ya scaffoldeada como ejemplo de referencia en `lib/features/auth/` (con
los DTOs, validadores, servicio, provider y pantallas del registro, CU01) — cualquier feature nueva sigue
esta misma forma:

```
lib/features/usuarios/
├── models/       # DTOs Dart del JSON de la API (request/response) de ESTE feature
├── providers/    # Riverpod: AsyncNotifier/FutureProvider (datos) + *UiProvider (estado de UI)
├── screens/      # Pantallas completas que apunta el GoRouter de core/routing/
├── services/     # Llamadas a la API de ESTE feature, vía el ApiClient de core/network/
└── widgets/      # Widgets específicos de esta feature -- se agrega recién cuando hace falta uno
```

Referencia de cada subcarpeta, en el resto de las rules:
[ESTADO_GLOBAL_MOBILE.md](mobile/ESTADO_GLOBAL_MOBILE.md) (`providers/`,
convención de nombres), [CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md)
(`services/`), [NAVEGACION_MOBILE.md](mobile/NAVEGACION_MOBILE.md) (cómo
`screens/` se conecta con el router central), [WIDGETS_UI_MOBILE.md](mobile/WIDGETS_UI_MOBILE.md)
(`widgets/`, y cuándo algo pasa a `shared/widgets/`).

> **Modelos de datos (DTOs Dart):** viven en `models/` dentro de cada
> feature (`features/{feature}/models/`), con las reglas de
> [CONSUMO_API_MOBILE.md](mobile/CONSUMO_API_MOBILE.md#modelos-de-datos-dtos).

## IA en el dispositivo (`ml/` y `assets/ml/`)

**Requisito del proyecto: la IA de la app móvil corre en el teléfono**, no en un servidor (ni local ni
alojado). Por eso el análisis de la foto del tablero (CU40) y la validación de la prueba de conducción (CU39)
se hacen dentro de la app, sin internet; el backend (`asistente-ia`) solo guarda los resultados.

- `projects/mobile/ml/`: **fuera de `lib/`**, el entrenamiento del modelo (Python/Keras, `entrenar_*.py`), su
  `requirements-ml.txt` y un `README.md` con las fotos, licencias y pasos. Las fotos de entrenamiento
  (`ml/datos/`) no se versionan.
- `projects/mobile/assets/ml/`: lo que se empaqueta en la app: el `.tflite`, sus etiquetas y los catálogos JSON
  (`luces_tablero.json`). El conocimiento de dominio (textos, gravedad) va en el catálogo, no en el modelo ni en
  el código.
- En la feature, el modelo va detrás de una **interfaz** (`ClasificadorTablero`) con la implementación real
  (`tflite_flutter`) y una falsa para los tests: `flutter test` nunca carga un modelo ni abre la cámara.
  La cámara/galería también va detrás de un provider (`selectorDeFotoProvider`).
- El preprocesado pesado (decodificar fotos de varios MB) corre en otro hilo (`Isolate.run`).
- **Asistente de voz de clases (CU50, `features/asistente/`):** dos modelos en el teléfono, cada uno detrás de una interfaz (`ModeloDeAcciones`, `ReconocedorDeVoz`) con una implementación falsa para los tests: el `.litertlm` de FunctionGemma (`flutter_litert_lm`) elige la acción y Whisper tiny (`sherpa_onnx` + `record`) convierte la voz en texto. Los archivos pesan ~284 MB y ~40 MB, no se versionan y se buscan en `<documentos>/ia/` (`acciones.litertlm` y `whisper/`); en desarrollo se copian con `adb push`. El audio nunca se guarda. El modelo solo elige una acción de un catálogo cerrado (`assets/ml/acciones_clases.json`, copia de `ml/acciones/acciones_pantallas.json`) y la app valida y confirma antes de cambiar nada.
- Solo Android por ahora. `tflite_flutter` necesita `kotlin.jvm.target.validation.mode=warning` en
  `android/gradle.properties` (su Java está en 11 y Kotlin toma el JDK).

## Avisos push (`features/push/`, CU36)

El backend (`notificaciones`) manda el push con Firebase Cloud Messaging; la app registra su teléfono para recibirlo.

- **Configuración**: `android/app/google-services.json` (configuración pública del cliente, va al repositorio; el proyecto es el
  mismo del panel web). El `applicationId` (`com.drivehub.app`) tiene que coincidir con su `package_name`. Cambiarlo hace que
  Android trate la app como otra distinta: no hereda sus datos (por ejemplo, los modelos de IA de la carpeta privada) y hay que
  registrarla de nuevo en la consola de Firebase. La clave de la cuenta de servicio del backend NO va en la app ni en el repositorio.
- **Registrar**: al haber sesión (`conectarPush` escucha la sesión), `RegistroDePush.activar` pide el permiso de notificaciones (una
  vez por ejecución; si se niega, no se insiste), obtiene el token y lo registra (`registrarDispositivo`, por GraphQL o REST según
  `NOTIFICACIONES_TRANSPORTE`). Una vez por persona; un token nuevo de Firebase se registra solo.
- **Quitar**: cerrar sesión lo quita ANTES de perder el token (gancho `antesDeCerrarSesion`, máximo 5 s; un fallo no impide salir).
- **Nunca bloquea**: sin permiso, sin servicios de Google, sin red o sin token de sesión, el push simplemente no se activa. Sin errores
  a la vista; un registro que falló se reintenta en el siguiente cambio de sesión.
- **Con la app abierta** Android no muestra el aviso del sistema: se refrescan el contador y la bandeja y se muestra un aviso breve
  (`mensajeroGlobal`). Con la app cerrada o en segundo plano lo muestra el sistema solo (el backend manda `notification` con título y mensaje).
- **Firebase detrás de una interfaz** (`MensajeriaPush`): los tests no tocan Firebase. Se inicia la primera vez que se usa.
- `getToken` y `onTokenRefresh` figuran como obsoletos en `firebase_messaging` 17 y se mantienen a propósito: el backend manda a
  tokens de registro de FCM; el reemplazo entrega un ID de instalación para «envío directo».
- El token de Firebase nunca se guarda ni se escribe en logs: solo se guarda el código del dispositivo que devuelve el servicio.

## Regla de dependencia

`features/{feature}/` puede depender de `core/` y `shared/` — nunca al
revés (`core/`/`shared/` no importan nada de `features/`). Una feature no
depende de otra feature directo salvo que la relación de negocio lo
justifique explícitamente, mismo criterio que
[ARQUITECTURA_BACKEND.md](ARQUITECTURA_BACKEND.md#regla-de-dependencia).
