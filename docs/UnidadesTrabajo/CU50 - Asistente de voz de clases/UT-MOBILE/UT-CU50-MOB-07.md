# UT-CU50-MOB-07 — Controlador del asistente (estados y errores)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-07` |
| **Nombre** | Controlador del asistente (estados y errores) |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-03, UT-CU50-MOB-04, UT-CU50-MOB-05, UT-CU50-MOB-06 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

Lleva el flujo completo: escuchar, entender, ejecutar o pedir confirmación, y explicar con claridad cuando algo no salió.

## Objetivo

Un `AsistenteNotifier` que encadena voz, modelo y ejecutores y expone un estado que la pantalla dibuja.

## Alcance

- `features/asistente/providers/asistente_provider.dart`: estado sellado (`Inactivo`, `Escuchando`, `Entendiendo(texto)`, `Resultado(mensaje)`, `PideConfirmacion(propuesta)`, `Elegir(candidatas)`, `NoEntendi(motivo)`, `Fallo(mensaje)`) y las operaciones `empezarAEscuchar`, `terminarDeEscuchar`, `elegir(candidata)` y `cerrar`.
- Orden: texto → `entender` → según la acción: consulta → ejecutor de consultas (MOB-03); cambio → preparador (MOB-04) y `PideConfirmacion` (con la clase identificada; la confirmación real es el diálogo o la hoja de `clases`); `fuera_de_alcance` → `NoEntendi`. El asistente **nunca** cambia una clase por su cuenta: solo la identifica y la pantalla abre la confirmación que ya existe en `clases` (CU15).
- Cubre los flujos A1 a A10 del CU: audio vacío, acción desconocida, dato faltante, día o instructor que no cuadra, varias clases posibles, sin clases, sin horas (ofrece comprar un plan), sin red y regla de CU15. Un error inesperado nunca cierra la app.
- Tests: `test/features/asistente/asistente_test.dart` con voz, modelo y servicios falsos, un caso por flujo y la comprobación de que el controlador nunca llama a cancelar ni a reprogramar.

Archivos esperados (crear/tocar): el provider y el test.

## Fuera de alcance (no tocar)

- Dibujar la interfaz (MOB-08), los permisos y los modelos ausentes (MOB-09).

## Resultado esperado

Con voz y modelo falsos, «cancela mi próxima clase» deja el estado en `PideConfirmacion`, y el controlador no cancela nada: lo hace el diálogo de `clases`.

## Validación

1. `flutter test test/features/asistente/asistente_test.dart` en verde.
2. `flutter analyze` sin avisos nuevos.
