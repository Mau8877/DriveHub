# UT-CU01-MOB-03 — Reglas de validación del registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-03` |
| **Nombre** | Reglas de validación del registro |
| **Caso de uso** | CU01-MOB |
| **Aplicación** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (validación nativa con `validator`, mensajes en español, mismas reglas que la web) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

Las reglas de cada campo deben ser las mismas que en la web y el backend, y estar en un solo archivo fácil de probar.

## Objetivo

Existen funciones `validator` puras para cada campo del registro, con tests.

## Alcance

- `lib/features/auth/validators/registro_validators.dart` con constantes (`edadMinimaRegistro = 18`, `passwordMin = 9`, `passwordMax = 64`, máximos 80, 80, 120, 20) y funciones que devuelven `String?` (mensaje en español o `null`), compatibles con `TextFormField.validator`:
  - `validarNombre`, `validarApellido`: obligatorio, máx. 80.
  - `validarEmail`: obligatorio, formato válido, máx. 120.
  - `validarPassword`: obligatoria, 9 a 64 caracteres.
  - `validarTelefono`: opcional, máx. 20.
  - `validarCodigoEscuela`: obligatorio.
  - `validarFechaNacimiento(DateTime? fecha, {DateTime? hoy})`: obligatoria, no futura, con al menos 18 años cumplidos a la fecha de `hoy` (el día en que cumple 18 es válido).
- Los mensajes de error son idénticos a los de la web (ver [UT-CU01-FE-01](../UT-FRONTEND/UT-CU01-FE-01.md)).
- Tests (`test/features/auth/registro_validators_test.dart`): 17 años y 364 días inválido; exactamente 18 años válido; fecha futura inválida; contraseña de 8 inválida, de 9 válida, de 65 inválida; correo sin `@` inválido; teléfono vacío válido.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/validators/registro_validators.dart`
- `projects/mobile/test/features/auth/registro_validators_test.dart`

## Fuera de alcance (no tocar)

- Widgets, pantalla y providers.
- Librerías externas de formularios (no se agregan).
- Validación de formato del teléfono más allá del largo.

## Resultado esperado

Las reglas del registro móvil existen, probadas, con los mismos criterios que web y backend.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
