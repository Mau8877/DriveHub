# UT-CU50-MOB-01 — Acciones del asistente y lectura de la salida del modelo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-01` |
| **Nombre** | Acciones del asistente y lectura de la salida del modelo |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Ver también el catálogo `projects/mobile/ml/acciones/acciones_pantallas.json`.

## Solicitud de negocio

El modelo devuelve una llamada a función; la app necesita una acción segura y tipada, o nada, para no ejecutar algo que el modelo se inventó.

## Objetivo

Existe `AccionAsistente` y una función que convierte la salida del modelo en una acción válida o en un rechazo explícito.

## Alcance

- `features/asistente/models/`: `NombreAccion` (enum con las 9 acciones del catálogo y su nombre en el JSON), `AccionAsistente` (acción + `dia`, `momento`, `instructor`, `hora` crudos tal como los dijo la persona; inmutable, `const`) y `ResultadoInterpretacion` (acción, o rechazo con motivo: `desconocida`, `faltaDato`, `fueraDeAlcance`).
- `interpretar(String nombre, Map<String, dynamic> argumentos)`: rechaza (no ignora) un nombre que no está en el catálogo y cualquier argumento con clave inexistente para esa función, que no sea texto o con un valor fuera de los permitidos (un argumento perdido ejecutaría algo distinto de lo pedido); valida que `hora` sea `HH:mm`; exige los datos obligatorios (`hora` en `seleccionar_horario`); `fuera_de_alcance` es un resultado, no una acción ejecutable.
- `assets/ml/acciones_clases.json` (copia de la pantalla `clases` del catálogo de entrenamiento) y su carga tipada (`CatalogoAcciones`) con las descripciones y el esquema de parámetros que se le pasan al modelo. `assets/ml/` ya está declarado en `pubspec.yaml`.
- Tests: `test/features/asistente/interpretar_test.dart` (cada acción válida, nombre inventado, argumento fuera del enum, falta `hora`) y un test que compara los nombres del asset con el enum.

Archivos esperados (crear/tocar): `lib/features/asistente/models/*.dart`, `assets/ml/acciones_clases.json` y el test.

## Fuera de alcance (no tocar)

- Llamar al modelo (MOB-05), resolver el día o el instructor contra listas (MOB-02) y ejecutar nada en la pantalla (MOB-03 y MOB-04).

## Resultado esperado

Una salida del modelo como `ver_mis_clases {dia: viernes}` se convierte en una acción tipada; `reservar_todo` o un argumento inventado se rechaza.

## Validación

1. `flutter test test/features/asistente/interpretar_test.dart` en verde.
2. `flutter analyze` sin avisos nuevos.
3. Cambiar un nombre del asset a mano: el test de alineación falla.
