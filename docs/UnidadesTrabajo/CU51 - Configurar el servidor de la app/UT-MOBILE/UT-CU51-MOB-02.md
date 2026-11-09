# UT-CU51-MOB-02 — Pantalla de servidor y prueba de conexión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU51-MOB-02` |
| **Nombre** | Pantalla de servidor y prueba de conexión |
| **Caso de uso** | CU51-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU51-MOB-01 |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (`Form`, `validator`, mensajes en español) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (diseño Cockpit, `AvisoCockpit`).

## Solicitud de negocio

La persona tiene que poder indicar el servidor y comprobar que es el correcto antes de seguir.

## Objetivo

Existe `ServidorScreen`, con el campo, «Probar conexión» y «Guardar y continuar».

## Alcance

- `features/servidor/services/probar_servidor.dart`: `probarServidor` pide la raíz de la API sin sesión y la clasifica en `ResultadoPrueba` (`servidorDriveHub` si responde con el sobre estándar —`status` y `timestamp`—, `respondeOtraCosa`, `sinRespuesta`). No lanza.
- `probadorServidorProvider`, para sustituir la prueba en los tests.
- `features/servidor/screens/servidor_screen.dart`: campo `campoServidor` con la dirección sugerida (sin `/api/v1`), «Probar conexión» (`botonProbarServidor`) con el resultado en un `AvisoCockpit`, «Guardar y continuar» (`botonGuardarServidor`, va a `/login`) y, si ya había servidor, «Cancelar» (`botonCancelarServidor`). Editar la dirección borra el resultado anterior.
- Tests: `probar_servidor_test.dart` y `servidor_screen_test.dart` (grupo «probar la conexión»).

**Archivos esperados (crear/tocar):** `probar_servidor.dart`, `servidor_provider.dart` (el probador), `servidor_screen.dart` y los dos tests.

## Fuera de alcance (no tocar)

- La ruta y el redirect (UT 03).
- Código QR o escaneo de la red.

## Resultado esperado

Se puede escribir, probar y guardar una dirección; guardar no depende de que la prueba salga bien.

## Validación

1. `flutter test test/features/servidor/probar_servidor_test.dart test/features/servidor/servidor_screen_test.dart` pasa.
2. Con la prueba en «no responde», el botón guardar igual guarda.
3. `flutter analyze` sin hallazgos.
