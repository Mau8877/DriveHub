# UT-CU50-MOB-02 — Resolver día, momento, instructor y hora

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-02` |
| **Nombre** | Resolver día, momento, instructor y hora |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-01 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Revisar `features/clases/models/momento_dia.dart` y `features/horarios/models/fechas.dart`.

## Solicitud de negocio

La persona dice «el viernes», «por la mañana» o «la profe Lucía»; la pantalla trabaja con fechas, un momento del día y el código de un instructor reales. Un dato que no cuadra no se inventa.

## Objetivo

Un resolvedor puro que traduce lo dicho a los valores de los filtros de la pantalla, o a «no coincide».

## Alcance

- `features/asistente/services/resolvedor_de_datos.dart`, sin Riverpod ni red, con la fecha de hoy y las listas por parámetro.
- `dia`: `hoy`, `manana`, `pasado_manana` y los siete días de la semana → el primero que llega, hoy incluido si es ese día. `momento` → `MomentoDia` (mañana, tarde, noche). La `hora` no se resuelve: el catálogo ya la entrega en `HH:mm` y MOB-01 la valida. `instructor` → el `InstructorDisponible` cuyo nombre coincide, sin distinguir tildes ni mayúsculas y por nombre parcial; ninguno o varios → `SinCoincidencia` / `Ambiguo` con las opciones.
- Tests: `test/features/asistente/resolvedor_de_datos_test.dart` con una fecha fija (de martes a viernes, el mismo día, cambio de semana, mes y año, tilde, tratamiento, nombre parcial, ambiguo).

Archivos esperados (crear/tocar): el resolvedor y su test.

## Fuera de alcance (no tocar)

- Leer el reloj o las listas desde providers (lo hace MOB-03) o llamar a la red. Un resolvedor puro se prueba sin montar nada.

## Resultado esperado

«el viernes» da la fecha del próximo viernes; «la profe Lucia» da el instructor Lucía; un nombre que no existe da `SinCoincidencia` con las opciones.

## Validación

1. `flutter test test/features/asistente/resolvedor_de_datos_test.dart` en verde.
2. `flutter analyze` sin avisos nuevos.
