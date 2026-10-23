# UT-CU12-BE-04 — `GeneradorFranjas`, `Clock` y configuración de horarios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-04` |
| **Nombre** | `GeneradorFranjas`, `Clock` y configuración de horarios |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (`record` para datos inmutables), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer las [decisiones de diseño 6 y 7](./README.md#decisiones-de-diseño).

## Solicitud de negocio

Del horario semanal del instructor (por ejemplo, "lunes de 08:00 a 12:00 en clases de una hora") tienen que salir las franjas concretas que se pueden reservar, sin generar horas que ya pasaron.

## Objetivo

Existe la lógica pura que calcula qué franjas corresponden a un horario en un rango de fechas, y la configuración (zona horaria, horizonte, reloj) que la alimenta.

## Alcance

- `features/horarios/service/GeneradorFranjas`: clase sin estado ni dependencias de Spring ni de Mongo, con un método:

  ```java
  List<FranjaCalculada> calcular(int diaSemana, LocalTime horaInicio, LocalTime horaFin, int duracionClaseMin,
                                 LocalDate vigenteDesde, LocalDate vigenteHasta /* nullable */,
                                 LocalDateTime ahora, int horizonteDias)
  ```

  y `record FranjaCalculada(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin)` (en el mismo paquete).
- Reglas del cálculo:
  - Fechas candidatas: desde `max(vigenteDesde, hoy)` hasta `min(vigenteHasta, hoy + horizonteDias − 1)`, donde `hoy = ahora.toLocalDate()` (la ventana de `horizonteDias` cuenta el día de hoy); `vigenteHasta` nulo = sin tope propio. Solo las fechas cuyo día de la semana coincide con `diaSemana` (1 = lunes a 7 = domingo, `DayOfWeek.getValue()`).
  - Por cada fecha, bloques consecutivos de `duracionClaseMin` desde `horaInicio` hasta `horaFin`.
  - Se descartan los bloques cuyo inicio **no es posterior** a `ahora` (un bloque que empieza exactamente ahora ya no se genera).
  - Orden de salida: por fecha y luego por hora.
  - No valida el horario (eso es de la UT 07): asume ventana múltiplo de la duración y `horaFin` posterior a `horaInicio`.
- `config/HorariosProperties`: `@ConfigurationProperties(prefix = "app.horarios")` como `record` con `ZoneId zonaHoraria` (default `America/La_Paz`), `int horizonteDias` (default `28`), `String extensionCron` (default `0 0 2 * * *`) y `boolean jobHabilitado` (default `true`). Registrado con `@EnableConfigurationProperties` o `@ConfigurationPropertiesScan`.
- `config/ClockConfig`: bean `Clock` (`Clock.system(zonaHoraria)`) para poder fijar la hora en los tests.
- `application.properties`: agregar el bloque `app.horarios.*` con sus defaults y un comentario sobre la zona horaria.
- Tests unitarios de `GeneradorFranjas` (hora fija, sin Spring): lunes 08:00–12:00 a 60 min en 28 días → 16 franjas, cuatro por lunes (08, 09, 10, 11); ventana de un solo bloque; el día de hoy a media mañana omite los bloques ya iniciados y el que empieza justo ahora; `vigenteDesde` futuro: no genera antes; `vigenteHasta` dentro de la ventana: corta ahí; `vigenteHasta` anterior a hoy: lista vacía; cada uno de los 7 días de la semana produce exactamente 4 fechas en la ventana de 28 días. Una duración que no divide la ventana queda fuera del contrato y no se prueba (la UT 07 la rechaza antes).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/GeneradorFranjas.java`, `FranjaCalculada.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/HorariosProperties.java`, `ClockConfig.java`
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/GeneradorFranjasTest.java`

## Fuera de alcance (no tocar)

- Guardar franjas en la base (UT 07 y 10) ni consultar horarios (UT 05).
- Validar el horario y lanzar errores de negocio (UT 07).
- La tarea programada y `@EnableScheduling` (UT 10).

## Resultado esperado

Dado un horario y la hora actual, `GeneradorFranjas` devuelve exactamente las franjas que hay que crear, y la zona horaria y el horizonte se leen de la configuración.

## Validación

1. `./mvnw test` en `academico` pasa con `GeneradorFranjasTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
